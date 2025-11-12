package com.teamup.app

import com.teamup.app.data.ChatGroup
import com.teamup.app.data.ChatMessage
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests unitaires pour la logique du chat
 * Ces tests vérifient la structure des données et la logique métier
 */
class ChatRepositoryTest {

    @Test
    fun chatMessage_creation_isCorrect() {
        // Arrange
        val messageId = "msg123"
        val groupId = "group456"
        val userId = "user789"
        val userName = "testuser"
        val content = "Hello World"
        val timestamp = System.currentTimeMillis()

        // Act
        val message = ChatMessage(
            messageId = messageId,
            groupId = groupId,
            userId = userId,
            userName = userName,
            content = content,
            timestamp = timestamp
        )

        // Assert
        assertEquals(messageId, message.messageId)
        assertEquals(groupId, message.groupId)
        assertEquals(userId, message.userId)
        assertEquals(userName, message.userName)
        assertEquals(content, message.content)
        assertEquals(timestamp, message.timestamp)
    }

    @Test
    fun chatMessage_emptyConstructor_createsValidObject() {
        // Act
        val message = ChatMessage()

        // Assert
        assertEquals("", message.messageId)
        assertEquals("", message.groupId)
        assertEquals("", message.userId)
        assertEquals("", message.userName)
        assertEquals("", message.content)
        assertTrue(message.timestamp >= 0)
    }

    @Test
    fun chatGroup_creation_isCorrect() {
        // Arrange
        val groupId = "group123"
        val groupName = "Test Group"
        val lastMessage = "Last message"
        val lastMessageTime = System.currentTimeMillis()
        val memberIds = listOf("user1", "user2", "user3")

        // Act
        val group = ChatGroup(
            groupId = groupId,
            groupName = groupName,
            lastMessage = lastMessage,
            lastMessageTime = lastMessageTime,
            memberIds = memberIds
        )

        // Assert
        assertEquals(groupId, group.groupId)
        assertEquals(groupName, group.groupName)
        assertEquals(lastMessage, group.lastMessage)
        assertEquals(lastMessageTime, group.lastMessageTime)
        assertEquals(3, group.memberIds.size)
        assertTrue(group.memberIds.contains("user1"))
        assertTrue(group.memberIds.contains("user2"))
        assertTrue(group.memberIds.contains("user3"))
    }

    @Test
    fun chatGroup_emptyConstructor_createsValidObject() {
        // Act
        val group = ChatGroup()

        // Assert
        assertEquals("", group.groupId)
        assertEquals("", group.groupName)
        assertEquals("", group.lastMessage)
        assertEquals(0L, group.lastMessageTime)
        assertTrue(group.memberIds.isEmpty())
    }

    @Test
    fun chatGroup_multipleMemberIds_areStored() {
        // Arrange
        val memberIds = listOf("user1", "user2", "user3", "user4", "user5")

        // Act
        val group = ChatGroup(
            groupId = "group1",
            groupName = "Multi-user Group",
            memberIds = memberIds
        )

        // Assert
        assertEquals(5, group.memberIds.size)
        assertEquals(memberIds, group.memberIds)
    }

    @Test
    fun chatMessage_userNameExtraction_fromEmail() {
        // Arrange
        val email = "testuser@example.com"

        // Act
        val userName = email.substringBefore("@")

        // Assert
        assertEquals("testuser", userName)
    }

    @Test
    fun chatMessage_userNameExtraction_withoutAtSymbol() {
        // Arrange
        val email = "testuser"

        // Act
        val userName = email.substringBefore("@")

        // Assert
        assertEquals("testuser", userName)
    }

    @Test
    fun chatGroup_sorting_byLastMessageTime() {
        // Arrange
        val group1 = ChatGroup(groupId = "1", groupName = "Group 1", lastMessageTime = 1000L)
        val group2 = ChatGroup(groupId = "2", groupName = "Group 2", lastMessageTime = 3000L)
        val group3 = ChatGroup(groupId = "3", groupName = "Group 3", lastMessageTime = 2000L)
        val groups = listOf(group1, group2, group3)

        // Act
        val sortedGroups = groups.sortedByDescending { it.lastMessageTime }

        // Assert
        assertEquals("2", sortedGroups[0].groupId)
        assertEquals("3", sortedGroups[1].groupId)
        assertEquals("1", sortedGroups[2].groupId)
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
    fun chatGroup_memberCheck_userIsMember() {
        // Arrange
        val userId = "user123"
        val group = ChatGroup(
            groupId = "group1",
            groupName = "Test",
            memberIds = listOf("user456", userId, "user789")
        )

        // Act
        val isMember = group.memberIds.contains(userId)

        // Assert
        assertTrue(isMember)
    }

    @Test
    fun chatGroup_memberCheck_userIsNotMember() {
        // Arrange
        val userId = "user999"
        val group = ChatGroup(
            groupId = "group1",
            groupName = "Test",
            memberIds = listOf("user456", "user123", "user789")
        )

        // Act
        val isMember = group.memberIds.contains(userId)

        // Assert
        assertFalse(isMember)
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
    fun chatGroup_addMember_simulation() {
        // Arrange
        val group = ChatGroup(
            groupId = "group1",
            groupName = "Test Group",
            memberIds = listOf("user1", "user2")
        )
        val newUserId = "user3"

        // Act
        val updatedMemberIds = group.memberIds + newUserId

        // Assert
        assertEquals(3, updatedMemberIds.size)
        assertTrue(updatedMemberIds.contains(newUserId))
        assertTrue(updatedMemberIds.contains("user1"))
        assertTrue(updatedMemberIds.contains("user2"))
    }

    @Test
    fun chatGroup_removeMember_simulation() {
        // Arrange
        val group = ChatGroup(
            groupId = "group1",
            groupName = "Test Group",
            memberIds = listOf("user1", "user2", "user3")
        )
        val userToRemove = "user2"

        // Act
        val updatedMemberIds = group.memberIds.filter { it != userToRemove }

        // Assert
        assertEquals(2, updatedMemberIds.size)
        assertFalse(updatedMemberIds.contains(userToRemove))
        assertTrue(updatedMemberIds.contains("user1"))
        assertTrue(updatedMemberIds.contains("user3"))
    }

    @Test
    fun chatMessage_multipleMessagesInGroup_correctGrouping() {
        // Arrange
        val groupId = "group1"
        val messages = listOf(
            ChatMessage(messageId = "1", groupId = groupId, content = "Msg 1"),
            ChatMessage(messageId = "2", groupId = groupId, content = "Msg 2"),
            ChatMessage(messageId = "3", groupId = "group2", content = "Msg 3"),
            ChatMessage(messageId = "4", groupId = groupId, content = "Msg 4")
        )

        // Act
        val groupMessages = messages.filter { it.groupId == groupId }

        // Assert
        assertEquals(3, groupMessages.size)
        assertEquals("Msg 1", groupMessages[0].content)
        assertEquals("Msg 2", groupMessages[1].content)
        assertEquals("Msg 4", groupMessages[2].content)
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
        assertTrue(timeDifference < 1000) // Less than 1 second
    }

    @Test
    fun chatGroup_lastMessageUpdate_simulation() {
        // Arrange
        val group = ChatGroup(
            groupId = "group1",
            groupName = "Test",
            lastMessage = "Old message",
            lastMessageTime = 1000L
        )
        val newMessage = "New message"
        val newTimestamp = 2000L

        // Act
        val updatedGroup = group.copy(
            lastMessage = newMessage,
            lastMessageTime = newTimestamp
        )

        // Assert
        assertEquals(newMessage, updatedGroup.lastMessage)
        assertEquals(newTimestamp, updatedGroup.lastMessageTime)
        assertEquals(group.groupId, updatedGroup.groupId)
        assertEquals(group.groupName, updatedGroup.groupName)
    }
}
