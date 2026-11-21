package com.example.myapplication.ui.login

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidatorTest {
    @Test
    fun `accepts common addresses`() {
        listOf("a@b.co", "first.last+tag@sub.example.com", " user@example.com ").forEach {
            assertTrue(it, EmailValidator.isValid(it))
        }
    }

    @Test
    fun `rejects malformed addresses`() {
        listOf("", "user", "user@", "@example.com", "user@example", "user@@example.com", "us er@example.com").forEach {
            assertFalse(it, EmailValidator.isValid(it))
        }
    }
}
