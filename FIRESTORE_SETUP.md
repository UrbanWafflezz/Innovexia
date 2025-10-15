# Firestore Setup for Persona 2.0

## Current Structure

Your Firestore currently shows:
```
/users/{uid}
  /chats/{chatId}
```

## Required Structure for Persona 2.0

You need to add:
```
/users/{uid}
  /personas/{personaId}  ← Add this collection
    - name, initial, color, bio, tags, etc.

/public
  /personas/{personaId}  ← Add this for published personas
```

## Quick Setup via Firebase Console

### Option 1: Manual Setup (Recommended for Testing)

1. **Go to your Firestore console** (already open)

2. **Navigate to your user document:**
   - Click on `users` collection
   - Click on `pR2ESVPZf1WgUDoeemG8pSXDcZA2` (your user ID)

3. **Start a new subcollection:**
   - Click "+ Start collection"
   - Collection ID: `personas`
   - Click "Next"

4. **Add first persona document:**
   - Document ID: (Auto-ID)
   - Add these fields:

   ```
   name: "Researcher" (string)
   initial: "R" (string)
   color: 4284377971 (number)
   bio: "Deep web researcher" (string)
   tags: ["research", "web"] (array)
   defaultLanguage: "en-US" (string)
   greeting: "How can I help you research today?" (string)
   isDefault: true (boolean)
   visibility: "private" (string)
   status: "draft" (string)
   createdAt: (timestamp - use server timestamp)
   updatedAt: (timestamp - use server timestamp)
   ```

   **Add nested objects:**

   Click "Add field" → Field name: `behavior` → Type: `map`
   Then add these inside `behavior`:
   ```
   conciseness: 0.6 (number)
   formality: 0.5 (number)
   empathy: 0.4 (number)
   creativityTemp: 0.7 (number)
   topP: 0.9 (number)
   thinkingDepth: "balanced" (string)
   ```

   Click "Add field" → Field name: `system` → Type: `map`
   Inside `system`:
   ```
   instructions: "You are a helpful research assistant..." (string)
   version: 1 (number)
   ```

   Click "Add field" → Field name: `memory` → Type: `map`
   Inside `memory`:
   ```
   scope: "per_chat" (string)
   retentionDays: 90 (number)
   ```

   Click "Add field" → Field name: `sources` → Type: `map`
   Inside `sources`:
   ```
   webAccess: "allowlist" (string)
   allow: ["arxiv.org", "nature.com"] (array)
   ```

   Click "Add field" → Field name: `tools` → Type: `map`
   Inside `tools`:
   ```
   web: true (boolean)
   code: false (boolean)
   vision: true (boolean)
   ```

   Click "Add field" → Field name: `limits` → Type: `map`
   Inside `limits`:
   ```
   maxOutputTokens: 1200 (number)
   maxContextTokens: 48000 (number)
   ```

5. **Click "Save"**

### Option 2: Using Android App

Once you compile and run the app:

1. Open the app
2. Tap on the persona chip (bottom left)
3. Tap "+ New" button
4. Fill out the form in the new Persona 2.0 dialog
5. Tap "Save Draft" or "Publish"

The app will automatically create the Firestore structure!

### Option 3: Import JSON (Quick!)

In Firebase Console:

1. Navigate to your user document
2. Click the three dots (⋮) menu
3. Select "Import"
4. Use this JSON:

```json
{
  "personas": {
    "researcher-001": {
      "name": "Researcher",
      "initial": "R",
      "color": 4284377971,
      "bio": "Deep web researcher",
      "tags": ["research", "web"],
      "defaultLanguage": "en-US",
      "greeting": "How can I help you research today?",
      "isDefault": true,
      "visibility": "private",
      "status": "draft",
      "behavior": {
        "conciseness": 0.6,
        "formality": 0.5,
        "empathy": 0.4,
        "creativityTemp": 0.7,
        "topP": 0.9,
        "thinkingDepth": "balanced",
        "proactivity": "ask_when_unclear",
        "safetyLevel": "standard",
        "hallucinationGuard": "prefer_idk",
        "selfCheck": {
          "enabled": true,
          "maxMs": 500
        },
        "citationPolicy": "when_uncertain",
        "formatting": {
          "markdown": true,
          "emoji": "light"
        }
      },
      "system": {
        "instructions": "You are a helpful research assistant who helps users find and analyze information.",
        "rules": [],
        "variables": ["{user_name}", "{today}", "{timezone}", "{app_name}"],
        "version": 1
      },
      "memory": {
        "scope": "per_chat",
        "retentionDays": 90,
        "autoWrite": "ask",
        "budgetTokens": 12000,
        "summarizeThreshold": 2000
      },
      "sources": {
        "webAccess": "allowlist",
        "allow": ["arxiv.org", "nature.com"],
        "deny": [],
        "files": [],
        "sync": "manual",
        "trustWeight": 0.8,
        "citationStyle": "inline"
      },
      "tools": {
        "web": true,
        "code": false,
        "vision": true,
        "audio": false,
        "functions": [],
        "modelRouting": {
          "preferred": "fast",
          "fallbacks": ["thinking"]
        }
      },
      "limits": {
        "maxOutputTokens": 1200,
        "maxContextTokens": 48000,
        "timeBudgetMs": 15000,
        "rateWeight": 1.0,
        "concurrency": 2
      },
      "testing": {
        "scenarios": []
      },
      "createdAt": {
        "__type__": "timestamp",
        "value": "2025-01-07T00:00:00Z"
      },
      "updatedAt": {
        "__type__": "timestamp",
        "value": "2025-01-07T00:00:00Z"
      },
      "author": {
        "uid": "pR2ESVPZf1WgUDoeemG8pSXDcZA2",
        "name": "User"
      }
    }
  }
}
```

## Create Public Personas Collection

For the public personas (optional):

1. Go to Firestore root
2. Click "+ Start collection"
3. Collection ID: `public`
4. Document ID: `personas`
5. Add field: `initialized` (boolean) = true
6. Save
7. Inside this document, create a subcollection named `personas`
8. Add the Innovexia default persona there

## Verify Setup

After setup, your Firestore should look like:

```
📂 (default)
├── 📁 users
│   └── 📄 pR2ESVPZf1WgUDoeemG8pSXDcZA2
│       ├── 📁 chats
│       └── 📁 personas ← NEW!
│           └── 📄 researcher-001
└── 📁 public ← NEW!
    └── 📄 personas
        └── 📁 personas ← Subcollection
            └── 📄 innovexia-default
```

## Testing

After adding data:

1. Compile and run the app: `./gradlew build`
2. Open the Personas sheet (tap persona chip)
3. Your "Researcher" persona should appear in "My" tab
4. Tap to select it
5. Try creating a new one with the "+ New" button

## Troubleshooting

**If personas don't appear:**
1. Check Firestore rules are deployed (see firestore.rules)
2. Check user is signed in
3. Check user ID matches: `pR2ESVPZf1WgUDoeemG8pSXDcZA2`
4. Check CreatePersonaViewModel is saving to correct path

**If you get permission errors:**
```bash
firebase deploy --only firestore:rules
```

## Next Steps

1. Add 2-3 more sample personas
2. Test editing a persona
3. Test publishing a persona
4. Deploy Cloud Functions for publish/unpublish
