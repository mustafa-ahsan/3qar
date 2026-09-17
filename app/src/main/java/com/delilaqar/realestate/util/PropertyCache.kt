package com.delilaqar.realestate.util

import com.delilaqar.realestate.data.Property
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object PropertyCache {
    const val PAGE_SIZE = 15L

    data class ConsumedCache(
        val latest: List<Property>,
        val featured: List<Property>,
        val favorites: Set<String>,
        val lastDoc: DocumentSnapshot?,
        val reachedEnd: Boolean
    )

    private var cachedLatestPage: List<Property>? = null
    private var cachedFeatured: List<Property>? = null
    private var cachedFavoriteIds: Set<String>? = null
    private var cachedLastDoc: DocumentSnapshot? = null
    private var cachedReachedEnd: Boolean = false

    fun preload() {
        val db = FirebaseFirestore.getInstance()

        db.collection("properties")
            .whereEqualTo("status", "active")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .get()
            .addOnSuccessListener { snapshot ->
                cachedLatestPage = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Property::class.java)?.apply { id = doc.id }
                }
                cachedLastDoc = snapshot.documents.lastOrNull()
                cachedReachedEnd = snapshot.documents.size < PAGE_SIZE
            }

        db.collection("properties")
            .whereEqualTo("status", "active")
            .whereEqualTo("featured", true)
            .get()
            .addOnSuccessListener { snapshot ->
                cachedFeatured = snapshot.documents.mapNotNull { doc ->
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

    fun consume(): ConsumedCache? {
        val latest = cachedLatestPage ?: return null
        val featured = cachedFeatured ?: emptyList()
        val favs = cachedFavoriteIds ?: emptySet()
        val lastDoc = cachedLastDoc
        val reachedEnd = cachedReachedEnd
        clear()
        return ConsumedCache(latest, featured, favs, lastDoc, reachedEnd)
    }

    fun clear() {
        cachedLatestPage = null
        cachedFeatured = null
        cachedFavoriteIds = null
        cachedLastDoc = null
        cachedReachedEnd = false
    }
}
