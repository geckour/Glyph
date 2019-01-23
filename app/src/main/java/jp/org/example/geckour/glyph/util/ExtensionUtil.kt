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

fun ArrayList<Job>.clearAll() {
    forEach {
        try {
            if (it.isActive) it.cancel()
        } catch (e: CancellationException) {
            Timber.e(e)
        }
    }

    clear()
}

fun Job.clear() {
    try {
        if (isActive) cancel()
    } catch (e: CancellationException) {
        Timber.e(e)
    }
}

inline fun <T> Iterable<T>.takeWhileIndexed(predicate: (Int, T) -> Boolean): List<T> {
    val list = ArrayList<T>()
    for ((i, item) in this.withIndex()) {
        if (!predicate(i, item))
            continue
        list.add(item)
    }
    return list
}

fun <T, R> Pair<T, R>.inverse(): Pair<R, T> = Pair(this.second, this.first)

fun List<Pair<Int, Int>>.mapToPointPathsFromDotPaths(dots: Array<PointF>): List<Pair<PointF, PointF>> =
        this.mapNotNull { dotPath ->
            return@mapNotNull if (dotPath.first in 0..dots.lastIndex
                    && dotPath.second in 0..dots.lastIndex) {
                Pair(
                        PointF(dots[dotPath.first].x, dots[dotPath.first].y),
                        PointF(dots[dotPath.second].x, dots[dotPath.second].y)
                )
            } else null
        }

fun List<Int>.convertDotsListToPaths(): List<Pair<Int, Int>> =
        when {
            this.isEmpty() -> listOf()

            this.size < 2 -> listOf(Pair(this[0], this[0]))

            else -> {
                val droppedList =
                        this.takeWhileIndexed { i, index -> i < 1 || this[i - 1] != index }

                when {
                    droppedList.isEmpty() -> listOf()
                    droppedList.size < 2 -> listOf(Pair(droppedList[0], droppedList[0]))
                    else -> (1..droppedList.lastIndex)
                            .map { Pair(droppedList[it - 1], droppedList[it]) }
                }
            }
        }

fun List<Pair<Int, Int>>.getNormalizedPaths(initialIndex: Int = 0): List<Pair<Int, Int>> =
        if (this.size > 1 && initialIndex < this.lastIndex) {
            ArrayList(this.subList(0, initialIndex + 1)).apply {
                addAll(
                        this@getNormalizedPaths.subList(
                                initialIndex + 1,
                                this@getNormalizedPaths.size
                        ).filter {
                            it != this@getNormalizedPaths[initialIndex]
                                    && it != this@getNormalizedPaths[initialIndex].inverse()
                        }
                )
            }.getNormalizedPaths(initialIndex + 1)
        } else this

fun DBShaper.match(path: List<Pair<Int, Int>>): Boolean =
        this.dots.toList().match(path)

fun DBShaper.parse(): Shaper =
        Shaper(this.id, this.name, this.dots.toList(), this.correctCount, this.examCount)

fun Shaper.match(path: List<Pair<Int, Int>>): Boolean =
        this.dots.match(path)

private fun List<Int>.match(path: List<Pair<Int, Int>>): Boolean {
    val glyphPath = this.convertDotsListToPaths()

    return path.size == glyphPath.size
            && glyphPath.size == glyphPath.count { path.contains(it) || path.contains(it.inverse()) }
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