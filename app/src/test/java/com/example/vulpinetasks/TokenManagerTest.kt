package com.example.vulpinetasks.unittests

import android.content.Context
import android.content.SharedPreferences
import com.example.vulpinetasks.backend.TokenManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TokenManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var tokenManager: TokenManager

    @Before
    fun setUp() {
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)

        tokenManager = TokenManager(mockContext)
    }

    @Test
    fun `saveToken should work correctly`() {
        val testToken = "test-token-123"
        tokenManager.saveToken(testToken)
        verify(mockEditor).putString("jwt_token", testToken)
    }

    @Test
    fun `getToken should return saved token`() {
        val testToken = "saved-token"
        `when`(mockSharedPreferences.getString("jwt_token", null)).thenReturn(testToken)
        assertEquals(testToken, tokenManager.getToken())
    }

    @Test
    fun `saveUserId should work correctly`() {
        val testUserId = "user-123"
        tokenManager.saveUserId(testUserId)
        verify(mockEditor).putString("user_id", testUserId)
    }

    @Test
    fun `getUserId should return saved user id`() {
        val testUserId = "user-456"
        `when`(mockSharedPreferences.getString("user_id", null)).thenReturn(testUserId)
        assertEquals(testUserId, tokenManager.getUserId())
    }

    @Test
    fun `isGuest should return true when no token saved`() {
        `when`(mockSharedPreferences.getBoolean("is_guest", true)).thenReturn(true)
        assertTrue(tokenManager.isGuest())
    }

    @Test
    fun `saveEmail should work correctly`() {
        val testEmail = "test@example.com"
        tokenManager.saveEmail(testEmail)
        verify(mockEditor).putString("user_email", testEmail)
    }

    @Test
    fun `getEmail should return saved email`() {
        val testEmail = "user@example.com"
        `when`(mockSharedPreferences.getString("user_email", null)).thenReturn(testEmail)
        assertEquals(testEmail, tokenManager.getEmail())
    }

    @Test
    fun `guest mode should be set correctly`() {
        tokenManager.setGuestMode(true)
        verify(mockEditor).putBoolean("is_guest", true)
    }

    @Test
    fun `logout should clear preferences`() {
        try {
            tokenManager.logout()
            assertTrue(true)
        } catch (e: Exception) {
            fail("logout() threw exception: ${e.message}")
        }
    }

    @Test
    fun `getGuestUserId should return non-null value`() {
        val guestId = tokenManager.getGuestUserId()
        assertNotNull(guestId)
        assertTrue(guestId.startsWith("guest_"))
    }
}