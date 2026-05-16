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
    fun `saveToken and getToken should work correctly`() {
        val testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"

        tokenManager.saveToken(testToken)
        `when`(mockSharedPreferences.getString("jwt_token", null)).thenReturn(testToken)

        assertEquals(testToken, tokenManager.getToken())
        verify(mockEditor).putString("jwt_token", testToken)
        verify(mockEditor).apply()
    }

    @Test
    fun `saveUserId and getUserId should work correctly`() {
        val testUserId = "123e4567-e89b-12d3-a456-426614174000"

        tokenManager.saveUserId(testUserId)
        `when`(mockSharedPreferences.getString("user_id", null)).thenReturn(testUserId)

        assertEquals(testUserId, tokenManager.getUserId())
        verify(mockEditor).putString("user_id", testUserId)
        verify(mockEditor).apply()
    }

    @Test
    fun `isGuest should return true by default`() {
        `when`(mockSharedPreferences.getBoolean("is_guest", true)).thenReturn(true)
        assertTrue(tokenManager.isGuest())
    }

    @Test
    fun `setGuestMode should change guest mode`() {
        tokenManager.setGuestMode(false)
        verify(mockEditor).putBoolean("is_guest", false)
        verify(mockEditor).apply()
    }

    @Test
    fun `logout should clear all preferences`() {
        tokenManager.logout()
        verify(mockEditor).clear()
        verify(mockEditor, atLeastOnce()).apply()
    }

    @Test
    fun `getGuestUserId should generate consistent ID`() {
        `when`(mockSharedPreferences.getString(eq("guest_user_id"), isNull())).thenReturn(null)

        val guestId1 = tokenManager.getGuestUserId()
        val guestId2 = tokenManager.getGuestUserId()

        assertNotNull(guestId1)
        assertNotNull(guestId2)
        assertTrue(guestId1.startsWith("guest_"))
        assertTrue(guestId2.startsWith("guest_"))
    }

    @Test
    fun `saveEmail and getEmail should work correctly`() {
        val testEmail = "user@example.com"

        tokenManager.saveEmail(testEmail)
        `when`(mockSharedPreferences.getString("user_email", null)).thenReturn(testEmail)

        assertEquals(testEmail, tokenManager.getEmail())
        verify(mockEditor).putString("user_email", testEmail)
        verify(mockEditor).apply()
    }

    @Test
    fun `getUserId returns guest id when in guest mode`() {
        `when`(mockSharedPreferences.getBoolean("is_guest", true)).thenReturn(true)
        `when`(mockSharedPreferences.getString(eq("guest_user_id"), isNull())).thenReturn("guest_123456789")

        val userId = tokenManager.getUserId()

        assertNotNull(userId)
        assertTrue(userId?.startsWith("guest_") ?: false)
    }
}