package com.teamup.app.ui.screens.group

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.teamup.app.data.TeamGroup
import com.teamup.app.data.repository.TeamRepository
import kotlinx.coroutines.launch

// ============================================================================
// ÉCRAN DE GESTION DU GROUPE
// ============================================================================

/**
 * Écran de gestion du groupe (membres, ID, informations)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var team by remember { mutableStateOf<TeamGroup?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var membersDetails by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isCreator by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(Unit) {
        team = TeamRepository.getUserTeam()
        team?.let {
            TeamRepository.getTeamMembersDetails(it.teamId).onSuccess { members ->
                membersDetails = members
            }
            isCreator = TeamRepository.isCreator(it.teamId)
            isAdmin = TeamRepository.isAdmin(it.teamId)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestion du groupe") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (team == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Aucun groupe trouvé",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Retour")
                    }
                }
            }
        } else {
            GroupInfoContent(
                team = team!!,
                membersDetails = membersDetails,
                isCreator = isCreator,
                isAdmin = isAdmin,
                context = context,
                onRenameClick = { showRenameDialog = true },
                onLeaveClick = { showLeaveDialog = true },
                onDeleteClick = { showDeleteDialog = true },
                onRemoveMember = { member -> memberToRemove = member },
                onPromoteToAdmin = { userId ->
                    scope.launch {
                        TeamRepository.promoteToAdmin(team!!.teamId, userId).fold(
                            onSuccess = {
                                Toast.makeText(context, "Membre promu admin !", Toast.LENGTH_SHORT).show()
                                team = TeamRepository.getUserTeam()
                                team?.let {
                                    TeamRepository.getTeamMembersDetails(it.teamId).onSuccess { members ->
                                        membersDetails = members
                                    }
                                }
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                onDemoteFromAdmin = { userId ->
                    scope.launch {
                        TeamRepository.demoteFromAdmin(team!!.teamId, userId).fold(
                            onSuccess = {
                                Toast.makeText(context, "Admin rétrogradé !", Toast.LENGTH_SHORT).show()
                                team = TeamRepository.getUserTeam()
                                team?.let {
                                    TeamRepository.getTeamMembersDetails(it.teamId).onSuccess { members ->
                                        membersDetails = members
                                    }
                                }
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                modifier = Modifier.padding(paddingValues)
            )
        }

        // Dialogs
        if (showRenameDialog && team != null) {
            RenameGroupDialog(
                currentName = team!!.teamName,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    scope.launch {
                        TeamRepository.renameTeam(team!!.teamId, newName).fold(
                            onSuccess = {
                                Toast.makeText(context, "Groupe renommé !", Toast.LENGTH_SHORT).show()
                                // Recharge les infos
                                team = TeamRepository.getUserTeam()
                                showRenameDialog = false
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        }

        if (showLeaveDialog && team != null) {
            LeaveGroupDialog(
                onDismiss = { showLeaveDialog = false },
                onConfirm = {
                    scope.launch {
                        TeamRepository.leaveTeam(team!!.teamId).fold(
                            onSuccess = {
                                Toast.makeText(context, "Vous avez quitté le groupe", Toast.LENGTH_SHORT).show()
                                navController.navigate("groupSelection") {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        }

        if (showDeleteDialog && team != null) {
            DeleteGroupDialog(
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    scope.launch {
                        TeamRepository.deleteTeam(team!!.teamId).fold(
                            onSuccess = {
                                Toast.makeText(context, "Groupe supprimé", Toast.LENGTH_SHORT).show()
                                navController.navigate("groupSelection") {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        }

        memberToRemove?.let { (userId, username) ->
            RemoveMemberDialog(
                username = username,
                onDismiss = { memberToRemove = null },
                onConfirm = {
                    scope.launch {
                        TeamRepository.removeMember(team!!.teamId, userId).fold(
                            onSuccess = {
                                Toast.makeText(context, "$username a été exclu du groupe", Toast.LENGTH_SHORT).show()
                                team = TeamRepository.getUserTeam()
                                team?.let {
                                    TeamRepository.getTeamMembersDetails(it.teamId).onSuccess { members ->
                                        membersDetails = members
                                    }
                                }
                                memberToRemove = null
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                                memberToRemove = null
                            }
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun GroupInfoContent(
    team: TeamGroup,
    membersDetails: List<Pair<String, String>>,
    isCreator: Boolean,
    isAdmin: Boolean,
    context: Context,
    onRenameClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRemoveMember: (Pair<String, String>) -> Unit,
    onPromoteToAdmin: (String) -> Unit,
    onDemoteFromAdmin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Carte du nom du groupe avec bouton édition
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Nom du groupe",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = team.teamName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                // Seul le créateur peut renommer
                if (isCreator) {
                    IconButton(onClick = onRenameClick) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Renommer",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Carte de l'ID du groupe
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ID du groupe",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = team.teamId,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            copyToClipboard(context, team.teamId)
                            Toast.makeText(
                                context,
                                "ID copié dans le presse-papier",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copier l'ID",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Text(
                    text = "Partagez cet ID pour inviter des membres à rejoindre votre groupe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Carte des membres avec liste détaillée
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Membres",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "${team.memberIds.size} membre${if (team.memberIds.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyLarge
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Liste des membres
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                membersDetails.forEach { (userId, username) ->
                    val isMemberCreator = team.creatorId == userId
                    val isMemberAdmin = team.adminIds.contains(userId)
                    val isCurrentUser = userId == currentUserId
                    
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = username,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isCurrentUser) {
                                        Text(
                                            text = "(Vous)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (isMemberCreator) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = "👑 Créateur",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    } else if (isMemberAdmin) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = "⭐ Admin",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Actions pour le créateur uniquement
                            if (isCreator && !isCurrentUser) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Promouvoir/Rétrograder admin
                                    if (!isMemberCreator) {
                                        if (isMemberAdmin) {
                                            TextButton(
                                                onClick = { onDemoteFromAdmin(userId) },
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.tertiary
                                                )
                                            ) {
                                                Text("Rétrograder", style = MaterialTheme.typography.labelSmall)
                                            }
                                        } else {
                                            TextButton(
                                                onClick = { onPromoteToAdmin(userId) },
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Text("Promouvoir", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                    // Exclure
                                    TextButton(
                                        onClick = { onRemoveMember(Pair(userId, username)) },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Exclure", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            } else if (isAdmin && !isCurrentUser && !isMemberAdmin && !isMemberCreator) {
                                // Admin peut exclure les membres simples
                                TextButton(
                                    onClick = { onRemoveMember(Pair(userId, username)) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Exclure", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Actions selon le rôle
        if (isCreator) {
            // Le créateur peut supprimer le groupe
            Button(
                onClick = onDeleteClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Supprimer le groupe")
            }
        } else {
            // Les membres peuvent quitter
            OutlinedButton(
                onClick = onLeaveClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quitter le groupe")
            }
        }

        // Note d'information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💡 Astuce",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pour inviter quelqu'un :\n" +
                            "1. Copiez l'ID du groupe\n" +
                            "2. Envoyez-le par message ou email\n" +
                            "3. La personne pourra rejoindre le groupe depuis l'écran de sélection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ============================================================================
// DIALOGUES
// ============================================================================

/**
 * Dialogue pour renommer le groupe
 */
@Composable
fun RenameGroupDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renommer le groupe") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nouveau nom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text("Renommer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Dialogue de confirmation pour quitter le groupe
 */
@Composable
fun LeaveGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Quitter le groupe ?") },
        text = {
            Text("Vous allez quitter ce groupe. Vous pourrez le rejoindre à nouveau avec l'ID du groupe.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Quitter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Dialogue de confirmation pour supprimer le groupe
 */
@Composable
fun DeleteGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Supprimer le groupe ?") },
        text = {
            Column {
                Text("⚠️ Attention : Cette action est irréversible !")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Toutes les données du groupe seront définitivement supprimées :",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Messages")
                Text("• Tâches")
                Text("• Événements de l'agenda")
                Text("• Liste des membres")
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Supprimer définitivement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Dialogue de confirmation pour exclure un membre
 */
@Composable
fun RemoveMemberDialog(
    username: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exclure $username ?") },
        text = {
            Text("Ce membre sera retiré du groupe et perdra l'accès à toutes les données.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Exclure")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

// ============================================================================
// FONCTIONS UTILITAIRES
// ============================================================================

/**
 * Copie du texte dans le presse-papier
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Team ID", text)
    clipboard.setPrimaryClip(clip)
}
