package com.teamup.app

import com.teamup.app.util.isValidPassword
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitaires pour les validations (passwords, etc.)
 */
class ValidationTest {

    @Test
    fun `mot de passe valide - tous les critères respectés`() {
        val (valid, message) = isValidPassword("Azerty123!")
        assertTrue(message, valid)
    }

    @Test
    fun `mot de passe trop court - moins de 6 caractères`() {
        val (valid, message) = isValidPassword("Ab1!")
        assertFalse(valid)
        assertTrue(message.contains("6 caractères"))
    }

    @Test
    fun `mot de passe sans majuscule`() {
        val (valid, message) = isValidPassword("azerty123!")
        assertFalse(valid)
        assertTrue(message.contains("majuscule"))
    }

    @Test
    fun `mot de passe sans minuscule`() {
        val (valid, message) = isValidPassword("AZERTY123!")
        assertFalse(valid)
        assertTrue(message.contains("minuscule"))
    }

    @Test
    fun `mot de passe sans chiffre`() {
        val (valid, message) = isValidPassword("Azertyuiop!")
        assertFalse(valid)
        assertTrue(message.contains("chiffre"))
    }

    @Test
    fun `mot de passe sans caractère spécial`() {
        val (valid, message) = isValidPassword("Azerty1234")
        assertFalse(valid)
        assertTrue(message.contains("spécial"))
    }

    @Test
    fun `mot de passe vide`() {
        val (valid, message) = isValidPassword("")
        assertFalse(valid)
    }

    @Test
    fun `mot de passe avec espaces valide`() {
        val (valid, message) = isValidPassword("Mon Pass123!")
        assertTrue(message, valid)
    }
}
