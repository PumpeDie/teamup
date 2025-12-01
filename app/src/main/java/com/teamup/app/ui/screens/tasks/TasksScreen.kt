package com.teamup.app.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.teamup.app.data.repository.TeamRepository


data class Task(
    var id: String = "",
    var title: String = "",
    var dueDate: String = "",
    var completed: Boolean = false,
    var completedByUserName: String? = null,
    var createdBy: String = "",
    var createdByName: String = "",
    var assignedTo: String? = null,
    var assignedToName: String? = null,
    var createdAt: Long = System.currentTimeMillis()
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    var teamId by remember { mutableStateOf<String?>(null) }
    var teamName by remember { mutableStateOf("Chargement...") }
    var userName by remember { mutableStateOf("Utilisateur") }

    LaunchedEffect(userId) {
        if (userId != null) {
            val team = TeamRepository.getUserTeam()
            teamId = team?.teamId
            teamName = team?.teamName ?: "Aucun groupe"
            
            // Récupérer le username depuis Firebase Database
            val database = FirebaseDatabase.getInstance()
            val usersRef = database.getReference("users").child(userId)
            usersRef.get().addOnSuccessListener { snapshot ->
                userName = snapshot.child("username").getValue(String::class.java) ?: "Utilisateur"
            }
        }
    }

    if (teamId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (teamId!!.isBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Veuillez d'abord créer ou rejoindre un Groupe de Connexion.")
        }
        return
    }

    val database = FirebaseDatabase.getInstance()
    val tasksRef = database.getReference("teams").child(teamId!!).child("tasks")
    val currentUser = auth.currentUser
    val currentUserName = currentUser?.email ?: "Utilisateur Inconnu"

    var tasks = remember { mutableStateListOf<Task>() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(teamId) {
        tasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tasks.clear()
                val fetchedTasks = snapshot.children.mapNotNull { taskSnapshot ->
                    val task = taskSnapshot.getValue(Task::class.java)
                    if (task != null && taskSnapshot.key != null) {
                        task.copy(id = taskSnapshot.key!!)
                    } else {
                        null
                    }
                }.sortedBy { it.completed }
                tasks.addAll(fetchedTasks)
                isLoading = false
            }
            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tâches - $teamName",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {

            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Filled.Add, "Ajouter une tâche")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Chargement...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                items(tasks) { task ->
                    TaskItem(
                        task = task,
                        onTaskClicked = { taskId, completed ->
                            toggleTaskCompletion(tasksRef, taskId, completed, currentUserName)
                        },
                        onTaskDoubleClicked = { task ->
                            taskToEdit = task
                            showEditDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onCreateTask = { title, date ->
                val newTaskId = tasksRef.push().key
                if (newTaskId != null) {
                    val newTask = Task(
                        id = newTaskId,
                        title = title,
                        dueDate = date,
                        completed = false,
                        completedByUserName = null,
                        createdBy = currentUser?.uid ?: "",
                        createdByName = userName,
                        assignedTo = null,
                        assignedToName = null,
                        createdAt = System.currentTimeMillis()
                    )
                    tasksRef.child(newTaskId).setValue(newTask)
                }
                showCreateDialog = false
            }
        )
    }

    if (showEditDialog) {
        taskToEdit?.let { task ->
            EditTaskDialog(
                task = task,
                onDismiss = {
                    showEditDialog = false
                    taskToEdit = null
                },
                onUpdateTask = { updatedTitle ->
                    tasksRef.child(task.id).child("title").setValue(updatedTitle)
                    showEditDialog = false
                    taskToEdit = null
                },
                onDeleteTask = {
                    tasksRef.child(task.id).removeValue()
                    showEditDialog = false
                    taskToEdit = null
                }
            )
        }
    }
}


@Composable
fun TaskItem(
    task: Task,
    onTaskClicked: (String, Boolean) -> Unit,
    onTaskDoubleClicked: (Task) -> Unit
) {
    val isTaskCompleted = task.completed
    val scope = rememberCoroutineScope()
    var lastTapTime by remember { mutableStateOf(0L) }

    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isTaskCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTapTime < 300) {
                            onTaskDoubleClicked(task)
                            lastTapTime = 0L
                        } else {
                            onTaskClicked(task.id, task.completed)
                            lastTapTime = currentTime
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(if (isTaskCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    .clickable { onTaskClicked(task.id, task.completed) },
                contentAlignment = Alignment.Center
            ) {
                if (isTaskCompleted) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Terminée",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (isTaskCompleted) TextDecoration.LineThrough else null
                    ),
                    color = if (isTaskCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.dueDate.isNotEmpty()) {
                    Text(
                        text = "Date de rendu: ${task.dueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (isTaskCompleted && task.completedByUserName != null) {
                    Text(
                        text = "Complétée par: ${task.completedByUserName!!.split("@").first()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreateTask: (String, String) -> Unit
) {
    var taskTitle by remember { mutableStateOf("") }
    var taskDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Nouvelle Tâche", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Titre de la tâche") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = taskDate,
                    onValueChange = { taskDate = it },
                    label = { Text("Date d'échéance (optionnel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateTask(taskTitle, taskDate) },
                enabled = taskTitle.isNotBlank()
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onUpdateTask: (String) -> Unit,
    onDeleteTask: () -> Unit
) {
    var updatedTitle by remember { mutableStateOf(task.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Modifier la Tâche", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            OutlinedTextField(
                value = updatedTitle,
                onValueChange = { updatedTitle = it },
                label = { Text("Titre de la tâche") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDeleteTask) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = { onUpdateTask(updatedTitle) },
                    enabled = updatedTitle.isNotBlank() && updatedTitle != task.title
                ) {
                    Text("Mettre à jour")
                }
            }
        }
    )
}

private fun toggleTaskCompletion(
    tasksRef: DatabaseReference,
    taskId: String,
    currentStatus: Boolean,
    userName: String
) {
    val newStatus = !currentStatus
    val userToSave = if (newStatus) userName else null

    val updates = hashMapOf<String, Any?>(
        "completed" to newStatus,
        "completedByUserName" to userToSave
    )
    tasksRef.child(taskId).updateChildren(updates)
}
