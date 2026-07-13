package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShip
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShipDamage
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShipDefinition
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
        fightersCollapsed: Boolean = true,
    ): Double =
        if (collapsed) HEADER_HEIGHT else HEADER_HEIGHT + POINT_ROW_HEIGHT * (definition.hullRows + if (definition.armourPoints == null) 0 else 1) + sectionHeight(
            definition,
            componentsCollapsed,
            weaponsCollapsed,
            fightersCollapsed,
        )

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
        fightersCollapsed: Boolean = true,
        onFightersCollapsedChanged: () -> Unit = {},
        deployedFightersBySquadron: Map<String, Int> = emptyMap(),
        onDamageChanged: (OlympiadDawnShipDamage) -> Unit = {},
    ) {
        MapViewerCollapsiblePanel.header(parent, width, HEADER_HEIGHT, "Ship Status", !collapsed, onCollapsedChanged)
        if (collapsed) return

        var top = HEADER_HEIGHT
        definition.armourPoints?.let { armourPoints ->
            renderPoints(parent, top, width, "Armour", armourPoints, damage.destroyedArmour, editable) { index ->
                onDamageChanged(damage.copy(destroyedArmour = damage.destroyedArmour.toggle(index)))
            }
            top += POINT_ROW_HEIGHT
        }
        val hullRowSizes = hullRowSizes(definition.hullPoints, definition.hullRows)
        repeat(definition.hullRows) { row ->
            val start = hullRowSizes.take(row).sum()
            val count = hullRowSizes[row]
            renderPoints(parent, top, width, if (row == 0) "Hull" else "", count, damage.destroyedHull.map { it - start }.toSet(), editable) { index ->
                onDamageChanged(damage.copy(destroyedHull = damage.destroyedHull.toggle(start + index)))
            }
            top += POINT_ROW_HEIGHT
        }
        val sections = parent.container().also { it.xy(0.0, top) }
        val componentHeight = renderComponentSection(sections, width, definition, damage, editable, componentsCollapsed, onComponentsCollapsedChanged, onDamageChanged)
        val weaponHeight = renderWeaponSection(sections, width, definition, damage, editable, componentHeight, weaponsCollapsed, onWeaponsCollapsedChanged, onDamageChanged)
        renderFighterSection(
            sections,
            width,
            definition,
            damage,
            editable,
            componentHeight + weaponHeight,
            fightersCollapsed,
            onFightersCollapsedChanged,
            deployedFightersBySquadron,
            onDamageChanged
        )
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
        MapViewerCollapsiblePanel.header(section, width, HEADER_HEIGHT, MapViewerCountText.of(intactSystems, "system"), !collapsed, onCollapsedChanged)
        if (collapsed) return HEADER_HEIGHT
        var top = HEADER_HEIGHT
        definition.systems.forEach { system ->
            val destroyed = system.id in damage.destroyedSystems
            val row = section.container().also { it.xy(0.0, top) }
            MapViewerUiBoxes.box(row, width, SYSTEM_HEIGHT, active = !destroyed)
            MapViewerCheckbox.render(
                parent = row,
                x = CHECKBOX_INSET,
                y = (SYSTEM_HEIGHT - CHECKBOX_SIZE) * 0.5,
                size = CHECKBOX_SIZE,
                checked = !destroyed,
                onToggle = if (editable) {
                    { onDamageChanged(damage.copy(destroyedSystems = damage.destroyedSystems.toggle(system.id))) }
                } else {
                    {}
                },
            )
            MapViewerTextViews.leftBox(
                row,
                system.name,
                width - TEXT_INSET,
                SYSTEM_HEIGHT,
                color = if (destroyed) Palette.Colours.RED else Palette.Colours.WHITE,
            ) { x = TEXT_INSET }
            top += SYSTEM_HEIGHT
        }
        return top
    }

    private fun renderWeaponSection(
        parent: Container,
        width: Double,
        definition: OlympiadDawnShipDefinition,
        damage: OlympiadDawnShipDamage,
        editable: Boolean,
        top: Double,
        collapsed: Boolean,
        onCollapsedChanged: () -> Unit,
        onDamageChanged: (OlympiadDawnShipDamage) -> Unit,
    ): Double {
        val section = parent.container().also { it.xy(0.0, top) }
        val intactWeapons = definition.weapons.indices.count { index -> OlympiadDawnShip.weaponKey(definition, index) !in damage.destroyedWeapons }
        MapViewerCollapsiblePanel.header(section, width, HEADER_HEIGHT, MapViewerCountText.of(intactWeapons, "weapon"), !collapsed, onCollapsedChanged)
        if (collapsed) return HEADER_HEIGHT
        definition.weapons.forEachIndexed { index, weapon ->
            val row = section.container().also { it.xy(0.0, HEADER_HEIGHT + index * SYSTEM_HEIGHT) }
            val weaponKey = OlympiadDawnShip.weaponKey(definition, index)
            val destroyed = weaponKey in damage.destroyedWeapons
            MapViewerUiBoxes.box(row, width, SYSTEM_HEIGHT, active = !destroyed)
            MapViewerCheckbox.render(
                parent = row,
                x = CHECKBOX_INSET,
                y = (SYSTEM_HEIGHT - CHECKBOX_SIZE) * 0.5,
                size = CHECKBOX_SIZE,
                checked = !destroyed,
                onToggle = if (editable) {
                    { onDamageChanged(damage.copy(destroyedWeapons = damage.destroyedWeapons.toggle(weaponKey))) }
                } else {
                    {}
                },
            )
            val arcs = weapon.arcs.takeIf(List<String>::isNotEmpty)?.joinToString(", ")?.let { " [$it]" }.orEmpty()
            MapViewerTextViews.leftBox(row, weapon.name + arcs, width - TEXT_INSET, SYSTEM_HEIGHT, color = if (destroyed) Palette.Colours.RED else Palette.Colours.WHITE) {
                x = TEXT_INSET
            }
        }
        return HEADER_HEIGHT + definition.weapons.size * SYSTEM_HEIGHT
    }

    private fun renderFighterSection(
        parent: Container,
        width: Double,
        definition: OlympiadDawnShipDefinition,
        damage: OlympiadDawnShipDamage,
        editable: Boolean,
        top: Double,
        collapsed: Boolean,
        onCollapsedChanged: () -> Unit,
        deployedFightersBySquadron: Map<String, Int>,
        onDamageChanged: (OlympiadDawnShipDamage) -> Unit,
    ) {
        if (definition.fighters.isEmpty()) return
        val section = parent.container().also { it.xy(0.0, top) }
        val intactFighters = definition.fighters.indices.sumOf { OlympiadDawnShip.remainingFighters(definition, damage, it) }
        MapViewerCollapsiblePanel.header(section, width, HEADER_HEIGHT, MapViewerCountText.of(intactFighters, "fighter"), !collapsed, onCollapsedChanged)
        if (collapsed) return
        var fighterTop = HEADER_HEIGHT
        definition.fighters.forEachIndexed { index, fighter ->
            val remaining = OlympiadDawnShip.remainingFighters(definition, damage, index)
            val deployed = deployedFightersBySquadron[OlympiadDawnShip.fighterKey(fighter)] ?: 0
            val label = fighter.name + if (deployed > 0) " ($deployed deployed)" else ""
            renderPoints(section, fighterTop, width, label, fighter.count, (remaining until fighter.count).toSet(), editable) {
                val updatedRemaining = if (it < remaining) remaining - 1 else remaining + 1
                val key = OlympiadDawnShip.fighterKey(fighter)
                onDamageChanged(damage.copy(remainingFighters = damage.remainingFighters + (key to updatedRemaining.coerceIn(0, fighter.count))))
            }
            fighterTop += POINT_ROW_HEIGHT
        }
        OlympiadDawnShip.fighterWeaponTotals(definition, damage).forEach { weapon ->
            val row = section.container().also { it.xy(0.0, fighterTop) }
            MapViewerUiBoxes.box(row, width, SYSTEM_HEIGHT, active = weapon.count > 0)
            val arcs = weapon.arcs.takeIf(List<String>::isNotEmpty)?.joinToString(", ")?.let { " [$it]" }.orEmpty()
            MapViewerTextViews.leftBox(
                row,
                "${weapon.count} ${weapon.name}$arcs",
                width - CHECKBOX_INSET,
                SYSTEM_HEIGHT,
                color = if (weapon.count == 0) Palette.Colours.RED else Palette.Colours.WHITE
            ) {
                x = CHECKBOX_INSET
            }
            fighterTop += SYSTEM_HEIGHT
        }
    }

    private fun sectionHeight(definition: OlympiadDawnShipDefinition, componentsCollapsed: Boolean, weaponsCollapsed: Boolean, fightersCollapsed: Boolean): Double {
        val components = HEADER_HEIGHT + if (componentsCollapsed) 0.0 else definition.systems.size * SYSTEM_HEIGHT
        val weapons = HEADER_HEIGHT + if (weaponsCollapsed) 0.0 else definition.weapons.size * SYSTEM_HEIGHT
        val fighterWeapons = OlympiadDawnShip.fighterWeaponTotals(definition, OlympiadDawnShipDamage()).size
        val fighters =
            if (definition.fighters.isEmpty()) 0.0 else HEADER_HEIGHT + if (fightersCollapsed) 0.0 else definition.fighters.size * POINT_ROW_HEIGHT + fighterWeapons * SYSTEM_HEIGHT
        return components + weapons + fighters
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

    internal fun hullRowSizes(points: Int, rows: Int): List<Int> {
        if (rows <= 0) return emptyList()
        val pointsPerRow = points / rows
        val remainder = points % rows
        return List(rows) { row -> pointsPerRow + if (row < remainder) 1 else 0 }
    }

    private const val HEADER_HEIGHT = 34.0
    private const val POINT_ROW_HEIGHT = 30.0
    private const val SYSTEM_HEIGHT = 30.0
    private const val LABEL_WIDTH = 82.0
    private const val POINT_SIZE = 28.0
    private const val POINT_GAP = 3.0
    private const val POINT_INSET = 2.0
    private const val CHECKBOX_SIZE = 20.0
    private const val CHECKBOX_INSET = 6.0
    private const val TEXT_INSET = 32.0
}
