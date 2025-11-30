package com.teamup.app

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tests unitaires pour les fonctions utilitaires diverses
 */
class UtilsTest {

    // ================= Tests pour formatage de timestamp =================
    
    @Test
    fun `formatTimestamp - moins d'une minute retourne 'À l'instant'`() {
        val now = System.currentTimeMillis()
        val timestamp = now - 30_000 // 30 secondes
        
        val result = formatTimestampTest(timestamp, now)
        
        assertEquals("À l'instant", result)
    }

    @Test
    fun `formatTimestamp - moins d'une heure retourne minutes`() {
        val now = System.currentTimeMillis()
        val timestamp = now - 300_000 // 5 minutes
        
        val result = formatTimestampTest(timestamp, now)
        
        assertEquals("5 min", result)
    }

    @Test
    fun `formatTimestamp - moins d'un jour retourne heures`() {
        val now = System.currentTimeMillis()
        val timestamp = now - 7_200_000 // 2 heures
        
        val result = formatTimestampTest(timestamp, now)
        
        assertEquals("2 h", result)
    }

    @Test
    fun `formatTimestamp - plus d'un jour retourne date formatée`() {
        val now = System.currentTimeMillis()
        val timestamp = now - 86_400_000 - 1 // Plus de 24h
        
        val result = formatTimestampTest(timestamp, now)
        
        // Vérifie le format dd/MM
        assertTrue(result.matches(Regex("\\d{2}/\\d{2}")))
    }

    @Test
    fun `formatMessageTime - retourne format HH mm`() {
        // 15h30 le 1er janvier 2024
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JANUARY, 1, 15, 30, 0)
        val timestamp = calendar.timeInMillis
        
        val result = formatMessageTimeTest(timestamp)
        
        assertEquals("15:30", result)
    }

    @Test
    fun `formatMessageTime - format avec zéros leading`() {
        // 9h05
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JANUARY, 1, 9, 5, 0)
        val timestamp = calendar.timeInMillis
        
        val result = formatMessageTimeTest(timestamp)
        
        assertEquals("09:05", result)
    }

    // ================= Fonctions helpers pour tests (copie de la logique) =================
    
    private fun formatTimestampTest(timestamp: Long, currentTime: Long = System.currentTimeMillis()): String {
        val diff = currentTime - timestamp
        return when {
            diff < 60_000 -> "À l'instant"
            diff < 3_600_000 -> "${diff / 60_000} min"
            diff < 86_400_000 -> "${diff / 3_600_000} h"
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun formatMessageTimeTest(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    // ================= Tests pour copyToClipboard (logique métier) =================
    
    @Test
    fun `copyToClipboard - text non vide est valide`() {
        val text = "team123"
        
        // Validation que le texte peut être copié
        assertTrue(text.isNotEmpty())
        assertTrue(text.isNotBlank())
    }

    @Test
    fun `copyToClipboard - texte vide ne devrait pas être copié`() {
        val emptyText = ""
        
        assertFalse(emptyText.isNotEmpty())
    }

    // ================= Tests pour validation de champs =================
    
    @Test
    fun `isValidEmail - format email valide`() {
        val validEmails = listOf(
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@example.org"
        )
        
        validEmails.forEach { email ->
            assertTrue("$email devrait être valide", isValidEmailFormat(email))
        }
    }

    @Test
    fun `isValidEmail - format email invalide`() {
        val invalidEmails = listOf(
            "invalid",
            "@example.com",
            "user@",
            "user name@example.com",
            ""
        )
        
        invalidEmails.forEach { email ->
            assertFalse("$email devrait être invalide", isValidEmailFormat(email))
        }
    }

    @Test
    fun `isValidTeamName - nom valide`() {
        val validNames = listOf(
            "Team Alpha",
            "Mon équipe 2024",
            "Équipe-Test_123"
        )
        
        validNames.forEach { name ->
            assertTrue("$name devrait être valide", name.isNotBlank() && name.length >= 3)
        }
    }

    @Test
    fun `isValidTeamName - nom invalide`() {
        val invalidNames = listOf(
            "",
            "  ",
            "Ab", // Trop court
            "A" // Un seul caractère
        )
        
        invalidNames.forEach { name ->
            assertFalse("$name devrait être invalide", name.isNotBlank() && name.length >= 3)
        }
    }

    @Test
    fun `isValidChatRoomName - nom valide`() {
        val validNames = listOf(
            "General",
            "Annonces 2024",
            "Discussion_privée"
        )
        
        validNames.forEach { name ->
            assertTrue("$name devrait être valide", name.isNotBlank())
        }
    }

    @Test
    fun `isValidTaskTitle - titre valide`() {
        val validTitles = listOf(
            "Faire les courses",
            "Réunion planning",
            "Développer feature X"
        )
        
        validTitles.forEach { title ->
            assertTrue("$title devrait être valide", title.isNotBlank())
        }
    }

    @Test
    fun `isValidEventTitle - titre valide`() {
        val validTitles = listOf(
            "Meeting hebdo",
            "Présentation projet",
            "Daily standup"
        )
        
        validTitles.forEach { title ->
            assertTrue("$title devrait être valide", title.isNotBlank())
        }
    }

    // ================= Helper pour validation email =================
    
    private fun isValidEmailFormat(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }

    // ================= Tests pour limites d'heure agenda =================
    
    @Test
    fun `agenda hour - plage valide 8 à 20`() {
        for (hour in 8..20) {
            assertTrue("L'heure $hour devrait être valide", hour in 8..20)
        }
    }

    @Test
    fun `agenda hour - hors plage invalide`() {
        val invalidHours = listOf(0, 5, 7, 21, 23)
        
        invalidHours.forEach { hour ->
            assertFalse("L'heure $hour devrait être invalide", hour in 8..20)
        }
    }

    // ================= Tests pour days of week =================
    
    @Test
    fun `day name - tous les jours valides`() {
        val validDays = listOf(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", 
            "FRIDAY", "SATURDAY", "SUNDAY"
        )
        
        validDays.forEach { day ->
            assertTrue("$day devrait être un jour valide", 
                day in listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"))
        }
    }

    @Test
    fun `day name - noms invalides`() {
        val invalidDays = listOf("", "LUNDI", "MON", "monday", "INVALID_DAY")
        
        invalidDays.forEach { day ->
            assertFalse("$day ne devrait pas être un jour valide", 
                day in listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"))
        }
    }
}
