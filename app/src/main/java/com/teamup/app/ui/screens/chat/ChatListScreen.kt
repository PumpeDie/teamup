package com.teamup.app.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.teamup.app.data.ChatRepository
import com.teamup.app.data.ChatRoom
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var teamId by remember { mutableStateOf<String?>(null) }
    var chatRooms by remember { mutableStateOf(emptyList<ChatRoom>()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        teamId = ChatRepository.getUserTeamId()
        isLoading = false
    }

        LaunchedEffect(teamId) {
        if (teamId != null && teamId!!.isNotBlank()) {
            ChatRepository.getTeamChatRooms(teamId!!)
                .collect { rooms ->
                    chatRooms = rooms
                }
        }
    }

     if (teamId == null || teamId!!.isBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Vous n'avez pas de Groupe de Connexion. Retournez à l'accueil.")
            }
        }
        return
    }

    val currentTeamId = teamId!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats du Team ${currentTeamId.take(6)}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, "Créer une salle de chat")
            }
        }
    ) { paddingValues ->
        if (chatRooms.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Aucune salle de chat créée pour ce groupe.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                items(chatRooms) { room ->
                    ChatRoomItem(
                        chatRoom = room,
                        onClick = {
                            navController.navigate("chat/$currentTeamId/${room.chatRoomId}")
                        }
                    )
                    Divider()
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateChatRoomDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                scope.launch {
                    ChatRepository.createChatRoom(currentTeamId, name)
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
fun ChatRoomItem(chatRoom: ChatRoom, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = chatRoom.chatName,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = chatRoom.lastMessage.ifBlank { "Démarrer la discussion..." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (chatRoom.lastMessageTime > 0) formatTimestamp(chatRoom.lastMessageTime) else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun CreateChatRoomDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var chatName by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Créer une Salle de Chat", style = MaterialTheme.typography.titleLarge)
                Text("Accessible uniquement aux membres de votre Groupe de Connexion.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = chatName,
                    onValueChange = { chatName = it },
                    label = { Text("Nom de la salle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onCreate(chatName) },
                        enabled = chatName.isNotBlank()
                    ) { Text("Créer") }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "À l'instant"
        diff < 3_600_000 -> "${diff / 60_000} min"
        diff < 86_400_000 -> "${diff / 3_600_000} h"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}