package com.teamup.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.teamup.app.data.TeamGroup
import kotlinx.coroutines.tasks.await

/**
 * Repository pour la gestion des équipes (TeamGroup)
 * Responsabilités :
 * - CRUD des équipes
 * - Gestion des membres
 * - Gestion des rôles (créateur, admins)
 * - Permissions et validations
 */
object TeamRepository {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val teamsRef = database.getReference("teams")

    // ============================================================================
    // RÉCUPÉRATION D'ÉQUIPES
    // ============================================================================

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
            Log.e("TeamRepository", "getUserTeam: User not logged in")
            return null
        }

        return try {
            Log.d("TeamRepository", "getUserTeam: Fetching teams for user $userId")
            val snapshot = teamsRef.get().await()

            if (!snapshot.exists()) {
                Log.d("TeamRepository", "getUserTeam: No teams exist in database")
                return null
            }

            val team = snapshot.children.mapNotNull {
                try {
                    it.getValue(TeamGroup::class.java)
                } catch (e: Exception) {
                    Log.e("TeamRepository", "Error parsing team: ${e.message}")
                    null
                }
            }.find { team ->
                team.memberIds.contains(userId)
            }

            if (team != null) {
                Log.d("TeamRepository", "getUserTeam: Found team ${team.teamId} (${team.teamName})")
            } else {
                Log.d("TeamRepository", "getUserTeam: No team found for user")
            }

            team
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error in getUserTeam: ${e.message}", e)
            null
        }
    }

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
                    Log.e("TeamRepository", "Error fetching username for $userId: ${e.message}")
                    membersDetails.add(Pair(userId, "Utilisateur inconnu"))
                }
            }

            Result.success(membersDetails)
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error getting team members: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ============================================================================
    // CRÉATION ET ADHÉSION
    // ============================================================================

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

            Log.d("TeamRepository", "Creating team: $teamId with name: $teamName")
            teamsRef.child(teamId).setValue(team).await()
            Log.d("TeamRepository", "Team created successfully")

            Result.success(teamId)
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error creating team: ${e.message}", e)
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
            Log.d("TeamRepository", "Attempting to join team: $teamId")
            val snapshot = teamRef.get().await()

            if (!snapshot.exists()) {
                Log.e("TeamRepository", "Team does not exist: $teamId")
                return Result.failure(Exception("Le TeamGroup n'existe pas."))
            }

            val team = snapshot.getValue(TeamGroup::class.java)

            if (team == null) {
                Log.e("TeamRepository", "Failed to parse team data")
                return Result.failure(Exception("Impossible de lire les données du TeamGroup."))
            }

            if (team.memberIds.contains(userId)) {
                Log.d("TeamRepository", "User already member of team")
                return Result.success(Unit)
            }

            val updatedMembers = team.memberIds + userId
            teamRef.child("memberIds").setValue(updatedMembers).await()
            Log.d("TeamRepository", "Successfully joined team")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("TeamRepository", "Error joining team: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ============================================================================
    // GESTION DE L'ÉQUIPE
    // ============================================================================

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
            Log.d("TeamRepository", "Team renamed successfully: $teamId -> $newName")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error renaming team: ${e.message}", e)
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
            Log.d("TeamRepository", "User left team successfully: $userId from $teamId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error leaving team: ${e.message}", e)
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
            Log.d("TeamRepository", "Team deleted successfully: $teamId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error deleting team: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ============================================================================
    // VÉRIFICATIONS DE RÔLES
    // ============================================================================

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
            Log.e("TeamRepository", "Error checking creator status: ${e.message}", e)
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
            Log.e("TeamRepository", "Error checking admin status: ${e.message}", e)
            false
        }
    }

    // ============================================================================
    // GESTION DES RÔLES
    // ============================================================================

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
            Log.d("TeamRepository", "User promoted to admin: $userId in $teamId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error promoting to admin: ${e.message}", e)
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
            Log.d("TeamRepository", "User demoted from admin: $userId in $teamId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error demoting from admin: ${e.message}", e)
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
            Log.d("TeamRepository", "User removed from team: $userId from $teamId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error removing member: ${e.message}", e)
            Result.failure(e)
        }
    }
}
