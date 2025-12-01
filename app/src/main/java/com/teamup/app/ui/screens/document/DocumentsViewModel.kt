package com.teamup.app.ui.screens.document

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.*
import com.teamup.app.data.model.DocumentModel
import com.teamup.app.data.repository.TeamRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class DocumentsViewModel(
    private val supabase: SupabaseClient   // Injecté dans MainActivity ou via DI si tu veux
) : ViewModel() {

    //private var dbRef = FirebaseDatabase.getInstance().getReference("file-storage")
    private lateinit var dbRef: DatabaseReference
    private var teamId: String? = null

    private val _documents = MutableStateFlow<List<DocumentModel>>(emptyList())
    val documents = _documents.asStateFlow()

    init {
        viewModelScope.launch {
            // Récupère le teamId pour l'utilisateur connecté
            val currentTeamId = TeamRepository.getUserTeamId()
            if (!currentTeamId.isNullOrBlank()) {
                teamId = currentTeamId
                // Référence Firebase sous teams/<teamId>/file-storage
                dbRef = FirebaseDatabase.getInstance()
                    .getReference("teams")
                    .child(teamId!!)
                    .child("file-storage")

                loadDocuments()
            }
        }
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "document"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOpenableColumnsDisplayNameSafe()
            if (cursor.moveToFirst() && nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }

        return name
    }

    // Extension pour rendre l'accès robuste
    fun android.database.Cursor.getColumnIndexOpenableColumnsDisplayNameSafe(): Int {
        return try { getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME) }
        catch (e: Exception) { -1 }
    }

    private fun loadDocuments() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val list = snapshot.children.mapNotNull { snap ->
                    snap.getValue(DocumentModel::class.java)?.copy(id = snap.key ?: "")
                }

                _documents.value = list
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    fun uploadFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                // 💡 TOUT LE BLOC I/O ET RÉSEAU DOIT ÊTRE ENVELOPPÉ DANS withContext(Dispatchers.IO)
                withContext(Dispatchers.IO) {

                    // 1. Opération I/O (lecture du fichier)
                    val fileBytes = context.contentResolver.openInputStream(uri)?.readBytes()
                        ?: throw Exception("Impossible de lire le fichier")

                    // Récupérer le type MIME
                    val mimeType = context.contentResolver.getType(uri) // ex: "image/png", "application/pdf"

                    // Convertir MIME en extension
                    val extension = mimeType?.substringAfterLast("/") ?: "dat" // fallback en .dat si inconnu

                    // Générer un nom unique avec l'extension
                    val originalName = getFileName(context, uri) // ex: "photo.png"
                    val safeOriginalName = originalName.replace(" ", "_") // éviter espaces

                    val fileName = "${UUID.randomUUID()}_$safeOriginalName"

                    //val fileName = "${UUID.randomUUID()}.$extension"

                    // 2. Opération Réseau (Upload Supabase)
                    supabase.storage
                        .from("file-storage")
                        .upload("$teamId/$fileName", fileBytes)

                    // 3. Opération Réseau (Récupérer l'URL publique)
                    val publicUrl = supabase.storage
                        .from("file-storage")
                        .publicUrl("$teamId/$fileName")

                    // --- Retour sur le Main Thread implicite (pour les mises à jour Firebase/UI si besoin) ---
                    val key = dbRef.push().key ?: return@withContext

                    val data = DocumentModel(
                        id = key,
                        url = publicUrl,
                        originalName = originalName,
                        uploadedAt = System.currentTimeMillis()
                    )

                    dbRef.child(key).setValue(data)

                } // Fin de withContext(Dispatchers.IO)

                // Le Toast peut être affiché après que l'opération est réussie ou échouée
                Toast.makeText(context, "Upload réussi", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Le Toast doit être appelé sur le Main Thread (ce qui est le cas ici car on est après withContext)
                Toast.makeText(context, "Erreur lors de l'upload: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    fun deleteFile(doc: DocumentModel, context: Context) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {

                    // 1) Extraire le fichier depuis l'URL Supabase
                    val storagePath = doc.url.substringAfterLast("/")

                    // 2) Supprimer dans Supabase
                    supabase.storage
                        .from("file-storage")
                        .delete("$teamId/$storagePath")

                    // 3) Supprimer dans Firebase
                    dbRef.child(doc.id).removeValue()
                }

                Toast.makeText(context, "Document supprimé", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

}
