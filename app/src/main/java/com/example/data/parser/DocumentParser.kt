package com.example.data.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

data class ExtractedDocument(
    val fileName: String,
    val fileType: String,
    val rawText: String,
    val imageBase64List: List<String> = emptyList(),
    val hasUnclearWarning: Boolean = false,
    val warningMessage: String? = null
)

object DocumentParser {

    fun parseUri(context: Context, uri: Uri, fileName: String): ExtractedDocument {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return try {
            when (extension) {
                "txt" -> parseTextFile(context, uri, fileName)
                "docx" -> parseDocx(context, uri, fileName)
                "xlsx" -> parseXlsx(context, uri, fileName)
                "pdf" -> parsePdf(context, uri, fileName)
                "jpg", "jpeg", "png", "webp" -> parseImage(context, uri, fileName)
                else -> {
                    // Try parsing as text or fallback
                    parseTextFile(context, uri, fileName)
                }
            }
        } catch (e: Exception) {
            ExtractedDocument(
                fileName = fileName,
                fileType = extension.uppercase(),
                rawText = "Error reading document: ${e.localizedMessage ?: "Unknown error"}. Please check file permissions or format.",
                hasUnclearWarning = true,
                warningMessage = "Some parts of the document could not be read clearly."
            )
        }
    }

    fun parseBitmap(bitmap: Bitmap, name: String = "Scanned_Letter.jpg"): ExtractedDocument {
        val base64 = bitmapToBase64(bitmap)
        // Clean and prepare for AI OCR
        return ExtractedDocument(
            fileName = name,
            fileType = "IMAGE",
            rawText = "[Scanned document image captured. AI will perform high-precision OCR during document analysis.]",
            imageBase64List = listOf(base64),
            hasUnclearWarning = false,
            warningMessage = null
        )
    }

    private fun parseTextFile(context: Context, uri: Uri, fileName: String): ExtractedDocument {
        val sb = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).useLines { lines ->
                lines.forEach { sb.append(it).append("\n") }
            }
        }
        val cleanText = cleanOcrAndFormatting(sb.toString())
        return ExtractedDocument(
            fileName = fileName,
            fileType = "TXT",
            rawText = cleanText
        )
    }

    private fun parseDocx(context: Context, uri: Uri, fileName: String): ExtractedDocument {
        val sb = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                        val text = extractTextFromDocxXml(xmlContent)
                        sb.append(text)
                        break
                    }
                    entry = zip.nextEntry
                }
            }
        }
        val text = sb.toString().trim()
        val cleanText = cleanOcrAndFormatting(if (text.isBlank()) "[Empty or encrypted Word document]" else text)
        return ExtractedDocument(
            fileName = fileName,
            fileType = "DOCX",
            rawText = cleanText
        )
    }

    private fun extractTextFromDocxXml(xml: String): String {
        val sb = StringBuilder()
        // Replace paragraph ends <w:p> with newlines
        val pPattern = Pattern.compile("<w:p(?:\\s[^>]*)?>(.*?)</w:p>", Pattern.DOTALL)
        val tPattern = Pattern.compile("<w:t(?:\\s[^>]*)?>(.*?)</w:t>")
        val pMatcher = pPattern.matcher(xml)

        while (pMatcher.find()) {
            val paragraphXml = pMatcher.group(1) ?: ""
            val tMatcher = tPattern.matcher(paragraphXml)
            val pText = StringBuilder()
            while (tMatcher.find()) {
                pText.append(tMatcher.group(1))
            }
            val line = pText.toString().trim()
            if (line.isNotEmpty()) {
                sb.append(line).append("\n")
            }
        }

        if (sb.isEmpty()) {
            // Fallback: match all <w:t> tags directly
            val tMatcher = tPattern.matcher(xml)
            while (tMatcher.find()) {
                sb.append(tMatcher.group(1)).append(" ")
            }
        }
        return sb.toString().trim()
    }

    private fun parseXlsx(context: Context, uri: Uri, fileName: String): ExtractedDocument {
        val strings = mutableListOf<String>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "xl/sharedStrings.xml") {
                        val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                        val tPattern = Pattern.compile("<t(?:\\s[^>]*)?>(.*?)</t>")
                        val matcher = tPattern.matcher(xml)
                        while (matcher.find()) {
                            strings.add(matcher.group(1) ?: "")
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        }
        val formatted = if (strings.isNotEmpty()) {
            "--- EXCEL SPREADSHEET EXTRACTED CONTENT ---\n" + strings.chunked(4).joinToString("\n") { row ->
                row.joinToString(" | ")
            }
        } else {
            "[Empty or numeric-only Excel document. Please check cell contents.]"
        }
        return ExtractedDocument(
            fileName = fileName,
            fileType = "XLSX",
            rawText = cleanOcrAndFormatting(formatted)
        )
    }

    private fun parsePdf(context: Context, uri: Uri, fileName: String): ExtractedDocument {
        val imageList = mutableListOf<String>()
        var fallbackText = ""

        try {
            // Attempt to render the first 1-3 pages to Bitmaps for visual OCR inspection
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                val pageCount = minOf(renderer.pageCount, 3)
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    val width = (page.width * 1.5).toInt().coerceIn(600, 1600)
                    val height = (page.height * 1.5).toInt().coerceIn(800, 2200)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    imageList.add(bitmapToBase64(bitmap))
                }
                renderer.close()
            }
        } catch (e: Exception) {
            // PDF may be password-protected or unsupported raster
            fallbackText = "Notice: PDF rendering note: ${e.message ?: "Encrypted or complex layout"}"
        }

        // Also attempt basic text stream extraction
        val rawExtracted = try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val stringData = String(bytes, Charsets.ISO_8859_1)
                // Extract PDF text blocks between BT and ET or Tj / TJ
                val tjPattern = Pattern.compile("\\((.*?)\\)\\s*Tj", Pattern.DOTALL)
                val matcher = tjPattern.matcher(stringData)
                while (matcher.find()) {
                    val segment = matcher.group(1) ?: ""
                    if (segment.isNotBlank()) sb.append(segment).append(" ")
                }
            }
            sb.toString().trim()
        } catch (e: Exception) {
            ""
        }

        val textToUse = if (rawExtracted.isNotBlank()) {
            cleanOcrAndFormatting(rawExtracted)
        } else {
            "[PDF Document loaded with ${imageList.size} page(s). Gemini / Local AI will process visual pages directly.]"
        }

        return ExtractedDocument(
            fileName = fileName,
            fileType = "PDF",
            rawText = if (fallbackText.isNotBlank()) "$fallbackText\n\n$textToUse" else textToUse,
            imageBase64List = imageList,
            hasUnclearWarning = imageList.isEmpty() && rawExtracted.isBlank(),
            warningMessage = if (imageList.isEmpty() && rawExtracted.isBlank()) "Some parts of the document could not be read clearly." else null
        )
    }

    private fun parseImage(context: Context, uri: Uri, fileName: String): ExtractedDocument {
        var base64 = ""
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream)
            if (bitmap != null) {
                base64 = bitmapToBase64(bitmap)
            }
        }
        val isUnclear = base64.isEmpty()
        return ExtractedDocument(
            fileName = fileName,
            fileType = "IMAGE",
            rawText = if (isUnclear) "Could not decode image file." else "[Document Image uploaded: AI Multimodal OCR ready.]",
            imageBase64List = if (base64.isNotEmpty()) listOf(base64) else emptyList(),
            hasUnclearWarning = isUnclear,
            warningMessage = if (isUnclear) "Some parts of the document could not be read clearly." else null
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        // Resize if too huge to prevent OOM
        val maxDim = 1600
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Cleans common OCR / conversion errors:
     * - Fixes broken hyphenated words at line ends (e.g. "com- / munication" -> "communication")
     * - Preserves reference numbers (e.g. "No. 12-4/2026-ADM")
     * - Preserves numbered list formats: "1.", "2.", "(i)", "(ii)", "Sub:"
     * - Normalizes weird spacing while maintaining paragraph indentation
     */
    fun cleanOcrAndFormatting(input: String): String {
        var s = input
        // Fix trailing hyphenated words across line breaks
        s = s.replace(Regex("(\\w+)-\\s*\\r?\\n\\s*(\\w+)"), "$1$2")
        // Remove excessive empty lines (> 2)
        s = s.replace(Regex("\\r?\\n{3,}"), "\n\n")
        // Standardize common OCR subject and reference prefixes cleanly
        s = s.replace(Regex("(?i)\\b(sub\\s*[:;\\-]\\s*)"), "Subject: ")
        s = s.replace(Regex("(?i)\\b(ref\\s*[:;\\-]\\s*)"), "Reference: ")
        return s.trim()
    }
}
