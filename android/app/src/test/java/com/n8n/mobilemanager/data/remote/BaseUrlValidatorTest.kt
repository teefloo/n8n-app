package com.n8n.mobilemanager.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseUrlValidatorTest {

    @Test
    fun `adds https and removes trailing slash`() {
        val result = normalizeN8nBaseUrl(" n8n.example.com/// ")

        assertTrue(result.isSuccess)
        assertEquals("https://n8n.example.com", result.getOrNull())
    }

    @Test
    fun `accepts local http instances`() {
        val result = normalizeN8nBaseUrl("http://10.0.2.2:5678/")

        assertTrue(result.isSuccess)
        assertEquals("http://10.0.2.2:5678", result.getOrNull())
    }

    @Test
    fun `rejects unsupported scheme`() {
        val result = normalizeN8nBaseUrl("ftp://n8n.example.com")

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects query parameters`() {
        val result = normalizeN8nBaseUrl("https://n8n.example.com?token=secret")

        assertTrue(result.isFailure)
    }
}
