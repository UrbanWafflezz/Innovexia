# Persona Memory System Architecture

## Visual Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          USER INTERACTION LAYER                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ "My name is John"
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            HomeViewModel                                     │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │  sendMessage(userMessage, persona)                                  │    │
│  │    ├─► User sends message                                           │    │
│  │    ├─► GeminiService generates AI response                          │    │
│  │    └─► After response completes...                                  │    │
│  └────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ ChatTurn { user: "My name is John",
                                      │            assistant: "Nice to meet you!" }
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         MEMORY ENGINE (Core API)                             │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │  MemoryEngine.ingest(turn, personaId, incognito)                   │    │
│  │    ├─► Check if memory enabled for persona                         │    │
│  │    └─► Forward to Ingestor                                          │    │
│  └────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         INGESTION PIPELINE                                   │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │  Ingestor.ingest()                                                  │    │
│  │    │                                                                 │    │
│  │    ├─► NORMALIZE: Clean text, remove noise                          │    │
│  │    │                                                                 │    │
│  │    ├─► CLASSIFY: Heuristics detect memory type                      │    │
│  │    │   • "My name is" → FACT                                        │    │
│  │    │   • "I like" → PREFERENCE                                      │    │
│  │    │   • "yesterday" → EVENT                                        │    │
│  │    │                                                                 │    │
│  │    ├─► EMBED: FakeEmbedder creates 384-dim vector                   │    │
│  │    │   [0.23, -0.51, 0.89, ...] → Quantize to int8                 │    │
│  │    │                                                                 │    │
│  │    └─► STORE: Write to 3 tables in parallel                         │    │
│  └────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
                    ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       STORAGE LAYER (Room/SQLite)                            │
│                                                                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐         │
│  │  memories        │  │  memories_fts    │  │  memory_vectors  │         │
│  ├──────────────────┤  ├──────────────────┤  ├──────────────────┤         │
│  │ id: "abc123"     │  │ id: "abc123"     │  │ memoryId: "abc"  │         │
│  │ personaId: "p1"  │  │ text: "my name"  │  │ dim: 384         │         │
│  │ text: "My name   │  │       "is john"  │  │ q8: [29,-65...]  │         │
│  │       is John"   │  │                  │  │ scale: 0.0078    │         │
│  │ kind: FACT       │  │ (FTS4 index)     │  │ (quantized vec)  │         │
│  │ importance: 0.8  │  └──────────────────┘  └──────────────────┘         │
│  │ createdAt: ...   │                                                       │
│  │ userId: "guest"  │                                                       │
│  └──────────────────┘                                                       │
│       │                                                                      │
│       └─► Indexed by: personaId, kind, createdAt, importance               │
└─────────────────────────────────────────────────────────────────────────────┘




         ═══════════════════════════════════════════════════════════
                          RETRIEVAL FLOW (Next Chat)
         ═══════════════════════════════════════════════════════════

                                      │
                                      │ User asks: "What's my name?"
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            GeminiService                                     │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │  generateReply(userText, persona, chatId)                           │    │
│  │    │                                                                 │    │
│  │    ├─► BEFORE generating response...                                │    │
│  │    └─► MemoryEngine.contextFor(userText, personaId, chatId)         │    │
│  └────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         RETRIEVAL PIPELINE                                   │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │  Retriever.retrieve()                                               │    │
│  │    │                                                                 │    │
│  │    ├─► FTS SEARCH: "what's my name" → finds "name"                  │    │
│  │    │   • memories_fts MATCH query                                   │    │
│  │    │   • Returns: ["abc123"]                                        │    │
│  │    │                                                                 │    │
│  │    ├─► VECTOR SEARCH: Embed query → find similar                    │    │
│  │    │   • Compute cosine similarity with all vectors                 │    │
│  │    │   • Returns top matches by similarity                          │    │
│  │    │                                                                 │    │
│  │    ├─► HYBRID RANKING:                                              │    │
│  │    │   score = 0.3×FTS + 0.4×Vector + 0.2×Recency + 0.1×Importance │    │
│  │    │                                                                 │    │
│  │    └─► Return top 10 memories                                       │    │
│  └────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ ContextBundle {
                                      │   longTerm: [Memory("My name is John")],
                                      │   shortTerm: [recent chat history]
                                      │ }
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        PROMPT CONSTRUCTION                                   │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │  GeminiService builds final prompt:                                 │    │
│  │                                                                      │    │
│  │  "Relevant memories about the user:                                 │    │
│  │   - My name is John                                                 │    │
│  │   - I prefer dark mode                                              │    │
│  │                                                                      │    │
│  │   User: What's my name?                                             │    │
│  │   Assistant: "                                                      │    │
│  └────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          GEMINI 2.5 FLASH                                    │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │  Sees memories in context → Responds:                               │    │
│  │  "Your name is John! 😊"                                            │    │
│  └────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
                                  USER SEES
                              "Your name is John!"
```

## Key Components

### 1. **Ingestion Flow** (Storing Memories)
```
User Message → Normalize → Classify → Embed → Quantize → Store (3 tables)
```

### 2. **Retrieval Flow** (Recalling Memories)
```
User Query → Embed Query → Hybrid Search (FTS + Vector) → Rank → Top Results
```

### 3. **Storage Strategy**
- **memories**: Core data (text, metadata, timestamps)
- **memories_fts**: Full-text search index (FTS4)
- **memory_vectors**: Quantized embeddings (127x compression)

### 4. **Hybrid Ranking Formula**
```
score = 0.3 × FTS_score +
        0.4 × vector_similarity +
        0.2 × recency_boost +
        0.1 × importance_weight
```

## Data Flow Example

### Storing: "My name is John"
```
1. Input: "My name is John"
2. Normalize: "my name is john"
3. Classify: kind=FACT, importance=0.8
4. Embed: [0.23, -0.51, 0.89, ..., 0.12] (384 dims)
5. Quantize: [29, -65, 113, ..., 15] (int8)
6. Store:
   - memories: Full record with metadata
   - memories_fts: Indexed text for search
   - memory_vectors: Compressed embedding
```

### Retrieving: "What's my name?"
```
1. Query: "what's my name"
2. FTS Search: MATCH "name" → ["abc123"]
3. Vector Search: embed("what's my name") → cosine similarity
4. Combine: Hybrid ranking
5. Result: Memory { text: "My name is John", score: 0.95 }
6. Inject into prompt
7. AI responds: "Your name is John!"
```

## Isolation & Privacy

```
┌────────────────────────────────────────────┐
│  Persona "Innovexia"                       │
│  ├─ Memory: "My name is John"              │
│  ├─ Memory: "I like dark mode"             │
│  └─ Memory: "Working on Android app"       │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│  Persona "Helper"                          │
│  ├─ Memory: "My name is John"              │
│  └─ Memory: "Need help with math"          │
└────────────────────────────────────────────┘

Each persona has ISOLATED memories
All queries filtered by: WHERE personaId = ?
```

## Performance Optimizations

1. **Int8 Quantization**: 127x smaller vectors (float32 → int8)
2. **FTS4 Indexing**: Fast text search without scanning
3. **Indexed Columns**: personaId, kind, createdAt, importance
4. **WAL Mode**: Write-ahead logging for concurrency
5. **Batch Embedding**: Future optimization for multiple memories

## Future Enhancements (Not Implemented)

- Replace FakeEmbedder with ONNX model (real embeddings)
- Add memory consolidation (merge similar memories)
- Add memory decay (reduce importance over time)
- Add user feedback (thumbs up/down on memories)
- Add privacy controls (delete specific memories)
