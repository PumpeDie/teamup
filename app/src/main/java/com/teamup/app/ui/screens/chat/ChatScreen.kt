package com.teamup.app.ui.screens.chat

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Group

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

import com.teamup.app.data.ChatMessage
import com.teamup.app.data.ChatRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    teamId: String,
    chatRoomId: String
) {
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    var messages by remember { mutableStateOf(emptyList<ChatMessage>()) }
    var chatName by remember { mutableStateOf("Chargement...") }
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

     LaunchedEffect(chatRoomId) {
        ChatRepository.getChatRoomMessages(teamId, chatRoomId)
            .collect { fetchedMessages ->
                messages = fetchedMessages
                if (messages.isNotEmpty()) {
                    listState.scrollToItem(messages.size - 1)
                }
            }
    }

    // Récupère le nom du chat room
    LaunchedEffect(teamId, chatRoomId) {
        ChatRepository.getTeamChatRooms(teamId).collect { rooms ->
            val room = rooms.find { it.chatRoomId == chatRoomId }
            chatName = room?.chatName ?: "Chat"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = chatName,
                        style = MaterialTheme.typography.titleLarge,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Afficher les membres du TeamGroup */ }) {
                        Icon(Icons.Default.Group, contentDescription = "Détails du groupe de connexion")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    label = { Text("Message...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            scope.launch {
                                ChatRepository.sendMessage(chatRoomId, teamId, messageInput)
                                messageInput = ""
                            }
                        }
                    },
                    enabled = messageInput.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message, currentUserId = currentUserId)
            }
        }
    }
}


@Composable
fun MessageBubble(message: ChatMessage, currentUserId: String) {
    val isCurrentUser = message.userId == currentUserId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = if (isCurrentUser) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
            },
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = { /* TODO: Afficher l'heure complète / Répondre */ },
                    onLongClick = { /* TODO: Copier / Supprimer */ }
                )
        ) {
            Column(modifier = Modifier.padding(10.dp)) {

                if (!isCurrentUser) {
                    Text(
                        text = message.userName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatMessageTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(if (isCurrentUser) Alignment.End else Alignment.Start)
                )
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

private fun formatMessageTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
