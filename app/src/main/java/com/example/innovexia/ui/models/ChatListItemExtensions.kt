package com.example.innovexia.ui.models

/**
 * Extension functions for converting between chat models
 */

/**
 * Convert RecentChat to ChatListItem
 */
fun RecentChat.toChatListItem(
    lastMessage: String = "No messages yet",
    updatedAt: Long = System.currentTimeMillis(),
    personaInitials: List<String> = emptyList(),
    state: ChatState = ChatState.ACTIVE
): ChatListItem {
    return ChatListItem(
        chatId = this.id,
        title = this.title,
        lastMessage = lastMessage,
        updatedAt = updatedAt,
        personaInitials = personaInitials,
        state = state,
        pinned = this.pinned
    )
}

/**
 * Convert list of RecentChat to list of ChatListItem
 */
fun List<RecentChat>.toChatListItems(
    state: ChatState = ChatState.ACTIVE
): List<ChatListItem> {
    return this.map { chat ->
        ChatListItem(
            chatId = chat.id,
            title = chat.title,
            lastMessage = "No messages yet", // TODO: Get from actual messages
            updatedAt = System.currentTimeMillis(), // TODO: Get actual timestamp
            personaInitials = listOfNotNull(chat.emoji?.take(1)), // Use emoji as placeholder
            state = state,
            pinned = chat.pinned
        )
    }
}
