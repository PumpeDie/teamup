package com.teamup.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.teamup.app.data.ChatMessage
import com.teamup.app.data.ChatRoom
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pour la gestion du chat (ChatRooms et ChatMessages)
 * Responsabilités :
 * - CRUD des salons de chat
 * - Envoi et réception de messages
 * - Flux temps réel des messages
 */
object ChatRepository {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val teamsRef = database.getReference("teams")

    // ============================================================================
    // GESTION DES CHAT ROOMS
    // ============================================================================

    /**
     * Récupère les salons de chat d'une équipe en temps réel
     */
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

    /**
     * Crée un nouveau salon de chat dans une équipe
     */
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

    // ============================================================================
    // GESTION DES MESSAGES
    // ============================================================================

    /**
     * Récupère les messages d'un chat room en temps réel
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
     * Envoie un message dans un chat room
     */
    suspend fun sendMessage(chatRoomId: String, teamId: String, content: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
            
            // Récupère le username depuis Firebase Database
            val usersRef = database.getReference("users")
            val userSnapshot = usersRef.child(user.uid).get().await()
            val username = userSnapshot.child("username").getValue(String::class.java)
                ?: user.email?.substringBefore("@") ?: "Utilisateur"

            val messagesRef = teamsRef.child(teamId).child("messages").child(chatRoomId)
            val messageId = messagesRef.push().key
                ?: return Result.failure(Exception("Failed to generate ID"))

            val message = ChatMessage(
                messageId = messageId,
                chatRoomId = chatRoomId,
                userId = user.uid,
                userName = username,
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
