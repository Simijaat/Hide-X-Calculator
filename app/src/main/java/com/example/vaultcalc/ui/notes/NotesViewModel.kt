package com.example.vaultcalc.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vaultcalc.data.security.VaultSecurityManager
import com.example.vaultcalc.data.crypto.VaultCryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class Note(val id: String, val title: String, val content: String)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val securityManager: VaultSecurityManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()
    
    private val notesFile: File
        get() {
            val dir = File(context.getExternalFilesDir(null), ".VaultCalc")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return File(dir, "notes_enc.bin")
        }
        
    init {
        loadNotes()
    }
    
    private fun loadNotes() {
        viewModelScope.launch {
            if (!notesFile.exists() || securityManager.activeMasterKey == null) return@launch
            
            try {
                val encryptedData = notesFile.readBytes()
                if (encryptedData.isEmpty()) return@launch
                
                // Assuming first 12 bytes are IV
                val iv = encryptedData.sliceArray(0..11)
                val cipherText = encryptedData.sliceArray(12 until encryptedData.size)
                
                val decryptedJson = String(VaultCryptoManager.decryptData(cipherText, iv, securityManager.activeMasterKey!!))
                val jsonArray = JSONArray(decryptedJson)
                
                val loadedNotes = mutableListOf<Note>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    loadedNotes.add(Note(obj.getString("id"), obj.getString("title"), obj.getString("content")))
                }
                _notes.value = loadedNotes
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun persistNotes() {
        if (securityManager.activeMasterKey == null) return
        try {
            val jsonArray = JSONArray()
            _notes.value.forEach { note ->
                val obj = JSONObject()
                obj.put("id", note.id)
                obj.put("title", note.title)
                obj.put("content", note.content)
                jsonArray.put(obj)
            }
            
            val (cipherText, iv) = VaultCryptoManager.encryptData(jsonArray.toString().toByteArray(), securityManager.activeMasterKey!!)
            notesFile.writeBytes(iv + cipherText)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun saveNote(id: String, title: String, content: String) {
        val currentNotes = _notes.value.toMutableList()
        if (id.isEmpty()) {
            currentNotes.add(Note(java.util.UUID.randomUUID().toString(), title, content))
        } else {
            val idx = currentNotes.indexOfFirst { it.id == id }
            if (idx != -1) {
                currentNotes[idx] = Note(id, title, content)
            }
        }
        _notes.value = currentNotes
        persistNotes()
    }
    
    fun deleteNote(id: String) {
        _notes.value = _notes.value.filter { it.id != id }
        persistNotes()
    }
}
