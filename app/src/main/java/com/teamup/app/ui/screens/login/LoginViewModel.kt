package com.teamup.app.ui.screens.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamup.app.data.repository.AuthRepository
import com.teamup.app.util.isValidPassword
import com.teamup.app.util.getFriendlyFirebaseAuthError
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    var message by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var isLoginMode by mutableStateOf(true)

    fun toggleMode() {
        isLoginMode = !isLoginMode
        message = ""
    }

    fun authenticate(email: String, password: String, username: String = "", onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            message = "Veuillez remplir tous les champs"
            return
        }

        if (!isLoginMode) {
            if (username.isBlank()) {
                message = "Veuillez entrer un nom d'utilisateur"
                return
            }
            val (valid, errorMsg) = isValidPassword(password)
            if (!valid) {
                message = errorMsg
                return
            }
        }

        isLoading = true
        viewModelScope.launch {
            val result = if (isLoginMode)
                authRepository.signIn(email, password)
            else
                authRepository.register(email, password, username)

            result.fold(
                onSuccess = {
                    message = if (isLoginMode) "Connexion réussie" else "Compte créé"
                    onSuccess()
                },
                onFailure = {
                    message = getFriendlyFirebaseAuthError(it)
                }
            )
            isLoading = false
        }
    }
}
