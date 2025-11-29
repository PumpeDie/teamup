package com.teamup.app

import com.teamup.app.data.ChatMessage
import com.teamup.app.data.ChatRoom
import com.teamup.app.data.TeamGroup
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests unitaires pour la logique du chat et des groupes
 * TeamGroup contient des ChatRooms qui contiennent des ChatMessages
 */
class ChatRepositoryTest {

    // ============ Tests ChatMessage ============
    
    @Test
    fun chatMessage_creation_isCorrect() {
        // Arrange
        val messageId = "msg123"
        val chatRoomId = "room456"
        val userId = "user789"
        val userName = "testuser"
        val content = "Hello World"
        val timestamp = System.currentTimeMillis()

        // Act
        val message = ChatMessage(
            messageId = messageId,
            chatRoomId = chatRoomId,
            userId = userId,
            userName = userName,
            content = content,
            timestamp = timestamp
        )

        // Assert
        assertEquals(messageId, message.messageId)
        assertEquals(chatRoomId, message.chatRoomId)
        assertEquals(userId, message.userId)
        assertEquals(userName, message.userName)
        assertEquals(content, message.content)
        assertEquals(timestamp, message.timestamp)
    }

    @Test
    fun chatMessage_sorting_byTimestamp() {
        // Arrange
        val msg1 = ChatMessage(messageId = "1", content = "First", timestamp = 1000L)
        val msg2 = ChatMessage(messageId = "2", content = "Second", timestamp = 2000L)
        val msg3 = ChatMessage(messageId = "3", content = "Third", timestamp = 1500L)
        val messages = listOf(msg1, msg2, msg3)

        // Act
        val sortedMessages = messages.sortedBy { it.timestamp }

        // Assert
        assertEquals("1", sortedMessages[0].messageId)
        assertEquals("3", sortedMessages[1].messageId)
        assertEquals("2", sortedMessages[2].messageId)
    }

    @Test
    fun chatMessage_contentValidation_notEmpty() {
        // Arrange & Act
        val message = ChatMessage(messageId = "1", content = "Hello")

        // Assert
        assertTrue(message.content.isNotBlank())
    }

    @Test
    fun chatMessage_contentValidation_isEmpty() {
        // Arrange & Act
        val message = ChatMessage(messageId = "1", content = "")

        // Assert
        assertTrue(message.content.isBlank())
    }

    @Test
    fun chatMessage_timestampIsRecent() {
        // Arrange
        val currentTime = System.currentTimeMillis()
        
        // Act
        val message = ChatMessage(
            messageId = "1",
            content = "Test",
            timestamp = currentTime
        )

        // Assert
        val timeDifference = System.currentTimeMillis() - message.timestamp
        assertTrue(timeDifference < 1000) // Moins d'une seconde
    }

    @Test
    fun chatMessage_multipleMessagesInRoom_correctGrouping() {
        // Arrange
        val roomId = "room1"
        val messages = listOf(
            ChatMessage(messageId = "1", chatRoomId = roomId, content = "Msg 1"),
            ChatMessage(messageId = "2", chatRoomId = roomId, content = "Msg 2"),
            ChatMessage(messageId = "3", chatRoomId = "room2", content = "Msg 3"),
            ChatMessage(messageId = "4", chatRoomId = roomId, content = "Msg 4")
        )

        // Act
        val roomMessages = messages.filter { it.chatRoomId == roomId }

        // Assert
        assertEquals(3, roomMessages.size)
        assertEquals("Msg 1", roomMessages[0].content)
        assertEquals("Msg 2", roomMessages[1].content)
        assertEquals("Msg 4", roomMessages[2].content)
    }

    // ============ Tests ChatRoom ============
    
    @Test
    fun chatRoom_creation_isCorrect() {
        // Arrange
        val roomId = "room123"
        val teamId = "team456"
        val name = "General"
        val lastMessage = "Last message"
        val lastMessageTime = System.currentTimeMillis()

        // Act
        val room = ChatRoom(
            chatRoomId = roomId,
            teamId = teamId,
            chatName = name,
            lastMessage = lastMessage,
            lastMessageTime = lastMessageTime
        )

        // Assert
        assertEquals(roomId, room.chatRoomId)
        assertEquals(teamId, room.teamId)
        assertEquals(name, room.chatName)
        assertEquals(lastMessage, room.lastMessage)
        assertEquals(lastMessageTime, room.lastMessageTime)
    }

    @Test
    fun chatRoom_sorting_byLastMessageTime() {
        // Arrange
        val room1 = ChatRoom(chatRoomId = "1", chatName = "Room 1", lastMessageTime = 1000L)
        val room2 = ChatRoom(chatRoomId = "2", chatName = "Room 2", lastMessageTime = 3000L)
        val room3 = ChatRoom(chatRoomId = "3", chatName = "Room 3", lastMessageTime = 2000L)
        val rooms = listOf(room1, room2, room3)

        // Act
        val sortedRooms = rooms.sortedByDescending { it.lastMessageTime }

        // Assert
        assertEquals("2", sortedRooms[0].chatRoomId)
        assertEquals("3", sortedRooms[1].chatRoomId)
        assertEquals("1", sortedRooms[2].chatRoomId)
    }

    @Test
    fun chatRoom_lastMessageUpdate_simulation() {
        // Arrange
        val room = ChatRoom(
            chatRoomId = "room1",
            teamId = "team1",
            chatName = "Test",
            lastMessage = "Old message",
            lastMessageTime = 1000L
        )
        val newMessage = "New message"
        val newTimestamp = 2000L

        // Act
        val updatedRoom = room.copy(
            lastMessage = newMessage,
            lastMessageTime = newTimestamp
        )

        // Assert
        assertEquals(newMessage, updatedRoom.lastMessage)
        assertEquals(newTimestamp, updatedRoom.lastMessageTime)
        assertEquals(room.chatRoomId, updatedRoom.chatRoomId)
        assertEquals(room.chatName, updatedRoom.chatName)
    }

    // ============ Tests TeamGroup ============
    
    @Test
    fun teamGroup_creation_withRoles() {
        // Arrange
        val teamId = "team123"
        val teamName = "Test Team"
        val creatorId = "user1"
        val adminIds = listOf("user1", "user2")
        val memberIds = listOf("user1", "user2", "user3")

        // Act
        val team = TeamGroup(
            teamId = teamId,
            teamName = teamName,
            creatorId = creatorId,
            adminIds = adminIds,
            memberIds = memberIds
        )

        // Assert
        assertEquals(teamId, team.teamId)
        assertEquals(teamName, team.teamName)
        assertEquals(creatorId, team.creatorId)
        assertEquals(2, team.adminIds.size)
        assertEquals(3, team.memberIds.size)
    }

    @Test
    fun teamGroup_memberCheck_userIsMember() {
        // Arrange
        val userId = "user123"
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Test",
            creatorId = "user1",
            memberIds = listOf("user456", userId, "user789")
        )

        // Act
        val isMember = team.memberIds.contains(userId)

        // Assert
        assertTrue(isMember)
    }

    @Test
    fun teamGroup_memberCheck_userIsNotMember() {
        // Arrange
        val userId = "user999"
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Test",
            creatorId = "user1",
            memberIds = listOf("user456", "user123", "user789")
        )

        // Act
        val isMember = team.memberIds.contains(userId)

        // Assert
        assertFalse(isMember)
    }

    @Test
    fun teamGroup_addMember_simulation() {
        // Arrange
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Test Team",
            creatorId = "user1",
            memberIds = listOf("user1", "user2")
        )
        val newUserId = "user3"

        // Act
        val updatedMemberIds = team.memberIds + newUserId

        // Assert
        assertEquals(3, updatedMemberIds.size)
        assertTrue(updatedMemberIds.contains(newUserId))
        assertTrue(updatedMemberIds.contains("user1"))
        assertTrue(updatedMemberIds.contains("user2"))
    }

    @Test
    fun teamGroup_removeMember_simulation() {
        // Arrange
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Test Team",
            creatorId = "user1",
            memberIds = listOf("user1", "user2", "user3")
        )
        val userToRemove = "user2"

        // Act
        val updatedMemberIds = team.memberIds.filter { it != userToRemove }

        // Assert
        assertEquals(2, updatedMemberIds.size)
        assertFalse(updatedMemberIds.contains(userToRemove))
        assertTrue(updatedMemberIds.contains("user1"))
        assertTrue(updatedMemberIds.contains("user3"))
    }

    @Test
    fun teamGroup_adminCheck_userIsAdmin() {
        // Arrange
        val userId = "user2"
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Test",
            creatorId = "user1",
            adminIds = listOf("user1", userId),
            memberIds = listOf("user1", userId, "user3")
        )

        // Act
        val isAdmin = team.adminIds.contains(userId)

        // Assert
        assertTrue(isAdmin)
    }

    @Test
    fun teamGroup_creatorIsAlwaysAdmin() {
        // Arrange
        val creatorId = "user1"
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Test",
            creatorId = creatorId,
            adminIds = listOf(creatorId),
            memberIds = listOf(creatorId, "user2", "user3")
        )

        // Assert
        assertTrue(team.adminIds.contains(creatorId))
        assertEquals(creatorId, team.creatorId)
    }

    // ============ Tests utilitaires ============
    
    @Test
    fun userName_extraction_fromEmail() {
        // Arrange
        val email = "testuser@example.com"

        // Act
        val userName = email.substringBefore("@")

        // Assert
        assertEquals("testuser", userName)
    }

    @Test
    fun userName_extraction_withoutAtSymbol() {
        // Arrange
        val email = "testuser"

        // Act
        val userName = email.substringBefore("@")

        // Assert
        assertEquals("testuser", userName)
    }
}
