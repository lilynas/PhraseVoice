package com.phrasevoice.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyCipher {
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + cipherText
        return "$VERSION_PREFIX${Base64.getEncoder().encodeToString(payload)}"
    }

    fun decrypt(value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (!value.startsWith(VERSION_PREFIX)) return null

        return runCatching {
            val payload = Base64.getDecoder().decode(value.removePrefix(VERSION_PREFIX))
            val iv = payload.copyOfRange(0, GCM_IV_SIZE_BYTES)
            val cipherText = payload.copyOfRange(GCM_IV_SIZE_BYTES, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
            cipher.doFinal(cipherText).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "phrasevoice_provider_api_keys"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val VERSION_PREFIX = "v1:"
        private const val GCM_IV_SIZE_BYTES = 12
        private const val GCM_TAG_SIZE_BITS = 128
    }
}
