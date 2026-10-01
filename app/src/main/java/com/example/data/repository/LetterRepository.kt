package com.example.data.repository

import com.example.data.local.LetterDao
import com.example.data.local.LetterDocumentEntity
import com.example.data.local.LetterTemplateEntity
import com.example.data.local.TemplateDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class LetterRepository(
    private val letterDao: LetterDao,
    private val templateDao: TemplateDao
) {
    val allLetters: Flow<List<LetterDocumentEntity>> = letterDao.getAllLetters()
    val allTemplates: Flow<List<LetterTemplateEntity>> = templateDao.getAllTemplates()

    fun searchLetters(query: String): Flow<List<LetterDocumentEntity>> = letterDao.searchLetters(query)

    suspend fun getLetterById(id: Long): LetterDocumentEntity? = letterDao.getLetterById(id)

    suspend fun saveLetter(letter: LetterDocumentEntity): Long {
        return if (letter.id == 0L) {
            letterDao.insertLetter(letter)
        } else {
            letterDao.updateLetter(letter)
            letter.id
        }
    }

    suspend fun deleteLetter(id: Long) {
        letterDao.deleteLetterById(id)
    }

    suspend fun deleteAllLetters() {
        letterDao.deleteAllLetters()
    }

    suspend fun saveTemplate(template: LetterTemplateEntity): Long {
        return templateDao.insertTemplate(template)
    }

    suspend fun deleteTemplate(id: Long) {
        templateDao.deleteTemplateById(id)
    }

    suspend fun initializeDefaultTemplatesIfNeeded() {
        val existing = templateDao.getAllTemplates().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val defaults = listOf(
                LetterTemplateEntity(
                    name = "Official Departmental Reply",
                    category = "Official Reply",
                    description = "Formal response referencing incoming letter with action taken / pending.",
                    subjectTemplate = "Reply to Letter No. [Letter No.] regarding [Subject]",
                    bodyTemplate = """
Sir / Madam,

1. Please refer to your letter no. [Letter No.] dated [Date] on the subject cited above.

2. It is intimated that the matter has been examined in detail by this office. The required factual verification has been concluded.

3. In this connection, it is submitted that necessary departmental compliance is being ensured.

4. In view of the above, this is for your kind information and further necessary action.

Yours faithfully,
[Signatory]
[Designation]
                    """.trimIndent(),
                    placeholders = "[Letter No.],[Date],[Subject],[Signatory],[Designation]"
                ),
                LetterTemplateEntity(
                    name = "Equipment Replacement Request",
                    category = "Replacement Request",
                    description = "Official requisition for replacing unserviceable equipment/batteries/machinery.",
                    subjectTemplate = "Requisition for replacement of unserviceable [Equipment / Items]",
                    bodyTemplate = """
Sir / Madam,

1. With reference to the subject cited above, it is brought to your kind notice that [Quantity] of [Equipment/Item] deployed at [Location/Vehicle No.] have become unserviceable during routine operations.

2. A joint inspection was conducted on [Date] by the technical inspection board, which confirmed that the items are beyond economical repair.

3. In view of the operational exigencies, it is requested that replacement of the said [Equipment/Item] may kindly be sanctioned and issued at an early date.

Yours faithfully,
[Name / Designation]
                    """.trimIndent(),
                    placeholders = "[Quantity],[Equipment/Item],[Location/Vehicle No.],[Date]"
                ),
                LetterTemplateEntity(
                    name = "Administrative Reminder Letter",
                    category = "Reminder",
                    description = "Urgent follow-up for pending departmental queries or delayed reports.",
                    subjectTemplate = "Reminder: Submission of [Report/Information] - Reg.",
                    bodyTemplate = """
Sir / Madam,

1. Kindly refer to this office letter of even number dated [Previous Date] regarding [Subject].

2. It is observed that the required report / clarification is still awaited from your end despite the lapse of stipulated time.

3. Therefore, it is requested that the matter may please be expedited and the requisite details furnished to this office by [Deadline Date] without fail.

Yours faithfully,
[Authority]
                    """.trimIndent(),
                    placeholders = "[Previous Date],[Subject],[Deadline Date],[Authority]"
                ),
                LetterTemplateEntity(
                    name = "Deficiency Report & Rectification",
                    category = "Deficiency Report",
                    description = "Official notification of material or execution defects noticed during site inspection.",
                    subjectTemplate = "Notice regarding deficiencies in [Work Order / Project]",
                    bodyTemplate = """
Sir / Madam,

1. During the site inspection of [Project Name] conducted on [Inspection Date], the following deficiencies were observed:
   (a) [Deficiency Point 1]
   (b) [Deficiency Point 2]

2. You are hereby called upon to rectify the pointed out defects within [Number] days of receipt of this communication and submit a compliance report.

Yours faithfully,
[Inspecting Officer]
                    """.trimIndent(),
                    placeholders = "[Project Name],[Inspection Date],[Deficiency Point 1],[Number]"
                ),
                LetterTemplateEntity(
                    name = "Forwarding Letter (Enclosures)",
                    category = "Forwarding Letter",
                    description = "Official routing memo transmitting files, bills, or confidential annexures.",
                    subjectTemplate = "Forwarding of [Document Name / Vouchers] - Regarding",
                    bodyTemplate = """
Sir / Madam,

1. Please find enclosed herewith [Document Description] bearing File No. [File No.] for your perusal and necessary departmental action.

2. Necessary verification has been carried out at our level.

Yours faithfully,
[Dispatching Officer]

Enclosures: As stated.
                    """.trimIndent(),
                    placeholders = "[Document Description],[File No.],[Dispatching Officer]"
                ),
                LetterTemplateEntity(
                    name = "Technical Clarification & Justification",
                    category = "Clarification",
                    description = "Engineering or financial defense responding to audit or administrative queries.",
                    subjectTemplate = "Clarification regarding [Audit Para / Query No.]",
                    bodyTemplate = """
Sir / Madam,

1. With reference to your memo no. [Memo No.] dated [Date], the following technical clarification is submitted for consideration:

2. The work / expenditure was carried out in accordance with approved specification [Specification No.] and administrative sanction dated [Sanction Date].

3. In view of the above clarification, it is requested that the observation may kindly be settled.

Yours faithfully,
[Engineer / In-charge]
                    """.trimIndent(),
                    placeholders = "[Memo No.],[Date],[Specification No.],[Sanction Date]"
                )
            )
            templateDao.insertAll(defaults)
        }
    }
}
