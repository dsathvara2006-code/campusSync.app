package com.campussync.app.core.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class ReceiptData(
    val studentName: String,
    val receiptNumber: String,
    val amount: String,
    val feeType: String,
    val utr: String,
    val date: String,
    val collegeName: String
)

class ReceiptPdfBuilder(private val context: Context) {

    /**
     * Generates a professional PDF receipt and saves it to the Downloads folder.
     * Returns the absolute path or content URI as a String on success.
     */
    suspend fun generateReceiptPdf(data: ReceiptData): Result<String> = withContext(Dispatchers.IO) {
        try {
            // A4 Size in points (1 point = 1/72 inch) -> 595 x 842
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val document = PdfDocument()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawReceiptContent(canvas, data)

            document.finishPage(page)

            // Save the document using Scoped Storage
            val fileName = "Receipt_${data.receiptNumber}.pdf"
            val uriString = savePdfToStorage(document, fileName)
            
            document.close()
            Result.success(uriString)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun drawReceiptContent(canvas: Canvas, data: ReceiptData) {
        // Setup Paints
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A") // Slate 900
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#64748B") // Slate 500
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val boldTextPaint = Paint(textPaint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0") // Slate 200
            strokeWidth = 2f
        }

        // Draw Watermark Stamp ("APPROVED")
        val watermarkPaint = Paint().apply {
            color = Color.parseColor("#D1FAE5") // Very light Emerald Green
            textSize = 100f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            alpha = 80 // Semi-transparent
            textAlign = Paint.Align.CENTER
        }
        canvas.save()
        canvas.rotate(-30f, 297f, 421f) // Rotate from center
        canvas.drawText("APPROVED", 297f, 421f, watermarkPaint)
        canvas.restore()

        // 1. Header
        canvas.drawText(data.collegeName.uppercase(), 297f, 80f, titlePaint)
        canvas.drawText("Official Fee Receipt", 297f, 105f, subtitlePaint)
        canvas.drawLine(50f, 130f, 545f, 130f, linePaint)

        // 2. Receipt Info details
        var startY = 170f
        val leftMargin = 50f
        
        canvas.drawText("Receipt No:", leftMargin, startY, textPaint)
        canvas.drawText(data.receiptNumber, leftMargin + 100f, startY, boldTextPaint)
        
        canvas.drawText("Date:", 400f, startY, textPaint)
        canvas.drawText(data.date, 450f, startY, boldTextPaint)

        startY += 40f
        canvas.drawText("Student Name:", leftMargin, startY, textPaint)
        canvas.drawText(data.studentName, leftMargin + 120f, startY, boldTextPaint)

        // 3. Table Box
        startY += 40f
        val tableTop = startY
        val tableBottom = startY + 150f
        
        val boxPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        // Draw Table Background and Border
        canvas.drawRect(leftMargin, tableTop, 545f, tableBottom, boxPaint)
        canvas.drawRect(leftMargin, tableTop, 545f, tableBottom, borderPaint)
        
        // Table Headers
        startY += 30f
        canvas.drawText("Fee Description", leftMargin + 20f, startY, boldTextPaint)
        canvas.drawText("Amount", 450f, startY, boldTextPaint)
        canvas.drawLine(leftMargin, startY + 15f, 545f, startY + 15f, borderPaint)

        // Table Content
        startY += 50f
        canvas.drawText(data.feeType, leftMargin + 20f, startY, textPaint)
        
        val amountPaint = Paint(boldTextPaint).apply { color = Color.parseColor("#10B981") } // Emerald Green
        canvas.drawText(data.amount, 450f, startY, amountPaint)
        
        // UTR Detail below table
        startY += 80f
        canvas.drawText("Transaction UTR:", leftMargin, startY, textPaint)
        canvas.drawText(data.utr, leftMargin + 140f, startY, boldTextPaint)

        // 4. Footer
        val footerPaint = Paint(subtitlePaint).apply { textSize = 12f }
        canvas.drawText("This is an electronically generated receipt.", 297f, 780f, footerPaint)
        canvas.drawText("CampusSync Inc.", 297f, 800f, footerPaint)
    }

    @Suppress("DEPRECATION")
    private fun savePdfToStorage(document: PdfDocument, fileName: String): String {
        val outputStream: OutputStream?
        val uriString: String

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CampusSync")
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uriString = uri?.toString() ?: ""
            outputStream = uri?.let { resolver.openOutputStream(it) }
        } else {
            // Fallback for older Android versions (App-specific external storage requires no permissions)
            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val campusSyncDir = File(targetDir, "CampusSync")
            if (!campusSyncDir.exists()) campusSyncDir.mkdirs()
            val file = File(campusSyncDir, fileName)
            uriString = file.absolutePath
            outputStream = FileOutputStream(file)
        }

        outputStream?.use {
            document.writeTo(it)
        } ?: throw Exception("Failed to open output stream")

        return uriString
    }
}
