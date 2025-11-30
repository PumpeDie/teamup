package com.teamup.app

import com.teamup.app.ui.screens.tasks.Task
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitaires pour la data class Task
 */
class TaskTest {

    @Test
    fun `Task - constructeur par défaut`() {
        val task = Task()
        
        assertEquals("", task.id)
        assertEquals("", task.title)
        assertEquals("", task.dueDate)
        assertEquals(false, task.completed)
        assertNull(task.completedByUserName)
        assertEquals("", task.createdBy)
        assertEquals("", task.createdByName)
        assertNull(task.assignedTo)
        assertNull(task.assignedToName)
        assertTrue(task.createdAt > 0)
    }

    @Test
    fun `Task - constructeur avec paramètres complets`() {
        val taskId = "task123"
        val title = "Faire les courses"
        val dueDate = "2024-12-31"
        val completed = false
        val createdBy = "user1"
        val createdByName = "John Doe"
        val assignedTo = "user2"
        val assignedToName = "Jane Smith"
        val timestamp = 1234567890L
        
        val task = Task(
            id = taskId,
            title = title,
            dueDate = dueDate,
            completed = completed,
            completedByUserName = null,
            createdBy = createdBy,
            createdByName = createdByName,
            assignedTo = assignedTo,
            assignedToName = assignedToName,
            createdAt = timestamp
        )
        
        assertEquals(taskId, task.id)
        assertEquals(title, task.title)
        assertEquals(dueDate, task.dueDate)
        assertEquals(completed, task.completed)
        assertNull(task.completedByUserName)
        assertEquals(createdBy, task.createdBy)
        assertEquals(createdByName, task.createdByName)
        assertEquals(assignedTo, task.assignedTo)
        assertEquals(assignedToName, task.assignedToName)
        assertEquals(timestamp, task.createdAt)
    }

    @Test
    fun `Task - tâche complétée stocke le username`() {
        val completedBy = "UserTest"
        val task = Task(
            id = "task1",
            title = "Test task",
            completed = true,
            completedByUserName = completedBy
        )
        
        assertTrue(task.completed)
        assertEquals(completedBy, task.completedByUserName)
    }

    @Test
    fun `Task - dueDate peut être vide`() {
        val task = Task(
            id = "task1",
            title = "Task without due date",
            dueDate = ""
        )
        
        assertEquals("", task.dueDate)
    }

    @Test
    fun `Task - assignedTo peut être null`() {
        val task = Task(
            id = "task1",
            title = "Unassigned task",
            assignedTo = null,
            assignedToName = null
        )
        
        assertNull(task.assignedTo)
        assertNull(task.assignedToName)
    }

    @Test
    fun `Task - createdAt utilise timestamp actuel par défaut`() {
        val before = System.currentTimeMillis()
        val task = Task(
            id = "task1",
            title = "New task"
        )
        val after = System.currentTimeMillis()
        
        assertTrue(task.createdAt >= before)
        assertTrue(task.createdAt <= after)
    }

    @Test
    fun `Task - completedByUserName null quand non complétée`() {
        val task = Task(
            id = "task1",
            title = "Incomplete task",
            completed = false,
            completedByUserName = null
        )
        
        assertFalse(task.completed)
        assertNull(task.completedByUserName)
    }

    @Test
    fun `Task - peut changer état completion`() {
        val task = Task(
            id = "task1",
            title = "Task to complete",
            completed = false
        )
        
        assertFalse(task.completed)
        
        // Simuler la complétion
        val updatedTask = task.copy(
            completed = true,
            completedByUserName = "TestUser"
        )
        
        assertTrue(updatedTask.completed)
        assertEquals("TestUser", updatedTask.completedByUserName)
    }

    @Test
    fun `Task - createdBy stocke userId créateur`() {
        val userId = "user123"
        val userName = "TestCreator"
        val task = Task(
            id = "task1",
            title = "Created task",
            createdBy = userId,
            createdByName = userName
        )
        
        assertEquals(userId, task.createdBy)
        assertEquals(userName, task.createdByName)
    }
}
