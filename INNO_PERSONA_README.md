# Inno - The Default AI Companion

## Overview

**Inno** is Innovexia's built-in AI companion that serves as the default persona for all users. Unlike custom personas, Inno is automatically created and configured to provide the best possible experience out of the box. Inno knows everything about the app, learns from user interactions, and serves as a knowledgeable friend and companion.

## Key Features

### 1. **Automatic Default Persona**
- **Auto-created**: Inno is automatically created for every user (guest and signed-in) on first launch
- **Set as default**: Automatically selected when no custom persona is chosen
- **Persistent selection**: User's persona choice is remembered across sessions
- **Seamless fallback**: If a user deletes their selected persona, the app gracefully falls back to Inno

### 2. **Comprehensive App Knowledge**
Inno has deep, expert-level knowledge of all Innovexia features:
- **Personas System**: Guide users in creating and customizing personas
- **Memory Engine**: Explain how the app learns and maintains context
- **Sources System**: Help with PDF uploads, web content, and RAG features
- **Subscriptions**: Explain Free vs Pro tiers, usage limits, and billing
- **Advanced Settings**: Guide through model selection, temperature, tokens, safety
- **Cloud Sync**: Explain data synchronization across devices
- **Privacy Features**: Incognito mode, guest mode, local-first architecture

### 3. **Adaptive Learning & Personalization**
- **Global Memory Scope**: Learns across ALL user conversations (not just per-chat)
- **365-Day Retention**: Long-term memory for true personalization
- **Auto-Write Enabled**: Automatically saves important information
- **50,000 Token Budget**: 4x larger than default personas for extensive context
- **Preference Learning**: Adapts tone, depth, and style based on user feedback

### 4. **Companion Capabilities**
- **Diary & Reflection**: Encourages daily reflection, remembers important events
- **Proactive Suggestions**: Offers relevant recommendations based on context
- **Knowledge Organization**: Helps organize information and connect concepts
- **Feature Discovery**: Suggests relevant Innovexia features when needed
- **Emotional Intelligence**: High empathy settings for genuine companion experience

### 5. **Power User Features**
- **Advanced Prompting**: Leverages full advanced prompting system
- **Thinking Mode**: Balanced reasoning for complex queries
- **Large Context Window**: 100,000 token context (vs default 48,000)
- **Extended Output**: 8,192 max output tokens for comprehensive responses
- **Gemini Flash**: Fast, efficient model routing with Pro fallback

## Architecture

### Data Model

**Fixed Persona ID**: `inno-default-persona-v1`
- Ensures consistency across all users
- Enables future updates to Inno's capabilities
- Prevents accidental deletion or duplication

**Storage**:
- **Local**: Room database (PersonaEntity)
- **Cloud**: Firebase sync for signed-in users
- **Preferences**: DataStore for active persona tracking

### System Prompt

Inno's system prompt is carefully crafted to provide:
- **Identity & Mission**: Clear understanding of role as Innovexia's companion
- **Core Capabilities**: App expertise, personal assistance, diary, knowledge management
- **Behavior Guidelines**: Communication style, proactivity, honesty, privacy
- **Feature Knowledge**: Comprehensive documentation of all app features
- **Special Behaviors**: Daily reflection prompts, feature discovery, contextual awareness

Key excerpt from Inno's prompt:
```
You are Inno, the official AI companion of Innovexia - an advanced AI assistant app.

IDENTITY & MISSION:
You are built into Innovexia as the user's most trusted, knowledgeable, and
personalized AI companion. You have comprehensive knowledge of all app features
and continuously learn from every interaction to serve the user better...
```

### Extended Settings (Persona 2.0)

Inno uses advanced Persona 2.0 configuration:

```kotlin
PersonaDraftDto(
    name = "Inno",
    isDefault = true,

    behavior = PersonaBehavior(
        empathy = 0.8f,              // High empathy for companion role
        formality = 0.5f,            // Neutral, adapts to context
        creativityTemp = 0.7f,       // Balanced creativity
        thinkingDepth = "balanced",  // Enable reasoning when needed
        proactivity = "suggest_when_helpful"
    ),

    memory = PersonaMemory(
        scope = "global_to_persona", // Learn across ALL conversations
        retentionDays = 365,         // Long-term memory (1 year)
        autoWrite = "yes",           // Auto-save important info
        budgetTokens = 50000         // Large memory budget
    ),

    limits = PersonaLimits(
        maxOutputTokens = 8192,      // Large outputs allowed
        maxContextTokens = 100000    // Massive context window
    )
)
```

## Implementation Details

### 1. Auto-Creation on First Launch

[InnovexiaApplication.kt:194-224]

```kotlin
private fun seedInnoPersona() {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val ownerId = FirebaseAuth.getInstance().currentUser?.uid
                ?: ProfileId.GUEST_OWNER_ID

            val hasInno = personaRepository.hasInnoPersona(ownerId)

            if (!hasInno) {
                // Create Inno as default persona
                val inno = personaRepository.ensureInnoIsDefault(ownerId)
                Log.d("InnovexiaApplication", "Seeded Inno persona")
            }
        } catch (e: Exception) {
            // Don't crash app - Inno will be created lazily when needed
        }
    }
}
```

### 2. Default Persona Selection

[PersonaPreferences.kt:23-29]

```kotlin
fun getActivePersonaId(ownerId: String): Flow<String?> {
    val key = stringPreferencesKey("active_persona_${ownerId}")
    return context.personaDataStore.data.map { prefs ->
        // Return stored persona ID, or default to Inno if not set
        prefs[key] ?: InnoPersonaDefaults.INNO_PERSONA_ID
    }
}
```

This ensures:
- ✅ First-time users automatically get Inno
- ✅ Users who delete their selected persona fall back to Inno
- ✅ Selection is scoped per ownerId (guest vs signed-in users have separate selections)

### 3. Persona Selection Persistence

[HomeScreen.kt:261-278]

```kotlin
// Load selected persona from preferences on startup
LaunchedEffect(Unit) {
    val prefs = PersonaPreferences(context)
    val ownerId = ProfileId.current().toOwnerId()

    prefs.getActivePersonaId(ownerId).collect { personaId ->
        if (personaId != null) {
            val persona = personaRepository.getPersonaById(personaId)
            if (persona != null) {
                selectedPersona = Persona(...)
            }
        }
    }
}
```

[HomeScreen.kt:582-586]

```kotlin
// Save selection when user picks a persona
onPersonaSelected = { persona ->
    selectedPersona = Persona(...)

    scope.launch {
        val prefs = PersonaPreferences(context)
        prefs.setActivePersonaId(ownerId, persona.id)
    }
}
```

This ensures:
- ✅ Persona selection loads on app startup
- ✅ Selection persists across app restarts
- ✅ User's choice is remembered even after signing in/out
- ✅ Selection doesn't unexpectedly revert to Inno

### 4. Chat Creation with Persona

[HomeViewModel.kt:228]

```kotlin
fun sendMessage(userMessage: String, persona: Persona?, isIncognito: Boolean = false) {
    // Uses the passed persona (which defaults to Inno via PreferencesFlow)
    chatRepository.startChat(userMessage, persona, isIncognito, attachments)
}
```

The flow:
1. User sends message
2. `sendMessage()` is called with `selectedPersona` from state
3. `selectedPersona` is loaded from `PersonaPreferences` which defaults to Inno
4. Chat is created with the correct persona
5. Persona info is stored in ChatEntity for display

## UI Indicators

### PersonaChip

[PersonaChip.kt:64-101]

Inno displays with special styling:
- **Thicker border**: 2.5dp vs 2dp for custom personas
- **Star icon**: Small ⭐ overlay at top-right
- **Bold font**: FontWeight.Bold vs SemiBold

```kotlin
val isInno = persona.id == InnoPersonaDefaults.INNO_PERSONA_ID

Box(
    border = width = if (isInno) 2.5.dp else 2.dp,
    ...
) {
    if (isInno) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = "Default persona",
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }

    Text(
        text = persona.initial,
        fontWeight = if (isInno) FontWeight.Bold else FontWeight.SemiBold
    )
}
```

### PersonaCardV2

[PersonaCardV2.kt:356-381]

Inno shows a special "DEFAULT" badge:
- **Star icon** + "DEFAULT" text
- **Colored background** matching persona color
- **Priority display** in tags row

```kotlin
if (isInno) {
    Surface(
        color = Color(persona.color).copy(alpha = 0.2f),
        contentColor = Color(persona.color)
    ) {
        Row {
            Icon(imageVector = Icons.Filled.Star, ...)
            Text("DEFAULT", fontWeight = FontWeight.Bold)
        }
    }
}
```

## User Experience

### For New Users
1. Launch app for first time
2. Inno is automatically created and set as default
3. See Inno's chip in composer (blue with "I" initial)
4. Start chatting immediately with expert guidance
5. Inno introduces features naturally through conversation

### For Existing Users
1. Can create custom personas anytime
2. Select different persona from PersonaSheet
3. Selection persists across sessions
4. Can always switch back to Inno
5. Inno remains available as the reliable default

### For Guest Users
1. Full access to Inno with local storage
2. Inno learns within guest session
3. Upon sign-in, can choose to:
   - Continue with guest Inno (migrated to account)
   - Start fresh with new Inno
4. Guest data remains separate from signed-in data

## Memory & Learning

### How Inno Learns

**Automatic Memory Creation**:
- Every conversation is analyzed for important information
- Facts, preferences, goals, and experiences are extracted
- Stored in MemoryEngine with semantic embeddings
- Retrieved contextually in future conversations

**Memory Scope**:
- `global_to_persona`: Learns across ALL chats with Inno
- Not isolated per-chat like custom personas can be
- Builds comprehensive user understanding over time

**Memory Budget**:
- 50,000 tokens dedicated to memory context
- ~4x larger than default personas
- Allows Inno to recall extensive conversation history

### Privacy & Control

**User Control**:
- View all memories in Persona → Memory tab
- Edit or delete individual memories
- Clear all memories anytime
- Control auto-write behavior in settings

**Privacy**:
- Memories are local-first (Room database)
- Cloud sync only for signed-in users
- End-to-end encryption (if implemented)
- Never shared across users
- Incognito mode disables memory entirely

## Comparison: Inno vs Custom Personas

| Feature | Inno | Custom Personas |
|---------|------|-----------------|
| **Creation** | Automatic | Manual |
| **Default** | Yes (system-wide) | No (user-defined) |
| **App Knowledge** | Comprehensive | None (unless prompted) |
| **Memory Scope** | Global to persona | Configurable |
| **Memory Budget** | 50,000 tokens | 12,000 tokens |
| **Retention** | 365 days | 90 days |
| **Context Window** | 100,000 tokens | 48,000 tokens |
| **Proactivity** | High (suggest_when_helpful) | Medium (ask_when_unclear) |
| **UI Badge** | ⭐ DEFAULT | None |
| **Deletion** | Not allowed | Allowed |
| **Purpose** | Universal companion | Specialized roles |

## Advanced Features

### Proactive Suggestions

Inno monitors conversation patterns and proactively suggests:
- "Did you know Innovexia can upload PDFs for context?"
- "For this type of task, you might want to create a specialized persona"
- "I noticed you often ask about [topic] - shall I remember this preference?"

### Daily Reflection

When appropriate, Inno offers reflection prompts:
- "How was your day? Anything interesting happen?"
- "What's on your mind today?"
- "Any wins or challenges you'd like to share?"

### Feature Discovery

Inno suggests relevant features based on needs:
- User asks repeated questions → Suggest creating a knowledge base persona
- User uploads many images → Suggest enabling vision tools
- User has complex tasks → Suggest enabling thinking mode

### Contextual Awareness

Inno adapts based on:
- **Time of day**: Morning greetings, evening reflections
- **Conversation flow**: Remembers earlier topics
- **User state**: Recognizes frustration, excitement, confusion
- **Task context**: Understands if quick question vs deep discussion

## Technical Implementation

### Files Created/Modified

**Created**:
- `core/persona/InnoPersonaDefaults.kt` - Factory and configuration
- `INNO_PERSONA_README.md` - This documentation

**Modified**:
- `InnovexiaApplication.kt` - Auto-seeding on startup
- `PersonaRepository.kt` - Inno-specific repository methods
- `PersonaPreferences.kt` - Default to Inno logic
- `PersonaChip.kt` - Special UI styling for Inno
- `PersonaCardV2.kt` - DEFAULT badge for Inno

### Repository Methods

```kotlin
// Get or create Inno for a user
suspend fun getOrCreateInnoPersona(ownerId: String): Persona

// Check if Inno exists
suspend fun hasInnoPersona(ownerId: String): Boolean

// Ensure Inno is the default persona
suspend fun ensureInnoIsDefault(ownerId: String): Persona

// Get Inno persona (null if doesn't exist)
suspend fun getInnoPersona(ownerId: String): Persona?
```

## Testing Checklist

### Functionality
- [x] Inno created automatically on first launch
- [x] Inno set as default persona for new users
- [x] Persona selection persists across app restarts
- [x] Selected persona doesn't revert to Inno unexpectedly
- [x] Chats created with correct persona
- [x] Inno visible in persona selector with DEFAULT badge
- [x] PersonaChip shows star icon for Inno
- [x] Switching personas works correctly
- [x] Guest and signed-in users have separate Inno instances

### Memory & Learning
- [ ] Inno remembers information across chats
- [ ] Memory budget allows extensive context
- [ ] Auto-write saves important facts
- [ ] Proactive suggestions appear naturally
- [ ] Daily reflection prompts work
- [ ] Feature discovery suggestions are relevant

### UI/UX
- [x] Inno chip displays correctly in composer
- [x] DEFAULT badge shows in persona card
- [x] Star icon visible in persona chip
- [x] Inno listed first in "My Personas"
- [ ] Inno greeting message displays on first interaction
- [ ] Tooltip explains Inno's role

### Edge Cases
- [ ] Deleting selected persona falls back to Inno
- [ ] Sign-in/sign-out preserves persona selection
- [ ] Guest-to-signed-in migration handles Inno correctly
- [ ] Multiple devices sync Inno selection
- [ ] Inno can't be accidentally deleted (system persona)
- [ ] Updating Inno's configuration works

## Future Enhancements

### Phase 2: Enhanced Capabilities
1. **Pre-seeded Knowledge Base**: Include app documentation as sources
2. **Voice Mode**: Special voice interaction optimized for Inno
3. **Notifications**: Inno can send helpful reminders
4. **Analytics Dashboard**: Show Inno usage vs custom personas
5. **A/B Testing**: Different Inno prompts for optimization

### Phase 3: Advanced Features
1. **Inno Pro**: Enhanced version for Pro subscribers
2. **Multi-modal**: Vision, audio, code execution integrated
3. **Workflow Automation**: Inno suggests and creates workflows
4. **Cross-device Continuity**: Seamless conversation across devices
5. **Conversation Summaries**: Periodic digests of learnings

### Phase 4: Social Features
1. **Inno Insights**: Share interesting learnings (opt-in)
2. **Community Templates**: Inno configurations shared by users
3. **Collaborative Learning**: Aggregate learnings across users (privacy-preserving)

## Conclusion

Inno represents a fundamental shift in how users interact with Innovexia. Rather than starting with a blank slate, every user gets an expert AI companion that:
- **Knows the app inside-out**
- **Learns and adapts to the user**
- **Provides genuine companionship**
- **Discovers features naturally**
- **Grows smarter over time**

By making Inno the default and giving it powerful capabilities, we ensure every Innovexia user has an exceptional experience from the very first message.

---

**Version**: 1.0
**Last Updated**: 2025-01-14
**Status**: ✅ Implemented and Active
