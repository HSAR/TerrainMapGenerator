package io.hsar.mapgenerator.commandgui.korge.launch

/**
 * Identifies the only launch-background generation allowed to publish tiles.
 * Starting a new segment invalidates all queued UI publications from earlier segments.
 */
internal class LaunchBackgroundGenerationSegments {
    private var activeSegment = 0L

    fun current(): Long = activeSegment

    fun replace(): Long = ++activeSegment

    fun isCurrent(segment: Long): Boolean = segment == activeSegment
}
