package com.delilaqar.realestate.util

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object NsfwModelManager {
    private const val MODEL_URL = "https://github.com/mustafa-ahsan/3qar/releases/download/nsfw-model-v1/falconsai_nsfw_quant.tflite"
    private const val MODEL_FILE_NAME = "falconsai_nsfw.tflite"
    private const val MIN_VALID_SIZE = 300_000_000L
    private const val MAX_RETRIES = 4

    enum class State { IDLE, DOWNLOADING, READY, FAILED }

    @Volatile
    var state: State = State.IDLE
        private set

    @Volatile
    var progressPercent: Int = 0
        private set

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    fun getModelFile(context: Context): File = File(context.filesDir, MODEL_FILE_NAME)

    fun isReady(context: Context): Boolean {
        val file = getModelFile(context)
        return file.exists() && file.length() >= MIN_VALID_SIZE
    }

    fun startBackgroundDownload(context: Context, onStateChanged: (() -> Unit)? = null) {
        val appContext = context.applicationContext

        if (isReady(appContext)) {
            state = State.READY
            progressPercent = 100
            onStateChanged?.invoke()
            return
        }
        if (state == State.DOWNLOADING) return

        state = State.DOWNLOADING
        progressPercent = 0
        onStateChanged?.invoke()

        Thread {
            var success = false
            var attempt = 0
            while (!success && attempt < MAX_RETRIES) {
                success = attemptDownload(appContext, onStateChanged)
                attempt++
            }
            state = if (success) State.READY else State.FAILED
            progressPercent = if (success) 100 else 0
            onStateChanged?.invoke()
        }.start()
    }

    fun retry(context: Context, onStateChanged: (() -> Unit)? = null) {
        state = State.IDLE
        progressPercent = 0
        startBackgroundDownload(context, onStateChanged)
    }

    private fun attemptDownload(context: Context, onStateChanged: (() -> Unit)?): Boolean {
        return try {
            val request = Request.Builder().url(MODEL_URL).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return false
            val body = response.body ?: return false
            val expectedLength = body.contentLength()

            val tempFile = File(context.filesDir, "$MODEL_FILE_NAME.tmp")
            var downloaded = 0L
            var lastReportedPercent = -1

            FileOutputStream(tempFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes

                        if (expectedLength > 0) {
                            val percent = ((downloaded * 100) / expectedLength).toInt().coerceIn(0, 99)
                            if (percent != lastReportedPercent) {
                                progressPercent = percent
                                lastReportedPercent = percent
                                onStateChanged?.invoke()
                            }
                        }

                        bytes = input.read(buffer)
                    }
                }
            }

            if (expectedLength > 0 && downloaded < expectedLength) {
                tempFile.delete()
                return false
            }
            if (downloaded < MIN_VALID_SIZE) {
                tempFile.delete()
                return false
            }

            val finalFile = getModelFile(context)
            finalFile.delete()
            tempFile.renameTo(finalFile)
        } catch (e: Exception) {
            false
        }
    }
}
