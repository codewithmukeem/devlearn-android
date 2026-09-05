# 🚀 DevLearn — Computer Science & Mobile Development Platform

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)
![API](https://img.shields.io/badge/Min_SDK-26+-orange.svg?style=for-the-badge)

**DevLearn** is an interactive, enterprise-grade Android learning application crafted with Jetpack Compose and modern Kotlin. Master C, C++, Java, and Native Android development with bite-sized lessons, interactive code sandboxes, and concept-reinforcing quizzes.

[📥 **Download Latest Release APK**](https://github.com/codewithmukeem/devlearn-android/releases/latest) • [Features](#-key-features) • [Tech Stack](#-tech-stack) • [Installation](#-installation)

</div>

---

## 📱 Download the Application (APK)

Get the latest production build directly on your Android phone:

👉 **[Download DevLearn APK (v1.0.0)](https://github.com/codewithmukeem/devlearn-android/releases/download/v1.0.0/devlearn-v1.0.0.apk)**

1. Download the `.apk` file above to your Android device.
2. Tap the file in your downloads to install.
3. If prompted, allow *"Install unknown apps"* for your browser or file manager.

---

## ✨ Key Features

### 🎓 4 Comprehensive Learning Tracks
- **C Programming (15 Lessons / 8 Chapters)**:
  - Compilation stages (`gcc`, `cpp`, `as`, `ld`), memory models, stack vs heap, pointer arithmetic, manual memory allocation (`malloc`, `free`), structs, and file I/O.
- **C++ Modern Standards (10 Lessons / 6 Chapters)**:
  - References, RAII (Resource Acquisition Is Initialization), smart pointers (`unique_ptr`, `shared_ptr`), class hierarchies, virtual tables, template programming, and STL containers.
- **Enterprise Java (8 Lessons / 5 Chapters)**:
  - JVM architecture, bytecode, class loaders, JIT compilation, String pool immutability, inheritance, dynamic polymorphism, and checked/unchecked exception hierarchies.
- **Android Studio & Compose (7 Lessons / 5 Chapters)**:
  - Kotlin null safety, data/sealed classes, Jetpack Compose declarative UI paradigms ($UI = f(State)$), State Hoisting, ViewModels, and Room Database persistence.

### 💻 Interactive Mobile Coding Sandbox
- Run code directly on-device with custom input, syntax highlighting, and instantaneous compilation simulation.
- Practice exercises attached to every lesson with hints, starter code, and verified solutions.

### 🧠 Knowledge Reinforcement Quizzes
- Multiple-choice assessments embedded at the end of each module.
- Immediate answer validation with conceptual explanations for both correct and incorrect options.

### 📊 Offline Persistence & Gamified Progress
- Built with **Room Database** for 100% offline capability.
- Track course completion percentages, active learning streaks, quiz scores, and achievements locally.

### 🎨 Material Design 3 UI
- Modern, edge-to-edge layout adhering to Google Material 3 guidelines.
- Dynamic color support, smooth animations, and high-contrast typography designed for readability.

---

## 🛠 Tech Stack

| Domain | Technology / Library |
|---|---|
| **Language** | Kotlin 2.0.0 |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Unidirectional Data Flow (UDF) |
| **Persistence** | Room SQLite Database (KSP) |
| **Concurrency** | Kotlin Coroutines & StateFlow |
| **Navigation** | Jetpack Navigation Compose (Type-safe routes) |
| **Icons & Design** | Material Symbols & Icons Extended |
| **Build System** | Gradle (Kotlin DSL `.gradle.kts`) |

---

## 📂 Project Architecture & Folder Structure

```
devlearn-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   └── courses/              # Curriculum JSON files
│   │   │   │       ├── c_course.json
│   │   │   │       ├── cpp_course.json
│   │   │   │       ├── java_course.json
│   │   │   │       └── android_course.json
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/                 # Room Database, DAOs, Entities, Models
│   │   │   │   ├── ui/                   # Jetpack Compose Screens & Components
│   │   │   │   │   ├── screens/          # CourseDetail, LessonView, Playground, Profile
│   │   │   │   │   └── theme/            # Material 3 Color Schemes & Typography
│   │   │   │   └── MainActivity.kt       # Single-activity root navigation
│   │   │   └── AndroidManifest.xml
│   │   └── res/                          # Vector drawables, launcher icons, strings
│   └── build.gradle.kts                  # App module dependencies
├── gradle/                               # Version catalogs & gradle wrapper
├── LICENSE                               # MIT License
├── CONTRIBUTING.md                       # Contribution guidelines
├── CHANGELOG.md                          # Version history
└── README.md                             # Project documentation
```

---

## 🚀 Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17 or JDK 21
- Android SDK API 35 (minSdk 26)

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/codewithmukeem/devlearn-android.git
   ```
2. Open the project in Android Studio.
3. Allow Gradle to synchronize dependencies.
4. Run on an Android device or emulator:
   ```bash
   ./gradlew installDebug
   ```

---

## 👨‍💻 Developer & Author

- **Author**: Mukeem Javaid ([@codewithmukeem](https://github.com/codewithmukeem))
- **Role**: Mobile Application Developer
- **GitHub**: [github.com/codewithmukeem](https://github.com/codewithmukeem)
- **Contact**: `ai.mukeem.company@gmail.com`

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
