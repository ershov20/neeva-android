package com.neeva.app

import android.net.Uri
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.URLConnection
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Starts up a server on the Android device that serves web pages out of the app's assets.
 *
 * An alternative to using this class would be to have devs and CircleCI start up a local webserver,
 * but having this be part of test setup _probably_ makes it easier for new people to jump in.
 */
class WebpageServingRule : TestRule {
    companion object {
        private const val LOCAL_TEST_URL = "http://127.0.0.1:8000"
        private const val TAG = "WebpageServingRule"

        /** Returns the URL to load to be served the given [filename] from the assets. */
        fun urlFor(filename: String): String = "$LOCAL_TEST_URL/$filename"
    }

    inner class ServingThread : Runnable, AutoCloseable {
        private val thread: Thread = Thread(this)

        init {
            thread.start()
        }

        @Throws(IOException::class)
        override fun run() {
            val serverSocket = ServerSocket(8000)

            while (true) {
                serverSocket.accept().use { socket ->
                    try {
                        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                        val writer = BufferedOutputStream(socket.getOutputStream())

                        // Read all the HTTP headers being sent.  We only care about the first line,
                        // which should indicate what file the client is requesting.
                        val firstLine: String? = reader.readLine()
                        var ignored = reader.readLine()
                        while (ignored.isNotEmpty()) {
                            ignored = reader.readLine()
                        }

                        // Figure out what file to load up using the first line of the request,
                        // which should take the form of:
                        // GET /file_being_requested.html HTTP/1.1
                        val filename: String = firstLine
                            ?.takeIf { it.startsWith("GET") }
                            ?.split(' ')
                            ?.getOrNull(1)
                            ?.drop(1) // Drop the leading "/" from the path.
                            ?.let { Uri.parse(it).path.takeUnless { path -> path.isNullOrEmpty() } }
                            ?: "index.html"

                        try {
                            // Try to load the file up from the assets.
                            val assets = InstrumentationRegistry.getInstrumentation().context.assets
                            val bytes = assets.open("html/$filename").buffered().use {
                                it.readBytes()
                            }

                            // Send the page if we found the file.
                            val mimeType = URLConnection.guessContentTypeFromName(filename)
                            val output =
                                "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: ${mimeType}\r\n" +
                                    "Content-Length: ${bytes.size}\r\n\r\n"
                            writer.write(output.toByteArray().plus(bytes))
                        } catch (e: FileNotFoundException) {
                            writer.write("HTTP/1.1 404 Not Found\r\n\r\n".toByteArray())
                        } catch (e: IOException) {
                            Log.e(TAG, "Exception while serving file", e)
                            writer.write("HTTP/1.1 500 Not Implemented\r\n\r\n".toByteArray())
                        }

                        writer.flush()
                        writer.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception caught for this socket", e)
                    }
                }
            }
        }

        override fun close() {
            thread.interrupt()
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                ServingThread().use {
                    base.evaluate()
                }
            }
        }
    }
}
