---
description: Repository Information Overview
alwaysApply: true
---

# Cattle Manager App Information

## Summary
A comprehensive Android application for managing cattle records, built with Jetpack Compose and Room database. The app allows tracking of cattle, pastures, and activities with a modern Material 3 UI.

## Structure
- **app/**: Main application module
  - **src/main/java/com/jumblemint/cows/**: Source code
    - **data/**: Data layer (models, DAOs, database)
    - **ui/**: UI components and screens
    - **navigation/**: Navigation configuration
  - **src/androidTest/**: Instrumentation tests
  - **src/test/**: Unit tests

## Language & Runtime
**Language**: Kotlin
**Version**: 2.0.21
**Build System**: Gradle (Kotlin DSL)
**Package Manager**: Gradle
**JVM Target**: Java 11
**Android SDK**: 
- **Compile SDK**: 36
- **Target SDK**: 36
- **Min SDK**: 30

## Dependencies
**Main Dependencies**:
- Jetpack Compose (2024.09.00)
- Room Database (2.6.1)
- Navigation Compose (2.8.5)
- Lifecycle ViewModel (2.9.2)
- Material 3
- Kotlin Coroutines

**Development Dependencies**:
- JUnit (4.13.2)
- Espresso (3.7.0)
- Compose UI Testing

## Build & Installation
```bash
./gradlew assembleDebug
```
Installation:
```bash
./gradlew installDebug
```

## Testing
**Frameworks**: 
- JUnit for unit tests
- AndroidX Test for instrumentation tests
- Espresso for UI testing

**Test Location**: 
- Unit tests: `app/src/test/`
- Instrumentation tests: `app/src/androidTest/`

**Run Command**:
```bash
./gradlew test           # Unit tests
./gradlew connectedCheck # Instrumentation tests
```

## Database Schema
**Database Engine**: Room (SQLite)
**Version**: 2
**Entities**:
- Cows: Animal records with tag numbers, birth dates, gender, etc.
- Pastures: Land management with acreage and assigned animals
- Activities: Events like moves, births, sales with dates and notes
- Settings: Application configuration storage

**Features**:
- Type converters for complex data types
- Migration support
- Foreign key relationships
- DAO pattern for data access