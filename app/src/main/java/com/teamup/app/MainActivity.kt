package com.teamup.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.teamup.app.ui.screens.agenda.AgendaScreen
import com.teamup.app.ui.screens.chat.ChatListScreen
import com.teamup.app.ui.screens.chat.ChatScreen
import com.teamup.app.ui.screens.document.DocumentsScreen
import com.teamup.app.ui.screens.home.MainScreen
import com.teamup.app.ui.screens.login.LoginScreen
import com.teamup.app.ui.screens.tasks.TasksScreen
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage

/**
 * MainActivity - Point d'entrée de TeamUp avec Jetpack Compose
 */

//file database


    private val supabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://psqwhvxxrlmhcummvlht.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBzcXdodnh4cmxtaGN1bW12bGh0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ1MzIyNjgsImV4cCI6MjA4MDEwODI2OH0.UsVbStaHz4xCQ-dylZou4dXPZBhAIo_y1QMW97gOGr4"
        ) {
            //httpEngine = Android.create()
            install(Storage)
            install(Auth)
        }
    }


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
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



@RequiresApi(Build.VERSION_CODES.O)
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

        composable("agenda") {
            AgendaScreen(navController)
        }

        composable("chatList") {
            ChatListScreen(navController)
        }
        
        // Chat: Conversation d'un groupe
        composable("chat/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            ChatScreen(navController, groupId)
        }

        composable("documents") {
            DocumentsScreen(navController,supabaseClient)
        }

    }
}