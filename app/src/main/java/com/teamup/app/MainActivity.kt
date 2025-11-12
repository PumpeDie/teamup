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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.FirebaseDatabase
import com.teamup.app.ui.screens.home.MainScreen
import com.teamup.app.ui.screens.login.LoginScreen
import com.teamup.app.ui.screens.tasks.TasksScreen
import com.teamup.app.ui.screens.chat.ChatListScreen
import com.teamup.app.ui.screens.chat.ChatScreen

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
            AppNavigation()
        }
    }
}



@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Détermine la page de démarrage selon la session Firebase
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        "home"
    } else {
        "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(navController)
        }
        composable("home") {
            MainScreen(navController)
        }
        composable("tasks") {
            TasksScreen(navController)
        }
        
        // Chat: Liste des groupes
        composable("chatList") {
            ChatListScreen(navController)
        }
        
        // Chat: Conversation d'un groupe
        composable("chat/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            ChatScreen(navController, groupId)
        }
    }
}