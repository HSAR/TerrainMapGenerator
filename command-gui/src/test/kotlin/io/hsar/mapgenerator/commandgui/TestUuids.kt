package io.hsar.mapgenerator.commandgui

import java.util.*
import java.util.concurrent.atomic.AtomicLong

private val nextTestUuidValue = AtomicLong(1L)

internal fun nextTestUuid(): UUID = UUID(0L, nextTestUuidValue.getAndIncrement())
