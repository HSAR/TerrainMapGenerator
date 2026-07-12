package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShipDamage
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShipDefinition
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShipWeapon
import io.hsar.mapgenerator.image.Palette
import korlibs.image.color.RGBA
import korlibs.korge.input.mouse
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy

internal object OlympiadDawnShipStatusPanel {
    fun height(
        definition: OlympiadDawnShipDefinition,
        collapsed: Boolean,
        componentsCollapsed: Boolean = true,
        weaponsCollapsed: Boolean = true,
    ): Double =
        if (collapsed) HEADER_HEIGHT else HEADER_HEIGHT + POINT_ROW_HEIGHT * (definition.hullRows + 1) + sectionHeight(definition, componentsCollapsed, weaponsCollapsed)

    fun render(
        parent: Container,
        width: Double,
        definition: OlympiadDawnShipDefinition,
        damage: OlympiadDawnShipDamage,
        collapsed: Boolean,
        editable: Boolean,
        onCollapsedChanged: () -> Unit,
        componentsCollapsed: Boolean = true,
        onComponentsCollapsedChanged: () -> Unit = {},
        weaponsCollapsed: Boolean = true,
        onWeaponsCollapsedChanged: () -> Unit = {},
        onDamageChanged: (OlympiadDawnShipDamage) -> Unit = {},
    ) {
        MapViewerCollapsiblePanel.header(parent, width, HEADER_HEIGHT, "Ship Status", !collapsed, onCollapsedChanged)
        if (collapsed) return

        var top = HEADER_HEIGHT
        renderPoints(parent, top, width, "Armour", definition.armourPoints, damage.destroyedArmour, editable) { index ->
            onDamageChanged(damage.copy(destroyedArmour = damage.destroyedArmour.toggle(index)))
        }
        top += POINT_ROW_HEIGHT
        val pointsPerRow = (definition.hullPoints + definition.hullRows - 1) / definition.hullRows
        repeat(definition.hullRows) { row ->
            val start = row * pointsPerRow
            val count = (definition.hullPoints - start).coerceIn(0, pointsPerRow)
            renderPoints(parent, top, width, if (row == 0) "Hull" else "", count, damage.destroyedHull.map { it - start }.toSet(), editable) { index ->
                onDamageChanged(damage.copy(destroyedHull = damage.destroyedHull.toggle(start + index)))
            }
            top += POINT_ROW_HEIGHT
        }
        val sections = parent.container().also { it.xy(0.0, top) }
        val componentHeight = renderComponentSection(sections, width, definition, damage, editable, componentsCollapsed, onComponentsCollapsedChanged, onDamageChanged)
        renderWeaponSection(sections, width, definition.weapons, componentHeight, weaponsCollapsed, onWeaponsCollapsedChanged)
    }

    private fun renderComponentSection(
        parent: Container,
        width: Double,
        definition: OlympiadDawnShipDefinition,
        damage: OlympiadDawnShipDamage,
        editable: Boolean,
        collapsed: Boolean,
        onCollapsedChanged: () -> Unit,
        onDamageChanged: (OlympiadDawnShipDamage) -> Unit,
    ): Double {
        val section = parent.container()
        val intactSystems = definition.systems.count { it.id !in damage.destroyedSystems }
        MapViewerCollapsiblePanel.header(section, width, HEADER_HEIGHT, "$intactSystems systems", !collapsed, onCollapsedChanged)
        if (collapsed) return HEADER_HEIGHT
        var top = HEADER_HEIGHT
        definition.systems.forEach { system ->
            val destroyed = system.id in damage.destroyedSystems
            val row = section.container().also { it.xy(0.0, top) }
            val rowButton = MapViewerUiBoxes.box(row, width, SYSTEM_HEIGHT, active = !destroyed)
            MapViewerTextViews.leftBox(
                row,
                "${if (destroyed) "X" else "O"}  ${system.name}",
                width,
                SYSTEM_HEIGHT,
                color = if (destroyed) Palette.Colours.RED else Palette.Colours.WHITE
            )
            if (editable) rowButton.mouse { onClick { onDamageChanged(damage.copy(destroyedSystems = damage.destroyedSystems.toggle(system.id))) } }
            top += SYSTEM_HEIGHT
        }
        return top
    }

    private fun renderWeaponSection(parent: Container, width: Double, weapons: List<OlympiadDawnShipWeapon>, top: Double, collapsed: Boolean, onCollapsedChanged: () -> Unit) {
        val section = parent.container().also { it.xy(0.0, top) }
        MapViewerCollapsiblePanel.header(section, width, HEADER_HEIGHT, "${weapons.size} weapons", !collapsed, onCollapsedChanged)
        if (collapsed) return
        weapons.forEachIndexed { index, weapon ->
            val row = section.container().also { it.xy(0.0, HEADER_HEIGHT + index * SYSTEM_HEIGHT) }
            MapViewerUiBoxes.box(row, width, SYSTEM_HEIGHT)
            MapViewerTextViews.leftBox(row, weapon.name, width, SYSTEM_HEIGHT, color = Palette.Colours.WHITE)
        }
    }

    private fun sectionHeight(definition: OlympiadDawnShipDefinition, componentsCollapsed: Boolean, weaponsCollapsed: Boolean): Double {
        val components = HEADER_HEIGHT + if (componentsCollapsed) 0.0 else definition.systems.size * SYSTEM_HEIGHT
        val weapons = HEADER_HEIGHT + if (weaponsCollapsed) 0.0 else definition.weapons.size * SYSTEM_HEIGHT
        return components + weapons
    }

    private fun renderPoints(
        parent: Container,
        top: Double,
        width: Double,
        label: String,
        count: Int,
        destroyed: Set<Int>,
        editable: Boolean,
        onToggle: (Int) -> Unit,
    ) {
        MapViewerTextViews.leftBox(parent, label, LABEL_WIDTH, POINT_ROW_HEIGHT, color = Palette.Colours.WHITE) { y = top }
        val pointWidth = ((width - LABEL_WIDTH) / count.coerceAtLeast(1)).coerceAtMost(POINT_SIZE)
        repeat(count) { index ->
            val point = parent.container().also { it.xy(LABEL_WIDTH + index * pointWidth, top + POINT_INSET) }
            val pointButton = MapViewerUiBoxes.box(point, pointWidth - POINT_GAP, POINT_SIZE - POINT_INSET * 2.0, active = index !in destroyed)
            if (index in destroyed) point.solidRect(pointWidth - POINT_GAP, POINT_SIZE - POINT_INSET * 2.0, RGBA(120, 20, 20, 255))
            if (editable) pointButton.mouse { onClick { onToggle(index) } }
        }
    }

    private fun <T> Set<T>.toggle(value: T): Set<T> =
        if (value in this) this - value else this + value

    private const val HEADER_HEIGHT = 34.0
    private const val POINT_ROW_HEIGHT = 30.0
    private const val SYSTEM_HEIGHT = 30.0
    private const val LABEL_WIDTH = 82.0
    private const val POINT_SIZE = 28.0
    private const val POINT_GAP = 3.0
    private const val POINT_INSET = 2.0
}
