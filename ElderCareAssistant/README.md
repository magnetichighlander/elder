# Elder Care Assistant

An Android application designed with elderly-friendly features including large buttons, high contrast colors, and accessible design patterns.

## Project Configuration

- **Project Name**: ElderCareAssistant
- **Package Name**: com.eldercare.assistant
- **Minimum SDK**: API 21 (Android 5.0 Lollipop)
- **Target SDK**: API 34 (Android 14)
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL

## Elderly-Friendly Features

### Design Features
- Large touch targets (minimum 80dp height for buttons)
- High contrast color scheme (black text on white background)
- Large font sizes (28sp for headings, 24sp for buttons)
- Simple, uncluttered layout
- Clear visual hierarchy

### Accessibility Features
- Content descriptions for screen readers
- Keep screen on to prevent accidental locks
- Optimized brightness for better visibility
- Material Design 3 components for better accessibility

### Technical Features
- View binding enabled for type-safe view references
- Navigation components for consistent navigation
- Lifecycle-aware components
- Support for RTL languages

## How to Build and Run

### Prerequisites
1. Install Android Studio (latest version recommended)
2. Install Java Development Kit (JDK) 8 or higher
3. Ensure Android SDK is installed through Android Studio

### Opening the Project
1. Open Android Studio
2. Click "Open an existing Android Studio project"
3. Navigate to the `ElderCareAssistant` folder and select it
4. Wait for Gradle sync to complete

### Building the Project
To build the project from command line:

```bash
# On Windows (using the project's gradlew.bat)
cd ElderCareAssistant
.\gradlew.bat build

# Alternative: Using Android Studio
# File -> Build -> Make Project (Ctrl+F9)
```

### Running the Project
1. Connect an Android device or start an emulator
2. Click the "Run" button in Android Studio (green play button)
3. Or use command line: `.\gradlew.bat installDebug`

## Project Structure

```
ElderCareAssistant/
├── app/
│   ├── src/main/
│   │   ├── java/com/eldercare/assistant/
│   │   │   ├── ElderCareApplication.kt
│   │   │   ├── di/ (Dependency Injection)
│   │   │   │   └── AppModule.kt
│   │   │   ├── ui/
│   │   │   │   ├── main/ (MainActivity)
│   │   │   │   │   └── MainActivity.kt
│   │   │   │   └── theme/
│   │   │   │       └── Theme.kt
│   │   │   └── utils/
│   │   │       ├── AccessibilityUtils.kt
│   │   │       └── Constants.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   ├── themes.xml
│   │   │   │   └── dimens.xml
│   │   │   ├── mipmap/ (app icons)
│   │   │   ├── font/ (custom fonts)
│   │   │   │   └── elderly_friendly_fonts.xml
│   │   │   └── xml/
│   │   │       ├── backup_rules.xml
│   │   │       └── data_extraction_rules.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew.bat
└── README.md
```

## Future Enhancements

- Voice command integration
- Emergency contact features
- Medication reminders
- Simple communication tools
- Health monitoring widgets

## Notes

- The project is configured with minimum API 21 to support older Android devices commonly used by elderly users
- All UI components follow accessibility guidelines
- The app uses Material Design 3 for consistent and accessible user experience
- Icon assets need to be added to the mipmap directories for the app icon to display properly

## Troubleshooting

If you encounter build issues:
1. Make sure Android Studio is up to date
2. Clean and rebuild the project (Build -> Clean Project, then Build -> Rebuild Project)
3. Sync Gradle files (File -> Sync Project with Gradle Files)
4. Check that all dependencies are properly downloaded
