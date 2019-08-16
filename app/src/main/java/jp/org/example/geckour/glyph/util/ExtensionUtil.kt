package jp.org.example.geckour.glyph.util

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.ui.model.Shaper
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import jp.org.example.geckour.glyph.db.model.Shaper as DBShaper


private const val VIBRATE_LENGTH: Long = 40L

private val vibrationEffect =
        if (Build.VERSION.SDK_INT >= 26)
            VibrationEffect.createOneShot(VIBRATE_LENGTH, 255)
        else null

fun <T> CoroutineScope.ui(onError: Throwable.() -> Unit = { printStackTrace() },
                          block: suspend CoroutineScope.() -> T) =
        launch(Dispatchers.Main) {
            try {
                block()
            } catch (e: Exception) {
                onError(e)
            }
        }

fun Job.clear() {
    try {
        if (isActive) cancel()
    } catch (e: CancellationException) {
        Timber.e(e)
    }
}

fun <T, R> Pair<T, R>.inverse(): Pair<R, T> = Pair(this.second, this.first)

fun List<Pair<Int, Int>>.mapToPointPaths(dots: Array<PointF>): List<Pair<PointF, PointF>> =
        this.mapNotNull { dotPath ->
            if (dotPath.first in dots.indices && dotPath.second in dots.indices) {
                Pair(
                        PointF(dots[dotPath.first].x, dots[dotPath.first].y),
                        PointF(dots[dotPath.second].x, dots[dotPath.second].y)
                )
            } else null
        }

fun List<Int>.mapToPaths(): List<Pair<Int, Int>> =
        this.fold(emptyList<Int>()) { acc, i ->
            if (acc.lastOrNull() == i) acc else acc + i
        }.let { distinct ->
            when {
                distinct.isEmpty() -> emptyList()
                distinct.size == 1 -> listOf(distinct[0] to distinct[0])
                else -> (1..distinct.lastIndex)
                        .map { distinct[it - 1] to distinct[it] }
            }
        }

fun List<Pair<Int, Int>>.normalized(): List<Pair<Int, Int>> =
        this.apply { Timber.d("geckglyph this: $this") }
                .map { if (it.first > it.second) (it.second to it.first) to true else it to false }
                .distinctBy { it.first }
                .map { if (it.second) it.first.second to it.first.first else it.first }
                .apply { Timber.d("geckglyph normarized: $this") }

fun DBShaper.match(normalizedPath: List<Pair<Int, Int>>): Boolean =
        this.dots.toList().mapToPaths().normalized().match(normalizedPath)

fun DBShaper.parse(): Shaper =
        Shaper(this.id, this.name, this.dots.toList(), this.correctCount, this.examCount)

fun Shaper.match(normalizedPath: List<Pair<Int, Int>>): Boolean =
        this.dots.mapToPaths().normalized().match(normalizedPath)

private fun List<Pair<Int, Int>>.match(path: List<Pair<Int, Int>>): Boolean {
    return path.size == this.size
            && this.size == this.count { path.contains(it) || path.contains(it.inverse()) }
}

fun Context.vibrate(hapticView: View) {
    if (PreferenceManager.getDefaultSharedPreferences(this)
                    .getBooleanValue(Key.HAPTIC_FEEDBACK)) {
        hapticView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                        or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
    } else {
        when (Build.VERSION.SDK_INT) {
            in 0..22 -> {
                (this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                        .vibrate(VIBRATE_LENGTH)
            }

            in 23..25 -> {
                this.getSystemService(Vibrator::class.java)
                        .vibrate(VIBRATE_LENGTH)
            }

            else -> {
                this.getSystemService(Vibrator::class.java)
                        .vibrate(vibrationEffect)
            }
        }
    }
}

fun Bitmap.getMutableImageWithShaper(shaper: Shaper, scale: Float = App.scale): Bitmap {
    val copy = this.copy(this.config, true)

    val paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 34f * scale
        strokeJoin = Paint.Join.BEVEL
    }

    val dotsPoint: Array<PointF> = Array(11) {
        val c = when (it) {
            1 -> 1
            2 -> 3
            3 -> 4
            4 -> 6
            in 5..10 -> it
            else -> 0
        }

        val uAngle = Math.PI / 3.0

        if (it == 0) PointF(0f, 0f)
        else {
            PointF(
                    (Math.cos(uAngle * (c - 0.5)) * (if (it < 5) 0.5 else 1.0)).toFloat(),
                    (Math.sin(uAngle * (c - 0.5)) * (if (it < 5) 0.5 else 1.0)).toFloat()
            )
        }
    }

    val path = Path().apply {
        shaper.dots
                .map { dotsPoint[it] }
                .forEachIndexed { i, pointF ->
                    if (i == 0) {
                        moveTo(
                                (pointF.x * 0.4f + 0.5f) * copy.width,
                                (pointF.y * 0.4f + 0.5f) * copy.height)
                    } else {
                        lineTo(
                                (pointF.x * 0.4f + 0.5f) * copy.width,
                                (pointF.y * 0.4f + 0.5f) * copy.height)
                    }
                }

        if (shaper.dots.first() == shaper.dots.last())
            close()
    }

    Canvas(copy).drawPath(path, paint)

    return copy
}

fun Long.toTimeStringPair(): Pair<String, String> =
        Pair((this / 1000).toString(), (this % 1000).format(2).take(2))

fun Int.getDifficulty(): Int =
        when (this) {
            in 0..1 -> 1
            2 -> 2
            in 3..5 -> 3
            in 6..7 -> 4
            8 -> 5
            else -> 0
        }

fun Number.format(digit: Int): String = String.format("%0${digit}d", this)

fun Activity.setCrashlytics() {
    Fabric.with(this, Crashlytics())
}

fun <T> List<T>.random(): T =
        if (isEmpty()) throw IllegalStateException()
        else get(Random().nextInt(size))