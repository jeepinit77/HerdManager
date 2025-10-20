package com.jumblemint.cows.data.model

enum class AnimalIdentifierMode {
    NAMES,
    TAG_NUMBERS,
    BOTH;

    companion object {
        fun fromValue(value: String?): AnimalIdentifierMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BOTH
        }
    }
}
