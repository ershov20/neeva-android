package com.neeva.app.browsing

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Encrypts files using a master key.
 *
 * TODO(dan.alcantara): See if we can use a master key that is automatically nuked when the user
 *                      exits incognito.
 */
class FileEncrypter(context: Context) {
    private val appContext = context.applicationContext

    private val masterKey: MasterKey

    init {
        val spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
            .build()

        masterKey = MasterKey.Builder(appContext).setKeyGenParameterSpec(spec).build()
    }

    fun getInputStream(file: File): InputStream {
        return EncryptedFile(appContext, file, masterKey).openFileInput()
    }

    fun getOutputStream(file: File): OutputStream {
        return EncryptedFile(appContext, file, masterKey).openFileOutput()
    }
}
