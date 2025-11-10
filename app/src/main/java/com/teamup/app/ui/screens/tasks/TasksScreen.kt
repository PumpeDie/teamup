package com.teamup.app.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


data class Task(
    var id: String = "",
    var title: String = "",
    var dueDate: String = "",
    var completed: Boolean = false,
    var completedByUserName: String? = null
)


@Composable
fun TasksScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val tasksRef = FirebaseDatabase.getInstance().getReference("tasks")

    val currentUser = auth.currentUser
    val currentUserId = currentUser?.uid ?: "unknown"
    val currentUserName = currentUser?.email ?: "Utilisateur Inconnu"

    var tasks = remember { mutableStateListOf<Task>() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF6200EE)),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Liste des Tâches", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chargement...", fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            ) {
                items(tasks) { task ->
                    TaskItemMinimal(
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            MinimalButton(
                onClick = { showCreateDialog = true },
                text = "+ Ajouter",
                backgroundColor = Color(0xFF03DAC5)
            )
        }
    }

    if (showCreateDialog) {
        CreateTaskDialogMinimal(
            onDismiss = { showCreateDialog = false },
            onCreateTask = { title, date ->
                val newTaskId = tasksRef.push().key
                if (newTaskId != null) {
                    val newTask = Task(
                        id = newTaskId,
                        title = title,
                        dueDate = date,
                        completed = false,
                        completedByUserName = null
                    )
                    tasksRef.child(newTaskId).setValue(newTask)
                }
                showCreateDialog = false
            }
        )
    }

    if (showEditDialog) {
        taskToEdit?.let { task ->
            EditTaskDialogMinimal(
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
fun TaskItemMinimal(
    task: Task,
    onTaskClicked: (String, Boolean) -> Unit,
    onTaskDoubleClicked: (Task) -> Unit
) {
    val isTaskCompleted = task.completed
    val textColor = if (isTaskCompleted) Color.Gray else Color.Black
    val scope = rememberCoroutineScope()
    var lastTapTime by remember { mutableStateOf(0L) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isTaskCompleted) Color(0xFFEEEEEE) else Color.White)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastTapTime < 300) {
                                onTaskDoubleClicked(task)
                                lastTapTime = 0L
                            } else {
                                lastTapTime = currentTime
                            }
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                    .background(if (isTaskCompleted) Color(0xFF6200EE) else Color.Transparent)
                    .clickable {
                        onTaskClicked(task.id, task.completed)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isTaskCompleted) {
                    Text("✓", color = Color.White, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (task.dueDate.isNotEmpty()) {
                    Text(
                        text = " Date Limite: ${task.dueDate}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
                if (isTaskCompleted && task.completedByUserName != null) {
                    Text(
                        text = "Fait par : ${task.completedByUserName}",
                        fontSize = 12.sp,
                        color = Color(0xFF6200EE)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateTaskDialogMinimal(onDismiss: () -> Unit, onCreateTask: (String, String) -> Unit) {
    var taskTitle by remember { mutableStateOf("") }
    var taskDate by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(24.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nouvelle Tâche", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (taskTitle.isEmpty()) {
                        Text("Titre de la tâche", color = Color.LightGray, fontSize = 16.sp)
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            BasicTextField(
                value = taskDate,
                onValueChange = { taskDate = it },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (taskDate.isEmpty()) {
                        Text("Date (ex: 15/11/2025)", color = Color.LightGray, fontSize = 16.sp)
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MinimalButton(
                    onClick = onDismiss,
                    text = "Annuler",
                    backgroundColor = Color.LightGray,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                MinimalButton(
                    onClick = { onCreateTask(taskTitle, taskDate) },
                    text = "Créer",
                    enabled = taskTitle.isNotBlank(),
                    backgroundColor = Color(0xFF6200EE),
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EditTaskDialogMinimal(
    task: Task,
    onDismiss: () -> Unit,
    onUpdateTask: (String) -> Unit,
    onDeleteTask: () -> Unit
) {
    var taskTitle by remember { mutableStateOf(task.title) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(24.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Modifier la Tâche", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (taskTitle.isEmpty()) {
                        Text("Titre de la tâche", color = Color.LightGray, fontSize = 16.sp)
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MinimalButton(
                    onClick = onDismiss,
                    text = "Annuler",
                    backgroundColor = Color.LightGray,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                MinimalButton(
                    onClick = { onUpdateTask(taskTitle) },
                    text = "Save",
                    enabled = taskTitle.isNotBlank(),
                    backgroundColor = Color(0xFF6200EE),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                MinimalButton(
                    onClick = onDeleteTask,
                    text = "Supprimer",
                    backgroundColor = Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MinimalButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    enabled: Boolean = true
) {
    val buttonColor = if (enabled) backgroundColor else Color.Gray

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(buttonColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
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

    tasksRef.child(taskId).updateChildren(updates as Map<String, Any>)
}
