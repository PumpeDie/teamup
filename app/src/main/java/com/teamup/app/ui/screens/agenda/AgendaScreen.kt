package com.teamup.app.ui.screens.agenda

import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.teamup.app.data.repository.TeamRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Keep
data class Event constructor(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var date: String = "", // Format: yyyy-MM-dd
    var day: String = DayOfWeek.MONDAY.name,
    var hour: Int = 8,
    var endHour: Int = 9, // Heure de fin
    var createdBy: String = "",
    var createdByName: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var participants: List<String> = emptyList(), // Liste des IDs des participants pour les réunions
    var isMeeting: Boolean = false, // true si c'est une réunion planifiée
    var room: String = "", // Salle de réunion ou lien visio
    var isVisio: Boolean = false // true si c'est une réunion visio
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var userName by remember { mutableStateOf("Utilisateur") }
    var teamId by remember { mutableStateOf<String?>(null) }
    var teamName by remember { mutableStateOf("Chargement...") }

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
    }

    if (teamId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Chargement du Team...")
        }
        return
    }

    if (teamId!!.isBlank()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                "Veuillez d'abord créer ou rejoindre un Groupe de Connexion pour voir l'agenda.",
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val database = FirebaseDatabase.getInstance()
    val agendaRef = database.getReference("teams").child(teamId!!).child("agenda")

    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var eventToEdit by remember { mutableStateOf<Event?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Gestion de la semaine actuelle
    var currentWeekStart by remember { mutableStateOf(LocalDate.now().with(DayOfWeek.MONDAY)) }
    var currentDayOffset by remember { mutableStateOf(0) } // Pour naviguer par tranches de 3 jours

    // Calculer les dates de des 3 jours
    val visibleDates = remember(currentWeekStart, currentDayOffset) {
        val startDate = currentWeekStart.plusDays(currentDayOffset.toLong())
        (0..2).map { startDate.plusDays(it.toLong()) } // 3 jours seulement
    }

    // Filtrer les événements visibles pendant les 3 jours
    val visibleEvents = remember(events, visibleDates) {
        val dateStrings = visibleDates.map { it.toString() }
        events.filter { event ->
            event.date in dateStrings || (event.date.isEmpty() && visibleDates.any {
                it.dayOfWeek.name == event.day
            })
        }
    }

    LaunchedEffect(teamId) {
        agendaRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedEvents = mutableListOf<Event>()
                snapshot.children.forEach { eventSnapshot ->
                    val event = eventSnapshot.getValue(Event::class.java)
                    if (event != null) {
                        event.id = eventSnapshot.key ?: ""
                        fetchedEvents.add(event)
                    }
                }
                events = fetchedEvents
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
                title = { Text("Agenda - $teamName") },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, "Ajouter un événement")
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            WeeklyAgendaView(
                modifier = Modifier.padding(innerPadding),
                events = visibleEvents,
                weekDates = visibleDates,
                currentWeekStart = currentWeekStart,
                currentDayOffset = currentDayOffset,
                onWeekChange = { newOffset ->
                    currentDayOffset = newOffset
                },
                onEventClick = { event ->
                    if (event.isMeeting) {
                        eventToEdit = event
                        showEditDialog = true
                    } else {
                        selectedEvent = event
                    }
                }
            )
        }

        if (showAddDialog) {
            AddEventDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { newEvent ->
                    newEvent.createdBy = currentUser?.uid ?: ""
                    newEvent.createdByName = userName
                    newEvent.createdAt = System.currentTimeMillis()

                    val eventId = agendaRef.push().key
                    if (eventId != null) {
                        newEvent.id = eventId
                        agendaRef.child(eventId).setValue(newEvent)
                    }
                    showAddDialog = false
                }
            )
        }

        if (selectedEvent != null) {
            EventDetailsDialog(
                event = selectedEvent!!,
                onDismiss = { selectedEvent = null },
                onEdit = { event ->
                    selectedEvent = null
                    eventToEdit = event
                },
                onDelete = {
                    agendaRef.child(selectedEvent!!.id).removeValue()
                    selectedEvent = null
                }
            )
        }

        if (eventToEdit != null && !showEditDialog) {
            EditEventDialog(
                event = eventToEdit!!,
                onDismiss = { eventToEdit = null },
                onConfirm = { updatedEvent ->
                    updatedEvent.id = eventToEdit!!.id
                    if (updatedEvent.createdBy.isEmpty()) {
                        updatedEvent.createdBy = eventToEdit!!.createdBy
                    }
                    if (updatedEvent.createdByName.isEmpty()) {
                        updatedEvent.createdByName = eventToEdit!!.createdByName
                    }
                    if (updatedEvent.createdAt == 0L) {
                        updatedEvent.createdAt = eventToEdit!!.createdAt
                    }

                    agendaRef.child(updatedEvent.id).setValue(updatedEvent)
                    eventToEdit = null
                }
            )
        }

        if (showEditDialog && eventToEdit != null) {
            EditMeetingDialog(
                event = eventToEdit!!,
                onDismiss = {
                    showEditDialog = false
                    eventToEdit = null
                },
                onConfirm = { updatedMeeting ->
                    updatedMeeting.id = eventToEdit!!.id
                    updatedMeeting.createdBy = eventToEdit!!.createdBy
                    updatedMeeting.createdByName = eventToEdit!!.createdByName
                    updatedMeeting.createdAt = eventToEdit!!.createdAt

                    agendaRef.child(updatedMeeting.id).setValue(updatedMeeting)
                    showEditDialog = false
                    eventToEdit = null
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (Event) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Par défaut c'est la date du jour
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedHour by remember { mutableStateOf(8) }

    var showDatePicker by remember { mutableStateOf(false) }
    var expandedHour by remember { mutableStateOf(false) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel Événement") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'événement") },
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
                        label = { Text("Heure") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newEvent = Event(
                        title = title,
                        description = description,
                        date = selectedDate.toString(),
                        day = selectedDate.dayOfWeek.name,
                        hour = selectedHour
                    )
                    onConfirm(newEvent)
                },
                enabled = title.isNotBlank()
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventDialog(
    event: Event,
    onDismiss: () -> Unit,
    onConfirm: (Event) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }

    var selectedDate by remember {
        mutableStateOf(
            if (event.date.isNotEmpty()) {
                try {
                    LocalDate.parse(event.date)
                } catch (e: Exception) {
                    LocalDate.now()
                }
            } else {
                LocalDate.now()
            }
        )
    }

    var selectedHour by remember { mutableStateOf(event.hour) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedHour by remember { mutableStateOf(false) }

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
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier l'Événement") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'événement") },
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
                            Icon(Icons.Filled.DateRange, contentDescription = "Changer la date")
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
                        label = { Text("Heure") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedEvent = event.copy(
                        title = title,
                        description = description,
                        date = selectedDate.toString(),
                        day = selectedDate.dayOfWeek.name,
                        hour = selectedHour
                    )
                    onConfirm(updatedEvent)
                },
                enabled = title.isNotBlank()
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeeklyAgendaView(
    modifier: Modifier = Modifier,
    events: List<Event>,
    weekDates: List<LocalDate>,
    currentWeekStart: LocalDate,
    currentDayOffset: Int = 0,
    onWeekChange: (Int) -> Unit,
    onEventClick: (Event) -> Unit
) {
    val hoursOfDay = (8..20).toList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Header(
            weekDates = weekDates,
            currentWeekStart = currentWeekStart,
            currentDayOffset = currentDayOffset,
            onPreviousDays = { onWeekChange(currentDayOffset - 3) },
            onNextDays = { onWeekChange(currentDayOffset + 3) },
            onCurrentDays = { onWeekChange(0) }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(hoursOfDay) { hour ->
                val eventsForHour = weekDates.map { date ->
                    val dateString = date.toString()
                    events.find {
                        (it.date == dateString || (it.date.isEmpty() && it.day == date.dayOfWeek.name))
                                && it.hour == hour
                    }
                }

                val hasEvents = eventsForHour.any { it != null }
                val maxDescriptionLines = eventsForHour
                    .filterNotNull()
                    .maxOfOrNull {
                        if (it.description.isNotBlank()) {
                            val estimatedLines = ((it.description.length / 35.0).toInt() + 1).coerceIn(1, 6)
                            estimatedLines
                        } else 0
                    } ?: 0

                val baseHeight = 80.dp
                val descriptionHeight = if (maxDescriptionLines > 0) {
                    (maxDescriptionLines * 14).dp + 8.dp
                } else {
                    0.dp
                }
                val minHeight = if (hasEvents) {
                    (baseHeight + descriptionHeight).coerceAtLeast(70.dp).coerceAtMost(200.dp) // Réduit les limites
                } else {
                    60.dp
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minHeight)
                        .background(
                            if (hour % 2 == 0) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            } else {
                                Color.Transparent
                            }
                        ),
                    verticalAlignment = Alignment.Top
                ) {
                    HourCell(hour = "$hour:00")
                    weekDates.forEachIndexed { index, date ->
                        val event = eventsForHour[index]
                        // Détection du Week-End
                        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

                        DayCell(
                            modifier = Modifier.weight(1f),
                            event = event,
                            isWeekend = isWeekend,
                            onEventClick = onEventClick
                        )
                    }
                }
                if (hour < hoursOfDay.last()) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    modifier: Modifier,
    event: Event?,
    isWeekend: Boolean, // Paramètre pour colorer le week-end
    onEventClick: (Event) -> Unit
) {
    // Couleur violette pour le week-end
    val weekendColor = Color(0xFFE1BEE7).copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(0.dp)
            )
            .background(
                if (isWeekend) {
                    weekendColor
                } else {
                    Color.Transparent
                }
            )
            .padding(horizontal = 5.dp, vertical = 3.dp)
            .clickable(enabled = event != null) {
                if (event != null) {
                    onEventClick(event)
                }
            },
        contentAlignment = Alignment.TopStart
    ) {
        if (event != null) {
            val colorIndex = event.title.hashCode() % 5
            val eventColors = listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
            )
            val textColors = listOf(
                MaterialTheme.colorScheme.onPrimaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer
            )

            val backgroundColor = eventColors[Math.abs(colorIndex)]
            val textColor = textColors[Math.abs(colorIndex)]

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor,
                                backgroundColor.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        color = textColor,
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (event.isMeeting) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = "Réunion",
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (event.isMeeting && event.participants.isNotEmpty()) {
                    Text(
                        text = "${event.participants.size} participant(s)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = textColor.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start
                    )
                }

                if (event.isMeeting) {
                    val timeRange = if (event.endHour > event.hour) {
                        "${event.hour}:00 - ${event.endHour}:00"
                    } else {
                        "${event.hour}:00"
                    }

                    Text(
                        text = timeRange,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = textColor.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start
                    )
                }

                if (event.isMeeting && event.room.isNotBlank()) {
                    val roomText = if (event.isVisio) "📹 ${event.room}" else "📍 ${event.room}"
                    Text(
                        text = roomText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = textColor.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start,
                        maxLines = 1
                    )
                }

                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = textColor.copy(alpha = 0.95f),
                        textAlign = TextAlign.Start,
                        lineHeight = 14.sp,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun Header(
    weekDates: List<LocalDate>,
    currentWeekStart: LocalDate,
    currentDayOffset: Int = 0,
    onPreviousDays: () -> Unit,
    onNextDays: () -> Unit,
    onCurrentDays: () -> Unit
) {
    val startDate = currentWeekStart.plusDays(currentDayOffset.toLong())
    val endDate = startDate.plusDays(2)
    val isCurrentDays = LocalDate.now().let { today ->
        today.isAfter(startDate.minusDays(1)) && today.isBefore(endDate.plusDays(1))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${startDate.dayOfMonth} ${startDate.month.name.take(3)} - ${endDate.dayOfMonth} ${endDate.month.name.take(3)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousDays,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "3 jours précédents",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (!isCurrentDays) onCurrentDays()
                        },
                        enabled = !isCurrentDays,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isCurrentDays) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                }
                            )
                    ) {
                        Icon(
                            Icons.Filled.Today,
                            contentDescription = "Aujourd'hui",
                            tint = if (isCurrentDays) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onNextDays,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "3 jours suivants",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Ligne des jours de la semaine
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Spacer(modifier = Modifier.width(70.dp))

            weekDates.forEach { date ->
                val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                val isToday = date == LocalDate.now()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                isWeekend -> Color(0xFFE1BEE7).copy(alpha = 0.5f) // Violet pour l'en-tête aussi
                                else -> Color.Transparent
                            }
                        )
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRENCH).uppercase(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isToday) {
                                MaterialTheme.colorScheme.primary
                            } else if (isWeekend) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = "${date.dayOfMonth}",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isToday) {
                                MaterialTheme.colorScheme.primary
                            } else if (isWeekend) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
    HorizontalDivider(
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    )
}

@Composable
private fun RowScope.HourCell(hour: String) {
    Box(
        modifier = Modifier
            .width(70.dp)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = hour,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
                .padding(horizontal = 6.dp, vertical = 6.dp)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventDetailsDialog(
    event: Event,
    onDismiss: () -> Unit,
    onEdit: (Event) -> Unit,
    onDelete: () -> Unit
) {
    val displayDate = try {
        if(event.date.isNotEmpty()) {
            val date = LocalDate.parse(event.date)
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH)
            "$dayName ${date.dayOfMonth}/${date.monthValue}/${date.year}"
        } else {
            val dayOfWeek = DayOfWeek.valueOf(event.day)
            dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH)
        }
    } catch (e: Exception) {
        event.day
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.widthIn(max = 400.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            displayDate.replaceFirstChar { it.titlecase(Locale.FRENCH) },
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Heure",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${event.hour}:00",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }

                HorizontalDivider()

                if (event.description.isNotBlank()) {
                    Column {
                        Text(
                            "Description",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            event.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        "Aucune description",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                if (event.createdByName.isNotBlank()) {
                    HorizontalDivider()
                    Text(
                        "Créé par: ${event.createdByName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onEdit(event) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modifier")
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Supprimer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMeetingDialog(
    event: Event,
    onDismiss: () -> Unit,
    onConfirm: (Event) -> Unit
) {
    var subject by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }
    var selectedHour by remember { mutableStateOf(event.hour) }
    var selectedEndHour by remember { mutableStateOf(event.endHour) }
    var room by remember { mutableStateOf(event.room) }
    var isVisio by remember { mutableStateOf(event.isVisio) }
    var selectedParticipants by remember { mutableStateOf(event.participants.toSet()) }
    var expandedHour by remember { mutableStateOf(false) }
    var expandedEndHour by remember { mutableStateOf(false) }
    var teamMembers by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoadingMembers by remember { mutableStateOf(true) }

    // Charger les membres du groupe
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val team = TeamRepository.getUserTeam()

        team?.teamId?.let { teamId ->
            isLoadingMembers = true
            TeamRepository.getTeamMembersDetails(teamId)
                .onSuccess { members ->
                    // Ajouter l'utilisateur courant s'il n'est pas dans la liste
                    val allMembers = if (currentUser != null && members.none { it.first == currentUser.uid }) {
                        members + Pair(currentUser.uid, currentUser.displayName ?: currentUser.email ?: "Utilisateur")
                    } else {
                        members
                    }
                    teamMembers = allMembers
                    isLoadingMembers = false
                }
                .onFailure { error ->
                    Log.e("EditMeeting", "Erreur chargement membres: ${error.message}")
                    isLoadingMembers = false
                }
        } ?: run {
            isLoadingMembers = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Modifier la réunion",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section Informations de base
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Informations de la réunion",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

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
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Section Horaires
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Horaires",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = expandedHour,
                                onExpandedChange = { expandedHour = !expandedHour },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = "$selectedHour:00",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Début") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHour) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = expandedHour, onDismissRequest = { expandedHour = false }) {
                                    (8..20).toList().forEach { hour ->
                                        DropdownMenuItem(
                                            text = { Text("$hour:00") },
                                            onClick = {
                                                selectedHour = hour
                                                if (selectedEndHour <= selectedHour) {
                                                    selectedEndHour = selectedHour + 1
                                                }
                                                expandedHour = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = expandedEndHour,
                                onExpandedChange = { expandedEndHour = !expandedEndHour },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = "$selectedEndHour:00",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Fin") },
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
                        }
                    }
                }

                // Section Lieu/Visio
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Lieu",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

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

                // Section Participants
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Participants (${selectedParticipants.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (isLoadingMembers) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.height(200.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(teamMembers) { (memberId, memberName) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (memberId in selectedParticipants) {
                                                    selectedParticipants -= memberId
                                                } else {
                                                    selectedParticipants += memberId
                                                }
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = memberId in selectedParticipants,
                                            onCheckedChange = {
                                                if (memberId in selectedParticipants) {
                                                    selectedParticipants -= memberId
                                                } else {
                                                    selectedParticipants += memberId
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = memberName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedEvent = event.copy(
                        title = subject,
                        description = description,
                        hour = selectedHour,
                        endHour = selectedEndHour,
                        room = room,
                        isVisio = isVisio,
                        participants = selectedParticipants.toList()
                    )
                    onConfirm(updatedEvent)
                },
                enabled = subject.isNotBlank() && room.isNotBlank() && selectedParticipants.isNotEmpty()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}