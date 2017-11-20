package jp.org.example.geckour.glyph.fragment.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Result(
        val id: Long,
        val correct: Boolean,
        val spentTime: Long
): Parcelable