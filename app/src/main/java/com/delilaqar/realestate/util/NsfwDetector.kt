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

    fun isModelReady(): Boolean = NsfwModelManager.isReady(context)

    fun isNsfw(bitmap: Bitmap): Pair<Boolean, String> {
        if (!NsfwModelManager.isReady(context)) {
            return Pair(false, "MODEL_NOT_READY")
        }

        try {
            val interpreter = loadInterpreter() ?: return Pair(true, "فشل تحميل الموديل - تم المنع احتياطياً")

            val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            val inputBuffer = java.nio.ByteBuffer.allocateDirect(4 * 3 * 224 * 224)
            inputBuffer.order(java.nio.ByteOrder.nativeOrder())

            val mean = floatArrayOf(123.675f, 116.28f, 103.53f)
            val std = floatArrayOf(58.395f, 57.12f, 57.375f)
            val pixels = IntArray(224 * 224)
            resized.getPixels(pixels, 0, 224, 0, 0, 224, 224)

            for (c in 0..2) {
                for (pixel in pixels) {
                    val value = when (c) {
                        0 -> (pixel shr 16) and 0xFF
                        1 -> (pixel shr 8) and 0xFF
                        else -> pixel and 0xFF
                    }
                    inputBuffer.putFloat((value - mean[c]) / std[c])
                }
            }

            val outputBuffer = java.nio.ByteBuffer.allocateDirect(4 * 2)
            outputBuffer.order(java.nio.ByteOrder.nativeOrder())
            interpreter.run(inputBuffer, outputBuffer)
            interpreter.close()

            outputBuffer.rewind()
            val logitNormal = outputBuffer.float
            val logitNsfw = outputBuffer.float

            val maxLogit = maxOf(logitNormal, logitNsfw)
            val expNormal = kotlin.math.exp((logitNormal - maxLogit).toDouble())
            val expNsfw = kotlin.math.exp((logitNsfw - maxLogit).toDouble())
            val probNsfw = (expNsfw / (expNormal + expNsfw)).toFloat()

            val isUnsafe = probNsfw > 0.15f
            return Pair(isUnsafe, "nsfw probability: ${String.format(java.util.Locale.US, "%.4f", probNsfw)}")
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(true, "خطأ: ${e.message} - تم المنع احتياطياً")
        }
    }

    private fun loadInterpreter(): org.tensorflow.lite.Interpreter? {
        val file = NsfwModelManager.getModelFile(context)
        if (!file.exists()) return null
        return try {
            val raf = java.io.RandomAccessFile(file, "r")
            val mappedBuffer = raf.channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, raf.channel.size())
            org.tensorflow.lite.Interpreter(mappedBuffer, org.tensorflow.lite.Interpreter.Options().apply { setNumThreads(4) })
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun close() {}
}
