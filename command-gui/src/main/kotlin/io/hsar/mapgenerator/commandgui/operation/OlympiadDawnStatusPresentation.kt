package io.hsar.mapgenerator.commandgui.operation

internal object OlympiadDawnStatusPresentation {
    fun hullRowSizes(points: Int, rows: Int): List<Int> {
        if (rows <= 0) return emptyList()
        val pointsPerRow = points / rows
        val remainder = points % rows
        return List(rows) { row -> pointsPerRow + if (row < remainder) 1 else 0 }
    }

    fun fighterStatusText(fighterId: String, status: OlympiadDawnFighterLaunchStatus?): String =
        status?.let { if (it.destroyed) "DESTROYED" else "LAUNCHED" } ?: fighterId

    fun systemDisplayName(
        system: OlympiadDawnShipSystem,
        fighterLaunchStatuses: Map<String, OlympiadDawnFighterLaunchStatus>,
    ): String = if (system.fighterSquadronIds.any(fighterLaunchStatuses::containsKey)) "Hangar - Launched" else system.name
}
