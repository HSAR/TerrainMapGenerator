package io.hsar.mapgenerator.commandgui.korge.ui

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.save.OperationSaveJson
import io.hsar.mapgenerator.image.Palette
import korlibs.event.MouseButton
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy
import korlibs.math.geom.Vector2D
import java.awt.Color

internal data class MapViewerUniversalListItem(
    val title: String,
    val subtitle: String? = null,
    val bitmap: Bitmap32? = null,
    val swatch: Color? = null,
    val trailingLabel: String? = null,
    val selected: Boolean = false,
)

internal data class MapViewerUnitAffiliationGroup(
    val affiliation: UnitAffiliation?,
    val label: String,
)

internal object MapViewerUniversalList {
    const val DEFAULT_ROW_HEIGHT = 44.0

    val unitAffiliationGroups = listOf(
        MapViewerUnitAffiliationGroup(UnitAffiliation.Friend, "Friend"),
        MapViewerUnitAffiliationGroup(UnitAffiliation.Hostile, "Hostile"),
        MapViewerUnitAffiliationGroup(UnitAffiliation.Neutral, "Neutral"),
        MapViewerUnitAffiliationGroup(UnitAffiliation.Unknown, "Unknown"),
        MapViewerUnitAffiliationGroup(null, "Unaffiliated"),
    )

    fun <T> renderUnitAffiliationGroups(
        parent: Container,
        width: Double,
        items: List<T>,
        affiliationOf: (T) -> UnitAffiliation?,
        collapsedGroups: MutableSet<UnitAffiliation?>,
        rowHeight: Double,
        rowGap: Double,
        headerHeight: Double,
        headerGap: Double,
        ungroupedSectionGap: Double = DEFAULT_UNGROUPED_SECTION_GAP,
        renderRow: (T, Double) -> Unit,
        onGroupsChanged: () -> Unit,
    ): Double {
        var top = 0.0
        var previousWasGroupedSection = false
        unitAffiliationGroups.forEach { group ->
            val groupItems = items.filter { affiliationOf(it) == group.affiliation }
            if (groupItems.isEmpty()) return@forEach
            if (groupItems.size < 2) {
                if (previousWasGroupedSection) top += ungroupedSectionGap
                renderRow(groupItems.single(), top)
                top += rowHeight + rowGap
                previousWasGroupedSection = false
                return@forEach
            }
            val collapsed = group.affiliation in collapsedGroups
            MapViewerCollapsiblePanel.header(
                parent = parent,
                width = width,
                height = headerHeight,
                label = group.label,
                expanded = !collapsed,
            ) {
                if (collapsed) collapsedGroups.remove(group.affiliation) else collapsedGroups.add(group.affiliation)
                onGroupsChanged()
            }.xy(0.0, top)
            top += headerHeight + headerGap
            if (!collapsed) {
                groupItems.forEach { item ->
                    renderRow(item, top)
                    top += rowHeight + rowGap
                }
            }
            previousWasGroupedSection = true
        }
        return top
    }

    fun <T> unitAffiliationGroupsHeight(
        items: List<T>,
        affiliationOf: (T) -> UnitAffiliation?,
        collapsedGroups: Set<UnitAffiliation?>,
        rowHeight: Double,
        rowGap: Double,
        headerHeight: Double,
        headerGap: Double,
        ungroupedSectionGap: Double = DEFAULT_UNGROUPED_SECTION_GAP,
    ): Double =
        unitAffiliationGroups.fold(Pair(0.0, false)) { (height, previousWasGroupedSection), group ->
            val count = items.count { affiliationOf(it) == group.affiliation }
            when {
                count == 0 -> height to previousWasGroupedSection
                count < 2 -> {
                    val sectionGap = if (previousWasGroupedSection) ungroupedSectionGap else 0.0
                    height + sectionGap + count * (rowHeight + rowGap) to false
                }

                else -> {
                    val itemsHeight = if (group.affiliation in collapsedGroups) 0.0 else count * (rowHeight + rowGap)
                    height + headerHeight + headerGap + itemsHeight to true
                }
            }
        }.first

    fun <T> visibleUnitAffiliationRowPositions(
        items: List<T>,
        affiliationOf: (T) -> UnitAffiliation?,
        collapsedGroups: Set<UnitAffiliation?>,
        rowHeight: Double,
        rowGap: Double,
        headerHeight: Double,
        headerGap: Double,
        ungroupedSectionGap: Double = DEFAULT_UNGROUPED_SECTION_GAP,
    ): List<Pair<T, Double>> {
        var top = 0.0
        var previousWasGroupedSection = false
        val positions = mutableListOf<Pair<T, Double>>()
        unitAffiliationGroups.forEach { group ->
            val groupItems = items.filter { affiliationOf(it) == group.affiliation }
            if (groupItems.isEmpty()) return@forEach
            if (groupItems.size < 2) {
                if (previousWasGroupedSection) top += ungroupedSectionGap
                positions += groupItems.single() to top
                top += rowHeight + rowGap
                previousWasGroupedSection = false
                return@forEach
            }
            top += headerHeight + headerGap
            if (group.affiliation !in collapsedGroups) {
                groupItems.forEach { item ->
                    positions += item to top
                    top += rowHeight + rowGap
                }
            }
            previousWasGroupedSection = true
        }
        return positions
    }

    fun <S> collapseStateDraft(
        collapsedSections: Set<S>,
        sectionKey: (S) -> String,
    ): ObjectNode =
        OperationSaveJson.objectMapper.createObjectNode().apply {
            putArray(COLLAPSED_SECTIONS_FIELD).also { sections ->
                collapsedSections.map(sectionKey).sorted().forEach(sections::add)
            }
        }

    fun <S> restoreCollapsedSections(
        draft: JsonNode?,
        sectionFromKey: (String) -> S?,
    ): Set<S> =
        draft
            ?.path(COLLAPSED_SECTIONS_FIELD)
            ?.takeIf(JsonNode::isArray)
            ?.mapNotNull { node -> node.textValue()?.let(sectionFromKey) }
            ?.toSet()
            .orEmpty()

    /**
     * Collapses sections until their content fits [availableHeight]. A supplied
     * [collapseOrder] takes precedence; otherwise sections collapse from the
     * end of [sections] backwards.
     */
    fun <S> collapseSectionsToFit(
        sections: List<S>,
        collapsedSections: MutableSet<S>,
        availableHeight: Double,
        contentHeight: () -> Double,
        collapseOrder: List<S>? = null,
    ) {
        val orderedSections = (collapseOrder ?: sections.asReversed())
            .filter { it in sections }
            .distinct()

        orderedSections
            .asSequence()
            .filter { it !in collapsedSections }
            .takeWhile { contentHeight() > availableHeight }
            .forEach(collapsedSections::add)
    }

    fun renderItem(
        parent: Container,
        item: MapViewerUniversalListItem,
        width: Double,
        top: Double,
        height: Double = DEFAULT_ROW_HEIGHT,
        onClick: (() -> Unit)? = null,
        onDown: ((MouseButton, Vector2D) -> Unit)? = null,
    ): Container = parent.container().also { row ->
        row.xy(0.0, top)
        MapViewerUiBoxes.box(row, width, height, style = { active -> MapViewerUiBoxes.defaultStyle(active && item.selected) })
        item.bitmap?.let { bitmap -> renderPreview(row, bitmap, height) }
        val textLeft = if (item.bitmap == null) INSET else PREVIEW_LEFT + PREVIEW_SIZE + TEXT_GAP
        val trailingWidth = when {
            item.swatch != null -> SWATCH_SIZE + INSET * 2.0
            item.trailingLabel != null -> TRAILING_LABEL_WIDTH + INSET
            else -> INSET
        }
        val textWidth = (width - textLeft - trailingWidth).coerceAtLeast(0.0)
        MapViewerTextViews.leftBox(
            parent = row,
            text = MapViewerTextViews.ellipsize(item.title, textWidth),
            width = textWidth,
            height = if (item.subtitle == null) height else 28.0,
            color = MapViewerUiStateStyle.text(item.selected),
        ) {
            x = textLeft
            y = if (item.subtitle == null) 0.0 else 5.0
        }
        item.subtitle?.let { subtitle ->
            MapViewerTextViews.leftBox(
                parent = row,
                text = MapViewerTextViews.ellipsize(subtitle, textWidth, size = 18.0),
                width = textWidth,
                height = 22.0,
                size = 18.0,
                color = Palette.Colours.LIGHT,
            ) {
                x = textLeft
                y = 33.0
            }
        }
        item.swatch?.let { color ->
            row.container().also { swatch ->
                swatch.xy(width - SWATCH_SIZE - INSET, (height - SWATCH_SIZE) * 0.5)
                MapViewerSwatchViews.render(swatch, color, SWATCH_SIZE)
            }
        }
        item.trailingLabel?.let { label ->
            MapViewerTextViews.box(
                parent = row,
                text = label,
                width = TRAILING_LABEL_WIDTH,
                height = height,
                role = MapViewerFontRole.FLAVOUR,
                size = 18.0,
                color = Palette.Colours.LIGHT,
            ) { x = width - TRAILING_LABEL_WIDTH - INSET }
        }
        if (onClick != null || onDown != null) {
            row.mouse {
                onClick { onClick?.invoke() }
                onDown { onDown?.invoke(button, currentPosGlobal) }
            }
        }
    }

    private fun renderPreview(parent: Container, bitmap: Bitmap32, height: Double) {
        val previewSize = minOf(PREVIEW_SIZE, (height - PREVIEW_INSET * 2.0).coerceAtLeast(0.0))
        val preview = parent.container()
        preview.xy(PREVIEW_LEFT, (height - previewSize) * 0.5)
        MapViewerUiBoxes.box(preview, previewSize, previewSize)
        val fitted = MapViewerIconFit.inside(bitmap, previewSize - PREVIEW_INSET * 2.0, previewSize - PREVIEW_INSET * 2.0)
        MapViewerImageViews.icon(preview, bitmap) {
            scale = fitted.scale
            x = fitted.x + PREVIEW_INSET
            y = fitted.y + PREVIEW_INSET
        }
    }

    private const val INSET = 10.0
    private const val PREVIEW_LEFT = 8.0
    private const val PREVIEW_SIZE = 46.0
    private const val PREVIEW_INSET = 3.0
    private const val TEXT_GAP = 10.0
    private const val SWATCH_SIZE = 24.0
    private const val TRAILING_LABEL_WIDTH = 54.0
    private const val COLLAPSED_SECTIONS_FIELD = "collapsedSections"
    private const val DEFAULT_UNGROUPED_SECTION_GAP = 8.0
}
