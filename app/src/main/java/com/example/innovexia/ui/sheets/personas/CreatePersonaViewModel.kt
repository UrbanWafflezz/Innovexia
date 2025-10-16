package com.example.innovexia.ui.sheets.personas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.core.persona.*
import com.example.innovexia.data.local.AppDatabase
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for Create/Edit Persona Dialog (Persona 2.0)
 * Saves to Room (local database) for instant updates
 */
class CreatePersonaViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val database = AppDatabase.getInstance(application)
    private val personaDao = database.personaDao()
    private val repository = PersonaRepository(personaDao)

    private val _draft = MutableStateFlow(PersonaDraftDto())
    val draft: StateFlow<PersonaDraftDto> = _draft.asStateFlow()

    private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errors: StateFlow<Map<String, String>> = _errors.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    private var originalDraft: PersonaDraftDto = PersonaDraftDto()
    private var editingId: String? = null

    /**
     * Load persona for editing
     */
    fun loadForEdit(uid: String, personaId: String) = viewModelScope.launch {
        _busy.value = true
        android.util.Log.d("CreatePersonaVM", "Loading persona for edit: uid=$uid, id=$personaId")

        try {
            val doc = firestore.collection("users")
                .document(uid)
                .collection("personas")
                .document(personaId)
                .get()
                .await()

            android.util.Log.d("CreatePersonaVM", "Document exists: ${doc.exists()}")

            if (doc.exists()) {
                val loadedDraft = doc.toPersonaDraftDto()
                android.util.Log.d("CreatePersonaVM", "Loaded persona: name=${loadedDraft.name}, initial=${loadedDraft.initial}")

                _draft.value = loadedDraft
                originalDraft = loadedDraft
                editingId = personaId
                _hasChanges.value = false
                _errors.value = emptyMap()
            } else {
                android.util.Log.w("CreatePersonaVM", "Persona document does not exist!")
            }
        } catch (e: Exception) {
            // Handle error - log it
            android.util.Log.e("CreatePersonaVM", "Failed to load persona: ${e.message}", e)
        } finally {
            _busy.value = false
        }
    }

    /**
     * Update draft and track changes
     */
    fun updateDraft(update: (PersonaDraftDto) -> PersonaDraftDto) {
        _draft.value = update(_draft.value)
        _hasChanges.value = _draft.value != originalDraft
        validateDraft()
    }

    /**
     * Set editing ID for updates
     */
    fun setEditingId(id: String) {
        editingId = id
        originalDraft = _draft.value
        _hasChanges.value = false
    }

    /**
     * Load persona from a Persona object (Room database)
     */
    fun loadFromPersona(id: String, draft: PersonaDraftDto) {
        android.util.Log.d("CreatePersonaVM", "Loading from Persona object: id=$id, name=${draft.name}, initial=${draft.initial}")
        _draft.value = draft
        originalDraft = draft
        editingId = id
        _hasChanges.value = false
        _errors.value = emptyMap()
        validateDraft()
        android.util.Log.d("CreatePersonaVM", "After loadFromPersona: editingId=$editingId")
    }

    /**
     * Validate current draft
     */
    fun validateDraft(): Boolean {
        val newErrors = mutableMapOf<String, String>()
        val d = _draft.value

        // Name validation
        if (d.name.isBlank()) {
            newErrors["name"] = "Name is required"
        } else if (d.name.length < 2) {
            newErrors["name"] = "Name must be at least 2 characters"
        } else if (d.name.length > 40) {
            newErrors["name"] = "Name must be 40 characters or less"
        }

        // Initial validation
        if (d.initial.length > 1) {
            newErrors["initial"] = "Initial must be 1 character"
        }

        // Tags validation
        if (d.tags.size > 6) {
            newErrors["tags"] = "Maximum 6 tags allowed"
        }

        // Bio validation
        if (d.bio.length > 140) {
            newErrors["bio"] = "Bio must be 140 characters or less"
        }

        // Limits validation
        if (d.limits.maxOutputTokens < 100 || d.limits.maxOutputTokens > 10000) {
            newErrors["limits.maxOutputTokens"] = "Must be between 100 and 10,000"
        }
        if (d.limits.maxContextTokens < 1000 || d.limits.maxContextTokens > 128000) {
            newErrors["limits.maxContextTokens"] = "Must be between 1,000 and 128,000"
        }
        if (d.limits.timeBudgetMs < 1000 || d.limits.timeBudgetMs > 60000) {
            newErrors["limits.timeBudgetMs"] = "Must be between 1,000 and 60,000 ms"
        }

        _errors.value = newErrors
        return newErrors.isEmpty()
    }

    /**
     * Save draft to Room (local database)
     */
    fun saveDraft() = viewModelScope.launch {
        if (!validateDraft()) {
            android.util.Log.w("CreatePersonaVM", "Validation failed, not saving")
            return@launch
        }

        val uid = auth.currentUser?.uid ?: "guest"
        _busy.value = true

        try {
            val draft = _draft.value
            val isNew = editingId == null

            android.util.Log.d("CreatePersonaVM", "=== SAVE DRAFT START ===")
            android.util.Log.d("CreatePersonaVM", "isNew=$isNew, editingId=$editingId, name=${draft.name}")
            android.util.Log.d("CreatePersonaVM", "behavior.conciseness=${draft.behavior.conciseness}")

            // Serialize extended settings to JSON
            val extendedSettings = Gson().toJson(mapOf(
                "behavior" to draft.behavior,
                "memory" to draft.memory,
                "sources" to draft.sources,
                "tools" to draft.tools,
                "limits" to draft.limits,
                "testing" to draft.testing,
                "defaultLanguage" to draft.defaultLanguage,
                "greeting" to draft.greeting,
                "visibility" to draft.visibility,
                "status" to draft.status
            ))

            if (isNew) {
                // Create new persona in Room
                val personaDto = PersonaDto(
                    name = draft.name,
                    initial = draft.initial,
                    color = draft.color,
                    summary = draft.bio,
                    tags = draft.tags,
                    system = draft.system.instructions,
                    isDefault = draft.isDefault
                )
                val newId = repository.createMyPersona(uid, personaDto, extendedSettings)
                editingId = newId
                android.util.Log.d("CreatePersonaVM", "Created new persona with ID: $newId")
            } else {
                // Update existing persona in Room
                val updates = mapOf(
                    "name" to draft.name,
                    "initial" to draft.initial,
                    "color" to draft.color,
                    "summary" to draft.bio,
                    "tags" to draft.tags,
                    "system" to draft.system.instructions,
                    "isDefault" to draft.isDefault,
                    "extendedSettings" to extendedSettings
                )
                repository.updateMyPersona(uid, editingId!!, updates)
                android.util.Log.d("CreatePersonaVM", "Updated persona ID: $editingId")
            }

            originalDraft = _draft.value
            _hasChanges.value = false
        } catch (e: Exception) {
            android.util.Log.e("CreatePersonaVM", "Failed to save: ${e.message}", e)
        } finally {
            _busy.value = false
        }
    }

    /**
     * Publish persona (creates copy in /public/personas)
     * In production, this would call a Cloud Function
     */
    fun publish() = viewModelScope.launch {
        if (!validateDraft()) return@launch

        val uid = auth.currentUser?.uid ?: return@launch
        _busy.value = true

        try {
            // First save draft
            saveDraft()

            // Update status and visibility
            val updatedDraft = _draft.value.copy(
                status = "published",
                visibility = "public"
            )
            _draft.value = updatedDraft

            // In production: call Cloud Function here
            // For now, just update the user's document
            if (editingId != null) {
                firestore.collection("users")
                    .document(uid)
                    .collection("personas")
                    .document(editingId!!)
                    .update(
                        mapOf(
                            "status" to "published",
                            "visibility" to "public"
                        )
                    )
                    .await()
            }

            originalDraft = updatedDraft
            _hasChanges.value = false
        } catch (e: Exception) {
            // Handle error
        } finally {
            _busy.value = false
        }
    }

    /**
     * Export persona as JSON
     */
    fun exportJson(): String {
        // TODO: Implement JSON serialization
        return "{}"
    }

    /**
     * Import persona from JSON
     */
    fun importJson(json: String) {
        // TODO: Implement JSON deserialization
    }

    /**
     * Reset draft to defaults
     */
    fun reset() {
        _draft.value = PersonaDraftDto()
        originalDraft = PersonaDraftDto()
        editingId = null
        _errors.value = emptyMap()
        _hasChanges.value = false
    }

    /**
     * Discard changes
     */
    fun discardChanges() {
        _draft.value = originalDraft
        _hasChanges.value = false
        _errors.value = emptyMap()
    }
}

/**
 * Extension to convert Firestore document to PersonaDraftDto
 * Handles both old simple format and new Persona 2.0 format
 */
private fun com.google.firebase.firestore.DocumentSnapshot.toPersonaDraftDto(): PersonaDraftDto {
    // Handle old simple persona format (backward compatibility)
    val name = getString("name") ?: ""
    val initial = getString("initial") ?: name.firstOrNull()?.uppercase()?.toString() ?: ""
    val summary = getString("summary") // Old field name
    val bio = getString("bio") ?: summary ?: "" // Try bio first, fallback to summary

    return PersonaDraftDto(
        name = name,
        initial = initial,
        color = getLong("color") ?: 0xFF60A5FA,
        bio = bio,
        tags = get("tags") as? List<String> ?: emptyList(),
        defaultLanguage = getString("defaultLanguage") ?: "en-US",
        greeting = getString("greeting") ?: "",
        isDefault = getBoolean("isDefault") ?: false,
        visibility = getString("visibility") ?: "private",
        status = getString("status") ?: "draft",
        behavior = (get("behavior") as? Map<*, *>)?.toPersonaBehavior() ?: PersonaBehavior(),
        system = (get("system") as? Map<*, *>)?.toPersonaSystem()
            ?: getString("system")?.let { PersonaSystem(instructions = it) } // Old format: system was a string
            ?: PersonaSystem(),
        memory = (get("memory") as? Map<*, *>)?.toPersonaMemory() ?: PersonaMemory(),
        sources = (get("sources") as? Map<*, *>)?.toPersonaSources() ?: PersonaSources(),
        tools = (get("tools") as? Map<*, *>)?.toPersonaTools() ?: PersonaTools(),
        limits = (get("limits") as? Map<*, *>)?.toPersonaLimits() ?: PersonaLimits(),
        testing = (get("testing") as? Map<*, *>)?.toPersonaTesting() ?: PersonaTesting(),
        createdAt = getTimestamp("createdAt"),
        updatedAt = getTimestamp("updatedAt"),
        author = (get("author") as? Map<*, *>)?.toAuthorMeta()
    )
}

// Helper extension functions for deserialization
private fun Map<*, *>.toPersonaBehavior() = PersonaBehavior(
    conciseness = (get("conciseness") as? Double)?.toFloat() ?: 0.6f,
    formality = (get("formality") as? Double)?.toFloat() ?: 0.5f,
    empathy = (get("empathy") as? Double)?.toFloat() ?: 0.4f,
    creativityTemp = (get("creativityTemp") as? Double)?.toFloat() ?: 0.7f,
    topP = (get("topP") as? Double)?.toFloat() ?: 0.9f,
    thinkingDepth = get("thinkingDepth") as? String ?: "balanced",
    proactivity = get("proactivity") as? String ?: "ask_when_unclear",
    safetyLevel = get("safetyLevel") as? String ?: "standard",
    hallucinationGuard = get("hallucinationGuard") as? String ?: "prefer_idk",
    selfCheck = (get("selfCheck") as? Map<*, *>)?.toSelfCheckConfig() ?: SelfCheckConfig(),
    citationPolicy = get("citationPolicy") as? String ?: "when_uncertain",
    formatting = (get("formatting") as? Map<*, *>)?.toFormattingConfig() ?: FormattingConfig()
)

private fun Map<*, *>.toSelfCheckConfig() = SelfCheckConfig(
    enabled = get("enabled") as? Boolean ?: true,
    maxMs = (get("maxMs") as? Long)?.toInt() ?: 500
)

private fun Map<*, *>.toFormattingConfig() = FormattingConfig(
    markdown = get("markdown") as? Boolean ?: true,
    emoji = get("emoji") as? String ?: "light"
)

private fun Map<*, *>.toPersonaSystem() = PersonaSystem(
    instructions = get("instructions") as? String ?: "",
    rules = (get("rules") as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.toSystemRule() } ?: emptyList(),
    variables = get("variables") as? List<String> ?: listOf("{user_name}", "{today}", "{timezone}", "{app_name}"),
    version = (get("version") as? Long)?.toInt() ?: 1
)

private fun Map<*, *>.toSystemRule() = SystemRule(
    `when` = get("when") as? String ?: "always",
    `do` = get("do") as? String ?: ""
)

private fun Map<*, *>.toPersonaMemory() = PersonaMemory(
    enabled = get("enabled") as? Boolean ?: true
)

private fun Map<*, *>.toPersonaSources() = PersonaSources(
    enabled = get("enabled") as? Boolean ?: true
)

private fun Map<*, *>.toPersonaTools() = PersonaTools(
    web = get("web") as? Boolean ?: true,
    code = get("code") as? Boolean ?: false,
    vision = get("vision") as? Boolean ?: true,
    audio = get("audio") as? Boolean ?: false,
    functions = (get("functions") as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.toFunctionConfig() } ?: emptyList(),
    modelRouting = (get("modelRouting") as? Map<*, *>)?.toModelRoutingConfig() ?: ModelRoutingConfig()
)

private fun Map<*, *>.toFunctionConfig() = FunctionConfig(
    name = get("name") as? String ?: "",
    allowed = get("allowed") as? Boolean ?: true
)

private fun Map<*, *>.toModelRoutingConfig() = ModelRoutingConfig(
    preferred = get("preferred") as? String ?: "fast",
    fallbacks = get("fallbacks") as? List<String> ?: listOf("thinking")
)

private fun Map<*, *>.toPersonaLimits() = PersonaLimits(
    maxOutputTokens = (get("maxOutputTokens") as? Long)?.toInt() ?: 1200,
    maxContextTokens = (get("maxContextTokens") as? Long)?.toInt() ?: 48000,
    timeBudgetMs = (get("timeBudgetMs") as? Long)?.toInt() ?: 15000,
    rateWeight = (get("rateWeight") as? Double)?.toFloat() ?: 1.0f,
    concurrency = (get("concurrency") as? Long)?.toInt() ?: 2
)

private fun Map<*, *>.toPersonaTesting() = PersonaTesting(
    scenarios = (get("scenarios") as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.toTestScenario() } ?: emptyList()
)

private fun Map<*, *>.toTestScenario() = TestScenario(
    prompt = get("prompt") as? String ?: "",
    expectedBehavior = get("expectedBehavior") as? String ?: ""
)

private fun Map<*, *>.toAuthorMeta() = AuthorMeta(
    uid = get("uid") as? String ?: "",
    name = get("name") as? String ?: ""
)
