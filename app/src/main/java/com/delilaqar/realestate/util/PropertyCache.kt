package com.delilaqar.realestate.util

import com.delilaqar.realestate.data.Property
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * ذاكرة مؤقتة تُحمّل بيانات العقارات والمفضلة بمجرد فتح شاشة الترحيب،
 * عشان تكون جاهزة فورًا لما يوصل المستخدم للشاشة الرئيسية (بعد تسجيل الدخول أو التصفح كضيف)
 * بدون أي وقت انتظار إضافي.
 */
object PropertyCache {
    var cachedProperties: List<Property>? = null
        private set
    var cachedFavoriteIds: Set<String>? = null
        private set

    fun preload() {
        val db = FirebaseFirestore.getInstance()

        db.collection("properties")
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { snapshot ->
                cachedProperties = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Property::class.java)?.apply { id = doc.id }
                }
            }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).collection("favorites").get()
                .addOnSuccessListener { snapshot ->
                    cachedFavoriteIds = snapshot.documents.map { it.id }.toSet()
                }
        } else {
            cachedFavoriteIds = emptySet()
        }
    }

    fun consume(): Pair<List<Property>, Set<String>>? {
        val props = cachedProperties ?: return null
        val favs = cachedFavoriteIds ?: emptySet()
        clear()
        return props to favs
    }

    fun clear() {
        cachedProperties = null
        cachedFavoriteIds = null
    }
}
