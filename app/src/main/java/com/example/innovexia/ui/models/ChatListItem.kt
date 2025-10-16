package com.example.innovexia.ui.models

/**
 * Represents a chat item in the drawer list
 */
data class ChatListItem(
    val chatId: String,
    val title: String,
    val lastMessage: String,
    val updatedAt: Long,
    val personaInitials: List<String>, // e.g. ["N", "R"]
    val state: ChatState, // ACTIVE / ARCHIVED / TRASH
    val pinned: Boolean = false,
    val isIncognito: Boolean = false,
    val isSyncedToCloud: Boolean = false // True if chat has been synced to cloud (has cloudId)
)

/**
 * Chat state for organizing chats in the drawer
 */
enum class ChatState {
    ACTIVE,
    ARCHIVED,
    TRASH
}
