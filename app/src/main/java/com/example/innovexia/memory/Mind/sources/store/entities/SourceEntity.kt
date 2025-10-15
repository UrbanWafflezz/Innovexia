package com.example.innovexia.memory.Mind.sources.store.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a source document (PDF, URL, Image)
 */
@Entity(
    tableName = "sources",
    indices = [
        Index("personaId"),
        Index("type"),
        Index("status"),
        Index("addedAt")
    ]
)
data class SourceEntity(
    @PrimaryKey val id: String,
    val personaId: String,
    val type: String,                // "PDF", "URL", "IMAGE", "TEXT", "DOCUMENT"
    val displayName: String,
    val fileName: String,            // final stored file name
    val mime: String,                // "application/pdf", "text/html", etc.
    val bytes: Long,
    val pageCount: Int,
    val addedAt: Long,
    val lastIndexedAt: Long?,
    val status: String,              // NOT_INDEXED | INDEXING | READY | ERROR
    val errorMsg: String?,
    val storagePath: String,         // absolute/internal path to file
    val thumbPath: String?,          // path to generated thumbnail (PNG)

    // Web-specific metadata (null for non-URL sources)
    val metaTitle: String? = null,   // <title> or <meta> title
    val metaDesc: String? = null,    // <meta name="description">
    val domain: String? = null,      // e.g., "reddit.com"
    val depth: Int? = null,          // crawl depth (0 = main page only)
    val contentType: String? = null, // "text/html", "application/xhtml+xml"
    val pagesIndexed: Int? = null    // number of pages crawled and indexed
)
