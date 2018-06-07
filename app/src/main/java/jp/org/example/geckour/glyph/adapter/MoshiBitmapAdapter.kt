package jp.org.example.geckour.glyph.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import timber.log.Timber
import java.io.ByteArrayOutputStream

class MoshiBitmapAdapter : JsonAdapter<Bitmap>() {
    override fun fromJson(reader: JsonReader): Bitmap? =
            try {
                Base64.decode(reader.nextString(), Base64.DEFAULT).let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }
            } catch (t: Throwable) {
                Timber.e(t)
                null
            }

    override fun toJson(writer: JsonWriter, value: Bitmap?) {
        if (value != null) {
            writer.value(
                    Base64.encodeToString(
                            ByteArrayOutputStream().apply {
                                value.compress(Bitmap.CompressFormat.PNG, 100, this)
                            }.toByteArray(),
                            Base64.DEFAULT)
            )
        } else {
            writer.nullValue()
        }
    }
}