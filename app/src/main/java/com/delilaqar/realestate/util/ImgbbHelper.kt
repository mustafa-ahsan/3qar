package com.delilaqar.realestate.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object ImgbbHelper {
    // معلومات حسابك في Cloudinary
    private const val CLOUD_NAME = "dnyvt31st"
    private const val UPLOAD_PRESET = "dalili3qar"
    private const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    private val client = OkHttpClient()

    suspend fun uploadImage(context: Context, imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. قراءة الصورة وتحويلها إلى Base64
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes == null) return@withContext null
                val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
                
                // 2. Cloudinary يحتاج هذه البادئة قبل كود الـ Base64
                val fileData = "data:image/jpeg;base64,$base64Image"

                // 3. تجهيز الطلب لإرساله إلى Cloudinary
                val formBody = FormBody.Builder()
                    .add("file", fileData)
                    .add("upload_preset", UPLOAD_PRESET)
                    .build()

                val request = Request.Builder()
                    .url(UPLOAD_URL)
                    .post(formBody)
                    .build()

                // 4. إرسال الطلب واستقبال الرابط الجديد
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    // Cloudinary يُرجع الرابط الآمن داخل متغير اسمه secure_url
                    return@withContext jsonObject.getString("secure_url")
                } else {
                    return@withContext null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }
}