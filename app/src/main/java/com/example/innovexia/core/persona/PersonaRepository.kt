package com.example.innovexia.core.persona

import com.example.innovexia.data.local.dao.PersonaDao
import com.example.innovexia.data.local.entities.PersonaEntity
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for persona operations.
 * LOCAL-FIRST architecture (like chats):
 * - My Personas: Stored in Room, backed up to Firebase
 * - Public Personas: Firebase-only (unless imported)
 */
class PersonaRepository(
    private val personaDao: PersonaDao
) {
    private val fs: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    // ═════════════════════════════════════════════════════════════════════════════
    // My Personas (Local Room + Firebase Backup)
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Observe user's personas from local Room database
     */
    fun observeMyPersonas(ownerId: String): Flow<List<Persona>> {
        return personaDao.observeForOwner(ownerId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Observe user's personas as entities (for UI layer with timestamps)
     */
    fun observeMyPersonasEntities(ownerId: String): Flow<List<PersonaEntity>> {
        return personaDao.observeForOwner(ownerId)
    }

    /**
     * Create a new persona (local + cloud backup)
     */
    suspend fun createMyPersona(ownerId: String, input: PersonaDto, extendedSettings: String? = null): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val initial = input.initial.ifBlank {
            input.name.trim().firstOrNull()?.uppercase() ?: "?"
        }

        val entity = PersonaEntity(
            id = id,
            ownerId = ownerId,
            name = input.name.trim(),
            initial = initial,
            color = input.color,
            summary = input.summary.trim(),
            tags = input.tags,
            system = input.system,
            createdAt = now,
            updatedAt = now,
            isDefault = input.isDefault,
            extendedSettings = extendedSettings
        )

        // Insert to local database first
        personaDao.upsert(entity)

        // TODO: Trigger cloud sync in background
        // syncPersonaToCloud(entity)

        return id
    }

    /**
     * Update an existing persona
     */
    suspend fun updateMyPersona(ownerId: String, id: String, patch: Map<String, Any?>) {
        val existing = personaDao.getById(id) ?: return
        val now = System.currentTimeMillis()

        @Suppress("UNCHECKED_CAST")
        val patchedTags = patch["tags"] as? List<String> ?: existing.tags

        val updated = existing.copy(
            name = patch["name"] as? String ?: existing.name,
            initial = patch["initial"] as? String ?: existing.initial,
            color = patch["color"] as? Long ?: existing.color,
            summary = patch["summary"] as? String ?: existing.summary,
            tags = patchedTags,
            system = patch["system"] as? String ?: existing.system,
            isDefault = patch["isDefault"] as? Boolean ?: existing.isDefault,
            extendedSettings = patch["extendedSettings"] as? String ?: existing.extendedSettings,
            updatedAt = now
        )

        personaDao.upsert(updated)
        android.util.Log.d("PersonaRepository", "Updated persona in Room: id=$id, name=${updated.name}")

        // TODO: Trigger cloud sync
    }

    /**
     * Delete a persona
     */
    suspend fun deleteMyPersona(ownerId: String, id: String) {
        personaDao.deleteById(id)

        // TODO: Delete from cloud backup if exists
    }

    /**
     * Rename a persona
     */
    suspend fun renamePersona(id: String, newName: String) {
        val initial = newName.trim().firstOrNull()?.uppercase() ?: "?"
        val now = System.currentTimeMillis()
        personaDao.updateName(id, newName.trim(), initial, now)
    }

    /**
     * Set a persona as default (unsets all others)
     */
    suspend fun setDefaultPersona(ownerId: String, id: String) {
        val now = System.currentTimeMillis()
        personaDao.clearAllDefaults(ownerId, now)
        personaDao.setDefault(id, true, now)
    }

    /**
     * Get default persona for owner
     */
    suspend fun getDefaultPersona(ownerId: String): Persona? {
        return personaDao.getDefaultPersona(ownerId)?.toDomainModel()
    }

    /**
     * Get persona by ID
     */
    suspend fun getPersonaById(id: String): Persona? {
        return personaDao.getById(id)?.toDomainModel()
    }

    /**
     * Update the lastUsedAt timestamp for a persona
     */
    suspend fun updateLastUsed(personaId: String) {
        val now = System.currentTimeMillis()
        personaDao.updateLastUsed(personaId, now)
        android.util.Log.d("PersonaRepository", "Updated lastUsedAt for persona $personaId to $now")
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // Inno Default Persona (System-Managed)
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Get or create Inno persona for a specific owner.
     * Inno is the default AI companion that's automatically created for all users.
     *
     * @param ownerId The owner ID (guest or Firebase UID)
     * @return The Inno persona (domain model)
     */
    suspend fun getOrCreateInnoPersona(ownerId: String): Persona {
        // Get per-user Inno ID
        val innoId = InnoPersonaDefaults.getInnoPersonaId(ownerId)

        // Check if Inno already exists for this owner
        val existing = personaDao.getById(innoId)

        if (existing != null && existing.ownerId == ownerId) {
            android.util.Log.d("PersonaRepository", "Inno persona already exists for owner $ownerId (ID: $innoId)")
            return existing.toDomainModel()
        }

        // Create Inno persona with per-user ID
        val now = System.currentTimeMillis()
        val innoEntity = InnoPersonaDefaults.createInnoPersonaEntity(ownerId, now)

        // Insert to local database
        personaDao.upsert(innoEntity)
        android.util.Log.d("PersonaRepository", "Created Inno persona for owner $ownerId (ID: $innoId)")

        // Trigger cloud sync in background for signed-in users (best effort)
        // Cloud sync will handle this in the next sync cycle

        return innoEntity.toDomainModel()
    }

    /**
     * Check if Inno persona exists for the given owner.
     *
     * @param ownerId The owner ID to check
     * @return true if Inno exists, false otherwise
     */
    suspend fun hasInnoPersona(ownerId: String): Boolean {
        val innoId = InnoPersonaDefaults.getInnoPersonaId(ownerId)
        val inno = personaDao.getById(innoId)
        return inno != null && inno.ownerId == ownerId
    }

    /**
     * Ensure Inno is the default persona for the given owner.
     * Creates Inno if it doesn't exist, and sets it as default.
     *
     * @param ownerId The owner ID
     * @return The Inno persona
     */
    suspend fun ensureInnoIsDefault(ownerId: String): Persona {
        // Get or create Inno
        val inno = getOrCreateInnoPersona(ownerId)
        val innoId = InnoPersonaDefaults.getInnoPersonaId(ownerId)

        // Check if Inno is already default
        val defaultPersona = getDefaultPersona(ownerId)
        if (defaultPersona?.id != innoId) {
            // Set Inno as default
            setDefaultPersona(ownerId, innoId)
            android.util.Log.d("PersonaRepository", "Set Inno as default persona for owner $ownerId (ID: $innoId)")
        }

        return inno
    }

    /**
     * Get Inno persona for the given owner, or null if it doesn't exist.
     *
     * @param ownerId The owner ID
     * @return Inno persona or null
     */
    suspend fun getInnoPersona(ownerId: String): Persona? {
        val innoId = InnoPersonaDefaults.getInnoPersonaId(ownerId)
        val inno = personaDao.getById(innoId)
        return if (inno != null && inno.ownerId == ownerId) {
            inno.toDomainModel()
        } else {
            null
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // Public Personas (Firebase-only)
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Observe public personas from Firebase collection
     */
    fun observePublicInnovexia(): Flow<List<Persona>> = callbackFlow {
        val sub = fs.collection("publicPersonas")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Convert all documents to Personas
                val personas = snap.documents.mapNotNull { it.toPersona() }
                trySend(personas)
            }
        awaitClose { sub.remove() }
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // Cloud Sync (Background)
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Sync personas to Firebase (for signed-in users only)
     * Called by CloudSyncEngine
     */
    suspend fun syncToCloud(uid: String) {
        val unsynced = personaDao.getUnsyncedForOwner(uid)

        unsynced.forEach { entity ->
            try {
                val cloudId = entity.cloudId ?: UUID.randomUUID().toString()
                val doc = fs.collection("users")
                    .document(uid)
                    .collection("personas")
                    .document(cloudId)

                val data = mapOf(
                    "name" to entity.name,
                    "initial" to entity.initial,
                    "color" to entity.color,
                    "summary" to entity.summary,
                    "tags" to entity.tags,
                    "system" to entity.system,
                    "isDefault" to entity.isDefault,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                doc.set(data, SetOptions.merge()).await()

                // Update sync metadata
                personaDao.updateSyncMetadata(
                    id = entity.id,
                    cloudId = cloudId,
                    syncedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                // Log error, continue with next
            }
        }
    }

    /**
     * Pull personas from cloud (for restore/migration)
     */
    suspend fun pullFromCloud(uid: String) {
        try {
            val snapshot = fs.collection("users")
                .document(uid)
                .collection("personas")
                .get()
                .await()

            val entities = snapshot.documents.mapNotNull { doc ->
                val persona = doc.toPersona() ?: return@mapNotNull null
                PersonaEntity(
                    id = UUID.randomUUID().toString(), // New local ID
                    ownerId = uid,
                    name = persona.name,
                    initial = persona.initial,
                    color = persona.color,
                    summary = persona.summary,
                    tags = persona.tags,
                    system = persona.system,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isDefault = persona.isDefault,
                    cloudId = doc.id,
                    lastSyncedAt = System.currentTimeMillis()
                )
            }

            personaDao.upsertAll(entities)
        } catch (e: Exception) {
            // Handle error
        }
    }

    /**
     * Make a persona public by copying it to the publicPersonas collection
     * All users can then see and import it
     */
    suspend fun makePersonaPublic(personaId: String, ownerId: String): Boolean {
        return try {
            android.util.Log.d("PersonaRepository", "makePersonaPublic START: personaId=$personaId, ownerId=$ownerId")

            // Get the persona from local database
            android.util.Log.d("PersonaRepository", "Querying personaDao.getById($personaId)...")
            val persona = personaDao.getById(personaId)

            if (persona == null) {
                android.util.Log.e("PersonaRepository", "Persona not found in Room DB with id=$personaId")

                // List all personas to debug
                android.util.Log.d("PersonaRepository", "All personas in Room for owner=$ownerId:")
                val allPersonas = personaDao.getForOwner(ownerId)
                if (allPersonas.isEmpty()) {
                    android.util.Log.d("PersonaRepository", "  (No personas found for this owner)")
                } else {
                    allPersonas.forEach {
                        android.util.Log.d("PersonaRepository", "  - id=${it.id}, name=${it.name}, ownerId=${it.ownerId}")
                    }
                }

                return false
            }

            android.util.Log.d("PersonaRepository", "Found persona: id=${persona.id}, name=${persona.name}, ownerId=${persona.ownerId}")

            // Create document in publicPersonas collection
            val publicDoc = fs.collection("publicPersonas").document(personaId)

            val publicData = mapOf(
                "id" to personaId,
                "name" to persona.name,
                "initial" to persona.initial,
                "color" to persona.color,
                "summary" to persona.summary,
                "tags" to persona.tags,
                "system" to persona.system,
                "isDefault" to false, // Public personas are never default
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "authorId" to ownerId,
                "visibility" to "public",
                "status" to "published",
                "extendedSettings" to persona.extendedSettings
            )

            android.util.Log.d("PersonaRepository", "Uploading to Firestore publicPersonas/$personaId...")
            publicDoc.set(publicData).await()

            android.util.Log.d("PersonaRepository", "✓ Successfully made persona public: id=$personaId, name=${persona.name}")
            true
        } catch (e: Exception) {
            android.util.Log.e("PersonaRepository", "✗ Failed to make persona public: ${e.message}", e)
            false
        }
    }

    // Helper extensions
    private fun FirebaseFirestore.user(uid: String) =
        collection("users").document(uid)

    private fun DocumentReference.personas() =
        collection("personas")
}

/**
 * Convert PersonaEntity to domain Persona
 */
private fun PersonaEntity.toDomainModel(): Persona {
    return Persona(
        id = id,
        name = name,
        initial = initial,
        color = color,
        summary = summary,
        tags = tags,
        system = system,
        isDefault = isDefault,
        updatedAt = com.google.firebase.Timestamp(updatedAt / 1000, 0),
        extendedSettings = extendedSettings
    )
}
