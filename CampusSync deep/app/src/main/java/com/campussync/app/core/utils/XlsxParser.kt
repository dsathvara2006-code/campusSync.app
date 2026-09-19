package com.campussync.app.core.utils

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

object XlsxParser {
    fun parse(inputStream: InputStream): List<List<String>> {
        val zip = ZipInputStream(inputStream)
        var sharedStrings = emptyList<String>()
        var sheetContent = ByteArray(0)
        
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name == "xl/sharedStrings.xml") {
                sharedStrings = parseSharedStrings(zip.readBytes().inputStream())
            } else if (entry.name.startsWith("xl/worksheets/sheet") && sheetContent.isEmpty()) {
                sheetContent = zip.readBytes()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        
        if (sheetContent.isEmpty()) return emptyList()
        
        return parseSheet(sheetContent.inputStream(), sharedStrings)
    }

    private fun parseSharedStrings(inputStream: InputStream): List<String> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        
        val strings = mutableListOf<String>()
        var eventType = parser.eventType
        var isT = false
        var currentText = ""
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        isT = true
                        currentText = ""
                    }
                }
                XmlPullParser.TEXT -> {
                    if (isT) {
                        currentText += parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        isT = false
                        strings.add(currentText)
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }
    
    private fun parseSheet(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        var isV = false
        var isT = false 
        var cellType = ""
        var currentValue = ""
        var cellRef = "" 
        
        fun getColIndex(ref: String): Int {
            val letters = ref.takeWhile { it.isLetter() }
            var col = 0
            for (char in letters) {
                col = col * 26 + (char - 'A' + 1)
            }
            return if (col > 0) col - 1 else 0
        }
        
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableListOf()
                        }
                        "c" -> {
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            cellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentValue = ""
                        }
                        "v" -> {
                            isV = true
                        }
                        "t" -> {
                            isT = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (isV || isT) {
                        currentValue += parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> {
                            isV = false
                        }
                        "t" -> {
                            isT = false
                        }
                        "c" -> {
                            val finalValue = if (cellType == "s" && currentValue.isNotEmpty()) {
                                sharedStrings.getOrNull(currentValue.toIntOrNull() ?: -1) ?: ""
                            } else {
                                currentValue
                            }
                            
                            val expectedColIndex = if (cellRef.isNotEmpty()) getColIndex(cellRef) else currentRow.size
                            while (currentRow.size < expectedColIndex) {
                                currentRow.add("") 
                            }
                            currentRow.add(finalValue.trim())
                        }
                        "row" -> {
                            rows.add(currentRow)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }
}
