package com.campussync.app.core.utils

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxWriter {

    fun write(outputStream: OutputStream, rows: List<List<String>>) {
        val zos = ZipOutputStream(outputStream)

        // 1. [Content_Types].xml
        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        zos.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""".toByteArray()
        )
        zos.closeEntry()

        // 2. _rels/.rels
        zos.putNextEntry(ZipEntry("_rels/.rels"))
        zos.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""".toByteArray()
        )
        zos.closeEntry()

        // 3. xl/workbook.xml
        zos.putNextEntry(ZipEntry("xl/workbook.xml"))
        zos.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets></workbook>""".toByteArray()
        )
        zos.closeEntry()

        // 4. xl/_rels/workbook.xml.rels
        zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zos.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""".toByteArray()
        )
        zos.closeEntry()

        // 5. xl/worksheets/sheet1.xml
        zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sheetHeader = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>"""
        zos.write(sheetHeader.toByteArray())

        for ((rowIndex, row) in rows.withIndex()) {
            val r = rowIndex + 1
            zos.write("""<row r="$r">""".toByteArray())
            for ((colIndex, cellValue) in row.withIndex()) {
                val colRef = getColumnReference(colIndex)
                val escapedValue = escapeXml(cellValue)
                zos.write("""<c r="$colRef$r" t="inlineStr"><is><t>$escapedValue</t></is></c>""".toByteArray())
            }
            zos.write("</row>".toByteArray())
        }

        val sheetFooter = """</sheetData></worksheet>"""
        zos.write(sheetFooter.toByteArray())
        zos.closeEntry()

        zos.finish()
        zos.close()
    }

    private fun getColumnReference(colIndex: Int): String {
        var n = colIndex
        var name = ""
        while (n >= 0) {
            name = (('A' + (n % 26)).toChar()) + name
            n = (n / 26) - 1
        }
        return name
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
