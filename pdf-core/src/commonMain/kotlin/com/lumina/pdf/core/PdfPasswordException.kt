package com.lumina.pdf.core

/**
 * Thrown by [com.lumina.pdf.renderer.PdfDocumentFactory.open] when a PDF is password-protected
 * and either no password, or an incorrect one, was supplied.
 */
class PdfPasswordRequiredException(val wrongPasswordTried: Boolean = false) : Exception(
    if (wrongPasswordTried) "Incorrect password" else "This PDF is password protected"
)

/** Thrown when a password-protected PDF is opened on a platform/OS version with no way to unlock it. */
class PdfPasswordUnsupportedException : Exception(
    "This PDF is password protected, which isn't supported on this device yet"
)
