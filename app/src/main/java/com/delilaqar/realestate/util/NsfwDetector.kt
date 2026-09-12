package com.delilaqar.realestate.util

import android.content.Context
import android.graphics.Bitmap
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Locale
import kotlin.math.exp

class NsfwDetector(private val context: Context) {

    companion object {
        private const val MODEL_URL = "https://github.com/mustafa-ahsan/3qar/releases/download/nsfw-model-v1/falconsai_nsfw_quant.tflite"
        private const val MODEL_FILE_NAME = "falconsai_nsfw.tflite"
        private const val INPUT_SIZE = 224
        private val MEAN = floatArrayOf(123.675f, 116.28f, 103.53f)
        private val STD = floatArrayOf(58.395f, 57.12f, 57.375f)
    }

    private val client = OkHttpClient()

    private fun getModelFile(): File = File(context.filesDir, MODEL_FILE_NAME)

    private fun ensureModelDownloaded(): Boolean {
        val file = getModelFile()
        if (file.exists() && file.length() > 0) return true
        return try {
            val request = Request.Builder().url(MODEL_URL).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return false
            val body = response.body ?: return false
            val tempFile = File(context.filesDir, "$MODEL_FILE_NAME.tmp")
            FileOutputStream(tempFile).use { output ->
                body.byteStream().use { input -> input.copyTo(output) }
            }
            tempFile.renameTo(file)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun loadInterpreter(): Interpreter? {
        val file = getModelFile()
        if (!file.exists()) return null
        return try {
            val raf = RandomAccessFile(file, "r")
            val mappedBuffer = raf.channel.map(FileChannel.MapMode.READ_ONLY, 0, raf.channel.size())
            Interpreter(mappedBuffer, Interpreter.Options().apply { setNumThreads(4) })
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isNsfw(bitmap: Bitmap): Pair<Boolean, String> {
        if (!ensureModelDownloaded()) {
            return Pair(true, "فشل تحميل موديل الفحص - تم المنع احتياطياً")
        }
        val interpreter = loadInterpreter() ?: return Pair(true, "فشل تحميل الموديل - تم المنع احتياطياً")

        return try {
            val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val inputBuffer = ByteBuffer.allocateDirect(4 * 3 * INPUT_SIZE * INPUT_SIZE)
            inputBuffer.order(ByteOrder.nativeOrder())

            val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
            resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

            for (c in 0..2) {
                for (pixel in pixels) {
                    val value = when (c) {
                        0 -> (pixel shr 16) and 0xFF
                        1 -> (pixel shr 8) and 0xFF
                        else -> pixel and 0xFF
                    }
                    inputBuffer.putFloat((value - MEAN[c]) / STD[c])
                }
            }

            val outputBuffer = ByteBuffer.allocateDirect(4 * 2)
            outputBuffer.order(ByteOrder.nativeOrder())
            interpreter.run(inputBuffer, outputBuffer)
            interpreter.close()

            outputBuffer.rewind()
            val logitNormal = outputBuffer.float
            val logitNsfw = outputBuffer.float

            val maxLogit = maxOf(logitNormal, logitNsfw)
            val expNormal = exp((logitNormal - maxLogit).toDouble())
            val expNsfw = exp((logitNsfw - maxLogit).toDouble())
            val probNsfw = (expNsfw / (expNormal + expNsfw)).toFloat()

            val isUnsafe = probNsfw > 0.15f
            Pair(isUnsafe, "nsfw probability: ${String.format(Locale.US, "%.4f", probNsfw)}")
        } catch (e: Exception) {
            e.printStackTrace()
            interpreter.close()
            Pair(true, "خطأ: ${e.message} - تم المنع احتياطياً")
        }
    }

    fun close() {}
}
