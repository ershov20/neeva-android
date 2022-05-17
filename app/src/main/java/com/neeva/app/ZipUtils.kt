package com.neeva.app

import android.util.Log
import androidx.annotation.WorkerThread
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import okhttp3.internal.closeQuietly

object ZipUtils {
    private val TAG = ZipUtils::class.simpleName

    @WorkerThread
    fun compress(file: File, outputZipFile: File): Boolean {
        var fileStream: FileOutputStream? = null
        var zipStream: ZipOutputStream? = null
        return try {
            fileStream = FileOutputStream(outputZipFile)
            zipStream = ZipOutputStream(fileStream)
            addFile("", file, zipStream)
            zipStream.flush()
            true
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to zip $file", exception)
            false
        } finally {
            zipStream?.closeQuietly()
            fileStream?.closeQuietly()
        }
    }

    private fun addFile(internalZipPath: String, file: File, zipStream: ZipOutputStream) {
        if (file.isDirectory) {
            // Go through each file in the directory and zip it up.
            file.listFiles()?.forEach { descendant ->
                val prefix = "$internalZipPath/".takeIf { internalZipPath.isNotEmpty() } ?: ""
                val childPath = "$prefix${file.name}"

                addFile(
                    internalZipPath = childPath,
                    file = descendant,
                    zipStream = zipStream
                )
            }
        } else {
            // Read in the file and save it out to the compressed stream.
            zipStream.putNextEntry(ZipEntry(internalZipPath + "/" + file.name))

            val buffer = ByteArray(1024)
            var length: Int
            val inputStream = FileInputStream(file)
            while (inputStream.read(buffer).also { length = it } > 0) {
                zipStream.write(buffer, 0, length)
            }
        }
    }
}
