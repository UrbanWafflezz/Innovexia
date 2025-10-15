package com.example.innovexia.memory.Mind.sources.store.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a chunk of indexed source content with embedding
 */
@Entity(
    tableName = "source_chunks",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sourceId"),
        Index("personaId")
    ]
)
data class SourceChunkEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val personaId: String,
    val pageStart: Int,              // 1-based (for PDFs)
    val pageEnd: Int,                // inclusive
    val text: String,                // raw text of the chunk
    val dim: Int,                    // embedding dimension
    val q8: ByteArray,               // quantized embedding
    val scale: Float                 // scale factor for dequantization
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SourceChunkEntity

        if (id != other.id) return false
        if (sourceId != other.sourceId) return false
        if (personaId != other.personaId) return false
        if (pageStart != other.pageStart) return false
        if (pageEnd != other.pageEnd) return false
        if (text != other.text) return false
        if (dim != other.dim) return false
        if (!q8.contentEquals(other.q8)) return false
        if (scale != other.scale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sourceId.hashCode()
        result = 31 * result + personaId.hashCode()
        result = 31 * result + pageStart
        result = 31 * result + pageEnd
        result = 31 * result + text.hashCode()
        result = 31 * result + dim
        result = 31 * result + q8.contentHashCode()
        result = 31 * result + scale.hashCode()
        return result
    }
}
