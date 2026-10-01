package com.example.data.model

data class DocumentAnalysisResult(
    val sender: String = "",
    val recipient: String = "",
    val letterNumber: String = "",
    val date: String = "",
    val subject: String = "",
    val previousReferences: List<String> = emptyList(),
    val mainIssue: String = "",
    val importantFacts: List<String> = emptyList(),
    val requestsOrInstructions: List<String> = emptyList(),
    val deadlines: List<String> = emptyList(),
    val quantities: List<String> = emptyList(),
    val financialFigures: List<String> = emptyList(),
    val equipmentOrVehicleNumbers: List<String> = emptyList(),
    val workOrderNumbers: List<String> = emptyList(),
    val annexures: List<String> = emptyList(),
    val actionRequiredFromRecipient: String = "",
    val rawSummary: String = "",
    val hasUnclearOcrWarning: Boolean = false,
    val ocrWarningMessage: String? = null
) {
    fun toFormattedSummary(): String {
        val sb = StringBuilder()
        if (sender.isNotBlank()) sb.append("• From: $sender\n")
        if (recipient.isNotBlank()) sb.append("• To: $recipient\n")
        if (letterNumber.isNotBlank()) sb.append("• Ref/Letter No: $letterNumber\n")
        if (date.isNotBlank()) sb.append("• Date: $date\n")
        if (subject.isNotBlank()) sb.append("• Subject: $subject\n")
        if (previousReferences.isNotEmpty()) sb.append("• References: ${previousReferences.joinToString(", ")}\n")
        if (mainIssue.isNotBlank()) sb.append("\nMain Issue:\n$mainIssue\n")
        if (requestsOrInstructions.isNotEmpty()) {
            sb.append("\nRequests / Instructions:\n")
            requestsOrInstructions.forEach { sb.append(" - $it\n") }
        }
        if (actionRequiredFromRecipient.isNotBlank()) {
            sb.append("\nAction Required:\n$actionRequiredFromRecipient\n")
        }
        if (deadlines.isNotEmpty()) {
            sb.append("\nDeadline: ${deadlines.joinToString("; ")}\n")
        }
        if (quantities.isNotEmpty() || financialFigures.isNotEmpty() || equipmentOrVehicleNumbers.isNotEmpty()) {
            sb.append("\nKey Figures / Equipment:\n")
            if (quantities.isNotEmpty()) sb.append(" - Quantities: ${quantities.joinToString(", ")}\n")
            if (financialFigures.isNotEmpty()) sb.append(" - Amounts: ${financialFigures.joinToString(", ")}\n")
            if (equipmentOrVehicleNumbers.isNotEmpty()) sb.append(" - Equipment/Vehicle Nos: ${equipmentOrVehicleNumbers.joinToString(", ")}\n")
        }
        return sb.toString().trim()
    }
}

data class FactCheckDiscrepancy(
    val category: String, // "Reference No", "Date", "Quantity", "Amount", "Name", "Deadline"
    val sourceValue: String,
    val replyValue: String,
    val isVerified: Boolean,
    val remarks: String
)

data class FactCheckResult(
    val isConsistent: Boolean,
    val scorePercent: Int,
    val discrepancies: List<FactCheckDiscrepancy> = emptyList(),
    val summaryReport: String
)
