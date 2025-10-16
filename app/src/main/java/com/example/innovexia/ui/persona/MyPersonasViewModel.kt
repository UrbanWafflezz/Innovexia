package com.example.innovexia.ui.persona

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.core.auth.ProfileId
import com.example.innovexia.core.persona.PersonaDto
import com.example.innovexia.core.persona.PersonaRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyPersonasViewModel(
    private val repo: PersonaRepository,
    private val prefs: com.example.innovexia.core.persona.PersonaPreferences
) : ViewModel() {

    val uid: String?
        get() = Firebase.auth.currentUser?.uid

    val isSignedIn: Boolean
        get() = uid != null

    val ownerId: String
        get() = uid ?: ProfileId.GUEST_OWNER_ID

    private val _my = MutableStateFlow<List<Persona>>(emptyList())
    val my: StateFlow<List<Persona>> = _my.asStateFlow()

    private val _public = MutableStateFlow<List<Persona>>(emptyList())
    val public: StateFlow<List<Persona>> = _public.asStateFlow()

    private val _activePersonaId = MutableStateFlow<String?>(null)
    val activePersonaId: StateFlow<String?> = _activePersonaId.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    fun start() {
        // IMPORTANT: First deduplicate any Inno personas before loading
        viewModelScope.launch {
            android.util.Log.d("MyPersonasViewModel", "Deduplicating Inno personas for ownerId=$ownerId")
            repo.deduplicateInnoPersonas(ownerId)
        }

        // Observe personas for current owner (guest or signed-in)
        viewModelScope.launch {
            repo.observeMyPersonasEntities(ownerId).collect { entities ->
                android.util.Log.d("MyPersonasViewModel", "Received ${entities.size} personas from Room for ownerId=$ownerId")
                entities.forEach { p ->
                    android.util.Log.d("MyPersonasViewModel", "  - id=${p.id}, name=${p.name}")
                }
                _my.value = entities.map { it.toUiPersona() }
            }
        }

        // Always observe public Innovexia persona
        viewModelScope.launch {
            repo.observePublicInnovexia().collect { personas ->
                _public.value = personas.map { it.toUiPersona() }
            }
        }

        // Observe active persona ID
        viewModelScope.launch {
            prefs.getActivePersonaId(ownerId).collect { id ->
                _activePersonaId.value = id
            }
        }
    }

    fun setActivePersona(personaId: String) = viewModelScope.launch {
        prefs.setActivePersonaId(ownerId, personaId)
        // Update lastUsedAt timestamp
        repo.updateLastUsed(personaId)
    }

    /**
     * Get core persona for editing (not converted to UI)
     */
    suspend fun getCorePersona(personaId: String): com.example.innovexia.core.persona.Persona? {
        return repo.getPersonaById(personaId)
    }

    fun create(
        name: String,
        color: Long,
        summary: String,
        tags: List<String>
    ) = viewModelScope.launch {
        _busy.value = true
        runCatching {
            repo.createMyPersona(
                ownerId,
                PersonaDto(
                    name = name.trim(),
                    color = color,
                    summary = summary.trim(),
                    tags = tags
                )
            )
        }.onFailure {
            _error.emit(it.message ?: "Failed to create persona")
        }
        _busy.value = false
    }

    fun delete(id: String) = viewModelScope.launch {
        runCatching {
            repo.deleteMyPersona(ownerId, id)
        }.onFailure {
            _error.emit("Delete failed")
        }
    }

    fun importPublic(persona: Persona) = viewModelScope.launch {
        _busy.value = true
        runCatching {
            repo.createMyPersona(
                ownerId,
                PersonaDto(
                    name = persona.name,
                    initial = persona.initial,
                    color = persona.color,
                    summary = persona.summary,
                    tags = persona.tags,
                    isDefault = false
                )
            )
        }.onFailure {
            _error.emit(it.message ?: "Import failed")
        }
        _busy.value = false
    }

    fun rename(id: String, newName: String) = viewModelScope.launch {
        runCatching {
            repo.renamePersona(id, newName)
        }.onFailure {
            _error.emit("Rename failed")
        }
    }

    fun toggleDefault(id: String) = viewModelScope.launch {
        runCatching {
            repo.setDefaultPersona(ownerId, id)
        }.onFailure {
            _error.emit("Failed to set default")
        }
    }

    /**
     * Make a persona public so all users can see and import it
     */
    fun makePublic(id: String) = viewModelScope.launch {
        android.util.Log.d("MyPersonasViewModel", "makePublic called with id=$id, ownerId=$ownerId")
        _busy.value = true
        runCatching {
            android.util.Log.d("MyPersonasViewModel", "Calling repo.makePersonaPublic...")
            val success = repo.makePersonaPublic(id, ownerId)
            android.util.Log.d("MyPersonasViewModel", "makePersonaPublic result: success=$success")
            if (success) {
                _error.emit("✓ Persona published successfully! All users can now import it.")
            } else {
                _error.emit("✗ Failed to publish persona - persona not found")
            }
        }.onFailure { e ->
            android.util.Log.e("MyPersonasViewModel", "makePublic failed", e)
            _error.emit("✗ Failed to publish: ${e.message}")
        }
        _busy.value = false
    }
}
