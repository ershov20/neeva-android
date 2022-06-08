package com.neeva.app

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
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
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

                    // Read all the HTTP headers being sent.  We only care about the first line,
                    // which should indicate what file the client is requesting.
                    val firstLine: String? = reader.readLine()
                    var ignored = reader.readLine()
                    while (ignored.isNotEmpty()) { ignored = reader.readLine() }

                    // Figure out what file to load up using the first line of the request, which
                    // should take the form of:
                    // GET /file_being_requested.html HTTP/1.1
                    val filename: String = firstLine
                        ?.takeIf { it.startsWith("GET") }
                        ?.split(' ')
                        ?.getOrNull(1)
                        ?.drop(1) // Drop the leading "/" from the path.
                        ?.let { Uri.parse(it).path.takeUnless { path -> path.isNullOrEmpty() } }
                        ?: "index.html"

                    val html: String? = try {
                        // Try to load the file up from the assets.
                        val context = InstrumentationRegistry.getInstrumentation().context
                        context.assets.open("html/$filename").use {
                            it.bufferedReader().use { reader ->
                                val stringBuilder = StringBuilder()
                                var nextLine: String? = reader.readLine()
                                while (nextLine != null) {
                                    stringBuilder.append("$nextLine\n")
                                    nextLine = reader.readLine()
                                }
                                stringBuilder.toString()
                            }
                        }
                    } catch (e: IOException) {
                        null
                    }

                    if (html != null) {
                        // Send the page if we found the file.
                        val output =
                            "HTTP/1.1 200 OK\n" +
                                "Content-Type: text/html\n" +
                                "Content-Length: ${html.length}\n\n" +
                                html

                        writer.write(output)
                    } else {
                        // Send a 404 for files we don't know about.
                        writer.write("HTTP/1.1 404 Not Found\n\n")
                    }

                    writer.flush()
                    writer.close()
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
