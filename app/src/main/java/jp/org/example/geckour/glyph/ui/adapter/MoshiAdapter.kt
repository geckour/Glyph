package jp.org.example.geckour.glyph.ui.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import timber.log.Timber
import java.io.ByteArrayOutputStream

class MoshiAdapter {
    @FromJson
    fun fromJson(bitmapString: String): Bitmap? =
            try {
                Base64.decode(bitmapString, Base64.DEFAULT).let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }
            } catch (t: Throwable) {
                Timber.e(t)
                null
            }

    @ToJson
    fun toJson(bitmap: Bitmap?): String =
            if (bitmap == null) ""
            else
                Base64.encodeToString(
                        ByteArrayOutputStream().apply {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
                        }.toByteArray(),
                        Base64.DEFAULT)
}