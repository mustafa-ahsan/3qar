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
    // المفتاح الخاص بك الذي أخذناه من موقع ImgBB
    private const val API_KEY = "0bdc5eaf9474f37249ff022c02fab9a3"
    private const val UPLOAD_URL = "https://api.imgbb.com/1/upload"

    private val client = OkHttpClient()

    /**
     * دالة معزولة لرفع الصورة. 
     * نستخدم Base64 لتجنب مشاكل مسارات الملفات في أندرويد.
     * تُرجع رابط الصورة المباشر في حال النجاح، أو null في حال الفشل.
     */
    suspend fun uploadImage(context: Context, imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. قراءة الصورة من الـ Uri وتحويلها إلى نص Base64
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes == null) return@withContext null
                val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)

                // 2. تجهيز الطلب لإرساله إلى API ImgBB
                val formBody = FormBody.Builder()
                    .add("key", API_KEY)
                    .add("image", base64Image)
                    .build()

                val request = Request.Builder()
                    .url(UPLOAD_URL)
                    .post(formBody)
                    .build()

                // 3. إرسال الطلب واستقبال الرد
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    // 4. استخراج رابط الصورة من الرد
                    val jsonObject = JSONObject(responseBody)
                    val dataObject = jsonObject.getJSONObject("data")
                    return@withContext dataObject.getString("url")
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