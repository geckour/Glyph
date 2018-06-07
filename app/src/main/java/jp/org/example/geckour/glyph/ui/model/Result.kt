package jp.org.example.geckour.glyph.ui.model

import android.graphics.Bitmap
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Result(
        val details: List<ResultDetail>,
        val count: Long
) {
    fun calcHackBonus(): Int {
        val c = when (details.size) {
            1 -> 38
            2 -> 60
            3 -> 85
            4 -> 120
            5 -> 162
            else -> 0
        }
        val correctRate = details.count { it.correct }.toFloat() / details.size

        return Math.round(c * correctRate)
    }

    fun calcSpeedBonus(allowableTime: Long): Int =
            if (details.count { it.correct } == details.size) {
                Math.round(
                        (allowableTime - details.map { it.spentTime }.sum()) * 100.0
                                / allowableTime
                ).toInt()
            } else 0
}

@JsonSerializable
data class ResultDetail(
        val id: Long,
        var name: String?,
        val correct: Boolean,
        val spentTime: Long,
        var bitmap: Bitmap?
)