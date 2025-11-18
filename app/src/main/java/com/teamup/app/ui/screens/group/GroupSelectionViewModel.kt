package com.teamup.app.ui.screens.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamup.app.data.ChatRepository
import kotlinx.coroutines.launch

class GroupSelectionViewModel : ViewModel() {

    var groupNameInput by mutableStateOf("")
    var groupCodeInput by mutableStateOf("")
    var message by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var isCreationMode by mutableStateOf(true)

    fun toggleMode() {
        isCreationMode = !isCreationMode
        message = ""
    }

    fun createOrJoinGroup(onSuccess: () -> Unit) {
        isLoading = true
        message = ""

        viewModelScope.launch {
            val result = if (isCreationMode) {
                if (groupNameInput.isBlank()) {
                    message = "Veuillez entrer un nom de groupe"
                    isLoading = false
                    return@launch
                }
                ChatRepository.createTeam(groupNameInput) // Crée un TeamGroup

            } else {
                 if (groupCodeInput.isBlank()) {
                    message = "Veuillez entrer un ID de groupe"
                    isLoading = false
                    return@launch
                }
                ChatRepository.joinTeam(groupCodeInput) // Rejoindre un TeamGroup
            }

             result.fold(
                onSuccess = {
                    message = if (isCreationMode) "Groupe de Connexion créé!" else "Groupe de Connexion rejoint!"
                    isLoading = false
                    onSuccess() },
                onFailure = {
                    message = "Erreur: ${it.message}"
                    isLoading = false
                }
            )
        }
    }
}