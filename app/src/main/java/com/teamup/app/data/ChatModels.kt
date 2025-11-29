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
    val creatorId: String = "",
    val adminIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList()
) {
    constructor() : this("", "", "", emptyList(), emptyList())
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
                creatorId = user.uid,
                adminIds = listOf(user.uid), // Le créateur est automatiquement admin
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

    // ============================================================================
    // GESTION DE GROUPE (TODO: à déplacer dans GroupRepository lors de la refactorisation)
    // ============================================================================

    /**
     * Récupère les informations détaillées des membres du groupe
     * Retourne une liste de Pair<userId, username>
     */
    suspend fun getTeamMembersDetails(teamId: String): Result<List<Pair<String, String>>> {
        return try {
            val snapshot = teamsRef.child(teamId).get().await()
            val team = snapshot.getValue(TeamGroup::class.java)
                ?: return Result.failure(Exception("Groupe introuvable"))

            val membersDetails = mutableListOf<Pair<String, String>>()
            val usersRef = database.getReference("users")
            
            for (userId in team.memberIds) {
                try {
                    val userSnapshot = usersRef.child(userId).get().await()
                    val username = userSnapshot.child("username").getValue(String::class.java)
                        ?: "Utilisateur inconnu"
                    membersDetails.add(Pair(userId, username))
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Error fetching username for $userId: ${e.message}")
                    membersDetails.add(Pair(userId, "Utilisateur inconnu"))
                }
            }

            Result.success(membersDetails)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting team members: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Renomme un groupe (seulement pour le créateur)
     */
    suspend fun renameTeam(teamId: String, newName: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("Utilisateur non connecté"))

            if (newName.isBlank()) {
                return Result.failure(Exception("Le nom ne peut pas être vide"))
            }

            val snapshot = teamsRef.child(teamId).get().await()
            val team = snapshot.getValue(TeamGroup::class.java)
                ?: return Result.failure(Exception("Groupe introuvable"))

            // Vérifie que l'utilisateur est le créateur
            if (team.creatorId != userId) {
                return Result.failure(Exception("Seul le créateur peut renommer le groupe"))
            }

            teamsRef.child(teamId).child("teamName").setValue(newName).await()
            Log.d("ChatRepository", "Team renamed successfully: $teamId -> $newName")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error renaming team: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Permet à l'utilisateur de quitter un groupe
     */
    suspend fun leaveTeam(teamId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("Utilisateur non connecté"))

            val snapshot = teamsRef.child(teamId).get().await()
            val team = snapshot.getValue(TeamGroup::class.java)
                ?: return Result.failure(Exception("Groupe introuvable"))

            // Le créateur ne peut pas quitter son groupe (il doit le supprimer)
            if (team.creatorId == userId) {
                return Result.failure(Exception("Le créateur ne peut pas quitter le groupe. Supprimez-le à la place."))
            }

            if (!team.memberIds.contains(userId)) {
                return Result.failure(Exception("Vous n'êtes pas membre de ce groupe"))
            }

            // Retire l'utilisateur de la liste des membres et admins
            val updatedMembers = team.memberIds.filter { it != userId }
            val updatedAdmins = team.adminIds.filter { it != userId }
            
            teamsRef.child(teamId).child("memberIds").setValue(updatedMembers).await()
            teamsRef.child(teamId).child("adminIds").setValue(updatedAdmins).await()
            Log.d("ChatRepository", "User left team successfully: $userId from $teamId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error leaving team: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Vérifie si l'utilisateur est le créateur du groupe
     */
    suspend fun isCreator(teamId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val snapshot = teamsRef.child(teamId).get().await()
            val team = snapshot.getValue(TeamGroup::class.java)
            team?.creatorId == userId
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error checking creator status: ${e.message}", e)
            false
        }
    }

    /**
     * Vérifie si l'utilisateur est admin du groupe
     */
    suspend fun isAdmin(teamId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val snapshot = teamsRef.child(teamId).get().await()
            val team = snapshot.getValue(TeamGroup::class.java)
            team?.adminIds?.contains(userId) ?: false
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error checking admin status: ${e.message}", e)
            false
        }
    }

    /**
     * Promouvoir un membre au rang d'admin (seulement pour le créateur)
     */
    suspend fun promoteToAdmin(teamId: String, userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("Utilisateur non connecté"))

            val snapshot = teamsRef.child(teamId).get().await()
            val team = snapshot.getValue(TeamGroup::class.java)
                ?: return Result.failure(Exception("Groupe introuvable"))

            if (team.creatorId != currentUserId) {
                return Result.failure(Exception("Seul le créateur peut promouvoir des admins"))
            }

            if (!team.memberIds.contains(userId)) {
                return Result.failure(Exception("Cet utilisateur n'est pas membre du groupe"))
            }

            if (team.adminIds.contains(userId)) {
                return Result.failure(Exception("Cet utilisateur est déjà admin"))
            }

            val updatedAdmins = team.adminIds + userId
            teamsRef.child(teamId).child("adminIds").setValue(updatedAdmins).await()
            Log.d("ChatRepository", "User promoted to admin: $userId in $teamId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error promoting to admin: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Rétrograder un admin au rang de membre (seulement pour le créateur)
     */
    suspend fun demoteFromAdmin(teamId: String, userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("Utilisateur non connecté"))

            val snapshot = teamsRef.child(teamId).get().await()
            val team = snapshot.getValue(TeamGroup::class.java)
                ?: return Result.failure(Exception("Groupe introuvable"))

            if (team.creatorId != currentUserId) {
                return Result.failure(Exception("Seul le créateur peut rétrograder des admins"))
            }

            if (team.creatorId == userId) {
                return Result.failure(Exception("Le créateur ne peut pas être rétrogradé"))
            }

            if (!team.adminIds.contains(userId)) {
                return Result.failure(Exception("Cet utilisateur n'est pas admin"))
            }

            val updatedAdmins = team.adminIds.filter { it != userId }
            teamsRef.child(teamId).child("adminIds").setValue(updatedAdmins).await()
            Log.d("ChatRepository", "User demoted from admin: $userId in $teamId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error demoting from admin: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Exclure un membre du groupe (créateur ou admin)
     */
    suspend fun removeMember(teamId: String, userId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("Utilisateur non connecté"))

            val snapshot = teamsRef.child(teamId).get().await()
            val team = snapshot.getValue(TeamGroup::class.java)
                ?: return Result.failure(Exception("Groupe introuvable"))

            // Seul le créateur ou un admin peut exclure
            if (team.creatorId != currentUserId && !team.adminIds.contains(currentUserId)) {
                return Result.failure(Exception("Seuls le créateur et les admins peuvent exclure des membres"))
            }

            // Ne peut pas exclure le créateur
            if (team.creatorId == userId) {
                return Result.failure(Exception("Le créateur ne peut pas être exclu"))
            }

            // Un admin ne peut pas exclure un autre admin (seul le créateur peut)
            if (team.adminIds.contains(userId) && team.creatorId != currentUserId) {
                return Result.failure(Exception("Seul le créateur peut exclure un admin"))
            }

            if (!team.memberIds.contains(userId)) {
                return Result.failure(Exception("Cet utilisateur n'est pas membre du groupe"))
            }

            // Retire l'utilisateur de la liste des membres et admins
            val updatedMembers = team.memberIds.filter { it != userId }
            val updatedAdmins = team.adminIds.filter { it != userId }
            
            teamsRef.child(teamId).child("memberIds").setValue(updatedMembers).await()
            teamsRef.child(teamId).child("adminIds").setValue(updatedAdmins).await()
            Log.d("ChatRepository", "User removed from team: $userId from $teamId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error removing member: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Supprime un groupe (seulement pour le créateur)
     */
    suspend fun deleteTeam(teamId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("Utilisateur non connecté"))

            val snapshot = teamsRef.child(teamId).get().await()
            val team = snapshot.getValue(TeamGroup::class.java)
                ?: return Result.failure(Exception("Groupe introuvable"))

            // Vérifie que l'utilisateur est le créateur
            if (team.creatorId != userId) {
                return Result.failure(Exception("Seul le créateur peut supprimer le groupe"))
            }

            // Supprime le groupe et toutes ses données
            teamsRef.child(teamId).removeValue().await()
            Log.d("ChatRepository", "Team deleted successfully: $teamId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting team: ${e.message}", e)
            Result.failure(e)
        }
    }
}