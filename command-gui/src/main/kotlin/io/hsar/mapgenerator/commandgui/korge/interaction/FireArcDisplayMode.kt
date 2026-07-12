package io.hsar.mapgenerator.commandgui.korge.interaction

internal enum class FireArcDisplayMode(val label: String) {
    AllUnits("All Units"),
    FriendliesOfSelected("Friendlies of Selected"),
    HostilesOfSelected("Hostiles of Selected"),
    SelectedOnly("Selected Only"),
    None("None");

    companion object {
        fun fromSettingsValue(value: String?): FireArcDisplayMode =
            entries.firstOrNull { it.label.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
                ?: AllUnits
    }
}

internal object FireArcSelectionResolver {
    fun <T> shouldDraw(
        mode: FireArcDisplayMode,
        selectedUnitIds: Set<T>,
        selectedAffiliations: List<UnitAffiliation?>,
        targetId: T,
        targetAffiliation: UnitAffiliation?,
    ): Boolean = when (mode) {
        FireArcDisplayMode.AllUnits -> true
        FireArcDisplayMode.None -> false
        FireArcDisplayMode.SelectedOnly,
        FireArcDisplayMode.FriendliesOfSelected,
        FireArcDisplayMode.HostilesOfSelected,
            -> if (selectedAffiliations.size != 1) {
            false
        } else {
            val selectedAffiliation = selectedAffiliations[0]
            when (mode) {
                FireArcDisplayMode.SelectedOnly -> targetId in selectedUnitIds
                FireArcDisplayMode.FriendliesOfSelected -> targetAffiliation == selectedAffiliation
                FireArcDisplayMode.HostilesOfSelected -> targetAffiliation.isHostileTo(selectedAffiliation)
                else -> false
            }
        }
    }

    private fun UnitAffiliation?.isHostileTo(selected: UnitAffiliation?): Boolean =
        when (selected) {
            UnitAffiliation.Friend -> this == UnitAffiliation.Hostile
            UnitAffiliation.Hostile -> this == UnitAffiliation.Friend
            UnitAffiliation.Neutral,
            UnitAffiliation.Unknown,
            null,
                -> false
        }
}
