package com.teamup.app.ui.screens.meeting

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.teamup.app.data.repository.TeamRepository
import com.teamup.app.ui.screens.agenda.Event
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingPlanningScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var userName by remember { mutableStateOf("Utilisateur") }
    var teamId by remember { mutableStateOf<String?>(null) }
    var teamName by remember { mutableStateOf("Chargement...") }
    var teamMembers by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(Unit) {
        val team = TeamRepository.getUserTeam()
        teamId = team?.teamId
        teamName = team?.teamName ?: "Aucun groupe"

        currentUser?.uid?.let { userId ->
            val database = FirebaseDatabase.getInstance()
            val usersRef = database.getReference("users").child(userId)
            usersRef.get().addOnSuccessListener { snapshot ->
                userName = snapshot.child("username").getValue(String::class.java) ?: "Utilisateur"
            }
        }

        // Charger les membres du groupe
        teamId?.let { id ->
            TeamRepository.getTeamMembersDetails(id)
                .onSuccess { members ->
                    teamMembers = members
                }
                .onFailure { error ->
                    // Handle error
                }
        }
    }

    if (teamId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Chargement du groupe...")
        }
        return
    }

    if (teamId!!.isBlank()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                "Veuillez d'abord créer ou rejoindre un groupe pour planifier une réunion.",
                textAlign = TextAlign.Center
            )
        }
        return
    }

    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedHour by remember { mutableStateOf(8) }
    var selectedEndHour by remember { mutableStateOf(9) }
    var room by remember { mutableStateOf("") }
    var isVisio by remember { mutableStateOf(false) }
    var selectedParticipants by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedHour by remember { mutableStateOf(false) }
    var expandedEndHour by remember { mutableStateOf(false) }
    var isScheduling by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planifier une réunion - $teamName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        if (isScheduling) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Planification de la réunion...")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Planifier une réunion",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = subject,
                                onValueChange = { subject = it },
                                label = { Text("Sujet de la réunion") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description (optionnel)") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = selectedDate.let { date ->
                                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH)
                                    val capitalizedDay = dayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                    "$capitalizedDay ${date.dayOfMonth}/${date.monthValue}/${date.year}"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Date") },
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Filled.DateRange, contentDescription = "Sélectionner une date")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePicker = true },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            ExposedDropdownMenuBox(expanded = expandedHour, onExpandedChange = { expandedHour = !expandedHour }) {
                                OutlinedTextField(
                                    value = "$selectedHour:00",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Heure de début") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHour) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = expandedHour, onDismissRequest = { expandedHour = false }) {
                                    (8..20).toList().forEach { hour ->
                                        DropdownMenuItem(
                                            text = { Text("$hour:00") },
                                            onClick = {
                                                selectedHour = hour
                                                expandedHour = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(expanded = expandedEndHour, onExpandedChange = { expandedEndHour = !expandedEndHour }) {
                                OutlinedTextField(
                                    value = "$selectedEndHour:00",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Heure de fin") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEndHour) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = expandedEndHour, onDismissRequest = { expandedEndHour = false }) {
                                    ((selectedHour + 1)..21).toList().forEach { hour ->
                                        DropdownMenuItem(
                                            text = { Text("$hour:00") },
                                            onClick = {
                                                selectedEndHour = hour
                                                expandedEndHour = false
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isVisio,
                                    onCheckedChange = { isVisio = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Réunion par visioconférence")
                            }

                            OutlinedTextField(
                                value = room,
                                onValueChange = { room = it },
                                label = { Text(if (isVisio) "Lien de visioconférence" else "Salle de réunion") },
                                placeholder = { Text(if (isVisio) "https://..." else "Ex: Salle A101") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Participants",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            if (teamMembers.isEmpty()) {
                                Text(
                                    text = "Chargement des membres...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    teamMembers.forEach { (userId, userName) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedParticipants = if (selectedParticipants.contains(userId)) {
                                                        selectedParticipants - userId
                                                    } else {
                                                        selectedParticipants + userId
                                                    }
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = selectedParticipants.contains(userId),
                                                onCheckedChange = { checked ->
                                                    selectedParticipants = if (checked) {
                                                        selectedParticipants + userId
                                                    } else {
                                                        selectedParticipants - userId
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = userName,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            isScheduling = true
                            val database = FirebaseDatabase.getInstance()
                            val agendaRef = database.getReference("teams").child(teamId!!).child("agenda")
                            
                            val meeting = Event(
                                title = subject,
                                description = description,
                                date = selectedDate.toString(),
                                day = selectedDate.dayOfWeek.name,
                                hour = selectedHour,
                                endHour = selectedEndHour,
                                participants = selectedParticipants.toList(),
                                isMeeting = true,
                                room = room,
                                isVisio = isVisio,
                                createdBy = currentUser?.uid ?: "",
                                createdByName = userName,
                                createdAt = System.currentTimeMillis()
                            )

                            val eventId = agendaRef.push().key
                            if (eventId != null) {
                                meeting.id = eventId
                                agendaRef.child(eventId).setValue(meeting).addOnCompleteListener { task ->
                                    isScheduling = false
                                    if (task.isSuccessful) {
                                        navController.popBackStack()
                                    }
                                }
                            } else {
                                isScheduling = false
                            }
                        },
                        enabled = subject.isNotBlank() && selectedParticipants.isNotEmpty() && room.isNotBlank() && !isScheduling,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isScheduling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Planifier la réunion")
                        }
                    }
                }
            }
        }
    }
}
