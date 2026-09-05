package com.delilaqar.realestate.util

import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.NavOptions

/**
 * Like setOnClickListener, but ignores extra rapid taps within [debounceMillis].
 * Prevents crashes caused by firing the same action (like navigation) twice at once.
 */
fun View.setOnSingleClickListener(debounceMillis: Long = 800L, action: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { view ->
        val now = System.currentTimeMillis()
        if (now - lastClickTime >= debounceMillis) {
            lastClickTime = now
            action(view)
        }
    }
}

/**
 * Safe wrapper around NavController.navigate that swallows the exception thrown
 * when navigate() is called twice in quick succession or from an invalid state,
 * instead of crashing the app.
 */
fun NavController.navigateSafe(actionId: Int) {
    try {
        navigate(actionId)
    } catch (e: Exception) {
        // Ignored: caused by rapid repeated navigation attempts.
    }
}

fun NavController.navigateSafe(actionId: Int, args: Bundle?, navOptions: NavOptions) {
    try {
        navigate(actionId, args, navOptions)
    } catch (e: Exception) {
        // Ignored: caused by rapid repeated navigation attempts.
    }
}
