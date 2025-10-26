package com.jumblemint.cows.util

import com.jumblemint.cows.data.model.AnimalIdentifierMode

private fun String?.normalized(): String? = this?.takeIf { it.isNotBlank() }

fun AnimalIdentifierMode.usesNames(): Boolean = this != AnimalIdentifierMode.TAG_NUMBERS

fun AnimalIdentifierMode.usesTags(): Boolean = this != AnimalIdentifierMode.NAMES

fun AnimalIdentifierMode.isIdentifierSatisfied(name: String?, tag: String?): Boolean {
    val hasName = name.normalized() != null
    val hasTag = tag.normalized() != null
    return when (this) {
        AnimalIdentifierMode.NAMES -> hasName
        AnimalIdentifierMode.TAG_NUMBERS -> hasTag
        AnimalIdentifierMode.BOTH -> hasName || hasTag
    }
}

fun AnimalIdentifierMode.identifierRequirementMessage(): String = when (this) {
    AnimalIdentifierMode.NAMES -> "Please enter a Name."
    AnimalIdentifierMode.TAG_NUMBERS -> "Please enter a Tag Number."
    AnimalIdentifierMode.BOTH -> "Please enter a Name or a Tag Number."
}

fun AnimalIdentifierMode.primaryIdentifier(
    name: String?,
    tag: String?,
    fallback: String = "Unnamed Animal"
): String {
    val nameValue = name.normalized()
    val tagValue = tag.normalized()
    return when (this) {
        AnimalIdentifierMode.NAMES -> nameValue ?: fallback
        AnimalIdentifierMode.TAG_NUMBERS -> tagValue ?: fallback
        AnimalIdentifierMode.BOTH -> nameValue ?: tagValue ?: fallback
    }
}

fun AnimalIdentifierMode.secondaryIdentifier(name: String?, tag: String?): String? {
    val nameValue = name.normalized()
    val tagValue = tag.normalized()
    return when (this) {
        AnimalIdentifierMode.BOTH -> if (nameValue != null && tagValue != null && !nameValue.equals(tagValue, true)) {
            tagValue
        } else {
            null
        }

        else -> null
    }
}
