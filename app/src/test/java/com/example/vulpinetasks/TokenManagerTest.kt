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
        `when`(mockEditor.apply()).thenReturn(Unit)

        tokenManager = TokenManager(mockContext)
    }

    @Test
    fun `saveToken and getToken should work correctly`() {
        val testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"

        tokenManager.saveToken(testToken)
        `when`(mockSharedPreferences.getString("jwt_token", null)).thenReturn(testToken)

        assertEquals(testToken, tokenManager.getToken())
    }

    @Test
    fun `saveUserId and getUserId should work correctly`() {
        val testUserId = "123e4567-e89b-12d3-a456-426614174000"

        tokenManager.saveUserId(testUserId)
        `when`(mockSharedPreferences.getString("user_id", null)).thenReturn(testUserId)

        assertEquals(testUserId, tokenManager.getUserId())
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
    }

    @Test
    fun `logout should clear all preferences`() {
        tokenManager.logout()
        verify(mockEditor).clear()
        verify(mockEditor).apply()
    }

    @Test
    fun `getGuestUserId should generate consistent ID`() {
        val guestId1 = tokenManager.getGuestUserId()
        val guestId2 = tokenManager.getGuestUserId()
        assertEquals(guestId1, guestId2)
    }

    @Test
    fun `saveEmail and getEmail should work correctly`() {
        val testEmail = "user@example.com"

        tokenManager.saveEmail(testEmail)
        `when`(mockSharedPreferences.getString("user_email", null)).thenReturn(testEmail)

        assertEquals(testEmail, tokenManager.getEmail())
    }
}