package com.example.data.model

enum class WritingMode(
    val displayName: String,
    val description: String,
    val promptInstruction: String = ""
) {
    OFFICIAL(
        "Official",
        "Standard government and administrative style",
        "Adopt standard departmental administrative tone, passive third-person formal constructions ('It is requested that...', 'With reference to...'), and strict hierarchical protocol."
    ),
    FORMAL(
        "Formal",
        "Polite, elevated office correspondence",
        "Use courteous, polished, elevated formal business and inter-departmental vocabulary, expressing mutual institutional respect."
    ),
    VERY_FORMAL(
        "Very Formal",
        "Strict protocol, high-ranking dignitary & ministerial tone",
        "Adhere to highest governmental and diplomatic protocol, ministerial deference, ceremonial openings and closings ('I have the honour to submit...', 'With highest regards')."
    ),
    CONCISE(
        "Concise",
        "Direct, brief, action-oriented",
        "Be direct, crisp, and to the point. Eliminate introductory preamble while preserving all dates, reference numbers, quantities, and specific directives."
    ),
    DETAILED(
        "Detailed",
        "Comprehensive with exhaustive facts & context",
        "Provide an exhaustive, in-depth explanation covering all background context, prior correspondences, statutory rules, and itemized arguments."
    ),
    TECHNICAL(
        "Technical",
        "Engineering, specification & technical precision",
        "Emphasize precise engineering specifications, technical parameters, model numbers, standards compliance, and quantitative criteria."
    ),
    REQUEST(
        "Request",
        "Official sanction, approval or supply requisition",
        "Draft as a formal requisition/request highlighting administrative urgency, necessity, justification, and specific sanction sought."
    ),
    REMINDER(
        "Reminder",
        "Follow-up on pending letters and overdue actions",
        "Draft as an urgent departmental reminder citing previous unanswered dispatches, elapsed timeframes, and soliciting immediate compliance."
    ),
    FOLLOW_UP(
        "Follow-up",
        "Status update and tracking of prior decisions",
        "Track past directives, provide milestone status updates, and articulate next actionable steps."
    ),
    COMPLAINT(
        "Complaint",
        "Official grievance, deficiency or breach notification",
        "Formulate a stern, formal notice detailing specific contractual deficiencies, lapses, violations, or grievances requiring rectification."
    ),
    REPLY(
        "Reply",
        "Standard point-by-point response to incoming letter",
        "Formulate a structured point-by-point response addressing every inquiry, request, or directive from the incoming letter in order."
    ),
    FORWARDING(
        "Forwarding Letter",
        "Enclosure transmission and departmental routing",
        "Draft an official forwarding letter (covering memo) transmitting attached documents, summaries, and action instructions to higher authority."
    ),
    CLARIFICATION(
        "Clarification",
        "Explaining ambiguity, procedures or policy query",
        "Provide thorough, unambiguous clarification resolving specific questions, policy interpretations, or conflicting references."
    ),
    JUSTIFICATION(
        "Justification",
        "Budgetary, operational or technical defense",
        "Articulate comprehensive justification demonstrating financial necessity, technical viability, and public/administrative urgency."
    )
}

enum class OutputLanguage(val displayName: String, val code: String) {
    ENGLISH("English", "en"),
    BENGALI("Bengali (বাংলা)", "bn"),
    HINDI("Hindi (हिन्दी)", "hi")
}

enum class AiProviderType(val displayName: String, val isLocal: Boolean) {
    GEMINI("Google Gemini (Cloud AI)", false),
    OLLAMA("Ollama (Local / Private AI)", true),
    OPENAI_COMPATIBLE("OpenAI-Compatible (Custom Server)", false),
    OFFLINE_ENGINE("Built-in Office Intelligence (Offline)", true)
}

enum class InlineAction(val label: String, val promptInstruction: String) {
    IMPROVE("Improve", "Improve the flow and professionalism of this text"),
    REWRITE("Rewrite", "Rewrite this section cleanly"),
    MAKE_FORMAL("Make Formal", "Elevate to formal office style"),
    MAKE_OFFICIAL("Make More Official", "Use standard departmental and administrative terminology"),
    MAKE_CONCISE("Make Concise", "Remove fluff while retaining all facts and figures"),
    EXPAND("Expand", "Elaborate with professional context and polite phrasing"),
    CORRECT_GRAMMAR("Correct Grammar", "Fix grammar, spelling and punctuation without changing facts"),
    SIMPLIFY("Simplify", "Make clear, direct and easy to comprehend"),
    TRANSLATE("Translate", "Translate accurately preserving formal tone"),
    CHECK_FACTS("Check Facts", "Verify against source facts")
}
