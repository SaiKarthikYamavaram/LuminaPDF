package com.lumina.pdf.renderer

import com.lumina.pdf.core.PdfPasswordRequiredException
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class PdfDocumentFactoryPasswordTest {

    private fun encryptedPdfBytes(userPassword: String, ownerPassword: String = "owner-$userPassword"): ByteArray {
        PDDocument().use { document ->
            document.addPage(PDPage())
            val policy = StandardProtectionPolicy(ownerPassword, userPassword, AccessPermission())
            policy.encryptionKeyLength = 128
            document.protect(policy)
            val out = ByteArrayOutputStream()
            document.save(out)
            return out.toByteArray()
        }
    }

    @Test
    fun openWithoutPasswordThrowsNotWrongPasswordTried() = runBlocking {
        val bytes = encryptedPdfBytes(userPassword = "hunter2")
        val factory = PdfDocumentFactory()
        try {
            factory.open(bytes)
            fail("Expected PdfPasswordRequiredException")
        } catch (e: PdfPasswordRequiredException) {
            assertFalse(e.wrongPasswordTried)
        }
    }

    @Test
    fun openWithWrongPasswordThrowsWrongPasswordTried() = runBlocking {
        val bytes = encryptedPdfBytes(userPassword = "hunter2")
        val factory = PdfDocumentFactory()
        try {
            factory.open(bytes, password = "not-it")
            fail("Expected PdfPasswordRequiredException")
        } catch (e: PdfPasswordRequiredException) {
            assertTrue(e.wrongPasswordTried)
        }
    }

    @Test
    fun openWithCorrectPasswordSucceeds() = runBlocking {
        val bytes = encryptedPdfBytes(userPassword = "hunter2")
        val factory = PdfDocumentFactory()
        val document = factory.open(bytes, password = "hunter2")
        assertTrue(document.pageCount() >= 1)
        document.close()
    }

    @Test
    fun openUnencryptedPdfWithNoPasswordSucceeds() = runBlocking {
        val out = ByteArrayOutputStream()
        PDDocument().use { document ->
            document.addPage(PDPage())
            document.save(out)
        }
        val factory = PdfDocumentFactory()
        val document = factory.open(out.toByteArray())
        assertTrue(document.pageCount() >= 1)
        document.close()
    }
}
