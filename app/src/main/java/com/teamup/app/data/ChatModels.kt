package com.teamup.app.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


data class TeamGroup(
    val teamId: String = "",
    val teamName: String = "",
    val memberIds: List<String> = emptyList()
) {
    constructor() : this("", "", emptyList())
}


data class ChatRoom(
    val chatRoomId: String = "",
    val teamId: String = "",
    val chatName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L
) {
    constructor() : this("", "", "", "", 0L)
}


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




object ChatRepository {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val teamsRef = database.getReference("teams")





    /**
     * Récupère l'ID du TeamGroup de l'utilisateur connecté.
     */
    suspend fun getUserTeamId(): String? {
        val team = getUserTeam()
        return team?.teamId
    }

    /**
     * Récupère le TeamGroup complet de l'utilisateur connecté.
     */
    suspend fun getUserTeam(): TeamGroup? {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.e("ChatRepository", "getUserTeam: User not logged in")
            return null
        }

        return try {
            Log.d("ChatRepository", "getUserTeam: Fetching teams for user $userId")
            val snapshot = teamsRef.get().await()

            if (!snapshot.exists()) {
                Log.d("ChatRepository", "getUserTeam: No teams exist in database")
                return null
            }

            val team = snapshot.children.mapNotNull {
                try {
                    it.getValue(TeamGroup::class.java)
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Error parsing team: ${e.message}")
                    null
                }
            }.find { team ->
                team.memberIds.contains(userId)
            }

            if (team != null) {
                Log.d("ChatRepository", "getUserTeam: Found team ${team.teamId} (${team.teamName})")
            } else {
                Log.d("ChatRepository", "getUserTeam: No team found for user")
            }

            team
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error in getUserTeam: ${e.message}", e)
            null
        }
    }

    /**
     * Crée un nouveau TeamGroup (Groupe de Connexion).
     */
    suspend fun createTeam(teamName: String): Result<String> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))

            val teamId = teamsRef.push().key
                ?: return Result.failure(Exception("Failed to generate ID"))

            val team = TeamGroup(
                teamId = teamId,
                teamName = teamName,
                memberIds = listOf(user.uid)
            )

            Log.d("ChatRepository", "Creating team: $teamId with name: $teamName")
            teamsRef.child(teamId).setValue(team).await()
            Log.d("ChatRepository", "Team created successfully")

            Result.success(teamId)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating team: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Permet à l'utilisateur actuel de rejoindre un TeamGroup (Groupe de Connexion) par son ID.
     */
    suspend fun joinTeam(teamId: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
        val userId = user.uid
        val teamRef = teamsRef.child(teamId)

        return try {
            Log.d("ChatRepository", "Attempting to join team: $teamId")
            val snapshot = teamRef.get().await()

            if (!snapshot.exists()) {
                Log.e("ChatRepository", "Team does not exist: $teamId")
                return Result.failure(Exception("Le TeamGroup n'existe pas."))
            }

            val team = snapshot.getValue(TeamGroup::class.java)

            if (team == null) {
                Log.e("ChatRepository", "Failed to parse team data")
                return Result.failure(Exception("Impossible de lire les données du TeamGroup."))
            }

            if (team.memberIds.contains(userId)) {
                Log.d("ChatRepository", "User already member of team")
                return Result.success(Unit)
            }

            val updatedMembers = team.memberIds + userId
            teamRef.child("memberIds").setValue(updatedMembers).await()
            Log.d("ChatRepository", "Successfully joined team")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("ChatRepository", "Error joining team: ${e.message}", e)
            Result.failure(e)
        }
    }


    fun getTeamChatRooms(teamId: String): Flow<List<ChatRoom>> = callbackFlow {
        val chatRoomsRef = teamsRef.child(teamId).child("chatRooms")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatRooms = snapshot.children.mapNotNull {
                    try {
                        it.getValue(ChatRoom::class.java)
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Error parsing chat room: ${e.message}")
                        null
                    }
                }


                trySend(chatRooms.sortedByDescending { it.lastMessageTime })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Error: ${error.message}")
            }
        }

        chatRoomsRef.addValueEventListener(listener)
        awaitClose { chatRoomsRef.removeEventListener(listener) }
    }


    suspend fun createChatRoom(teamId: String, chatName: String): Result<String> {
        return try {
            val chatRoomsRef = teamsRef.child(teamId).child("chatRooms")
            val chatRoomId = chatRoomsRef.push().key
                ?: return Result.failure(Exception("Failed to generate ID"))

            val chatRoom = ChatRoom(
                chatRoomId = chatRoomId,
                teamId = teamId,
                chatName = chatName,
                lastMessage = "",
                lastMessageTime = System.currentTimeMillis()
            )

            Log.d("ChatRepository", "Creating chat room: $chatRoomId")
            chatRoomsRef.child(chatRoomId).setValue(chatRoom).await()
            Log.d("ChatRepository", "Chat room created successfully")

            Result.success(chatRoomId)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating chat room: ${e.message}", e)
            Result.failure(e)
        }
    }


    /**
     * Récupère les messages d'un chat room en temps réel.
     */
    fun getChatRoomMessages(teamId: String, chatRoomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val messagesRef = teamsRef.child(teamId).child("messages").child(chatRoomId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull {
                    try {
                        it.getValue(ChatMessage::class.java)
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Error parsing message: ${e.message}")
                        null
                    }
                }
                trySend(messages.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Error: ${error.message}")
            }
        }

        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }


    /**
     * Envoie un message dans un chat room.
     */
    suspend fun sendMessage(chatRoomId: String, teamId: String, content: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))

            val messagesRef = teamsRef.child(teamId).child("messages").child(chatRoomId)
            val messageId = messagesRef.push().key
                ?: return Result.failure(Exception("Failed to generate ID"))

            val message = ChatMessage(
                messageId = messageId,
                chatRoomId = chatRoomId,
                userId = user.uid,
                userName = user.email?.substringBefore("@") ?: "Utilisateur",
                content = content,
                timestamp = System.currentTimeMillis()
            )

            messagesRef.child(messageId).setValue(message).await()

            // Met à jour le dernier message dans le chatRoom
            val updates = mapOf(
                "lastMessage" to content,
                "lastMessageTime" to message.timestamp
            )
            teamsRef.child(teamId).child("chatRooms").child(chatRoomId).updateChildren(updates).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }
}
