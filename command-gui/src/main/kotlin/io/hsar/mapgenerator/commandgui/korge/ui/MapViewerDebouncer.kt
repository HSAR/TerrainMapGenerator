package io.hsar.mapgenerator.commandgui.korge.ui

internal class MapViewerDebouncer<T>(
    private val delayMilliseconds: Long,
    private val nowMilliseconds: () -> Long = System::currentTimeMillis,
    private val onReady: (T) -> Unit,
) {
    private var pending: T? = null
    private var readyAtMilliseconds = Long.MAX_VALUE

    fun schedule(value: T) {
        pending = value
        readyAtMilliseconds = nowMilliseconds() + delayMilliseconds
    }

    fun flush() {
        pending
            ?.takeIf { nowMilliseconds() >= readyAtMilliseconds }
            ?.also {
                pending = null
                readyAtMilliseconds = Long.MAX_VALUE
            }
            ?.let(onReady)
    }

    fun cancel() {
        pending = null
        readyAtMilliseconds = Long.MAX_VALUE
    }
}
