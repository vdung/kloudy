package vdung.android.kloudy.data.user

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.*
import java.security.spec.AlgorithmParameterSpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal


class KeyStoreWrapper(private val context: Context) {
    private val ALIAS = "KLOUDY_USER"

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    private val keyGenerator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
    private val spec: AlgorithmParameterSpec
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                val start = Calendar.getInstance()
                val end = Calendar.getInstance().apply {
                    add(Calendar.YEAR, 10)
                }
                return KeyPairGeneratorSpec.Builder(context)
                        .setAlias(ALIAS).setSubject(X500Principal("CN=$ALIAS"))
                        .setSerialNumber(BigInteger.valueOf(42))
                        .setStartDate(start.time)
                        .setEndDate(end.time)
                        .build()
            } else {
                return KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setUserAuthenticationRequired(false)
                        .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                        .build()
            }
        }

    private val cipher: Cipher get() = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    private val publicKey: PublicKey get() = keyStore.getCertificate(ALIAS).publicKey
    private val privateKey: PrivateKey get() = keyStore.getKey(ALIAS, null) as PrivateKey

    private fun prepareKeyStore() {
        val key = keyStore.getKey(ALIAS, null)
        val certificate = keyStore.getCertificate(ALIAS)
        if (key != null && certificate != null) {
            return
        }

        keyGenerator.run {
            initialize(spec)
            generateKeyPair()
        }
    }

    fun encrypt(value: ByteArray): ByteArray {
        prepareKeyStore()

        val secureRandom = SecureRandom()
        val key = ByteArray(16)
        secureRandom.nextBytes(key)
        val secretKey = SecretKeySpec(key, "AES")

        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)

        val encrypted = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            doFinal(value)
        }

        val buffer = ByteBuffer.allocate(256 + 12 + encrypted.size)

        cipher.run {
            init(Cipher.WRAP_MODE, publicKey)
            buffer.put(wrap(secretKey))
            buffer.put(iv)
            buffer.put(encrypted)
        }

        return buffer.array()
    }

    fun decrypt(value: ByteArray): ByteArray {
        prepareKeyStore()

        val buffer = ByteBuffer.wrap(value)
        val key = ByteArray(256)
        val iv = ByteArray(12)
        buffer.apply {
            get(key)
            get(iv)
        }

        val encrypted = ByteArray(buffer.remaining()).also {
            buffer.get(it)
        }

        return cipher.run {
            init(Cipher.UNWRAP_MODE, privateKey)

            Cipher.getInstance("AES/GCM/NoPadding").also {
                it.init(Cipher.DECRYPT_MODE, unwrap(key, "AES", Cipher.SECRET_KEY), GCMParameterSpec(128, iv))
            }.doFinal(encrypted)
        }
    }
}