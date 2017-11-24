package jp.org.example.geckour.glyph.util

import android.content.Context
import android.graphics.*
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.App.Companion.version
import jp.org.example.geckour.glyph.view.model.Shaper
import kotlinx.coroutines.experimental.*
import jp.org.example.geckour.glyph.db.model.Shaper as DBShaper
import kotlinx.coroutines.experimental.android.UI
import kotlin.coroutines.experimental.CoroutineContext


private const val VIBRATE_LENGTH: Long = 1L

fun <T> async(context: CoroutineContext = CommonPool, block: suspend CoroutineScope.() -> T) =
        kotlinx.coroutines.experimental.async(context, block = block)

fun ArrayList<Job>.insertUILaunch(onError: Throwable.() -> Unit = { printStackTrace() }, block: suspend CoroutineScope.() -> Unit) =
        uiLaunch(onError, block).apply { this@insertUILaunch.add(this@apply) }

fun <T> uiLaunch(onError: Throwable.() -> Unit = { printStackTrace() }, block: suspend CoroutineScope.() -> T) =
        launch(UI) { try { block() } catch (e: Exception) { onError(e) } }

fun ArrayList<Job>.clearJobs() {
    forEach { try { if (it.isActive) it.cancel() } catch (e: CancellationException) {} }
    clear()
}

fun Job.clear() { try { if (isActive) cancel() } catch (e: CancellationException) {} }

fun Job.kick() { if (!this.isActive) this.start() }

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
            if (dotPath.first in 0..dots.lastIndex && dotPath.second in 0..dots.lastIndex)
                Pair(PointF(dots[dotPath.first].x, dots[dotPath.first].y), PointF(dots[dotPath.second].x, dots[dotPath.second].y))
            else null
        }

fun List<Int>.convertDotsListToPaths(): List<Pair<Int, Int>> =
        when {
            this.isEmpty() -> listOf()
            this.size < 2 -> listOf(Pair(this[0], this[0]))
            else -> {
                val droppedList = this.takeWhileIndexed { i, index -> i < 1 || this[i - 1] != index }
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
                        this@getNormalizedPaths.subList(initialIndex + 1, this@getNormalizedPaths.size)
                                .filter { it != this@getNormalizedPaths[initialIndex] && it != this@getNormalizedPaths[initialIndex].inverse() }
                )
            }.getNormalizedPaths(initialIndex + 1)
        } else this

fun DBShaper.match(path: List<Pair<Int, Int>>): Boolean {
    val glyphPath = this.dots.convertDotsListToPaths()
    return if (path.size == glyphPath.size) {
        glyphPath.size == glyphPath.count { path.contains(it) || path.contains(it.inverse()) }
    } else false
}

fun DBShaper.parse(): Shaper = Shaper(this.id, this.name, this.dots.toList(), this.correctCount, this.examCount)

fun Shaper.match(path: List<Pair<Int, Int>>): Boolean {
    val glyphPath = this.dots.convertDotsListToPaths()
    return if (path.size == glyphPath.size) {
        glyphPath.size == glyphPath.count { path.contains(it) || path.contains(it.inverse()) }
    } else false
}

fun AppCompatActivity.vibrate() {
    when (version) {
        in 0..22 -> (this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VIBRATE_LENGTH)
        in 23..25 -> this.getSystemService(Vibrator::class.java).vibrate(VIBRATE_LENGTH)
        else -> this.getSystemService(Vibrator::class.java).vibrate(VibrationEffect.createOneShot(VIBRATE_LENGTH, 255))
    }
}

fun Fragment.vibrate() {
    when (version) {
        in 0..22 -> (activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VIBRATE_LENGTH)
        in 23..25 -> activity.getSystemService(Vibrator::class.java).vibrate(VIBRATE_LENGTH)
        else -> activity.getSystemService(Vibrator::class.java).vibrate(VibrationEffect.createOneShot(VIBRATE_LENGTH, 255))
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

    Canvas(copy).drawPath(
            shaper.dots
                    .map { dotsPoint[it] }
                    .let {
                        Path().apply {
                            it.forEachIndexed { i, pointF ->
                                if (i < 1) moveTo((pointF.x * 0.4f + 0.5f) * copy.width, (pointF.y * 0.4f + 0.5f) * copy.height)
                                else lineTo((pointF.x * 0.4f + 0.5f) * copy.width, (pointF.y * 0.4f + 0.5f) * copy.height)
                            }
                            if (shaper.dots.first() == shaper.dots.last()) close()
                        }
                    }, paint
    )

    return copy
}

fun Long.toTimeStringPair(): Pair<String, String> = Pair((this / 1000).toString(), (this % 1000).format(2).take(2))

fun Int.getDifficulty(): Int =
        when (this) {
            in 0..1 -> 1
            2 -> 2
            in 3..5 -> 3
            in 6..7 -> 4
            8 -> 5
            else -> 0
        }

fun Number.format(disit: Int): String = String.format("%0${disit}d", this)