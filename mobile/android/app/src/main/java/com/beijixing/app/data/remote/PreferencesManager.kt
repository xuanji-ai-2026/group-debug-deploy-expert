package com.beijixing.app.data.remote

import android.content.Context
import com.beijixing.app.BuildConfig
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import javax.inject.Inject

class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences by lazy {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    private val gson = Gson()

    companion object {
        const val TOKEN_KEY = "auth_token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val USER_ID_KEY = "user_id"
        const val USER_JSON_KEY = "user_json"

        private const val AES_ALGORITHM = "AES/CBC/PKCS7Padding"
        private const val ENCRYPTION_KEY = "BeijiXing2026Sec"

        private var cipher: Cipher? = null
        private var decryptCipher: Cipher? = null

        fun initializeCiphers() {
            try {
                val keySpec = SecretKeySpec(
                    ENCRYPTION_KEY.toByteArray(Charsets.UTF_8).copyOf(16),
                    "AES"
                )
                val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val ivSpec = IvParameterSpec(iv)

                cipher = Cipher.getInstance(AES_ALGORITHM).apply {
                    init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
                }

                decryptCipher = Cipher.getInstance(AES_ALGORITHM).apply {
                    init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.e("PreferencesManager", "Cipher init failed", e)
            }
        }
    }

    fun saveToken(token: String) {
        try {
            val encryptedToken = encryptData(token)
            sharedPreferences.edit()
                .putString(TOKEN_KEY, encryptedToken)
                .apply()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("PreferencesManager", "Error saving credential", e)
        }
    }

    fun getToken(): String? {
        return try {
            val encryptedToken = sharedPreferences.getString(TOKEN_KEY, null)
            if (!encryptedToken.isNullOrEmpty()) {
                decryptData(encryptedToken)
            } else null
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("PreferencesManager", "Error reading credential", e)
            null
        }
    }

    fun clearToken() {
        try {
            sharedPreferences.edit().remove(TOKEN_KEY).apply()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("PreferencesManager", "Error clearing credential", e)
        }
    }

    fun saveRefreshToken(refreshToken: String) {
        try {
            val encryptedToken = encryptData(refreshToken)
            sharedPreferences.edit()
                .putString(REFRESH_TOKEN_KEY, encryptedToken)
                .apply()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("PreferencesManager", "Error saving credential", e)
        }
    }

    fun getRefreshToken(): String? {
        return try {
            val encryptedToken = sharedPreferences.getString(REFRESH_TOKEN_KEY, null)
            if (!encryptedToken.isNullOrEmpty()) {
                decryptData(encryptedToken)
            } else null
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("PreferencesManager", "Error reading credential", e)
            null
        }
    }

    fun clearRefreshToken() {
        try {
            sharedPreferences.edit().remove(REFRESH_TOKEN_KEY).apply()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("PreferencesManager", "Error clearing credential", e)
        }
    }

    fun getContext(): Context = context

    fun clearAll() {
        try {
            kotlinx.coroutines.runBlocking { clearToken() }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("PreferencesManager", "Error in clearAll", e)
        }
    }

    private fun encryptData(data: String): String {
        return if (cipher != null) {
            android.util.Base64.encodeToString(cipher!!.doFinal(data.toByteArray(Charsets.UTF_8)), android.util.Base64.DEFAULT)
        } else data
    }

    private fun decryptData(encryptedData: String): String {
        return if (decryptCipher != null) {
            String(decryptCipher!!.doFinal(android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)), Charsets.UTF_8)
        } else encryptedData
    }

    init {
        initializeCiphers()
    }
}
