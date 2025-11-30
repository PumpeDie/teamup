package com.teamup.app

import com.teamup.app.data.ChatMessage
import com.teamup.app.data.ChatRoom
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitaires pour les data classes ChatMessage et ChatRoom
 */
class ChatModelsTest {

    @Test
    fun `ChatRoom - constructeur par défaut`() {
        val room = ChatRoom()
        
        assertEquals("", room.chatRoomId)
        assertEquals("", room.teamId)
        assertEquals("", room.chatName)
        assertEquals("", room.lastMessage)
        assertEquals(0L, room.lastMessageTime)
    }

    @Test
    fun `ChatRoom - constructeur avec paramètres`() {
        val roomId = "room123"
        val teamId = "team456"
        val name = "General"
        val lastMsg = "Hello world"
        val timestamp = 1234567890L
        
        val room = ChatRoom(
            chatRoomId = roomId,
            teamId = teamId,
            chatName = name,
            lastMessage = lastMsg,
            lastMessageTime = timestamp
        )
        
        assertEquals(roomId, room.chatRoomId)
        assertEquals(teamId, room.teamId)
        assertEquals(name, room.chatName)
        assertEquals(lastMsg, room.lastMessage)
        assertEquals(timestamp, room.lastMessageTime)
    }

    @Test
    fun `ChatMessage - constructeur par défaut`() {
        val message = ChatMessage()
        
        assertEquals("", message.messageId)
        assertEquals("", message.chatRoomId)
        assertEquals("", message.userId)
        assertEquals("", message.userName)
        assertEquals("", message.content)
        assertEquals(0L, message.timestamp)
    }

    @Test
    fun `ChatMessage - constructeur avec paramètres`() {
        val msgId = "msg123"
        val roomId = "room456"
        val userId = "user789"
        val userName = "TestUser"
        val content = "Test message"
        val timestamp = 1234567890L
        
        val message = ChatMessage(
            messageId = msgId,
            chatRoomId = roomId,
            userId = userId,
            userName = userName,
            content = content,
            timestamp = timestamp
        )
        
        assertEquals(msgId, message.messageId)
        assertEquals(roomId, message.chatRoomId)
        assertEquals(userId, message.userId)
        assertEquals(userName, message.userName)
        assertEquals(content, message.content)
        assertEquals(timestamp, message.timestamp)
    }

    @Test
    fun `ChatMessage - timestamp par défaut utilise System currentTimeMillis`() {
        val before = System.currentTimeMillis()
        val message = ChatMessage(
            messageId = "msg1",
            chatRoomId = "room1",
            userId = "user1",
            userName = "User",
            content = "Test"
        )
        val after = System.currentTimeMillis()
        
        // Le timestamp devrait être entre before et after
        assertTrue(message.timestamp >= before)
        assertTrue(message.timestamp <= after)
    }

    @Test
    fun `ChatRoom - lastMessageTime peut être 0 pour nouveau salon`() {
        val room = ChatRoom(
            chatRoomId = "room1",
            teamId = "team1",
            chatName = "New Room",
            lastMessage = "",
            lastMessageTime = 0L
        )
        
        assertEquals(0L, room.lastMessageTime)
    }

    @Test
    fun `ChatMessage - content peut être vide`() {
        val message = ChatMessage(
            messageId = "msg1",
            chatRoomId = "room1",
            userId = "user1",
            userName = "User",
            content = "",
            timestamp = 1234567890L
        )
        
        assertEquals("", message.content)
    }

    @Test
    fun `ChatMessage - userName stocke le username utilisateur`() {
        val username = "CoolUser123"
        val message = ChatMessage(
            messageId = "msg1",
            chatRoomId = "room1",
            userId = "user1",
            userName = username,
            content = "Hello",
            timestamp = 1234567890L
        )
        
        assertEquals(username, message.userName)
    }
}
