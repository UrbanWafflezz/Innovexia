# Gemini Markdown Best Practices

## How to Configure Gemini to Use Markdown Properly

To get the best markdown formatting from Gemini AI in your Innovexia app, configure your prompts and system instructions properly.

---

## 🎯 System Instructions

Add this to your Gemini system instructions (in your app's GeminiService or prompts):

```kotlin
val systemInstruction = """
You are Innovexia AI, a helpful assistant integrated into an Android app.

FORMATTING GUIDELINES:
- Use **bold** for emphasis and important terms
- Use `inline code` for technical terms, class names, and variables
- Use code blocks with language tags for all code examples
- Use numbered lists for step-by-step instructions
- Use bullet lists for features or options
- Use headings (##) to organize long responses
- Use tables for comparisons
- Use > quotes for important notes
- Keep responses clear and well-structured

EXAMPLE RESPONSE FORMAT:
# Topic Title

Brief introduction paragraph.

## Main Concept

Explanation with **key terms** in bold and `code` inline.

### Code Example

```kotlin
fun example() {
    // Your code here
}
```

### Key Points

1. First point
2. Second point
3. Third point

> **Note:** Important information here
""".trimIndent()
```

---

## ✅ DO's

### ✅ DO: Use Code Blocks with Language Tags
**Good:**
````
```kotlin
class MyViewModel : ViewModel() {
    val data = MutableLiveData<String>()
}
```
````

**Bad:**
```
MyViewModel code:
class MyViewModel : ViewModel() { ... }
```

---

### ✅ DO: Use Proper Headings for Organization
**Good:**
```markdown
# Main Topic

## Subtopic 1

Content here

## Subtopic 2

More content
```

**Bad:**
```
MAIN TOPIC
Subtopic 1
Content
```

---

### ✅ DO: Use Inline Code for Technical Terms
**Good:**
```markdown
The `ViewModel` class extends `AndroidViewModel` and manages UI data.
```

**Bad:**
```markdown
The ViewModel class extends AndroidViewModel and manages UI data.
```

---

### ✅ DO: Use Numbered Lists for Steps
**Good:**
```markdown
1. Open Android Studio
2. Create new project
3. Configure build.gradle
4. Sync project
```

**Bad:**
```markdown
First, open Android Studio. Then create new project. Next, configure build.gradle...
```

---

### ✅ DO: Use Tables for Comparisons
**Good:**
```markdown
| Feature | Free | Premium |
|---------|------|---------|
| Storage | 5GB  | 100GB   |
| Support | Email| 24/7    |
```

**Bad:**
```markdown
Free: 5GB storage, email support
Premium: 100GB storage, 24/7 support
```

---

## ❌ DON'Ts

### ❌ DON'T: Mix Inline Code with Bold
**Bad:**
```markdown
**`ViewModel`** is used for...
```

**Good:**
```markdown
The `ViewModel` class is **essential** for...
```

---

### ❌ DON'T: Use Headings Inside Lists
**Bad:**
```markdown
1. Step one
## Heading
2. Step two
```

**Good:**
```markdown
## Step One
1. Do this
2. Then this

## Step Two
1. Now this
```

---

### ❌ DON'T: Over-Bold Everything
**Bad:**
```markdown
**Android** uses **Kotlin** and **Java** for **app development**
```

**Good:**
```markdown
Android uses Kotlin and Java for app development. **Kotlin** is now the preferred language.
```

---

## 🔧 Gemini Prompt Engineering

### Example User Prompt
```
Explain how to create a RecyclerView in Android.
Use code examples with proper formatting.
```

### Gemini's Response (Well-Formatted)
````markdown
# Creating a RecyclerView in Android

## Overview

`RecyclerView` is a **flexible** view for displaying large datasets efficiently.

## Implementation Steps

1. Add dependency to `build.gradle`
2. Create layout XML
3. Create ViewHolder class
4. Create Adapter class
5. Initialize in Activity

## Step 1: Add Dependency

```kotlin
dependencies {
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
```

## Step 2: Layout XML

```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recyclerView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

## Step 3: Create ViewHolder

```kotlin
class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val textView: TextView = view.findViewById(R.id.textView)
}
```

## Step 4: Create Adapter

```kotlin
class MyAdapter(private val items: List<String>) :
    RecyclerView.Adapter<MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textView.text = items[position]
    }

    override fun getItemCount() = items.size
}
```

## Step 5: Initialize

```kotlin
val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
recyclerView.layoutManager = LinearLayoutManager(this)
recyclerView.adapter = MyAdapter(myDataList)
```

> **Tip:** Use `DiffUtil` for efficient updates!

## Common Issues

| Issue | Solution |
|-------|----------|
| Items not showing | Check layout manager |
| Slow scrolling | Use ViewHolder pattern |
| Memory leaks | Clean up in onDetachedFromWindow |

---

**Need more help?** Check the [Android documentation](https://developer.android.com/guide/topics/ui/layout/recyclerview)
````

---

## 📊 Response Quality Checklist

When Gemini responds, it should:

- [ ] Use proper markdown formatting
- [ ] Include code blocks with language tags
- [ ] Use headings for organization
- [ ] Use bold for emphasis (sparingly)
- [ ] Use inline code for technical terms
- [ ] Use lists for steps or features
- [ ] Include tables for comparisons (if relevant)
- [ ] Add quotes for important notes
- [ ] Provide clear, structured information

---

## 🎨 Formatting Consistency

### For Code Explanations
```markdown
# [Topic]

Brief intro with **key concepts** and `technical terms`.

## How It Works

Step-by-step explanation.

## Example

```language
code here
```

## Key Points

- Point 1
- Point 2
```

### For Troubleshooting
```markdown
# Error: [Error Name]

## Cause

Explanation of why this happens.

## Solution

1. Step one
2. Step two
3. Step three

> **Note:** Additional tip

## Prevention

How to avoid this in the future.
```

### For Comparisons
```markdown
# [Topic A] vs [Topic B]

| Feature | Option A | Option B |
|---------|----------|----------|
| Speed   | Fast     | Slow     |
| Memory  | High     | Low      |

## Recommendation

Use **Option A** when...
```

---

## 🚀 Testing Gemini Responses

### Test Prompt 1: Simple Explanation
```
"Explain what a ViewModel is in Android"
```

**Expected Format:**
```markdown
# Android ViewModel

A `ViewModel` is a class that **stores and manages UI-related data** in a lifecycle-conscious way.

## Key Benefits

1. Survives configuration changes
2. Separates UI from business logic
3. Makes testing easier

## Example

```kotlin
class MyViewModel : ViewModel() {
    private val _data = MutableLiveData<String>()
    val data: LiveData<String> = _data
}
```
```

### Test Prompt 2: Code Request
```
"Show me how to make a network request in Kotlin"
```

**Expected Format:**
````markdown
# Network Request in Kotlin

Use `Retrofit` for professional API calls.

## Setup

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

## Interface

```kotlin
interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<User>
}
```

## Usage

```kotlin
val service = retrofit.create(ApiService::class.java)
val users = service.getUsers()
```

> **Tip:** Use coroutines for async operations!
````

### Test Prompt 3: Troubleshooting
```
"My RecyclerView isn't showing items, what could be wrong?"
```

**Expected Format:**
```markdown
# RecyclerView Not Showing Items

## Common Causes

1. **Layout Manager not set**
   ```kotlin
   recyclerView.layoutManager = LinearLayoutManager(this)
   ```

2. **Adapter not attached**
   ```kotlin
   recyclerView.adapter = myAdapter
   ```

3. **Empty dataset**
   ```kotlin
   // Check if data list has items
   Log.d("TAG", "Item count: ${adapter.itemCount}")
   ```

## Debug Steps

| Step | Check |
|------|-------|
| 1 | Layout manager set? |
| 2 | Adapter attached? |
| 3 | Data list not empty? |
| 4 | Item layout valid? |
```

---

## 💡 Advanced Tips

### 1. Use Callouts for Warnings
```kotlin
// In your prompt or system instructions
"Use :::warning for important warnings"
```

**Gemini should output:**
```markdown
:::warning
Never store passwords in plain text!
:::
```

### 2. Use Collapsible for Long Content
```markdown
+++See full code example
[Long code here]
+++
```

### 3. Combine Features
```markdown
# Tutorial

## Prerequisites

- Android Studio installed
- Basic Kotlin knowledge

## Steps

1. Create project
2. Add dependencies

```kotlin
implementation("androidx.core:core-ktx:1.12.0")
```

3. Configure manifest

> **Important:** Don't forget internet permission!

## Troubleshooting

| Error | Fix |
|-------|-----|
| Sync failed | Check connection |
```

---

## 📝 Summary

✅ **Always Use:**
- Code blocks with language (```` ```kotlin ````)
- Headings for structure (`##`)
- Inline code for terms (`` `ViewModel` ``)
- Lists for steps (numbered) or features (bullets)
- Bold for emphasis (**important**)

❌ **Avoid:**
- Plain text code (no formatting)
- UPPERCASE FOR EMPHASIS
- Over-bolding everything
- Mixing code inline with bold
- Unstructured walls of text

🎯 **Result:**
Clean, professional, easy-to-read AI responses that look great in your Innovexia app!

---

**Rebuild your app and test with these prompts to see the beautiful markdown rendering!** 🚀
