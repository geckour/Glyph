package jp.org.example.geckour.glyph.util

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

fun Number.format(disit: Int): String = String.format("%0${disit}d", this)