package com.jumblemint.cows.data.model

enum class ActivityType(val displayName: String) {
    MOVED("Moved"),
    WEANED("Weaned"),
    SOLD("Sold"),
    DECEASED("Deceased"),
    WORKED("Worked"),
    CASTRATED("Castrated"),
    BIRTH("Birth"),
    OTHER("Other")
}