# 🚀 DevLearn — Computer Science & Mobile Development Platform

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)
![API](https://img.shields.io/badge/Min_SDK-26+-orange.svg?style=for-the-badge)

**DevLearn** is a comprehensive, production-grade Android programming learning platform built with Jetpack Compose and Kotlin. Master C, C++, Java, and Native Android development with deep architectural lessons, in-app PDF course handbooks, a sandboxed virtual coding lab, an intelligent pedagogical AI tutor, interactive quizzes, and verified course completion certificates.

[📥 **Download Latest Release APK**](https://github.com/codewithmukeem/devlearn-android/releases/latest) • [Features](#-key-features) • [Tech Stack](#-tech-stack) • [Installation](#-installation)

</div>

---

## 📱 Download the Application (APK)

Get the production build directly on your Android device:

👉 **[Download DevLearn APK (v1.0.0)](https://github.com/codewithmukeem/devlearn-android/releases/download/v1.0.0/devlearn-v1.0.0.apk)**

1. Download the `.apk` file above to your Android phone or tablet.
2. Tap the downloaded file in your notification bar or file manager to install.
3. If prompted, toggle *"Allow from this source"* for your browser or file manager.

---

## ✨ Key Features

### 🎓 4 In-Depth Curriculum Tracks
- **C Programming Track (8 Chapters / 15 Comprehensive Lessons)**:
  - 4-stage compilation pipeline (`cpp`, `gcc`, `as`, `ld`), memory models, stack vs. heap allocation, raw pointer arithmetic, manual memory management (`malloc`, `calloc`, `free`), pointers to functions, structs, unions, bitwise manipulation, and POSIX file streams.
- **Modern C++ Track (6 Chapters / 10 Comprehensive Lessons)**:
  - References vs. pointers, RAII (Resource Acquisition Is Initialization), smart pointers (`std::unique_ptr`, `std::shared_ptr`, `std::weak_ptr`), OOP inheritance, virtual method tables (vtable), move semantics, template metaprogramming, and standard STL containers.
- **Enterprise Java Track (5 Chapters / 8 Comprehensive Lessons)**:
  - JVM internal architecture, classloaders, bytecode execution, JIT compilation, String pool immutability, object-oriented design, dynamic polymorphism, interfaces vs. abstract classes, collections framework, and checked/unchecked exception handling.
- **Native Android & Compose Track (5 Chapters / 7 Comprehensive Lessons)**:
  - Kotlin idioms and null-safety, data and sealed classes, Jetpack Compose declarative UI architecture, State Hoisting, ViewModel integration, Coroutines/Flow, Room database local persistence, and Material 3 design systems.

### 📚 Dedicated In-App PDF & Ebook Companion Reader
- Native high-performance PDF renderer directly inside the application.
- Smooth page navigation, multi-level zoom (100% to 250%), and dark/night mode reading filter.
- Automatic page progress memory and quick bookmarking system.
- Accessible directly from each course syllabus and lesson header for deep reading.

### 💻 Virtual Coding Sandbox (C, C++, Java, Kotlin)
- Sandboxed local execution engine supporting syntax validation, loops, variables, standard I/O, and arithmetic evaluation.
- Pre-configured starter templates for rapid experimentation.
- Instant console output diagnostics and execution time metrics.
- Seamless one-tap integration with the AI Tutor to diagnose bugs or explore alternative algorithms.

### 🤖 Pedagogical AI Tutor (Powered by Gemini)
- Built with a true teacher persona: **Explain → Hint → Guide → Solution**.
- Helps students diagnose compiler errors, understand runtime behavior, and unpack complex concepts without spoiling answers prematurely.
- Context-aware: automatically receives the current course, lesson topic, active code snippet, and compiler diagnostics.
- Graceful offline fallback with structured pedagogical guidance.

### 🧠 Knowledge Reinforcement Quizzes & Practice Exercises
- Formative conceptual quizzes at the end of each module.
- Instant validation with clear explanations for all multiple-choice options.
- Hands-on coding exercises with starter code, progressive hints, and verified solutions.

### 🏆 Progress Tracking & Verified Certificates
- 100% offline persistence using **Room Database**.
- Tracks lesson completion percentage, quiz scores, and learning streaks.
- Generates official, cryptographic-hash-verified Certificates of Achievement upon completing each course.

### 🎨 Material Design 3 UI & Responsive Layout
- High-contrast, classic slate-and-navy palette designed for comfortable long-form reading.
- Fully edge-to-edge layout with dynamic WindowInsets handling.
- Optimized for handheld phones, foldables, and tablets.

---

## 🛠 Tech Stack

| Domain | Technology / Library | Description |
|---|---|---|
| **Language** | Kotlin 2.0.0 | Type-safe modern programming language |
| **UI Framework** | Jetpack Compose (M3) | Declarative UI toolkit with Material Design 3 |
| **Architecture** | MVVM + Clean Architecture | Unidirectional Data Flow with Repository pattern |
| **Persistence** | Android Room Database (KSP) | Local SQLite persistence for progress and certificates |
| **PDF Rendering** | Android Native `PdfRenderer` | High-performance bitmap page rendering |
| **AI Integration** | Google Gemini API (2.5 Flash) | Context-aware AI programming tutor |
| **Networking** | OkHttp3 | Lightweight, secure HTTP client for Gemini |
| **Concurrency** | Kotlin Coroutines & Flow | Non-blocking reactive programming |
| **Navigation** | Navigation Compose | Type-safe screen routing and deep linking |
| **Build System** | Gradle (Kotlin DSL) | Modern Android Gradle Plugin build pipeline |

---

## 📂 Project Architecture & Folder Structure

```
devlearn-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   └── courses/              # Curriculum JSON files
│   │   │   │       ├── c_course.json     # 8 Chapters / 15 Lessons
│   │   │   │       ├── cpp_course.json   # 6 Chapters / 10 Lessons
│   │   │   │       ├── java_course.json  # 5 Chapters / 8 Lessons
│   │   │   │       └── android_course.json # 5 Chapters / 7 Lessons
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/                 # Data Layer
│   │   │   │   │   ├── local/            # Room Database, DAOs & Entities
│   │   │   │   │   ├── model/            # Course, Chapter, Lesson, Quiz models
│   │   │   │   │   ├── pdf/              # PDF Document Generator & Reading Manager
│   │   │   │   │   ├── lab/              # Sandboxed Code Execution Engine
│   │   │   │   │   └── repository/       # Course, Progress & AI Tutor Repositories
│   │   │   │   ├── ui/                   # Presentation Layer
│   │   │   │   │   ├── components/       # Reusable M3 widgets, editors, dialogs
│   │   │   │   │   ├── navigation/       # Screen routes & DevLearnNavGraph
│   │   │   │   │   ├── screens/          # Home, Courses, Lesson, Lab, AI Tutor, PDF, Progress
│   │   │   │   │   └── theme/            # Theme, Color, Type, Shape
│   │   │   │   └── MainActivity.kt       # Single-activity edge-to-edge host
│   │   │   └── AndroidManifest.xml
│   │   └── res/                          # Vector drawables, adaptive launcher icons
│   └── build.gradle.kts                  # App-level dependencies & plugins
├── gradle/                               # Version catalogs & gradle wrapper
├── LICENSE                               # MIT License
├── CONTRIBUTING.md                       # Contribution guidelines
├── CHANGELOG.md                          # Release history
└── README.md                             # Project documentation
```

---

## 🚀 Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17 or JDK 21
- Android SDK API 35 (minimum SDK 26)

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/codewithmukeem/devlearn-android.git
   ```
2. Open the project in Android Studio.
3. Allow Gradle to synchronize dependencies.
4. Run the debug build on an Android device or emulator:
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
