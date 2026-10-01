package com.example.data.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportManager {

    fun copyToClipboard(context: Context, text: String, label: String = "Official Letter") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Letter via"))
    }

    /**
     * Writes formatted PDF directly to any OutputStream (e.g. FileOutputStream or SAF Uri OutputStream)
     */
    fun writePdfToStream(title: String, content: String, outputStream: OutputStream) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points: 595 x 842
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val normalPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate dark
            textSize = 13f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val margin = 50f
        val contentWidth = 595f - (margin * 2)
        var yPos = 60f

        // Draw letterhead title
        canvas.drawText(title.take(60), margin, yPos, headerPaint)
        yPos += 20f

        normalPaint.color = Color.LTGRAY
        canvas.drawLine(margin, yPos, margin + contentWidth, yPos, normalPaint)
        yPos += 25f
        normalPaint.color = Color.BLACK

        val lines = content.lines()
        var pageNumber = 1

        for (rawLine in lines) {
            val isIndented = rawLine.startsWith("    ") || rawLine.startsWith("\t")
            val currentMargin = if (isIndented) margin + 20f else margin
            val currentContentWidth = if (isIndented) contentWidth - 20f else contentWidth

            val words = rawLine.trimStart().split(" ")
            var currentLine = ""

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val measure = normalPaint.measureText(testLine)

                if (measure > currentContentWidth) {
                    if (yPos > 780f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        val nextPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdfDocument.startPage(nextPageInfo)
                        canvas = page.canvas
                        yPos = 60f
                    }
                    canvas.drawText(currentLine, currentMargin, yPos, normalPaint)
                    yPos += 16f
                    currentLine = word
                } else {
                    currentLine = testLine
                }
            }

            if (currentLine.isNotEmpty()) {
                if (yPos > 780f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    val nextPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(nextPageInfo)
                    canvas = page.canvas
                    yPos = 60f
                }
                canvas.drawText(currentLine, currentMargin, yPos, normalPaint)
                yPos += 16f
            }

            if (rawLine.isBlank()) {
                yPos += 8f
            }
        }

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    fun exportToPdf(context: Context, title: String, content: String): File {
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
        val file = File(context.cacheDir, "${cleanTitle}_Letter.pdf")
        FileOutputStream(file).use { out ->
            writePdfToStream(title, content, out)
        }
        return file
    }

    /**
     * Converts editor content into genuine OpenXML .docx with bold, italic, underline, and indent formatting.
     */
    fun writeDocxToStream(title: String, content: String, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            val contentTypes = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            zos.write(contentTypes)
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            val rootRels = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            zos.write(rootRels)
            zos.closeEntry()

            // 3. word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            val paragraphsXml = StringBuilder()

            // Title header paragraph
            paragraphsXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"28\"/></w:rPr><w:t>")
            paragraphsXml.append(escapeXml(title))
            paragraphsXml.append("</w:t></w:r></w:p>")

            // Body paragraphs
            for (line in content.lines()) {
                val isIndented = line.startsWith("    ") || line.startsWith("\t")
                val trimmed = line.trimStart()

                paragraphsXml.append("<w:p>")
                if (isIndented) {
                    paragraphsXml.append("<w:pPr><w:ind w:left=\"720\"/></w:pPr>")
                }

                // Parse inline formatting: **bold**, *italic*, __underline__
                formatLineToWordRuns(paragraphsXml, trimmed)

                paragraphsXml.append("</w:p>")
            }

            val docXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    $paragraphsXml
                    <w:sectPr>
                      <w:pgSz w:w="11906" w:h="16838"/>
                      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
                    </w:sectPr>
                  </w:body>
                </w:document>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            zos.write(docXml)
            zos.closeEntry()
        }
    }

    private fun formatLineToWordRuns(sb: StringBuilder, line: String) {
        if (line.isEmpty()) {
            sb.append("<w:r><w:t xml:space=\"preserve\"></w:t></w:r>")
            return
        }

        val pattern = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|__.*?__)")
        var lastIdx = 0
        for (match in pattern.findAll(line)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > lastIdx) {
                val plain = line.substring(lastIdx, start)
                sb.append("<w:r><w:rPr><w:sz w:val=\"24\"/></w:rPr><w:t xml:space=\"preserve\">")
                sb.append(escapeXml(plain))
                sb.append("</w:t></w:r>")
            }
            val matchText = match.value
            when {
                matchText.startsWith("**") && matchText.endsWith("**") && matchText.length >= 4 -> {
                    val inner = matchText.substring(2, matchText.length - 2)
                    sb.append("<w:r><w:rPr><w:b/><w:sz w:val=\"24\"/></w:rPr><w:t xml:space=\"preserve\">")
                    sb.append(escapeXml(inner))
                    sb.append("</w:t></w:r>")
                }
                matchText.startsWith("__") && matchText.endsWith("__") && matchText.length >= 4 -> {
                    val inner = matchText.substring(2, matchText.length - 2)
                    sb.append("<w:r><w:rPr><w:u w:val=\"single\"/><w:sz w:val=\"24\"/></w:rPr><w:t xml:space=\"preserve\">")
                    sb.append(escapeXml(inner))
                    sb.append("</w:t></w:r>")
                }
                matchText.startsWith("*") && matchText.endsWith("*") && matchText.length >= 2 -> {
                    val inner = matchText.substring(1, matchText.length - 1)
                    sb.append("<w:r><w:rPr><w:i/><w:sz w:val=\"24\"/></w:rPr><w:t xml:space=\"preserve\">")
                    sb.append(escapeXml(inner))
                    sb.append("</w:t></w:r>")
                }
                else -> {
                    sb.append("<w:r><w:rPr><w:sz w:val=\"24\"/></w:rPr><w:t xml:space=\"preserve\">")
                    sb.append(escapeXml(matchText))
                    sb.append("</w:t></w:r>")
                }
            }
            lastIdx = end
        }

        if (lastIdx < line.length) {
            val rest = line.substring(lastIdx)
            sb.append("<w:r><w:rPr><w:sz w:val=\"24\"/></w:rPr><w:t xml:space=\"preserve\">")
            sb.append(escapeXml(rest))
            sb.append("</w:t></w:r>")
        }
    }

    fun exportToDocx(context: Context, title: String, content: String): File {
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
        val file = File(context.cacheDir, "${cleanTitle}_Document.docx")
        FileOutputStream(file).use { out ->
            writeDocxToStream(title, content, out)
        }
        return file
    }

    fun writeTxtToStream(title: String, content: String, outputStream: OutputStream) {
        val text = "$title\n\n$content"
        outputStream.write(text.toByteArray(Charsets.UTF_8))
    }

    fun exportToTxt(context: Context, title: String, content: String): File {
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
        val file = File(context.cacheDir, "${cleanTitle}_Letter.txt")
        FileOutputStream(file).use { out ->
            writeTxtToStream(title, content, out)
        }
        return file
    }

    /**
     * Saves exported content directly to a destination URI selected via the Android Storage Access Framework (SAF)
     * file save picker (`ActivityResultContracts.CreateDocument`).
     */
    fun saveContentToUri(context: Context, destinationUri: Uri, format: String, title: String, content: String) {
        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
            when (format.lowercase()) {
                "pdf" -> writePdfToStream(title, content, out)
                "docx" -> writeDocxToStream(title, content, out)
                else -> writeTxtToStream(title, content, out)
            }
        }
    }

    /**
     * Launches Android's native share dialog with FileProvider URI for sending via Gmail, Drive, WhatsApp, Print, etc.
     */
    fun shareExportedFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Official Correspondence"))
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
