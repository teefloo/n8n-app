package com.n8n.mobilemanager.data.local

/**
 * Encrypts n8n API keys at rest with a dedicated Android Keystore key.
 */
internal object ApiKeyCipher {
    private const val KEY_ALIAS = "n8n_api_keys_key"

    fun isEncrypted(value: String): Boolean = KeystoreCipher.isEncrypted(value)

    fun encrypt(value: String): String = KeystoreCipher.encrypt(value, KEY_ALIAS)

    fun decrypt(value: String): String? = KeystoreCipher.decrypt(value, KEY_ALIAS)
}
