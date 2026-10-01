package com.example

import com.example.data.ai.BuiltinOfficeAiEngine
import com.example.data.model.OutputLanguage
import com.example.data.model.WritingMode
import com.example.data.parser.DocumentParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testOcrCleaning_preservesFormatting() {
        val messyText = "Sub: Urgent Requirement of 2 nos Batteries\nRef: No. ADM-2026/04\nThis is an offi-\ncial letter."
        val cleaned = DocumentParser.cleanOcrAndFormatting(messyText)
        assertTrue(cleaned.contains("Subject: Urgent Requirement of 2 nos Batteries"))
        assertTrue(cleaned.contains("Reference: No. ADM-2026/04"))
        assertTrue(cleaned.contains("official letter"))
    }

    @Test
    fun testBuiltinOfficeAiEngine_analysisAndReply() = runBlocking {
        val sampleLetter = """
            OFFICE OF THE EXECUTIVE ENGINEER
            Letter No: ENGG/HQ/2026/89
            Date: 15-10-2026

            To:
            The Superintending Officer

            Subject: Requisition for replacement of 2 nos heavy-duty vehicle batteries.

            Sir,
            It is requested that 2 nos batteries for inspection vehicle DL-01-AB-1234 may be replaced within 7 days.
        """.trimIndent()

        val analysisResult = BuiltinOfficeAiEngine.analyzeDocument(sampleLetter).getOrNull()
        assertNotNull(analysisResult)
        assertEquals("ENGG/HQ/2026/89", analysisResult?.letterNumber)
        assertEquals("15-10-2026", analysisResult?.date)

        val reply = BuiltinOfficeAiEngine.generateReply(
            sourceText = sampleLetter,
            analysis = analysisResult,
            mode = WritingMode.OFFICIAL,
            language = OutputLanguage.ENGLISH
        ).getOrNull()

        assertNotNull(reply)
        assertTrue(reply!!.contains("ENGG/HQ/2026/89"))
        assertTrue(reply.contains("Yours faithfully"))

        val factCheck = BuiltinOfficeAiEngine.factCheck(sampleLetter, reply).getOrNull()
        assertNotNull(factCheck)
        assertTrue(factCheck!!.scorePercent >= 80)
    }

    @Test
    fun testAiProviderConfig_cloudAndLocalToggle() {
        val cloudConfig = com.example.data.model.AiProviderConfig(
            providerType = com.example.data.model.AiProviderType.GEMINI,
            geminiModel = "gemini-3.5-flash"
        )
        val providerManager = com.example.data.ai.AiProviderManager(cloudConfig)
        assertEquals(false, providerManager.isCurrentProviderLocal)
        assertTrue(providerManager.providerBadgeText.contains("gemini-3.5-flash"))

        val localConfig = cloudConfig.copy(
            providerType = com.example.data.model.AiProviderType.OLLAMA,
            ollamaModel = "llama3"
        )
        providerManager.updateConfig(localConfig)
        assertEquals(true, providerManager.isCurrentProviderLocal)
        assertTrue(providerManager.providerBadgeText.contains("Ollama"))
    }

    @Test
    fun testExportManager_docxAndTxtStreams() {
        val title = "Requisition Letter"
        val content = "**Bold Header**\n    *Indented Italic Note*\n__Underlined Reference__\n1. First point"

        // Test TXT stream
        val txtBytesOut = java.io.ByteArrayOutputStream()
        com.example.data.export.ExportManager.writeTxtToStream(title, content, txtBytesOut)
        val txtStr = txtBytesOut.toString(Charsets.UTF_8.name())
        assertTrue(txtStr.contains("Requisition Letter"))
        assertTrue(txtStr.contains("**Bold Header**"))

        // Test DOCX stream
        val docxBytesOut = java.io.ByteArrayOutputStream()
        com.example.data.export.ExportManager.writeDocxToStream(title, content, docxBytesOut)
        val docxBytes = docxBytesOut.toByteArray()
        assertTrue(docxBytes.isNotEmpty())
        // DOCX is a ZIP format starting with PK (0x50, 0x4B)
        assertEquals(0x50.toByte(), docxBytes[0])
        assertEquals(0x4B.toByte(), docxBytes[1])
    }

    @Test
    fun testOcrService_segmentationAndErrorChecking() {
        val messyLetter = """
            OFFICE OF THE SUPERINTENDING ENGINEER
            Letter No: SE/HQ/2026/899
            Date: 12-10-2026
            To:
            The Executive Engineer
            Sub: Supply of emergency spare parts for gene-
            rators
            Sir,
            1. It is directed that necessary spares may kindly be procured within 15 days.
            2. Compliance report must be submitted.
            Yours faithfully,
            R. K. Verma
            Superintending Engineer
            Copy to: Accounts Officer
        """.trimIndent()

        val result = com.example.data.ocr.OcrService.analyzeMlKitOutput(messyLetter)
        assertNotNull(result)
        // Check hyphenation stitching
        assertTrue(result.cleanedText.contains("generators"))
        // Check segmented fields
        val seg = result.segmentedDocument
        assertTrue(seg.headerLetterhead.contains("SUPERINTENDING ENGINEER"))
        assertTrue(seg.recipientBlock.contains("Executive Engineer"))
        assertTrue(seg.subjectAndRef.contains("Subject:"))
        assertEquals(2, seg.bodyParagraphs.size)
        assertTrue(seg.signOffBlock.contains("Yours faithfully"))
        assertEquals(1, seg.endorsements.size)
        assertTrue(seg.endorsements[0].contains("Accounts Officer"))
    }
}
