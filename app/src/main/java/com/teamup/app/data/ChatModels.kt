package com.teamup.app.data

// ============================================================================
// DATA CLASSES - Modèles de données pour l'application TeamUp
// ============================================================================

/**
 * Représente un groupe d'équipe (TeamGroup)
 */
data class TeamGroup(
    val teamId: String = "",
    val teamName: String = "",
    val creatorId: String = "",
    val adminIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList()
) {
    constructor() : this("", "", "", emptyList(), emptyList())
}

/**
 * Représente un salon de chat dans une équipe
 */
data class ChatRoom(
    val chatRoomId: String = "",
    val teamId: String = "",
    val chatName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L
) {
    constructor() : this("", "", "", "", 0L)
}

/**
 * Représente un message dans un salon de chat
 */
data class ChatMessage(
    val messageId: String = "",
    val chatRoomId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", 0L)
}