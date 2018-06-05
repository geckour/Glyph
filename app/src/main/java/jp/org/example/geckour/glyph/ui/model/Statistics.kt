package jp.org.example.geckour.glyph.ui.model

import android.graphics.Bitmap

data class Statistics(
        val sequenceData: Data,
        val individualData: List<Data>
) {
    data class Data(
            val id: Long,
            val name: String,
            val correctCount: Long,
            val totalCount: Long,
            var bitmap: Bitmap?
    )
}