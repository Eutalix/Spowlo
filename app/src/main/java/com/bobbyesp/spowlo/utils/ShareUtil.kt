package com.bobbyesp.spowlo.utils

import android.content.Context
import android.content.Intent

/**
 * Simple text share helper using ACTION_SEND (text/plain).
 */
object ShareUtil {
    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(
            Intent.createChooser(intent, title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}