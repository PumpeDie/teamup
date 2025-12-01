package com.teamup.app.ui.screens.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.jan.supabase.SupabaseClient

class DocumentsViewModelFactory(
    private val supabase: SupabaseClient
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocumentsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DocumentsViewModel(supabase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
