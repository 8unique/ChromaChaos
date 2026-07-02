# Chroma Chaos

Chroma Chaos is a fast-paced color-based arcade game developed for
Android using Kotlin and modern Android development practices. The
project focuses on responsive touch interaction, scalable architecture,
performance optimization, and modular game design.

This repository contains the complete Android project including game
logic, UI layers, audio management, and build configuration.

------------------------------------------------------------------------

## Overview

Chroma Chaos delivers reactive gameplay centered around color-based
mechanics, combo multipliers, and progressive difficulty scaling. The
game is designed for short, engaging play sessions while maintaining
smooth rendering performance and clean architecture.

Target Platform: Android\
Language: Kotlin\
Build System: Gradle (Kotlin DSL)

------------------------------------------------------------------------

## Core Gameplay

-   Color-based interaction mechanics
-   Combo multiplier scoring system
-   Dynamic difficulty progression
-   Hazard and power-up system
-   Real-time score tracking
-   Offline-first gameplay

The core loop revolves around player interaction with dynamically
spawned colored objects. Consecutive successful actions increase score
multipliers, while hazards introduce risk and challenge.

------------------------------------------------------------------------

## Controls

-   Tap --- Primary interaction\
-   Swipe --- Directional actions\
-   Drag --- Movement interactions (if applicable)

Touch input is optimized for low latency and smooth responsiveness.

------------------------------------------------------------------------

## Technical Architecture

The project follows a modular and maintainable structure separating
responsibilities into clear layers.

Architecture Highlights:

-   Modular separation of UI, game logic, audio, and utilities
-   Custom update/render loop
-   Deterministic spawn logic for reproducible testing
-   Object pooling to minimize runtime allocations
-   60 FPS performance target
-   Structured testing strategy

------------------------------------------------------------------------

## Project Structure

    .
    ├── app
    │   ├── build.gradle.kts
    │   ├── src
    │   │   └── main
    │   │       ├── java
    │   │       │   └── com
    │   │       │       └── chromachaos
    │   │       │           └── game
    │   │       │               ├── ui
    │   │       │               │   └── screens
    │   │       │               │       └── GameScreen.kt
    │   │       │               ├── game
    │   │       │               ├── audio
    │   │       │               └── util
    │   │       └── res
    │   │           ├── drawable
    │   │           ├── layout
    │   │           ├── raw
    │   │           └── values
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── gradle
        └── libs.versions.toml

Key file:

app/src/main/java/com/ChromaChaos/game/ui/screens/GameScreen.kt

------------------------------------------------------------------------

## Performance Considerations

-   Object pooling for frequently spawned entities
-   Reduced per-frame allocations
-   Efficient bitmap and asset handling
-   Optimized particle rendering
-   SoundPool for low-latency sound effects
-   MediaPlayer for background music

Performance profiling is performed using Android Studio Profiler and
memory analysis tools.

------------------------------------------------------------------------

## Persistence

Player data includes:

-   High scores
-   Game settings
-   Audio preferences

Storage is handled using Android persistent storage mechanisms such as
SharedPreferences or DataStore.

------------------------------------------------------------------------

## Build and Run

### Prerequisites

-   JDK 11 or higher
-   Android Studio (latest stable)
-   Android SDK matching compileSdk version

### Build (Windows)

    .\gradlew.bat clean assembleDebug

### Install on connected device

    .\gradlew.bat installDebug

Run directly from Android Studio on emulator or physical device.

------------------------------------------------------------------------

## Testing

Run unit tests:

    .\gradlew.bat test

Run instrumented Android tests:

    .\gradlew.bat connectedAndroidTest

Recommended tools:

-   Android Studio Profiler
-   LeakCanary (debug builds)
-   Frame-time performance monitoring

------------------------------------------------------------------------

## Security and Privacy

-   Minimal required permissions
-   HTTPS communication for network features
-   No sensitive user data stored locally
-   Analytics designed to follow privacy compliance standards

------------------------------------------------------------------------

## Development Status

Active development.

Core gameplay is functional. Performance optimization, accessibility
features, analytics integration, and additional modes are planned.

------------------------------------------------------------------------

## Roadmap

Short-Term: - Add accessibility features (color-blind mode) - Optimize
particle system - Expand automated testing coverage

Mid-Term: - Online leaderboards - Achievements system - Cosmetic unlocks

Long-Term: - Cross-platform expansion - Additional game modes - Seasonal
content updates

------------------------------------------------------------------------

## Contributing

1.  Fork the repository\
2.  Create a feature branch: feature/short-description\
3.  Run tests and ensure lint checks pass\
4.  Submit a pull request with a clear description

Coding standards:

-   Idiomatic Kotlin
-   Avoid heavy allocations in render loop
-   Use coroutines for background operations
-   Maintain modular separation of concerns

------------------------------------------------------------------------

## License

This project is licensed under the MIT License.\
See the  [LICENSE](LICENSE)  file for details.

------------------------------------------------------------------------

## Contact

Developer: 8unique\
Email: game.ChromaChaos@gmail.com
