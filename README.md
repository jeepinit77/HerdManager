# Cattle Manager App

A comprehensive Android application for managing cattle records, built with Jetpack Compose and Room database.

## Features Implemented

### Core Animal Records ✅
- Cow Name (optional, free text)
- Tag Number (numeric; not required to be unique)
- Tag Color (select from configurable list)
- Birth Date (calendar picker; used for age calculation)
- Gender (male / female / TBD)
- Classification (dynamic; examples: calf, bull, heifer, cow, steer)
- Color/Markings (free text description)
- Mother/Father selection (from active cows)
- Related Cows (auto-generated lists of siblings by mother and father)
- Status (active / sold / deceased)
- Location (pasture assignment)

### Pasture Records ✅
- Pasture Name
- Acreage (numeric field)
- List of Assigned Cows (linked dynamically)
- Calves automatically assigned to system-defined Calf Pasture at birth
- Moving calves does not affect mother's pasture

### Activities / Events ✅
- Date of Activity
- Activity Type (configurable list: moved, weaned, sold, deceased, worked, castrated, birth, other)
- Notes / Description (optional, required for "worked" and "other")
- Moved: updates cow's pasture assignment
- Castrated: automatically updates classification from Bull → Steer
- Birth Event: creates new calf record, links to mother/father, assigns to Calf Pasture
- Bulk Activity: create single activity and apply to multiple cows simultaneously

### Reports & Smart Lists ✅
- Cows that haven't calved in last 9 months
- Herd breakdown: active vs sold vs deceased
- Grouped by pasture
- Grouped by classification (cows, bulls, heifers, steers, calves)
- Age-based listings (under 1 year, 1-5 years, 5-10 years, over 10 years)

### Settings & Customization ✅
- Manage list of available tag colors
- Manage activity types (add/edit/remove)
- Define default Calf Pasture
- Data export options (CSV/JSON - framework ready)

### Database & Architecture ✅
- Room database with offline-first approach
- MVVM architecture with ViewModels
- Repository pattern for data access
- Type converters for complex data types
- Comprehensive DAO methods for all operations

## Technology Stack

- **UI**: Jetpack Compose with Material 3
- **Database**: Room (SQLite)
- **Architecture**: MVVM with Repository pattern
- **Navigation**: Navigation Compose
- **Language**: Kotlin

## Project Structure

```
app/src/main/java/com/jumblemint/cows/
├── data/
│   ├── model/          # Data models (Cow, Pasture, Activity, Settings)
│   ├── dao/            # Data Access Objects
│   ├── database/       # Room database setup
│   └── repository/     # Repository layer
├── ui/
│   ├── screens/        # Compose screens
│   ├── components/     # Reusable UI components
│   └── viewmodel/      # ViewModels
├── navigation/         # Navigation setup
└── MainActivity.kt     # Main activity
```

## Getting Started

1. Open the project in Android Studio
2. Sync the project to download dependencies
3. Run the app on an emulator or device

## Features To Be Implemented

### Phase 2 - Enhanced Features
- [ ] Photo upload and gallery per cow
- [ ] Auto age calculation and age-based classification updates
- [ ] Weaning workflow with automatic classification updates
- [ ] Enhanced search and filtering options

### Phase 3 - Multi-User & Sync Features
- [ ] Multi-user accounts (each user has their own farm account)
- [ ] Local offline-first database per user
- [ ] Cloud backend for multi-user farm collaboration
- [ ] Google Drive sync for personal use/backup
- [ ] Complete data export/import implementation (CSV/JSON)

### Phase 4 - Advanced Features
- [ ] Notifications (calving reminders, overdue weaning)
- [ ] QR code or NFC tag scanning for quick cow lookup
- [ ] Multiple calf pastures (e.g., Heifer Calves vs Bull Calves)
- [ ] Advanced reporting and analytics
- [ ] Breeding management and tracking

### Phase 5 - Future Enhancements
- [ ] Mobile app optimization
- [ ] Tablet-specific layouts
- [ ] Integration with livestock management systems
- [ ] Weather and feed tracking
- [ ] Veterinary records integration

## Database Schema

### Cows Table
- id, name, tagNumber, tagColor, birthDate
- gender, classification, colorMarkings
- motherId, fatherId, status, pastureId
- photos, createdAt, updatedAt

### Pastures Table
- id, name, acreage, isCalfPasture
- createdAt, updatedAt

### Activities Table
- id, cowId, date, activityType
- notes, fromPastureId, toPastureId, createdAt

### Settings Table
- key, value (for configurable options)

## Contributing

This is a personal cattle management project. Feel free to fork and adapt for your own use.