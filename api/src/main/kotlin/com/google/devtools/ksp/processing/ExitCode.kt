package com.google.devtools.ksp.processing

enum class ExitCode(code: Int) {
    OK(0),

    // Whenever there are some error messages.
    PROCESSING_ERROR(1),

    // Let exceptions pop through to the caller. Don't catch and convert them to, e.g., INTERNAL_ERROR.
}
