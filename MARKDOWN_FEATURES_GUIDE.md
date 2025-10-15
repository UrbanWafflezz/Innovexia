# Markdown Features Guide - ResponseBubbleV2

## Complete Markdown Support for Gemini Responses

ResponseBubbleV2 now has **professional-grade markdown rendering** that handles all common formats Gemini AI uses.

---

## ✅ Supported Features

### 1. **Text Formatting**

#### Bold
```markdown
**This is bold text**
```
Output: **This is bold text**

#### Italic
```markdown
*This is italic* or _this is also italic_
```
Output: *This is italic*

Note: Underscores won't italicize snake_case variables

#### Bold + Italic
```markdown
***This is bold and italic***
```
Output: ***This is bold and italic***

#### Strikethrough
```markdown
~~This is struck through~~
```
Output: ~~This is struck through~~

#### Inline Code
```markdown
Use the `console.log()` function
```
Output: Use the `console.log()` function
- Displays in monospace with blue tint
- Subtle background highlight

---

### 2. **Headings**

```markdown
# Heading 1
## Heading 2
### Heading 3
#### Heading 4
##### Heading 5
###### Heading 6
```

**Sizes:**
- H1: 18sp (largest)
- H2: 17sp
- H3: 16sp
- H4-6: 15sp

---

### 3. **Lists**

#### Unordered Lists
```markdown
- First item
- Second item
- Third item
```

Or use asterisks:
```markdown
* First item
* Second item
* Third item
```

#### Ordered Lists
```markdown
1. First step
2. Second step
3. Third step
```

**Rendering:**
- Proper indentation
- Bullet symbols: `•`
- Numbers for ordered lists

---

### 4. **Code Blocks**

#### Basic Code Block
````markdown
```
Code without language highlighting
```
````

#### With Language
````markdown
```python
def hello():
    print("Hello World")
```
````

#### Supported Languages
The parser auto-detects and normalizes:
- **Python**: `python`, `py`, `python3`
- **JavaScript**: `javascript`, `js`
- **TypeScript**: `typescript`, `ts`
- **Kotlin**: `kotlin`, `kt`
- **Java**: `java`
- **C++**: `cpp`, `c++`
- **C#**: `csharp`, `cs`
- **Ruby**: `ruby`, `rb`
- **Go**: `go`, `golang`
- **Rust**: `rust`, `rs`
- **Swift**: `swift`
- **PHP**: `php`
- **Bash**: `bash`, `sh`, `shell`
- **SQL**: `sql`
- **HTML**: `html`
- **CSS**: `css`
- **JSON**: `json`
- **XML**: `xml`
- **YAML**: `yaml`, `yml`
- **Markdown**: `markdown`, `md`

**Features:**
- Language label in header (e.g., "PYTHON", "KOTLIN")
- Copy button (copies entire code)
- Fullscreen button
- Monospace font
- Horizontal scrolling for long lines

---

### 5. **Block Quotes**

```markdown
> This is a quote
> It can span multiple lines
```

**Rendering:**
- Left border accent
- Italic text
- Slightly transparent

---

### 6. **Links**

```markdown
[Visit Google](https://google.com)
```

**Rendering:**
- Blue underlined text
- Clickable (opens in browser)

---

### 7. **Tables**

```markdown
| Name    | Age | City      |
|---------|-----|-----------|
| Alice   | 25  | New York  |
| Bob     | 30  | London    |
| Charlie | 35  | Tokyo     |
```

**Features:**
- Header row with bold text
- Alternating row colors
- Horizontal scrolling for wide tables
- Border and dividers

---

### 8. **Horizontal Dividers**

```markdown
---
```

Or:
```markdown
***
___
```

All render as a horizontal line

---

### 9. **Images** (Placeholder)

```markdown
![Alt text](https://example.com/image.jpg)
```

**Current Status:**
- Shows icon + alt text placeholder
- Will load actual images once Coil is synced

---

### 10. **Callouts** (Special Syntax)

#### Info Callout
```markdown
:::info
This is important information
:::
```

#### Warning Callout
```markdown
:::warning
This is a warning message
:::
```

#### Tip Callout
```markdown
:::tip
This is a helpful tip
:::
```

**Rendering:**
- Icon (ℹ️, ⚠️, 💡)
- Colored background
- Colored border

---

### 11. **Collapsible Sections**

#### Using HTML syntax:
```markdown
<details>
<summary>Click to expand</summary>
Hidden content goes here
</details>
```

#### Using custom syntax:
```markdown
+++Show more details
This content is hidden by default
+++
```

**Features:**
- Rotating arrow icon
- Smooth expand/collapse animation
- Click anywhere to toggle

---

## 🎯 How Gemini Should Use Markdown

### **For Code Examples:**
Always use fenced code blocks with language:
````
```kotlin
fun greet(name: String) {
    println("Hello, $name!")
}
```
````

### **For Step-by-Step Instructions:**
Use ordered lists:
```
1. Open the file
2. Make the changes
3. Save and test
```

### **For Highlighting Important Info:**
Use callouts:
```
:::warning
Make sure to back up your data first!
:::
```

### **For Technical Terms:**
Use inline code:
```
The `ViewModel` class manages UI-related data
```

### **For Emphasis:**
Use bold for strong emphasis:
```
This is **very important** to remember
```

---

## 📊 Examples in Context

### Example 1: Code Explanation
````markdown
# How to Create a ViewModel

To create a ViewModel in Android, follow these steps:

1. Add the dependency to your `build.gradle`
2. Create a class extending `ViewModel`
3. Initialize it in your Activity

Here's an example:

```kotlin
class MyViewModel : ViewModel() {
    private val _data = MutableLiveData<String>()
    val data: LiveData<String> = _data

    fun updateData(newValue: String) {
        _data.value = newValue
    }
}
```

**Important:** ViewModels survive configuration changes!

:::tip
Use `ViewModelProvider` to instantiate your ViewModel
:::
````

### Example 2: Comparison Table
````markdown
## Android Architectures Comparison

| Pattern | Complexity | Testability | Learning Curve |
|---------|------------|-------------|----------------|
| MVC     | Low        | Medium      | Easy           |
| MVP     | Medium     | High        | Medium         |
| MVVM    | High       | High        | Hard           |
````

### Example 3: Troubleshooting Guide
````markdown
# Common Build Errors

## Error: "Unresolved reference"

This usually means:
- Missing import statement
- Typo in class/function name
- Dependency not added to `build.gradle`

**Solution:**
1. Check your imports
2. Verify spelling
3. Sync Gradle files

> If the issue persists, try **Build → Clean Project**
````

---

## 🎨 Visual Appearance

### Inline Code
- **Color**: Blue tint (#3B82F6)
- **Background**: 12% opacity overlay
- **Font**: Monospace, 13sp

### Code Blocks
- **Background**: Surface variant (30% opacity)
- **Border**: Subtle outline
- **Header**: Language label + action buttons
- **Font**: Monospace, 12sp, 18sp line height

### Lists
- **Spacing**: 2dp between items
- **Bullet**: • (centered)
- **Numbers**: 1., 2., 3.
- **Indent**: 20dp

### Tables
- **Header**: Bold, background tint
- **Rows**: Alternating 3% background
- **Borders**: All cells
- **Scroll**: Horizontal for overflow

---

## ⚙️ Parser Features

### Smart Detection
- **Code blocks**: Handles missing closing ```
- **Lists**: Auto-detects ordered vs unordered
- **Paragraphs**: Combines multiple lines
- **Special lines**: Stops paragraph at headings, lists, etc.

### Language Normalization
- `py` → `python`
- `js` → `javascript`
- `kt` → `kotlin`
- `cpp` → `c++`
- Etc.

### Error Handling
- Missing closing delimiters: Shows what's available
- Empty blocks: Skipped
- Invalid syntax: Falls back to paragraph

---

## 🚀 Testing Markdown

### Test Message 1: Basic Formatting
```
**Bold text**, *italic text*, ~~strikethrough~~, and `inline code`.
```

### Test Message 2: Code Block
````
Here's a Kotlin example:

```kotlin
fun main() {
    println("Hello, Innovexia!")
}
```
````

### Test Message 3: Lists
```
Shopping list:
1. Milk
2. Eggs
3. Bread

Features:
- Fast
- Reliable
- Easy to use
```

### Test Message 4: Complex Example
````
# Android Development Tips

## Best Practices

1. **Use ViewModel** for data persistence
2. **Follow SOLID** principles
3. **Write tests** for critical code

### Example ViewModel

```kotlin
class UserViewModel : ViewModel() {
    private val repository = UserRepository()
    val users = repository.getUsers()
}
```

:::warning
Don't store Context in ViewModel!
:::

> ViewModels are lifecycle-aware components

For more info, see [Android Docs](https://developer.android.com)
````

---

## 📝 Summary

✅ **Fully Supported:**
- Bold, italic, strikethrough
- Inline code with color
- Headings (H1-H6)
- Unordered & ordered lists
- Code blocks (30+ languages)
- Block quotes
- Links (clickable)
- Tables (scrollable)
- Dividers
- Callouts (info/warning/tip)
- Collapsible sections

🚧 **Partial Support:**
- Images (placeholder until Coil synced)

❌ **Not Supported:**
- Nested lists (coming soon)
- Footnotes
- Task lists `- [ ]`
- Emoji shortcodes `:smile:`

---

**The markdown renderer is now ready for professional AI conversations!** 🎉
