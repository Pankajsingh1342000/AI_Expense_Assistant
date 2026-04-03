package com.epic.aiexpensevoice.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.epic.aiexpensevoice.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "ai_expense_voice_session")

data class UserSession(
    val accessToken: String? = null,
    val tokenType: String = "bearer",
    val email: String? = null,
    val name: String? = null,
    val baseUrlOverride: String? = null,
) {
    val isLoggedIn: Boolean = !accessToken.isNullOrBlank()
    val resolvedBaseUrl: String = baseUrlOverride?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_BASE_URL
}

class SessionManager(private val context: Context) {
    private object Keys {
        val AccessToken = stringPreferencesKey("access_token")
        val TokenType = stringPreferencesKey("token_type")
        val Email = stringPreferencesKey("email")
        val Name = stringPreferencesKey("name")
        val BaseUrl = stringPreferencesKey("base_url")
    }

    val session: Flow<UserSession> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            UserSession(
                accessToken = preferences[Keys.AccessToken],
                tokenType = preferences[Keys.TokenType] ?: "bearer",
                email = preferences[Keys.Email],
                name = preferences[Keys.Name],
                baseUrlOverride = preferences[Keys.BaseUrl],
            )
        }

    suspend fun saveAuth(accessToken: String, tokenType: String, email: String, name: String?) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AccessToken] = accessToken
            preferences[Keys.TokenType] = tokenType
            preferences[Keys.Email] = email
            preferences[Keys.Name] = name ?: email.substringBefore("@")
        }
    }

    suspend fun updateBaseUrl(baseUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.BaseUrl] = baseUrl.trim()
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
