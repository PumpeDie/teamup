package com.teamup.app.ui.screens.group

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.teamup.app.data.repository.TeamRepository

@Composable
fun GroupSelectionScreen(
    navController: NavController,
    viewModel: GroupSelectionViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var isCheckingExistingTeam by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
        val existingTeamId = TeamRepository.getUserTeamId()
        if (!existingTeamId.isNullOrBlank()) {
            navController.navigate("home") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        } else {
             isCheckingExistingTeam = false
        }
    }

     if (isCheckingExistingTeam) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Vérification du Groupe de Connexion...")
            }
        }
        return
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (viewModel.isCreationMode) "Créer un Groupe de Connexion" else "Rejoindre un Groupe de Connexion",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (viewModel.isCreationMode) {
                OutlinedTextField(
                    value = viewModel.groupNameInput,
                    onValueChange = { viewModel.groupNameInput = it },
                    label = { Text("Nom du Groupe de Connexion") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = viewModel.groupCodeInput,
                    onValueChange = { viewModel.groupCodeInput = it },
                    label = { Text("ID du Groupe de Connexion") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (viewModel.message.isNotEmpty()) {
                Text(
                    text = viewModel.message,
                    color = if (viewModel.message.startsWith("Erreur:")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.createOrJoinGroup {

                        navController.navigate("home") {

                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading
            ) {
                Text(if (viewModel.isCreationMode) "Créer et Rejoindre" else "Rejoindre")
            }

            TextButton(onClick = { viewModel.toggleMode() }) {
                Text(
                    if (viewModel.isCreationMode)
                        "Déjà un ID de groupe ? Rejoignez-le"
                    else
                        "Pas de groupe ? Créez-en un nouveau"
                )
            }

            if (viewModel.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    }
}
