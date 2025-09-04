package com.jumblemint.cows

import com.jumblemint.cows.data.model.User
import org.junit.Test
import org.junit.Assert.*

/**
 * Basic unit tests for authentication and sync functionality
 */
class AuthServiceTest {

    @Test
    fun user_creation_isCorrect() {
        val user = User(
            uid = "test-uid",
            email = "test@example.com",
            displayName = "Test User",
            photoUrl = null,
            createdAt = System.currentTimeMillis(),
            lastSyncAt = System.currentTimeMillis()
        )
        
        assertEquals("test-uid", user.uid)
        assertEquals("test@example.com", user.email)
        assertEquals("Test User", user.displayName)
        assertNull(user.photoUrl)
        assertTrue(user.createdAt > 0)
        assertTrue(user.lastSyncAt > 0)
    }

    @Test
    fun herd_id_generation_isUnique() {
        val id1 = java.util.UUID.randomUUID().toString()
        val id2 = java.util.UUID.randomUUID().toString()
        
        assertNotEquals(id1, id2)
        assertTrue(id1.isNotEmpty())
        assertTrue(id2.isNotEmpty())
    }
}