# Contributing to DevLearn

Thank you for your interest in contributing to DevLearn Android!

## Code of Conduct
We expect all contributors to maintain a respectful, welcoming, and harassment-free environment.

## How Can I Contribute?

### Reporting Bugs
- Search existing issues to verify the bug hasn't already been reported.
- Open a new issue with detailed reproduction steps, device specifications (OS version, screen size), and relevant crash logs.

### Submitting Pull Requests
1. Fork the repository on GitHub.
2. Create a new topic branch:
   ```bash
   git checkout -b feature/awesome-new-module
   ```
3. Commit your changes with clear, conventional messages:
   ```bash
   git commit -m "feat(course): add interactive playground for Kotlin Coroutines"
   ```
4. Push to your fork:
   ```bash
   git push origin feature/awesome-new-module
   ```
5. Open a Pull Request pointing to the `main` branch.

## Architecture & Code Standards
- **UI Framework**: Jetpack Compose adhering to Material Design 3 guidelines.
- **Languages**: 100% Kotlin with Coroutines and StateFlow.
- **Architecture**: MVVM with Unidirectional Data Flow (UDF).
- **Data Persistence**: Android Room database.
- **Code Style**: Android Kotlin official formatting guidelines.
