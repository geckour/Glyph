package jp.org.example.geckour.glyph.ui.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class Shaper(
        val id: Long,

        val name: String,

        val dots: List<Int>,

        val correctCount: Long,

        val examCount: Long
): Parcelable