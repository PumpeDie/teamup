package com.teamup.app.ui.screens.document

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.teamup.app.data.model.DocumentModel
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.teamup.app.R
import coil.compose.AsyncImage
import com.teamup.app.data.repository.TeamRepository
import io.github.jan.supabase.SupabaseClient




@Composable
fun DocumentsScreen(
    navController: NavController,
    supabase: SupabaseClient,
) {

    val viewModel: DocumentsViewModel = viewModel(
        factory = DocumentsViewModelFactory(supabase)
    )

    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()

    // Launcher pour importer un fichier
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.uploadFile(uri, context)
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // Bouton importer en haut
        Button(
            onClick = { launcher.launch(arrayOf("*/*")) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Importer un document")
        }

        // Liste des documents
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            items(documents) { doc ->
                DocumentCard(doc, context,viewModel)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun DocumentCard(
    doc: DocumentModel,
    context: Context,
    viewModel: DocumentsViewModel
) {

    var showDialog by remember { mutableStateOf(false) }

    // --- Popup Confirmation ---
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Supprimer le document ?") },
            text = { Text("Cette action est définitive.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        viewModel.deleteFile(doc, context)
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // --- Carte clickable pour ouvrir ---
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable { openDocument(context, doc.url) },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Preview fichier
            DocumentPreview(doc.url)

            Spacer(modifier = Modifier.width(16.dp))

            // Titre + Date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.originalName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(Date(doc.uploadedAt)),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // --- Icône supprimer ---
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = "Supprimer",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        showDialog = true
                    }
            )
        }
    }
}


@Composable
fun DocumentPreview(url: String) {
    val isImage = url.endsWith(".jpg") || url.endsWith(".png")

    if (isImage) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        // Icône PDF / Document
        Icon(
            painter = painterResource(id = R.drawable.ic_file),
            contentDescription = "Fichier",
            modifier = Modifier.size(50.dp)
        )
    }
}

fun openDocument(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(url.toUri(), "*/*")
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

