package com.teamup.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp

import com.google.firebase.database.FirebaseDatabase

/**
 * MainActivity - Point d'entrée de TeamUp avec Jetpack Compose
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

       FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Référence vers la base de données
        val database = FirebaseDatabase.getInstance()

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

/**
 * Écran principal de l'app
 */
@Composable
fun MainScreen() {
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
                text = "MVP - Développez nos 3 fonctionnalités",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Boutons pour les 3 fonctionnalités
            Button(
                onClick = { /* TODO: Navigation vers Groupes */ },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("📋 Groupes")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { /* TODO: Navigation vers Tâches */ },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("✓ Tâches")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { /* TODO: Navigation vers Chat */ },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("💬 Chat")
            }
        }
    }
}