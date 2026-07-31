package io.hsar.mapgenerator.commandgui.compose

internal data class ComposeAutosaveObservation(
    val revision: Long,
    val savedRevision: Long,
    val savingRevision: Long?,
    val failed: Boolean,
    val exiting: Boolean,
) {
    val eligible get() = revision > savedRevision && savingRevision == null && !failed && !exiting
}

internal class ComposeAutosaveScheduler(
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val debounceMillis: Long = 2_000L,
) {
    private var scheduledRevision: Long? = null
    private var dueAtMillis: Long? = null

    fun observe(value: ComposeAutosaveObservation): Long? {
        if (!value.eligible) {
            cancel()
            return null
        }
        if (scheduledRevision != value.revision) {
            scheduledRevision = value.revision
            dueAtMillis = nowMillis() + debounceMillis
        }
        return dueAtMillis?.minus(nowMillis())?.coerceAtLeast(0L)
    }

    fun consumeIfDue(value: ComposeAutosaveObservation): Long? =
        scheduledRevision
            ?.takeIf { revision -> value.eligible && revision == value.revision && nowMillis() >= (dueAtMillis ?: Long.MAX_VALUE) }
            ?.also { cancel() }

    fun cancel() {
        scheduledRevision = null
        dueAtMillis = null
    }
}
