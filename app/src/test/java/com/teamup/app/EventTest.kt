package com.teamup.app

import android.os.Build
import androidx.annotation.RequiresApi
import com.teamup.app.ui.screens.agenda.Event
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek

/**
 * Tests unitaires pour la data class Event
 */
class EventTest {

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun `Event - constructeur par défaut`() {
        val event = Event()
        
        assertEquals("", event.id)
        assertEquals("", event.title)
        assertEquals(DayOfWeek.MONDAY.name, event.day)
        assertEquals(8, event.hour)
        assertEquals("", event.createdBy)
        assertEquals("", event.createdByName)
        assertTrue(event.createdAt > 0)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun `Event - constructeur avec paramètres`() {
        val eventId = "event123"
        val title = "Réunion d'équipe"
        val day = DayOfWeek.WEDNESDAY.name
        val hour = 14
        val createdBy = "user1"
        val createdByName = "John Doe"
        val timestamp = 1234567890L
        
        val event = Event(
            id = eventId,
            title = title,
            day = day,
            hour = hour,
            createdBy = createdBy,
            createdByName = createdByName,
            createdAt = timestamp
        )
        
        assertEquals(eventId, event.id)
        assertEquals(title, event.title)
        assertEquals(day, event.day)
        assertEquals(hour, event.hour)
        assertEquals(createdBy, event.createdBy)
        assertEquals(createdByName, event.createdByName)
        assertEquals(timestamp, event.createdAt)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun `Event - heure par défaut est 8`() {
        val event = Event(
            id = "event1",
            title = "Morning event"
        )
        
        assertEquals(8, event.hour)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun `Event - jour par défaut est MONDAY`() {
        val event = Event(
            id = "event1",
            title = "Monday event"
        )
        
        assertEquals(DayOfWeek.MONDAY.name, event.day)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun `Event - accepte toutes les heures valides (8-20)`() {
        for (hour in 8..20) {
            val event = Event(
                id = "event$hour",
                title = "Event at $hour",
                hour = hour
            )
            
            assertEquals(hour, event.hour)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun `Event - accepte tous les jours de la semaine`() {
        DayOfWeek.values().forEach { dayOfWeek ->
            val event = Event(
                id = "event-${dayOfWeek.name}",
                title = "Event on ${dayOfWeek.name}",
                day = dayOfWeek.name
            )
            
            assertEquals(dayOfWeek.name, event.day)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun `Event - createdAt utilise timestamp actuel par défaut`() {
        val before = System.currentTimeMillis()
        val event = Event(
            id = "event1",
            title = "New event"
        )
        val after = System.currentTimeMillis()
        
        assertTrue(event.createdAt >= before)
        assertTrue(event.createdAt <= after)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun `Event - createdBy stocke userId créateur`() {
        val userId = "user123"
        val userName = "TestCreator"
        val event = Event(
            id = "event1",
            title = "Created event",
            createdBy = userId,
            createdByName = userName
        )
        
        assertEquals(userId, event.createdBy)
        assertEquals(userName, event.createdByName)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun `Event - title peut être modifié`() {
        val event = Event(
            id = "event1",
            title = "Original title"
        )
        
        val updatedEvent = event.copy(title = "Updated title")
        
        assertEquals("Updated title", updatedEvent.title)
        assertEquals("Original title", event.title) // Original non modifié
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun `Event - peut être déplacé à un autre jour et heure`() {
        val event = Event(
            id = "event1",
            title = "Movable event",
            day = DayOfWeek.MONDAY.name,
            hour = 10
        )
        
        val movedEvent = event.copy(
            day = DayOfWeek.FRIDAY.name,
            hour = 15
        )
        
        assertEquals(DayOfWeek.FRIDAY.name, movedEvent.day)
        assertEquals(15, movedEvent.hour)
    }
}
