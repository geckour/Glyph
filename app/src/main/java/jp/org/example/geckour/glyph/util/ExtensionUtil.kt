package jp.org.example.geckour.glyph.util

import android.content.Context
import android.graphics.PointF
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import jp.org.example.geckour.glyph.App.Companion.version
import jp.org.example.geckour.glyph.view.model.Shaper
import jp.org.example.geckour.glyph.db.model.Shaper as DBShaper
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext


fun <T> async(context: CoroutineContext = CommonPool, block: suspend CoroutineScope.() -> T) =
        kotlinx.coroutines.experimental.async(context, block = block)

fun ui(managerList: ArrayList<Job>, onError: (Throwable) -> Unit = {}, block: suspend CoroutineScope.() -> Unit) =
        launch(UI) {
            try {
                block()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
            }
        }.apply {
            managerList.add(this)
        }

fun clearJobs(managerList: ArrayList<Job>) =
        managerList.apply {
            forEach { it.cancel() }
            clear()
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
        dots.let {
            map { path ->
                if (path.first in 0..it.size)
                    Pair(PointF(it[path.first].x, it[path.first].y), PointF(it[path.second].x, it[path.second].y))
                else null
            }
        }.filterNotNull()

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

fun List<Pair<Int, Int>>.getNormalizedPaths(initialIndex: Int = 0): List<Pair<Int, Int>> {
    return if (this.size > 1 && initialIndex < this.lastIndex) {
        ArrayList(this.subList(0, initialIndex + 1)).apply {
            addAll(
                    this@getNormalizedPaths.subList(initialIndex + 1, this@getNormalizedPaths.size)
                            .filter { it != this@getNormalizedPaths[initialIndex] && it != this@getNormalizedPaths[initialIndex].inverse() }
            )
        }.getNormalizedPaths(initialIndex + 1)
    } else this
}

fun DBShaper.match(path: List<Pair<Int, Int>>): Boolean {
    val shaperPath = this.dots.convertDotsListToPaths()
    return if (path.size == shaperPath.size) {
        shaperPath.size == shaperPath.count { path.contains(it) || path.contains(it.inverse()) }
    } else false
}

fun DBShaper.parse(): Shaper = Shaper(this.id, this.name, this.dots.toList())

fun Shaper.match(path: List<Pair<Int, Int>>): Boolean {
    val shaperPath = this.dots.convertDotsListToPaths()
    return if (path.size == shaperPath.size) {
        shaperPath.size == shaperPath.count { path.contains(it) || path.contains(it.inverse()) }
    } else false
}

fun AppCompatActivity.vibrate() {
    when (version) {
        in 0..22 -> (this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(30)
        in 23..25 -> this.getSystemService(Vibrator::class.java).vibrate(30)
        else -> this.getSystemService(Vibrator::class.java).vibrate(VibrationEffect.createOneShot(30L, 255))
    }
}

fun Fragment.vibrate() {
    when (version) {
        in 0..22 -> (activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(30)
        in 23..25 -> activity.getSystemService(Vibrator::class.java).vibrate(30)
        else -> activity.getSystemService(Vibrator::class.java).vibrate(VibrationEffect.createOneShot(30L, 255))
    }
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