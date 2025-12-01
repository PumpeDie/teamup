package com.teamup.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.teamup.app.data.repository.TeamRepository


/**
 * Écran principal de l'app
 */
@Composable
fun MainScreen(navController: NavController) {

    var teamName by remember { mutableStateOf("Chargement...") }

    LaunchedEffect(Unit) {
        val team = TeamRepository.getUserTeam()
        teamName = if (team == null) {
            "Aucun groupe"
        } else {
            team.teamName
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bienvenue sur TeamUp!",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(16.dp))


            Text(
                text = "Groupe : $teamName",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate("groupInfo") },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Gestion du groupe")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { navController.navigate("tasks") },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Tâches")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { navController.navigate("chatList") },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Chats")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { navController.navigate("agenda") },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Agenda")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { navController.navigate("documents") },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Documents")
            }

            Spacer(modifier = Modifier.height(12.dp))


            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true } }
                },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Se déconnecter")
            }



        }
    }
}
