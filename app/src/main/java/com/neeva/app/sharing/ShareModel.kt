package com.neeva.app.sharing
import android.content.Context
import android.content.Intent
import android.net.Uri

class ShareModel {
    fun shareURL(uri: Uri, title: String?, context: Context) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"

            putExtra(Intent.EXTRA_TEXT, uri.toString())
            putExtra(Intent.EXTRA_TITLE, title)
        }

        share(sendIntent, context)
    }

    private fun share(intent: Intent, context: Context) {
        val shareIntent = Intent.createChooser(intent, null)
        context.startActivity(shareIntent)
    }
}
