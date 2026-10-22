package com.example.features.diagnostics

enum class DiagnosticSeverity {
    INFO,
    WARNING,
    ERROR
}

enum class DiagnosticStatus {
    PASSED,
    WARNING,
    FAILED,
    NOT_RUN
}

data class DiagnosticCheckResult(
    val checkName: String,
    val category: String,
    val status: DiagnosticStatus,
    val message: String,
    val details: String = ""
)

data class StructuredLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val severity: DiagnosticSeverity,
    val category: String,
    val summary: String,
    val details: String = "",
    val component: String = "App"
)
