package com.teamup.app.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// ============================================================================
// MODÈLES DE DONNÉES
// ============================================================================

/**
 * Représente un message dans un chat de groupe
 */
data class ChatMessage(
    val messageId: String = "",
    val groupId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", 0L)
}

/**
 * Représente un groupe de chat
 */
data class ChatGroup(
    val groupId: String = "",
    val groupName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val memberIds: List<String> = emptyList()
) {
    constructor() : this("", "", "", 0L, emptyList())
}

// ============================================================================
// REPOSITORY FIREBASE
// ============================================================================

/**
 * Repository pour gérer les chats Firebase
 */
object ChatRepository {
    
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val messagesRef = database.getReference("messages")
    private val groupsRef = database.getReference("groups")
    
    /**
     * Récupère les groupes de l'utilisateur en temps réel
     */
    fun getUserGroups(): Flow<List<ChatGroup>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: ""
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = mutableListOf<ChatGroup>()
                
                snapshot.children.forEach { groupSnapshot ->
                    val group = groupSnapshot.getValue(ChatGroup::class.java)
                    if (group != null && group.memberIds.contains(userId)) {
                        groups.add(group)
                    }
                }
                
                groups.sortByDescending { it.lastMessageTime }
                trySend(groups)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Error: ${error.message}")
            }
        }
        
        groupsRef.addValueEventListener(listener)
        awaitClose { groupsRef.removeEventListener(listener) }
    }
    
    /**
     * Récupère les messages d'un groupe en temps réel
     */
    fun getGroupMessages(groupId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()
                
                snapshot.children.forEach { messageSnapshot ->
                    val message = messageSnapshot.getValue(ChatMessage::class.java)
                    if (message != null) {
                        messages.add(message)
                    }
                }
                
                messages.sortBy { it.timestamp }
                trySend(messages)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Error: ${error.message}")
            }
        }
        
        messagesRef.child(groupId).addValueEventListener(listener)
        awaitClose { messagesRef.child(groupId).removeEventListener(listener) }
    }
    
    /**
    * Crée un nouveau groupe
    */
    suspend fun createGroup(groupName: String): Result<String> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
            
            val groupId = groupsRef.push().key
                ?: return Result.failure(Exception("Failed to generate ID"))
            
            val group = ChatGroup(
                groupId = groupId,
                groupName = groupName,
                lastMessage = "",
                lastMessageTime = System.currentTimeMillis(),
                memberIds = listOf(user.uid)
            )
            
            groupsRef.child(groupId).setValue(group).await()
            
            Result.success(groupId)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Envoie un message
     */
    suspend fun sendMessage(groupId: String, content: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
            
            val messageId = messagesRef.child(groupId).push().key
                ?: return Result.failure(Exception("Failed to generate ID"))
            
            val message = ChatMessage(
                messageId = messageId,
                groupId = groupId,
                userId = user.uid,
                userName = user.displayName ?: user.email ?: "Anonyme",
                content = content,
                timestamp = System.currentTimeMillis()
            )
            
            messagesRef.child(groupId).child(messageId).setValue(message).await()
            
            // Mettre à jour le dernier message
            val updates = mapOf(
                "lastMessage" to content,
                "lastMessageTime" to message.timestamp
            )
            groupsRef.child(groupId).updateChildren(updates).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Récupère un groupe
     */
    suspend fun getGroup(groupId: String): Result<ChatGroup> {
        return try {
            val snapshot = groupsRef.child(groupId).get().await()
            val group = snapshot.getValue(ChatGroup::class.java)
            
            if (group != null) {
                Result.success(group)
            } else {
                Result.failure(Exception("Group not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}