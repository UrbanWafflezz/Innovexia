# Innovexia

An AI-powered chat application with advanced features including multi-model support, document processing, voice input, and cloud synchronization.

## Download

**Latest Release:** [Download APK](https://github.com/UrbanWafflezz/Innovexia/releases/latest)

### Installation Instructions

1. **Enable Unknown Sources:**
   - Open your Android device's **Settings**
   - Navigate to **Security** or **Privacy** (location varies by device)
   - Enable **Install from Unknown Sources** or **Allow app installs from unknown sources**
   - For Android 8+ (Oreo), you'll grant this permission per-app when installing

2. **Download the APK:**
   - Visit the [Releases page](https://github.com/UrbanWafflezz/Innovexia/releases)
   - Download the latest `.apk` file

3. **Install the App:**
   - Open the downloaded APK file from your device's Downloads folder
   - If prompted, grant permission to install from this source
   - Tap **Install**
   - Once installed, tap **Open** to launch the app

4. **Updates:**
   - Check the [Releases page](https://github.com/UrbanWafflezz/Innovexia/releases) for new versions
   - Download and install the new APK (it will update the existing app)

---

## About

A modern Android chat application built with Jetpack Compose and Material 3, featuring a beautiful gradient UI and powerful AI capabilities.

## 🎯 Features

- **AI Chat Interface**: Powered by Google Gemini AI
- **Multiple Personas**: Switch between different AI personalities and modes
- **Document Support**: Upload and discuss PDFs, images, Word docs, and more
- **Voice Input**: Speak your messages naturally
- **Search Grounding**: Get real-time web search results integrated with AI responses
- **Cloud Sync**: Sync conversations and settings across devices (Firebase)
- **Local Models**: Support for offline AI models (TensorFlow Lite)
- **Modern UI**: Beautiful gradient interface with Material 3 design
- **Dark Mode**: Full dark theme support that follows system settings
- **Premium Features**: Unlock advanced capabilities via Stripe subscriptions

## 🛠 Tech Stack

- **Kotlin**: 2.1.0 with K2 compiler
- **Jetpack Compose**: Modern declarative UI with Material 3
- **Google Gemini AI**: Generative AI for chat
- **Firebase**: Authentication, Firestore, Cloud Storage
- **Room Database**: Local data persistence
- **Hilt**: Dependency injection
- **TensorFlow Lite**: On-device ML models
- **Stripe**: Payment and subscription management
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)

## System Requirements

- Android 7.0 (API 24) or higher
- Internet connection (for AI and cloud features)
- ~100MB storage space

## 🏗 Architecture

### Project Structure

```
app/src/main/java/com/example/innovexia/
├── MainActivity.kt
├── ui/
│   ├── theme/
│   │   ├── Color.kt          # Light/Dark color tokens
│   │   ├── Theme.kt          # Material 3 theme & gradients
│   │   └── Type.kt           # Inter font typography
│   ├── components/
│   │   ├── GradientScaffold.kt    # Gradient background wrapper
│   │   ├── BottomBar.kt           # Avatar + settings bar
│   │   ├── IconButtons.kt         # Chevron & gear buttons
│   │   └── ChatComposer.kt        # Reusable text input
│   └── screens/
│       ├── HomeScreen.kt          # Main empty state screen
│       └── ChatScreen.kt          # Placeholder chat screen
```

### Design System

#### Color Tokens

**Light Theme:**
- Primary Text: `#111827`
- Secondary Text: `#6B7280`
- Surface Elevated: `#FFFFFF`
- Accent Blue: `#3B82F6`
- Gradient: `#BFE8FF` → `#EAF6FF` → `#FFFFFF`

**Dark Theme:**
- Primary Text: `#E5E7EB`
- Secondary Text: `#9CA3AF`
- Surface Elevated: `#161B22`
- Accent Blue: `#60A5FA`
- Gradient: `#0F172A` → `#111827` → `#0B1220`

#### Typography

- **Font**: Inter (via Google Fonts)
- **Title**: 16sp, SemiBold
- **Subtitle**: 14sp, Normal
- **Button**: 16sp, SemiBold
- **Body**: 16sp, Normal

#### Spacing & Sizing

- Touch targets: 40dp minimum
- Icon size: 24dp
- Avatar: 28dp
- Bottom bar: 64dp height
- Button: 44dp height, 22dp radius
- ChatComposer: 24dp radius

## 🚀 Getting Started

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17+
- Android SDK 35

### Build & Run

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on emulator or device (API 24+)

### Gradle Optimizations

The project includes performance optimizations:
- Configuration cache enabled
- Build cache enabled
- Kotlin incremental compilation
- R8 full mode in release builds
- Non-transitive R classes

## 📱 UI Components

### HomeScreen

Main screen featuring:
- Top-right chevron button for navigation
- Center empty state with bubble icon
- "Your conversations" title
- "There's nothing here yet." subtitle
- "Chat with Innovexia" CTA button
- Bottom avatar + settings bar

### ChatComposer (Reusable)

Modular text field component with:
```kotlin
@Composable
fun ChatComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Message Innovexia…",
    trailingActions: @Composable RowScope.() -> Unit = {}
)
```

**Features:**
- Rounded 24dp container
- Min 1 line, grows to 4 lines
- Send button enabled when text is non-blank
- Dark/light theme support
- Focus/disabled states

## 🎨 Customization

### Changing Colors

Edit `ui/theme/Color.kt`:
```kotlin
object LightColors {
    val PrimaryText = Color(0xFF111827)
    val AccentBlue = Color(0xFF3B82F6)
    // ...
}
```

### Changing Fonts

Edit `ui/theme/Type.kt`:
```kotlin
val InterFontFamily = FontFamily(
    Font(R.font.inter_font, FontWeight.Normal)
)
```

### Changing Gradients

Edit `ui/theme/Theme.kt`:
```kotlin
Brush.verticalGradient(
    colors = listOf(
        LightColors.GradientTop,
        LightColors.GradientCenter,
        LightColors.GradientBottom
    )
)
```

## 📋 Build Configuration

### Version Catalog

Versions are managed in `gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
composeBom = "2024.12.01"
```

### Gradle Properties

Performance flags in `gradle.properties`:
```properties
kotlin.compiler.execution.strategy=in-process
org.gradle.configuration-cache=true
org.gradle.caching=true
android.nonTransitiveRClass=true
```

## 🔍 Accessibility

- 40dp minimum touch targets
- Content descriptions on all interactive elements
- Contrast-compliant colors in both themes
- Semantic component structure

## 📦 Resources

### Drawables
- `ic_bubble_outline.xml` - Chat bubble icon
- `ic_chevrons_right.xml` - Double chevron navigation

### Values
- `strings.xml` - All UI strings
- `dimens.xml` - Spacing and sizing values
- `font_certs.xml` - Google Fonts certificates

## 🧪 Testing

Build the project:
```bash
./gradlew assembleDebug
```

Run tests:
```bash
./gradlew test
```

## 📄 License

This project is created for demonstration purposes.

## 🤝 Contributing

This is a UI-only demonstration project. No backend or networking is implemented.

---

**Built with ❤️ using Jetpack Compose & Material 3**
