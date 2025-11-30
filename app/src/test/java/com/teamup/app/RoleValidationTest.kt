package com.teamup.app

import com.teamup.app.data.TeamGroup
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitaires pour la validation des rôles et permissions
 */
class RoleValidationTest {

    // Fonctions utilitaires pour tester la logique des rôles
    private fun isCreator(team: TeamGroup, userId: String): Boolean {
        return team.creatorId == userId
    }

    private fun isAdmin(team: TeamGroup, userId: String): Boolean {
        return team.adminIds.contains(userId)
    }

    private fun isMember(team: TeamGroup, userId: String): Boolean {
        return team.memberIds.contains(userId)
    }

    private fun canRenameGroup(team: TeamGroup, userId: String): Boolean {
        return isCreator(team, userId)
    }

    private fun canPromoteToAdmin(team: TeamGroup, userId: String, targetUserId: String): Boolean {
        return isCreator(team, userId) && isMember(team, targetUserId) && !isAdmin(team, targetUserId)
    }

    private fun canDemoteFromAdmin(team: TeamGroup, userId: String, targetUserId: String): Boolean {
        return isCreator(team, userId) && isAdmin(team, targetUserId) && !isCreator(team, targetUserId)
    }

    private fun canRemoveMember(team: TeamGroup, userId: String, targetUserId: String): Boolean {
        if (isCreator(team, targetUserId)) return false // Ne peut pas exclure le créateur
        if (isCreator(team, userId)) return true // Créateur peut exclure tout le monde
        if (isAdmin(team, userId) && !isAdmin(team, targetUserId)) return true // Admin peut exclure les membres simples
        return false
    }

    private fun canDeleteGroup(team: TeamGroup, userId: String): Boolean {
        return isCreator(team, userId)
    }

    private fun canLeaveGroup(team: TeamGroup, userId: String): Boolean {
        return !isCreator(team, userId) && isMember(team, userId)
    }

    @Test
    fun `seul le créateur peut renommer le groupe`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "creator",
            adminIds = listOf("creator", "admin1"),
            memberIds = listOf("creator", "admin1", "member1")
        )

        assertTrue(canRenameGroup(team, "creator"))
        assertFalse(canRenameGroup(team, "admin1"))
        assertFalse(canRenameGroup(team, "member1"))
    }

    @Test
    fun `seul le créateur peut promouvoir des admins`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "creator",
            adminIds = listOf("creator"),
            memberIds = listOf("creator", "member1", "member2")
        )

        assertTrue(canPromoteToAdmin(team, "creator", "member1"))
        assertFalse(canPromoteToAdmin(team, "member1", "member2"))
    }

    @Test
    fun `seul le créateur peut rétrograder des admins`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "creator",
            adminIds = listOf("creator", "admin1"),
            memberIds = listOf("creator", "admin1", "member1")
        )

        assertTrue(canDemoteFromAdmin(team, "creator", "admin1"))
        assertFalse(canDemoteFromAdmin(team, "admin1", "admin1"))
    }

    @Test
    fun `le créateur ne peut pas être rétrogradé`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "creator",
            adminIds = listOf("creator"),
            memberIds = listOf("creator")
        )

        assertFalse(canDemoteFromAdmin(team, "creator", "creator"))
    }

    @Test
    fun `le créateur peut exclure n'importe qui sauf lui-même`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "creator",
            adminIds = listOf("creator", "admin1"),
            memberIds = listOf("creator", "admin1", "member1")
        )

        assertFalse(canRemoveMember(team, "creator", "creator"))
        assertTrue(canRemoveMember(team, "creator", "admin1"))
        assertTrue(canRemoveMember(team, "creator", "member1"))
    }

    @Test
    fun `un admin peut exclure les membres simples`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "creator",
            adminIds = listOf("creator", "admin1"),
            memberIds = listOf("creator", "admin1", "member1")
        )

        assertTrue(canRemoveMember(team, "admin1", "member1"))
    }

    @Test
    fun `un admin ne peut pas exclure un autre admin`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "creator",
            adminIds = listOf("creator", "admin1", "admin2"),
            memberIds = listOf("creator", "admin1", "admin2", "member1")
        )

        assertFalse(canRemoveMember(team, "admin1", "admin2"))
    }

    @Test
    fun `un admin ne peut pas exclure le créateur`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "creator",
            adminIds = listOf("creator", "admin1"),
            memberIds = listOf("creator", "admin1", "member1")
        )

        assertFalse(canRemoveMember(team, "admin1", "creator"))
    }

    @Test
    fun `un membre simple ne peut exclure personne`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "creator",
            adminIds = listOf("creator"),
            memberIds = listOf("creator", "member1", "member2")
        )

        assertFalse(canRemoveMember(team, "member1", "member2"))
    }

    @Test
    fun `seul le créateur peut supprimer le groupe`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "creator",
            adminIds = listOf("creator", "admin1"),
            memberIds = listOf("creator", "admin1", "member1")
        )

        assertTrue(canDeleteGroup(team, "creator"))
        assertFalse(canDeleteGroup(team, "admin1"))
        assertFalse(canDeleteGroup(team, "member1"))
    }

    @Test
    fun `le créateur ne peut pas quitter le groupe`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "creator",
            adminIds = listOf("creator"),
            memberIds = listOf("creator", "member1")
        )

        assertFalse(canLeaveGroup(team, "creator"))
    }

    @Test
    fun `les membres et admins peuvent quitter le groupe`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "creator",
            adminIds = listOf("creator", "admin1"),
            memberIds = listOf("creator", "admin1", "member1")
        )

        assertTrue(canLeaveGroup(team, "admin1"))
        assertTrue(canLeaveGroup(team, "member1"))
    }
}
