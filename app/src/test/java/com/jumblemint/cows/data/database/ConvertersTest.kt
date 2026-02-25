package com.jumblemint.cows.data.database

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun toStringList_parsesLegacyCommaSeparatedValues() {
        val result = converters.toStringList("one,two,three")

        assertEquals(listOf("one", "two", "three"), result)
    }

    @Test
    fun stringList_roundTripsUriContainingComma() {
        val original = listOf(
            "content://media/external/images/media/12345?display_name=IMG_2026-02-24,09-15-00.jpg"
        )

        val encoded = converters.fromStringList(original)
        val decoded = converters.toStringList(encoded)

        assertEquals(original, decoded)
    }
}
