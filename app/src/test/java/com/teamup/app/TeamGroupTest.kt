package com.teamup.app

import com.teamup.app.data.TeamGroup
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitaires pour la classe TeamGroup
 */
class TeamGroupTest {

    @Test
    fun `TeamGroup - constructeur par défaut crée un objet vide`() {
        val team = TeamGroup()
        
        assertEquals("", team.teamId)
        assertEquals("", team.teamName)
        assertEquals("", team.creatorId)
        assertTrue(team.adminIds.isEmpty())
        assertTrue(team.memberIds.isEmpty())
    }

    @Test
    fun `TeamGroup - constructeur avec paramètres initialise correctement`() {
        val teamId = "team123"
        val teamName = "Test Team"
        val creatorId = "user1"
        val adminIds = listOf("user1", "user2")
        val memberIds = listOf("user1", "user2", "user3")
        
        val team = TeamGroup(
            teamId = teamId,
            teamName = teamName,
            creatorId = creatorId,
            adminIds = adminIds,
            memberIds = memberIds
        )
        
        assertEquals(teamId, team.teamId)
        assertEquals(teamName, team.teamName)
        assertEquals(creatorId, team.creatorId)
        assertEquals(adminIds, team.adminIds)
        assertEquals(memberIds, team.memberIds)
    }

    @Test
    fun `TeamGroup - le créateur est dans adminIds`() {
        val creatorId = "creator123"
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = creatorId,
            adminIds = listOf(creatorId),
            memberIds = listOf(creatorId, "user2")
        )
        
        assertTrue(team.adminIds.contains(creatorId))
    }

    @Test
    fun `TeamGroup - le créateur est dans memberIds`() {
        val creatorId = "creator123"
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = creatorId,
            adminIds = listOf(creatorId),
            memberIds = listOf(creatorId, "user2")
        )
        
        assertTrue(team.memberIds.contains(creatorId))
    }

    @Test
    fun `TeamGroup - tous les admins sont dans memberIds`() {
        val adminIds = listOf("user1", "user2", "user3")
        val memberIds = listOf("user1", "user2", "user3", "user4")
        
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "user1",
            adminIds = adminIds,
            memberIds = memberIds
        )
        
        adminIds.forEach { adminId ->
            assertTrue("Admin $adminId devrait être dans memberIds", 
                team.memberIds.contains(adminId))
        }
    }

    @Test
    fun `TeamGroup - peut avoir des membres non-admins`() {
        val team = TeamGroup(
            teamId = "team1",
            teamName = "Team",
            creatorId = "user1",
            adminIds = listOf("user1"),
            memberIds = listOf("user1", "user2", "user3")
        )
        
        val nonAdmins = team.memberIds.filter { !team.adminIds.contains(it) }
        assertEquals(2, nonAdmins.size)
        assertTrue(nonAdmins.contains("user2"))
        assertTrue(nonAdmins.contains("user3"))
    }
}
