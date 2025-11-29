package com.teamup.app

import com.teamup.app.ui.screens.group.GroupSelectionScreen
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.teamup.app.data.ChatRepository
import com.teamup.app.ui.screens.home.MainScreen
import com.teamup.app.ui.screens.login.LoginScreen
import com.teamup.app.ui.screens.tasks.TasksScreen
import com.teamup.app.ui.screens.chat.ChatListScreen
import com.teamup.app.ui.screens.chat.ChatScreen
import com.teamup.app.ui.screens.agenda.AgendaScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

     val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
         "loadingTeamCheck"
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
  composable("loadingTeamCheck") {
            LoadingGroupCheckScreen(navController)
        }

        composable("groupSelection") {
            GroupSelectionScreen(navController)
        }

        composable("groupInfo") {
            com.teamup.app.ui.screens.group.GroupManagementScreen(navController)
        }

        composable("home") {
            MainScreen(navController)
        }

        composable("tasks") {
            TasksScreen(navController)
        }
        composable("chatList") {
            ChatListScreen(navController)
        }


        composable("chat/{teamId}/{chatRoomId}") { backStackEntry ->
            ChatScreen(
                navController = navController,
                teamId = backStackEntry.arguments?.getString("teamId") ?: "",
                chatRoomId = backStackEntry.arguments?.getString("chatRoomId") ?: ""
            )
        }
        composable("agenda") {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                AgendaScreen(navController)
            } else {

                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("L'agenda nécessite Android 8.0+")
                }
            }
        }
    }
}

@Composable
fun LoadingGroupCheckScreen(navController: NavController) {
    var isChecking by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val teamId = ChatRepository.getUserTeamId() // Vérifie l'appartenance au TeamGroup

            if (!teamId.isNullOrBlank()) {

                navController.navigate("home") {

                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            } else {

                navController.navigate("groupSelection") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        } catch (e: Exception) {
            Log.e("LoadingGroupCheck", "Erreur lors de la vérification du groupe", e)
            errorMessage = e.message
            isChecking = false


            kotlinx.coroutines.delay(2000)
            navController.navigate("groupSelection") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isChecking) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Vérification du Groupe de Connexion...")
            } else {
                Text(
                    text = "Erreur: ${errorMessage ?: "Erreur inconnue"}",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Redirection vers la sélection de groupe...")
            }
        }
    }
}
