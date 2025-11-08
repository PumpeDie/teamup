package com.teamup.app.ui.screens.login

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException


fun isValidPassword(password: String): Pair<Boolean, String> {
    if (password.length < 6) return Pair(false, "Le mot de passe doit contenir au moins 6 caractères")
    if (!password.any { it.isUpperCase() }) return Pair(false, "Le mot de passe doit contenir au moins une majuscule")
    if (!password.any { it.isLowerCase() }) return Pair(false, "Le mot de passe doit contenir au moins une minuscule")
    if (!password.any { it.isDigit() }) return Pair(false, "Le mot de passe doit contenir au moins un chiffre")
    if (!password.any { "!@#\$%^&*()-_=+[]{};:'\",.<>?/|\\~`".contains(it) })
        return Pair(false, "Le mot de passe doit contenir au moins un caractère spécial")
    return Pair(true, "")
}



fun getFriendlyFirebaseAuthError(e: Exception): String {
    Log.d("TAG", e.toString())
    return when (e) {
        is FirebaseAuthInvalidUserException -> "Aucun compte trouvé avec cet e-mail."
        is FirebaseAuthInvalidCredentialsException -> "Mot de passe incorrect."
        is FirebaseAuthUserCollisionException -> "Cette adresse mail est déjà associé à un compte"
        is FirebaseAuthException -> "Erreur d’authentification, veuillez réessayer."
        else -> "Une erreur est survenue : ${e.localizedMessage}"
    }
}






@Composable
fun LoginScreen(
    navController: NavController,
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) } // true = login, false = register
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLoginMode) "Connexion" else "Inscription",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Adresse e-mail") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (message.isNotEmpty()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isLoading = true
                if (isLoginMode) {
                    signInUser(auth, email, password, navController) { message = it; isLoading = false }
                } else {
                    val (valid, errorMsg) = isValidPassword(password)
                    if (!valid) {
                        message = errorMsg
                        isLoading = false
                    } else {
                        registerUser(auth, email, password, navController) { message = it; isLoading = false }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )  {
            Text(if (isLoginMode) "Se connecter" else "Créer un compte")
        }

        TextButton(onClick = { isLoginMode = !isLoginMode }) {
            Text(
                if (isLoginMode)
                    "Pas de compte ? Créez-en un"
                else
                    "Déjà un compte ? Connectez-vous"
            )
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

private fun signInUser(
    auth: FirebaseAuth,
    email: String,
    password: String,
    navController: NavController,
    onResult: (String) -> Unit
) {
    if (email.isBlank() || password.isBlank()) {
        onResult("Veuillez remplir tous les champs")
        return
    }

    auth.signInWithEmailAndPassword(email, password)
        .addOnSuccessListener {
            onResult("Connexion réussie")
            // Redirige vers Home et supprime l’écran de login
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
        .addOnFailureListener { exception ->
            onResult(getFriendlyFirebaseAuthError(exception))
        }
}

private fun registerUser(
    auth: FirebaseAuth,
    email: String,
    password: String,
    navController: NavController,
    onResult: (String) -> Unit
) {
    if (email.isBlank() || password.isBlank()) {
        onResult("Veuillez remplir tous les champs")
        return
    }

    auth.createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener {
            onResult("Compte créé")
            // 🔥 Redirige vers Home automatiquement après inscription
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
        .addOnFailureListener { exception ->
            onResult(getFriendlyFirebaseAuthError(exception))
        }
}
