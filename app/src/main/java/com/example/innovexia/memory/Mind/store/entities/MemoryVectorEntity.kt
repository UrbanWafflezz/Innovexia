package com.example.innovexia.memory.Mind.store.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Quantized vector embeddings for similarity search
 */
@Entity(
    tableName = "memory_vectors",
    foreignKeys = [ForeignKey(
        entity = MemoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["memoryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("memoryId")]
)
data class MemoryVectorEntity(
    @PrimaryKey val memoryId: String,
    val dim: Int,
    val q8: ByteArray, // int8 quantized vector
    val scale: Float // dequantization scale
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MemoryVectorEntity
        if (memoryId != other.memoryId) return false
        if (dim != other.dim) return false
        if (!q8.contentEquals(other.q8)) return false
        if (scale != other.scale) return false
        return true
    }

    override fun hashCode(): Int {
        var result = memoryId.hashCode()
        result = 31 * result + dim
        result = 31 * result + q8.contentHashCode()
        result = 31 * result + scale.hashCode()
        return result
    }
}
