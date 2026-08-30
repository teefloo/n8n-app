package com.n8n.mobilemanager.data.local

/**
 * Encrypts credentials at rest with an AES key held by Android Keystore.
 * The key never leaves the device's keystore and is not backed up with app data.
 */
internal object CredentialCipher {
    private const val KEY_ALIAS = "n8n_credentials_key"

    fun isEncrypted(value: String): Boolean = KeystoreCipher.isEncrypted(value)

    fun encrypt(value: String): String = KeystoreCipher.encrypt(value, KEY_ALIAS)

    fun decrypt(value: String): String? = KeystoreCipher.decrypt(value, KEY_ALIAS)
}
