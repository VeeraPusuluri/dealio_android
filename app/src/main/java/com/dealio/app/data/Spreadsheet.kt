package com.dealio.app.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Reads a CSV or XLSX file into rows of cells.
 *
 * XLSX is parsed directly rather than with Apache POI: POI pulls in several MB
 * of classes and needs desugaring, which is a steep price for reading a contact
 * list. An .xlsx is a zip of XML, and [android.util.Xml] is already on the
 * platform, so the whole reader costs nothing at build time.
 */
object Spreadsheet {

    /** True when [fileName] looks like a workbook rather than a delimited text file. */
    fun isWorkbook(fileName: String): Boolean = fileName.endsWith(".xlsx", ignoreCase = true)

    fun read(bytes: ByteArray, fileName: String): List<List<String>> =
        if (isWorkbook(fileName)) readXlsx(bytes) else readDelimited(bytes.decodeToString())

    // ── CSV ──────────────────────────────────────────────────────────────────

    /**
     * RFC-4180-ish: honours quoted fields so an address containing a comma
     * ("Gachibowli, Hyderabad") stays one cell, and `""` as an escaped quote.
     * Tab-separated files are handled too — Excel's "Unicode text" export.
     */
    fun readDelimited(text: String): List<List<String>> {
        val body = text.removePrefix("﻿") // Excel writes a BOM
        val delimiter = if (body.lineSequence().first().count { it == '\t' } >
            body.lineSequence().first().count { it == ',' }
        ) '\t' else ','

        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        fun endCell() { row.add(cell.toString().trim()); cell.setLength(0) }
        fun endRow() { endCell(); if (row.any { it.isNotEmpty() }) rows.add(row.toList()); row.clear() }

        while (i < body.length) {
            val c = body[i]
            when {
                quoted && c == '"' && i + 1 < body.length && body[i + 1] == '"' -> { cell.append('"'); i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == delimiter -> endCell()
                !quoted && (c == '\n') -> endRow()
                !quoted && c == '\r' -> Unit
                else -> cell.append(c)
            }
            i++
        }
        endRow()
        return rows
    }

    // ── XLSX ─────────────────────────────────────────────────────────────────

    private fun readXlsx(bytes: ByteArray): List<List<String>> {
        var sharedXml: ByteArray? = null
        var sheetXml: ByteArray? = null
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                when {
                    entry.name == "xl/sharedStrings.xml" -> sharedXml = zip.readBytes()
                    // The first worksheet; workbook.xml order isn't needed for a
                    // single-sheet export, which is what people actually upload.
                    sheetXml == null && entry.name.startsWith("xl/worksheets/sheet") &&
                        entry.name.endsWith(".xml") -> sheetXml = zip.readBytes()
                }
            }
        }
        val sheet = sheetXml ?: return emptyList()
        return parseSheet(sheet, sharedXml?.let(::parseSharedStrings) ?: emptyList())
    }

    /** `<si><t>Rahul</t></si>` — concatenating the `t` runs handles rich text. */
    private fun parseSharedStrings(xml: ByteArray): List<String> {
        val out = mutableListOf<String>()
        val text = StringBuilder()
        var inItem = false
        var inText = false
        forEachEvent(xml) { p, event ->
            when (event) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> { inItem = true; text.setLength(0) }
                    "t" -> inText = true
                }
                XmlPullParser.TEXT -> if (inItem && inText) text.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> inText = false
                    "si" -> { out.add(text.toString()); inItem = false }
                }
            }
        }
        return out
    }

    private fun parseSheet(xml: ByteArray, shared: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val value = StringBuilder()
        var cellType = ""
        var cellCol = 0
        var inValue = false
        forEachEvent(xml) { p, event ->
            when (event) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> row = mutableListOf()
                    "c" -> {
                        cellType = p.getAttributeValue(null, "t").orEmpty()
                        cellCol = columnIndex(p.getAttributeValue(null, "r").orEmpty())
                        value.setLength(0)
                    }
                    // `v` for normal cells, `t` for the inline-string form.
                    "v", "t" -> inValue = true
                }
                XmlPullParser.TEXT -> if (inValue) value.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "v", "t" -> inValue = false
                    "c" -> {
                        // Blank cells are simply absent from the XML, so pad to the
                        // cell's real column or every later value shifts left.
                        while (row.size < cellCol) row.add("")
                        val raw = value.toString()
                        row.add(if (cellType == "s") shared.getOrElse(raw.toIntOrNull() ?: -1) { "" } else raw)
                    }
                    "row" -> if (row.any { it.isNotBlank() }) rows.add(row.toList())
                }
            }
        }
        return rows
    }

    /** "BC12" -> 54. Letters are base-26, 1-indexed; the result is 0-indexed. */
    private fun columnIndex(ref: String): Int {
        var n = 0
        for (c in ref) {
            if (!c.isLetter()) break
            n = n * 26 + (c.uppercaseChar() - 'A' + 1)
        }
        return (n - 1).coerceAtLeast(0)
    }

    private inline fun forEachEvent(xml: ByteArray, block: (XmlPullParser, Int) -> Unit) {
        val p = Xml.newPullParser()
        p.setInput(ByteArrayInputStream(xml), null)
        var event = p.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            block(p, event)
            event = p.next()
        }
    }
}
