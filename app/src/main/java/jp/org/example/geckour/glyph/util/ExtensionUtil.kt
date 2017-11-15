package jp.org.example.geckour.glyph.util

import android.graphics.PointF
import jp.org.example.geckour.glyph.db.model.Shaper
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

fun Shaper.match(path: List<Pair<Int, Int>>): Boolean {
    val shaperPath = this.dots.convertDotsListToPaths()
    return if (path.size == shaperPath.size) {
        shaperPath.size == shaperPath.count { path.contains(it) || path.contains(it.inverse()) }
    } else false
}

fun Number.format(disit: Int): String = String.format("%0${disit}d", this)