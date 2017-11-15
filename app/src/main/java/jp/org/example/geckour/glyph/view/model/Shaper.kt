package jp.org.example.geckour.glyph.view.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class Shaper(
        val id: Long,

        val name: String,

        val dots: List<Int>
): Parcelable