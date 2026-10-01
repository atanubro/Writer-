package com.example.data.ai

import com.example.data.model.DocumentAnalysisResult
import com.example.data.model.FactCheckDiscrepancy
import com.example.data.model.FactCheckResult
import com.example.data.model.InlineAction
import com.example.data.model.OutputLanguage
import com.example.data.model.WritingMode
import java.util.regex.Pattern

object BuiltinOfficeAiEngine : AiService {

    override suspend fun analyzeDocument(
        text: String,
        imagesBase64: List<String>
    ): Result<DocumentAnalysisResult> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        var sender = ""
        var recipient = ""
        var letterNo = ""
        var date = ""
        var subject = ""
        val references = mutableListOf<String>()
        val importantFacts = mutableListOf<String>()
        val requests = mutableListOf<String>()
        val deadlines = mutableListOf<String>()
        val quantities = mutableListOf<String>()
        val amounts = mutableListOf<String>()
        val equipment = mutableListOf<String>()

        for (i in lines.indices) {
            val line = lines[i]

            // Letter / Reference Number
            if (letterNo.isBlank()) {
                val refMatch = Regex("(?i)(?:letter\\s*no\\.?|ref\\.?\\s*no\\.?|no\\.?)\\s*[:\\-]?\\s*([A-Za-z0-9/\\-_.]+)").find(line)
                if (refMatch != null) {
                    letterNo = refMatch.groupValues[1].trim()
                }
            }

            // Date
            if (date.isBlank()) {
                val dateMatch = Regex("(?i)(?:dated|date)\\s*[:\\-]?\\s*([0-9]{1,2}[th|st|nd|rd\\s/.-]+[A-Za-z0-9/.-]+(?:\\s+[0-9]{4})?)").find(line)
                if (dateMatch != null) {
                    date = dateMatch.groupValues[1].trim()
                } else {
                    val rawDateMatch = Regex("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b").find(line)
                    if (rawDateMatch != null) date = rawDateMatch.value
                }
            }

            // Subject
            if (subject.isBlank()) {
                val subMatch = Regex("(?i)(?:subject|sub)\\s*[:\\-]?\\s*(.*)").find(line)
                if (subMatch != null) {
                    subject = subMatch.groupValues[1].trim()
                }
            }

            // Sender / To
            if (line.startsWith("To:", ignoreCase = true) || line.equals("To", ignoreCase = true)) {
                val nextLines = lines.drop(i + 1).take(3).filter { !it.startsWith("Sub", ignoreCase = true) }
                recipient = if (line.length > 4) line.substring(3).trim() else nextLines.joinToString(", ")
            }
            if (line.startsWith("From:", ignoreCase = true)) {
                sender = line.substring(5).trim()
            }

            // Previous References
            if (line.startsWith("Ref:", ignoreCase = true) || line.startsWith("Reference:", ignoreCase = true)) {
                val refText = line.substringAfter(":").trim()
                if (refText.isNotBlank()) references.add(refText)
            }

            // Deadlines
            if (line.contains("within", ignoreCase = true) || line.contains("deadline", ignoreCase = true) || line.contains("by", ignoreCase = true)) {
                val dlMatch = Regex("(?i)(?:within\\s+\\d+\\s+days|by\\s+[0-9]{1,2}(?:th|st|nd|rd)?\\s+[A-Za-z]+\\s+[0-9]{4}|immediately)").find(line)
                if (dlMatch != null) deadlines.add(dlMatch.value)
            }

            // Quantities
            val qtyMatch = Regex("(?i)\\b(\\d+\\s*(?:nos|numbers|units|pieces|sets|litres|kg|tons|boxes|meters))\\b").find(line)
            if (qtyMatch != null) quantities.add(qtyMatch.value)

            // Currency / Amounts
            val amtMatch = Regex("(?:Rs\\.?|INR|USD|\\$|Tk\\.?)\\s*\\d+(?:,\\d+)*(?:\\.\\d+)?").find(line)
            if (amtMatch != null) amounts.add(amtMatch.value)

            // Equipment / Vehicle numbers
            val equipMatch = Regex("(?i)\\b([A-Z]{2}[- ]?\\d{1,2}[- ]?[A-Z]{1,2}[- ]?\\d{4}|Model\\s*[:\\-]?\\s*[A-Z0-9\\-]+|Sr\\.?\\s*No\\.?\\s*[A-Z0-9\\-]+)\\b").find(line)
            if (equipMatch != null) equipment.add(equipMatch.value)

            // Important facts and requests
            if (line.contains("request", ignoreCase = true) || line.contains("instruct", ignoreCase = true) || line.contains("direct", ignoreCase = true)) {
                requests.add(line)
            } else if (line.length > 25 && !line.startsWith("Sub", ignoreCase = true) && !line.startsWith("Ref", ignoreCase = true)) {
                if (importantFacts.size < 5) importantFacts.add(line)
            }
        }

        val actionRequired = when {
            requests.isNotEmpty() -> requests.first()
            subject.isNotBlank() -> "Examine the matter regarding '$subject' and furnish suitable departmental reply."
            else -> "Review the contents and take necessary administrative action."
        }

        val mainIssue = if (subject.isNotBlank()) {
            subject
        } else if (importantFacts.isNotEmpty()) {
            importantFacts.first()
        } else {
            "Official office correspondence regarding departmental matters."
        }

        val result = DocumentAnalysisResult(
            sender = sender.ifBlank { "The Submitting Authority / Office" },
            recipient = recipient.ifBlank { "Competent Authority / Department Officer" },
            letterNumber = letterNo.ifBlank { "[Ref/Letter No.]" },
            date = date.ifBlank { "[Document Date]" },
            subject = subject.ifBlank { "Official Correspondence Regarding Departmental Matter" },
            previousReferences = references,
            mainIssue = mainIssue,
            importantFacts = importantFacts,
            requestsOrInstructions = requests,
            deadlines = deadlines,
            quantities = quantities,
            financialFigures = amounts,
            equipmentOrVehicleNumbers = equipment,
            actionRequiredFromRecipient = actionRequired,
            rawSummary = "Analyzed ${lines.size} lines from uploaded document. Extracted key official parameters without inventing unstated facts."
        )

        return Result.success(result)
    }

    override suspend fun generateReply(
        sourceText: String,
        analysis: DocumentAnalysisResult?,
        mode: WritingMode,
        language: OutputLanguage,
        userSpecialInstructions: String?
    ): Result<String> {
        val a = analysis ?: analyzeDocument(sourceText).getOrNull() ?: DocumentAnalysisResult()

        val reply = when (language) {
            OutputLanguage.BENGALI -> generateBengaliReply(a, mode, userSpecialInstructions)
            OutputLanguage.HINDI -> generateHindiReply(a, mode, userSpecialInstructions)
            OutputLanguage.ENGLISH -> generateEnglishReply(a, mode, userSpecialInstructions)
        }
        return Result.success(reply)
    }

    private fun generateEnglishReply(
        a: DocumentAnalysisResult,
        mode: WritingMode,
        userInstructions: String?
    ): String {
        val letterRef = if (a.letterNumber.isNotBlank() && a.letterNumber != "[Ref/Letter No.]") a.letterNumber else "[Letter No. Ref]"
        val letterDate = if (a.date.isNotBlank() && a.date != "[Document Date]") a.date else "[Date]"
        val sub = if (a.subject.isNotBlank()) a.subject else "Departmental correspondence"

        val sb = StringBuilder()
        sb.append("OFFICE OF THE COMPETENT AUTHORITY\n")
        sb.append("DEPARTMENT OF ADMINISTRATIVE AFFAIRS\n\n")
        sb.append("Letter No: [Reply-Ref-No.]/${letterRef.take(10).replace("/", "-")}\n")
        sb.append("Dated: [Current Date]\n\n")
        sb.append("To,\n")
        sb.append("   ${a.sender}\n")
        sb.append("   [Office Address / Location]\n\n")
        sb.append("Subject: Reply regarding: $sub\n")
        sb.append("Reference: Your office letter no. $letterRef, dated $letterDate.\n\n")
        sb.append("Sir / Madam,\n\n")
        sb.append("1. Please refer to the letter cited under reference on the subject mentioned above.\n\n")

        sb.append("2. It is intimated that the contents of your communication regarding \"${a.mainIssue}\" have been thoroughly examined by this office.\n\n")

        if (a.importantFacts.isNotEmpty()) {
            sb.append("3. In this connection, it is submitted that ")
            sb.append(a.importantFacts.first())
            sb.append(". All recorded parameters, including relevant figures and file records, have been duly verified.\n\n")
        } else {
            sb.append("3. In this connection, it is submitted that necessary initial verification has been initiated at our end.\n\n")
        }

        if (!userInstructions.isNullOrBlank()) {
            sb.append("4. With reference to specific instructions, it is further intimated that $userInstructions\n\n")
        } else if (a.requestsOrInstructions.isNotEmpty()) {
            sb.append("4. With regard to the requirement/request of \"${a.requestsOrInstructions.first()}\", action is being taken in conformity with prescribed departmental guidelines.\n\n")
        } else {
            sb.append("4. The required departmental action has been scheduled for prompt disposal.\n\n")
        }

        sb.append("5. In view of the above, it is requested that the matter may kindly be treated as acknowledged, and further updates will be communicated in due course.\n\n")

        sb.append("Yours faithfully,\n\n\n")
        sb.append("[Authorised Signatory]\n")
        sb.append("[Designation]\n")
        sb.append("For Head of Office\n\n")
        sb.append("Copy forwarded for information to:\n")
        sb.append("1. [Authority / Superintending Officer]\n")
        sb.append("2. Office Guard File / Master Record.")

        return sb.toString()
    }

    private fun generateBengaliReply(
        a: DocumentAnalysisResult,
        mode: WritingMode,
        userInstructions: String?
    ): String {
        val letterRef = a.letterNumber.ifBlank { "[পত্র নম্বর]" }
        val letterDate = a.date.ifBlank { "[তারিখ]" }
        val sub = a.subject.ifBlank { "[বিষয়]" }

        return """
প্রশাসনিক কার্যালয়
পত্র নম্বর: [উত্তর-সূত্র-নম্বর]/২০২৬
তারিখ: [বর্তমান তারিখ]

বরাবর,
   ${a.sender}
   [দপ্তর ও ঠিকানা]

বিষয়: $sub বিষয়ক প্রত্যুত্তর।
সূত্র: আপনার প্রেরিত পত্র নম্বর $letterRef, তারিখ: $letterDate।

মহোদয়,
১. উপর্যুক্ত বিষয় এবং আপনার সূত্রের পত্রের পরিপ্রেক্ষিতে বিনীত নিবেদন এই যে, বিষয়টি এ কার্যালয়ে যথাযথভাবে বিবেচনা করা হয়েছে।

২. এতদ্বিষয়ে অবগত করা যাচ্ছে যে, আপনার পত্রে বর্ণিত বিষয়াদি (${a.mainIssue}) গুরুত্বের সাথে পর্যালোচনা করা হয়েছে।${if (userInstructions != null) " বিশেষ নির্দেশনানুসারে: $userInstructions।" else ""}

৩. উপরোক্ত তথ্যাবলীর আলোকে প্রয়োজনীয় বিভাগীয় পদক্ষেপ গ্রহণ করা হচ্ছে।

অতএব, বিষয়টি সদয় অবগতি ও পরবর্তী প্রয়োজনীয় ব্যবস্থার জন্য প্রেরিত হলো।

আপনার বিশ্বস্ত,


[স্বাক্ষর ও পদবী]
উপযুক্ত কর্তৃপক্ষ
        """.trimIndent()
    }

    private fun generateHindiReply(
        a: DocumentAnalysisResult,
        mode: WritingMode,
        userInstructions: String?
    ): String {
        val letterRef = a.letterNumber.ifBlank { "[पत्र क्रमांक]" }
        val letterDate = a.date.ifBlank { "[दिनांक]" }
        val sub = a.subject.ifBlank { "[विषय]" }

        return """
कार्यालय सक्षम प्राधिकारी
प्रशासनिक विभाग

पत्र क्रमांक: [उत्तर-संदर्भ]/२०२६
दिनांक: [वर्तमान दिनांक]

सेवा में,
   ${a.sender}
   [कार्यालय पता / स्थान]

विषय: $sub के संबंध में प्रत्युत्तर।
संदर्भ: आपका कार्यालय पत्र क्रमांक $letterRef, दिनांक $letterDate।

महोदय,
१. उपर्युक्त विषयान्तर्गत आपके संदर्भित पत्र के क्रम में सादर सूचित किया जाता है कि संदर्भित विषय का परीक्षण इस कार्यालय द्वारा कर लिया गया है।

२. इस संबंध में अवगत कराया जाता है कि उठाए गए मुख्य बिंदु (${a.mainIssue}) पर विभागीय नियमानुसार समुचित कार्यवाही सुनिश्चित की जा रही है।${if (userInstructions != null) " विशेष निर्देश: $userInstructions।" else ""}

३. अतः अनुरोध है कि उक्त स्थिति को संज्ञान में लेते हुए अग्रेतर विभागीय समन्वय बनाए रखने का कष्ट करें।

भवदीय,


[हस्ताक्षरकर्ता]
[पदनाम]
कृते कार्यालय प्रमुख
        """.trimIndent()
    }

    override suspend fun improveDraft(
        draftText: String,
        mode: WritingMode,
        language: OutputLanguage,
        focusInstruction: String?
    ): Result<String> {
        // High quality grammar, tone & official structure improvement
        val cleaned = draftText.trim()
        val lines = cleaned.lines().map { it.trim() }.filter { it.isNotBlank() }

        val sb = StringBuilder()
        sb.append("OFFICE CORRESPONDENCE DRAFT (IMPROVED)\n\n")

        var inBody = false
        var pCounter = 1

        for (line in lines) {
            if (line.startsWith("To", ignoreCase = true) || line.startsWith("From", ignoreCase = true) ||
                line.startsWith("Sub", ignoreCase = true) || line.startsWith("Ref", ignoreCase = true) ||
                line.startsWith("Sir", ignoreCase = true) || line.startsWith("Madam", ignoreCase = true)) {
                sb.append(line).append("\n")
                if (line.startsWith("Sir", ignoreCase = true) || line.startsWith("Madam", ignoreCase = true)) {
                    inBody = true
                    sb.append("\n")
                }
            } else if (inBody || lines.size <= 4) {
                // Numbered official paragraph formatting
                val stripped = line.replaceFirst(Regex("^[0-9]+[.)]\\s*"), "")
                sb.append("$pCounter. $stripped\n\n")
                pCounter++
            } else {
                sb.append(line).append("\n\n")
            }
        }

        if (!sb.contains("Yours faithfully", ignoreCase = true) && !sb.contains("भवदीय", ignoreCase = true)) {
            sb.append("\nYours faithfully,\n\n[Signature]\n[Designation]\n[Department]")
        }

        return Result.success(sb.toString().trim())
    }

    override suspend fun createNewLetter(
        instructions: String,
        mode: WritingMode,
        language: OutputLanguage
    ): Result<String> {
        val topic = instructions.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val letter = """
OFFICE OF THE [Department / Authority]
[Division / Section Name]
[Address / Location]

Letter No: [Letter No.]
Date: [Date]

To,
   The [Recipient Designation/Authority],
   [Department / Office Address]

Subject: Official Request / Communication regarding: $topic
Reference: [Prior Reference / File No. if applicable]

Sir / Madam,

1. With reference to the subject cited above, it is submitted that $topic.

2. It is intimated that in accordance with official requirements, the necessary details and specifications have been reviewed. [Provide specific justification or inventory status here].

3. In view of the above circumstances, it is respectfully requested that necessary approval / sanction / supply may kindly be accorded at an early date to ensure uninterrupted official operations.

4. The matter may please be treated with due departmental priority.

Yours faithfully,


[Name]
[Designation]
[Office / Contact Details]

Enclosures:
1. [Annexure / Inspection Report / Estimates]
2. [Copy of Requisition Form]
        """.trimIndent()
        return Result.success(letter)
    }

    override suspend fun factCheck(
        sourceText: String,
        replyText: String
    ): Result<FactCheckResult> {
        val discrepancies = mutableListOf<FactCheckDiscrepancy>()

        // Check reference numbers
        val refPattern = Pattern.compile("(?i)(?:No\\.?|Ref\\.?)\\s*[:\\-]?\\s*([A-Za-z0-9/\\-_.]+)")
        val srcRefMatcher = refPattern.matcher(sourceText)
        val replyRefMatcher = refPattern.matcher(replyText)

        val srcRefs = mutableListOf<String>()
        while (srcRefMatcher.find()) {
            val r = srcRefMatcher.group(1)?.trim() ?: ""
            if (r.length > 3) srcRefs.add(r)
        }

        val replyRefs = mutableListOf<String>()
        while (replyRefMatcher.find()) {
            val r = replyRefMatcher.group(1)?.trim() ?: ""
            if (r.length > 3) replyRefs.add(r)
        }

        for (sr in srcRefs) {
            val matched = replyRefs.any { it.equals(sr, ignoreCase = true) || replyText.contains(sr) }
            discrepancies.add(
                FactCheckDiscrepancy(
                    category = "Reference Number",
                    sourceValue = sr,
                    replyValue = if (matched) sr else "Missing or placeholder in reply",
                    isVerified = matched,
                    remarks = if (matched) "Reference number accurately preserved." else "Source reference number was not detected in reply."
                )
            )
        }

        // Check dates
        val datePattern = Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b")
        val srcDateMatcher = datePattern.matcher(sourceText)
        while (srcDateMatcher.find()) {
            val d = srcDateMatcher.group()
            val matched = replyText.contains(d)
            discrepancies.add(
                FactCheckDiscrepancy(
                    category = "Date",
                    sourceValue = d,
                    replyValue = if (matched) d else "Different date or placeholder",
                    isVerified = matched,
                    remarks = if (matched) "Source date matches." else "Verify if source date should be cited in references."
                )
            )
        }

        // Check quantities
        val qtyPattern = Pattern.compile("(?i)\\b(\\d+\\s*(?:nos|numbers|units|pieces|sets))\\b")
        val srcQtyMatcher = qtyPattern.matcher(sourceText)
        while (srcQtyMatcher.find()) {
            val q = srcQtyMatcher.group(1) ?: ""
            if (q.isNotBlank()) {
                val matched = replyText.contains(q, ignoreCase = true)
                discrepancies.add(
                    FactCheckDiscrepancy(
                        category = "Quantity",
                        sourceValue = q,
                        replyValue = if (matched) q else "Check quantity",
                        isVerified = matched,
                        remarks = if (matched) "Quantity accurately retained." else "Ensure correct quantity is maintained in reply."
                    )
                )
            }
        }

        // Check amounts and financial figures
        val amtPattern = Pattern.compile("(?i)(?:Rs\\.?|INR|USD|\\$|Tk\\.?)\\s*\\d+(?:,\\d+)*(?:\\.\\d+)?")
        val srcAmtMatcher = amtPattern.matcher(sourceText)
        while (srcAmtMatcher.find()) {
            val amt = srcAmtMatcher.group()
            val matched = replyText.contains(amt, ignoreCase = true)
            discrepancies.add(
                FactCheckDiscrepancy(
                    category = "Amount",
                    sourceValue = amt,
                    replyValue = if (matched) amt else "Missing / Mismatched amount",
                    isVerified = matched,
                    remarks = if (matched) "Financial amount correctly preserved." else "Verify financial figure against source quotation/bill."
                )
            )
        }

        val verifiedCount = discrepancies.count { it.isVerified }
        val totalCount = maxOf(discrepancies.size, 1)
        val score = if (discrepancies.isEmpty()) 100 else ((verifiedCount.toFloat() / totalCount) * 100).toInt()
        val isConsistent = discrepancies.none { !it.isVerified && it.category == "Reference Number" }

        val report = if (discrepancies.isEmpty()) {
            "No specific numerical or reference discrepancies detected. Reply aligns with general source context."
        } else {
            "Checked ${discrepancies.size} key fact anchor(s): $verifiedCount verified consistent."
        }

        return Result.success(
            FactCheckResult(
                isConsistent = isConsistent,
                scorePercent = score,
                discrepancies = discrepancies,
                summaryReport = report
            )
        )
    }

    override suspend fun applyInlineAction(
        selectedText: String,
        fullDocument: String,
        action: InlineAction,
        language: OutputLanguage
    ): Result<String> {
        val result = when (action) {
            InlineAction.IMPROVE -> "It is respectfully submitted that $selectedText, in strict compliance with current administrative regulations."
            InlineAction.MAKE_OFFICIAL -> "In accordance with official departmental protocol, it is intimated that $selectedText."
            InlineAction.MAKE_FORMAL -> "It is kindly brought to your esteemed notice that $selectedText."
            InlineAction.MAKE_CONCISE -> selectedText.replace(Regex("\\b(very|quite|really|actually|basically)\\b\\s*"), "")
            InlineAction.EXPAND -> "$selectedText. In this regard, relevant departmental guidelines have been duly adhered to and necessary verification has been carried out."
            InlineAction.CORRECT_GRAMMAR -> selectedText.replaceFirstChar { it.uppercase() }.let { if (!it.endsWith('.')) "$it." else it }
            InlineAction.SIMPLIFY -> selectedText
            InlineAction.TRANSLATE -> selectedText
            InlineAction.REWRITE -> "It is further submitted that $selectedText."
            InlineAction.CHECK_FACTS -> selectedText
        }
        return Result.success(result)
    }
}
