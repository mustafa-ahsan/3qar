package com.delilaqar.realestate.util

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.util.Locale

class NsfwDetector(context: Context) {
    private var interpreter: Interpreter? = null

    init {
        try {
            val assetManager = context.assets
            val files = assetManager.list("")
            if (files != null && files.contains("nsfw.tflite")) {
                val model = FileUtil.loadMappedFile(context, "nsfw.tflite")
                interpreter = Interpreter(model, Interpreter.Options().apply { setNumThreads(4) })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isNsfw(bitmap: Bitmap): Pair<Boolean, String> {
        val tflite = interpreter
        if (tflite == null) {
            return Pair(true, "لم يتم العثور على الموديل - تم المنع احتياطياً")
        }

        try {
            val inputTensor = tflite.getInputTensor(0)
            val inputShape = inputTensor.shape()
            val imageSizeY = inputShape[1]
            val imageSizeX = inputShape[2]
            val dataType = inputTensor.dataType()

            val imageProcessorBuilder = ImageProcessor.Builder()
                .add(ResizeOp(imageSizeY, imageSizeX, ResizeOp.ResizeMethod.BILINEAR))

            if (dataType == DataType.FLOAT32) {
                // التطبيع الصحيح لهذا الموديل تحديدًا: قسمة على 255 (مدى 0 إلى 1)
                imageProcessorBuilder.add(NormalizeOp(0f, 255f))
            }

            val imageProcessor = imageProcessorBuilder.build()

            var tensorImage = TensorImage(dataType)
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            val outputTensor = tflite.getOutputTensor(0)
            val outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())

            tflite.run(tensorImage.buffer, outputBuffer.buffer.rewind())

            val probabilities = outputBuffer.floatArray
            val debugInfo = probabilities.joinToString(" | ") { String.format(Locale.US, "%.3f", it) }

            // ترتيب الأصناف الرسمي: drawings, hentai, neutral, porn, sexy
            val isBad = if (probabilities.size >= 5) {
                (probabilities[1] + probabilities[3] + probabilities[4]) > 0.60f
            } else {
                probabilities.last() > 0.60f
            }

            return Pair(isBad, "drawings|hentai|neutral|porn|sexy = $debugInfo")

        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(true, "خطأ: ${e.message} - تم المنع احتياطياً")
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
