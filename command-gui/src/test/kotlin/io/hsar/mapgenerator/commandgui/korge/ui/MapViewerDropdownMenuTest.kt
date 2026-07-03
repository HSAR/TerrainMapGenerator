package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.image.Palette
import korlibs.math.geom.Vector2D
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test

class MapViewerDropdownMenuTest {

    @Test
    fun `menu bounds clamp to the viewport edges`() {
        val bounds = MapViewerDropdownLayoutBuilder.menuBounds(
            anchor = Vector2D(12.0, 18.0),
            triggerHeight = 32.0,
            viewportWidth = 320,
            viewportHeight = 240,
            menuWidth = 200.0,
            menuHeight = 172.0,
        )

        assertThat(bounds.left, equalTo(12.0))
        assertThat(bounds.top, equalTo(56.0))
        assertThat(bounds.right <= 320.0, equalTo(true))
        assertThat(bounds.bottom <= 240.0, equalTo(true))
    }

    @Test
    fun `controller selection closes the menu and notifies`() {
        val selections = mutableListOf<UnitAffiliation?>()
        val controller = MapViewerDropdownController<UnitAffiliation> { selections += it }

        controller.open(ScreenBounds(left = 20.0, top = 40.0, width = 200.0, height = 172.0))
        controller.select(UnitAffiliation.Hostile)

        assertThat(controller.selectedValue, equalTo(UnitAffiliation.Hostile))
        assertThat(controller.isOpen, equalTo(false))
        assertThat(controller.menuBounds, equalTo(null))
        assertThat(selections, equalTo(listOf(UnitAffiliation.Hostile)))
    }

    @Test
    fun `controller selection can clear affiliation`() {
        val selections = mutableListOf<UnitAffiliation?>()
        val controller = MapViewerDropdownController<UnitAffiliation> { selections += it }

        controller.open(ScreenBounds(left = 20.0, top = 40.0, width = 200.0, height = 172.0))
        controller.select(null)

        assertThat(controller.selectedValue, equalTo(null))
        assertThat(controller.isOpen, equalTo(false))
        assertThat(selections, equalTo(listOf(null)))
    }

    @Test
    fun `controller dismisses without changing selection`() {
        val controller = MapViewerDropdownController<UnitAffiliation> { }

        controller.open(ScreenBounds(left = 20.0, top = 40.0, width = 200.0, height = 172.0))
        controller.dismiss()

        assertThat(controller.isOpen, equalTo(false))
        assertThat(controller.menuBounds, equalTo(null))
    }

    @Test
    fun `affiliation ui exposes the expected labels and colours`() {
        assertThat(
            UnitAffiliationUi.options.map { it.label },
            equalTo(listOf("No Affiliation", "Friend", "Hostile", "Neutral", "Unknown")),
        )
        assertThat(UnitAffiliationUi.label(null), equalTo("No Affiliation"))
        assertThat(UnitAffiliationUi.label(UnitAffiliation.Unknown), equalTo("Unknown"))
        assertThat(UnitAffiliationUi.swatchColor(null), equalTo(Palette.Colours.MAIN))
        assertThat(UnitAffiliationUi.swatchColor(UnitAffiliation.Friend), equalTo(Palette.Colours.BLUE))
        assertThat(UnitAffiliationUi.swatchColor(UnitAffiliation.Hostile), equalTo(Palette.Colours.RED))
        assertThat(UnitAffiliationUi.swatchColor(UnitAffiliation.Neutral), equalTo(Palette.Colours.GREEN))
        assertThat(UnitAffiliationUi.swatchColor(UnitAffiliation.Unknown), equalTo(Palette.Colours.YELLOW))
    }

    @Test
    fun `minimum menu width grows with the longest label`() {
        val shortWidth = MapViewerDropdownLayoutBuilder.minimumMenuWidth(listOf("A"))
        val longWidth = MapViewerDropdownLayoutBuilder.minimumMenuWidth(listOf("A much longer option"))

        assertThat(longWidth, greaterThan(shortWidth))
    }

    @Test
    fun `minimum menu width can omit icon spacing`() {
        val labels = listOf("MGRS", "Epsilon")
        val iconWidth = MapViewerDropdownLayoutBuilder.minimumMenuWidth(labels, hasIcons = true)
        val iconlessWidth = MapViewerDropdownLayoutBuilder.minimumMenuWidth(labels, hasIcons = false)

        assertThat(iconlessWidth, equalTo(iconWidth - 20.0))
    }
}
