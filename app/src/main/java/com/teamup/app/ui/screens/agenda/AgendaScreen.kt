package com.teamup.app.ui.screens.agenda

import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable // Import
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale


@RequiresApi(Build.VERSION_CODES.O)
@Keep
data class Event  constructor(
    var id: String = "",
    var title: String = "",
    var day: String = DayOfWeek.MONDAY.name,
    var hour: Int = 8
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(navController: NavController) {

    val eventsRef = FirebaseDatabase.getInstance().getReference("events")
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    var selectedEvent by remember { mutableStateOf<Event?>(null) }


    LaunchedEffect(Unit) {
        eventsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedEvents = snapshot.children.mapNotNull { eventSnapshot ->
                    val event = eventSnapshot.getValue(Event::class.java)
                    event?.id = eventSnapshot.key ?: ""
                    event
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
                title = { Text("Agenda Hebdomadaire") },
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
                events = events,
                onEventClick = { event ->
                    selectedEvent = event
                }
            )
        }




        if (showAddDialog) {
            AddEventDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { newEvent ->
                    val newEventId = eventsRef.push().key
                    if (newEventId != null) {
                        newEvent.id = newEventId
                        eventsRef.child(newEventId).setValue(newEvent)
                    }
                    showAddDialog = false
                }
            )
        }

        // NOUVEAU: Dialogue des détails de l'événement
        if (selectedEvent != null) {
            EventDetailsDialog(
                event = selectedEvent!!,
                onDismiss = { selectedEvent = null }, // Pour fermer le dialogue
                onDelete = {
                    eventsRef.child(selectedEvent!!.id).removeValue()
                    selectedEvent = null
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(onDismiss: () -> Unit, onConfirm: (Event) -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf(DayOfWeek.MONDAY) }
    var selectedHour by remember { mutableStateOf(8) }
    var expandedDay by remember { mutableStateOf(false) }
    var expandedHour by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel Événement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'événement") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(expanded = expandedDay, onExpandedChange = { expandedDay = !expandedDay }) {
                    OutlinedTextField(
                        value = selectedDay.getDisplayName(TextStyle.FULL, Locale.FRENCH),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Jour") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDay) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedDay, onDismissRequest = { expandedDay = false }) {
                        DayOfWeek.values().forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day.getDisplayName(TextStyle.FULL, Locale.FRENCH)) },
                                onClick = {
                                    selectedDay = day
                                    expandedDay = false
                                }
                            )
                        }
                    }
                }

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
                        day = selectedDay.name,
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
@Composable
fun WeeklyAgendaView(
    modifier: Modifier = Modifier,
    events: List<Event>,
    onEventClick: (Event) -> Unit
) {
    val daysOfWeek = DayOfWeek.values()
    val hoursOfDay = (8..20).toList()

    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Header(days = daysOfWeek)
        LazyColumn {
            items(hoursOfDay) { hour ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HourCell(hour = "$hour:00")
                    daysOfWeek.forEach { day ->
                        val event = events.find { it.day == day.name && it.hour == hour }
                        DayCell(
                            modifier = Modifier.weight(1f),
                            event = event,
                            onEventClick = onEventClick
                        )
                    }
                }
                Divider()
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    modifier: Modifier,
    event: Event?,
    onEventClick: (Event) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .padding(2.dp)
            .clickable(enabled = event != null) {
                if (event != null) {
                    onEventClick(event)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (event != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun Header(days: Array<DayOfWeek>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(50.dp))
        days.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.FRENCH).uppercase(),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Divider()
}

@Composable
private fun RowScope.HourCell(hour: String) {
    Text(
        text = hour,
        modifier = Modifier
            .width(50.dp)
            .padding(end = 4.dp),
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodySmall
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventDetailsDialog(
    event: Event,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val dayOfWeek = try {
        DayOfWeek.valueOf(event.day)
    } catch (e: Exception) {
        null
    }
    val dayDisplayName = dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.FRENCH) ?: event.day

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Détails de l'événement") },
        text = {
            // Afficher les détails de l'événement
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Titre:", style = MaterialTheme.typography.labelMedium)
                Text(event.title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))

                Text("Jour:", style = MaterialTheme.typography.labelMedium)
                Text(dayDisplayName.replaceFirstChar { it.titlecase(Locale.FRENCH) }, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))

                Text("Heure:", style = MaterialTheme.typography.labelMedium)
                Text("${event.hour}:00", style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Supprimer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}