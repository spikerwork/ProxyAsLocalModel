package io.github.stream29.proxy.client

fun <T> List<T>.mergeBy(
    selector: (T) -> String,
    merger: (T, T) -> T,
): List<T> {
    val result = mutableListOf<T>()
    var lastItem: T? = null
    for (item in this) {
        result.add(
            if (lastItem != null && selector(lastItem) == selector(item)) {
                merger(lastItem, item).also { lastItem = it }
            } else {
                lastItem = item
                item
            }
        )
    }
    return result
}