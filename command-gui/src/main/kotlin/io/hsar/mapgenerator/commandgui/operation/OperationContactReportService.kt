package io.hsar.mapgenerator.commandgui.operation

import io.hsar.mapgenerator.grid.GridLayer

internal enum class OperationContactReportMode(
    val emptySummaryText: String,
    val sensorMode: OperationSensorReportMode,
) {
    SCIREP("No hostile contacts to report.", OperationSensorReportMode.SCIREP),
    NAVREP("No allied contacts to report.", OperationSensorReportMode.NAVREP),
}

internal data class OperationContactReportRequest(
    val observer: OperationUnit,
    val queryService: OperationUnitQueryService,
    val mode: OperationContactReportMode,
    val currentState: OperationSitrepContactState = OperationSitrepContactState(),
    val previousState: OperationSitrepContactState = OperationSitrepContactState(),
    val selectedContactIds: Set<OperationObjectId> = currentState.sensorContactIds,
    val selectedIdentifiedIds: Set<OperationObjectId> = currentState.identifiedIds,
    val ecmIntensity: Int = 0,
    val gridLayer: GridLayer? = null,
)

internal data class OperationContactReportResult(
    val assessment: OperationSensorAssessment,
    val contactState: OperationSitrepContactState,
)

internal object OperationContactReportService {
    fun generate(request: OperationContactReportRequest): OperationContactReportResult = when (request.mode) {
        OperationContactReportMode.SCIREP -> scirep(request)
        OperationContactReportMode.NAVREP -> navrep(request)
    }

    private fun scirep(request: OperationContactReportRequest): OperationContactReportResult {
        val content = request.queryService.content
        val observerAffiliation = OperationUnitPresentation.affiliation(request.observer, content)
        val natural = assessment(request)
        val trackable = natural.trackableChecks().associateBy { it.unit.id }
        val activeChecks = trackable.values.filterNot { it.unit.destroyed }
        val activeIds = activeChecks.mapTo(linkedSetOf()) { it.unit.id }
        val ecmIds = activeChecks.filter { it.ecmContact != OperationEcmSensorContact.NONE }.mapTo(hashSetOf()) { it.unit.id }
        val identifiedIds = (request.selectedIdentifiedIds + request.currentState.identifiedIds + natural.visibleChecks.map { it.unit.id })
            .intersect(activeIds) - ecmIds

        val contactNumbers = request.currentState.sierraContactNumbers.filterKeys { it in activeIds }.toMutableMap()
        val usedNumbers = (request.currentState.usedSierraContactNumbers + request.currentState.sierraContactNumbers.values +
                request.previousState.usedSierraContactNumbers + request.previousState.sierraContactNumbers.values).toMutableSet()
        val matchedLostIds = mutableSetOf<OperationObjectId>()
        val lowConfidence = mutableMapOf<OperationObjectId, OperationLostContact>()
        val availableLost = (request.previousState.lostContacts + request.currentState.lostContacts).distinctBy { it.contactId }

        activeChecks.filter { it.unit.id !in contactNumbers }.forEach { check ->
            availableLost.asSequence()
                .filter { it.contactId !in matchedLostIds }
                .filter { request.queryService.distanceBetween(check.unit.coordinate, it.lastSeenPosition) <= LOW_CONFIDENCE_DISTANCE_UNITS }
                .minByOrNull { request.queryService.distanceBetween(check.unit.coordinate, it.lastSeenPosition) }
                ?.let { lost ->
                    contactNumbers[check.unit.id] = lost.sierraNumber
                    usedNumbers += lost.sierraNumber
                    matchedLostIds += lost.contactId
                    lowConfidence[check.unit.id] = lost
                }
        }
        var nextNumber = (usedNumbers.maxOrNull() ?: 0) + 1
        activeChecks.sortedWith(compareBy<OperationSensorVisibilityCheck> { it.distanceUnits }.thenBy { it.unit.id.value }).forEach { check ->
            if (check.unit.id !in contactNumbers) {
                while (nextNumber in usedNumbers) nextNumber++
                contactNumbers[check.unit.id] = nextNumber
                usedNumbers += nextNumber++
            }
        }

        val newlyLost = request.previousState.sensorContactIds.filter { it !in activeIds }.mapNotNull { contactId ->
            val currentCheck = trackable[contactId]
            request.previousState.lastSeenContacts[contactId]?.withDestructionState(currentCheck, observerAffiliation, content)
                ?: request.previousState.lastSeenPositions[contactId]?.let { position ->
                    request.previousState.sierraContactNumbers[contactId]?.let { number ->
                        OperationLostContact(
                            contactId = contactId,
                            sierraNumber = number,
                            lastSeenPosition = position,
                            lastSeenRangeUnits = 0.0,
                            wasIdentified = contactId in request.previousState.identifiedIds,
                        )
                    }
                }
        }
        val historicalLost = (request.previousState.lostContacts + request.currentState.lostContacts + newlyLost)
            .filterNot { it.contactId in matchedLostIds }
            .distinctBy { it.contactId }
        val currentLastSeen = activeChecks.associate { check ->
            val affiliation = OperationUnitPresentation.affiliation(check.unit, content)
            check.unit.id to OperationLostContact(
                contactId = check.unit.id,
                sierraNumber = contactNumbers.getValue(check.unit.id),
                lastSeenPosition = check.unit.coordinate,
                lastSeenRangeUnits = check.distanceUnits,
                wasIdentified = check.unit.id in identifiedIds,
                wasFriendly = observerAffiliation != null && affiliation == observerAffiliation,
                identifiedAffiliation = affiliationDescription(affiliation, observerAffiliation),
                unitTypeName = content.unitTemplate(check.unit.unitTemplateId)?.name,
                unitClassCode = OperationUnitPresentation.unitClass(check.unit, content),
                unitInstanceName = OperationUnitPresentation.displayName(check.unit, content)
                    .takeIf { it != content.unitTemplate(check.unit.unitTemplateId)?.name },
            )
        }
        val state = OperationSitrepContactState(
            sensorContactIds = activeIds,
            identifiedIds = identifiedIds,
            sierraContactNumbers = contactNumbers,
            usedSierraContactNumbers = usedNumbers,
            lastSeenPositions = currentLastSeen.mapValues { it.value.lastSeenPosition },
            lastSeenContacts = currentLastSeen,
            lostContacts = historicalLost,
        )
        val selected = OperationSensorService.visibilityReport(
            observer = request.observer,
            unitQueryService = request.queryService,
            gridLayer = request.gridLayer,
            includedIds = identifiedIds,
            unidentifiedIds = activeIds - identifiedIds,
            candidateFilter = { true },
            emptySummaryText = request.mode.emptySummaryText,
            sensorReportMode = request.mode.sensorMode,
            ecmIntensity = request.ecmIntensity,
        )
        val displayed = selected.copy(
            visibleChecks = selected.visibleChecks.filter { OperationUnitPresentation.affiliation(it.unit, content) != observerAffiliation },
            hiddenChecks = selected.hiddenChecks.filter { OperationUnitPresentation.affiliation(it.unit, content) != observerAffiliation },
        )
        val enemyChecks = (displayed.visibleChecks + displayed.unidentifiedChecks)
            .filter { OperationUnitPresentation.affiliation(it.unit, content) != observerAffiliation }
            .sortedBy { contactNumbers.getValue(it.unit.id) }
        val reportText = OperationSensorService.scirepText(
            checks = enemyChecks,
            identifiedIds = displayed.visibleChecks.mapTo(hashSetOf()) { it.unit.id },
            contactNumbers = contactNumbers,
            content = content,
            mapWidth = request.queryService.mapWidth,
            mapHeight = request.queryService.mapHeight,
            gridLayer = request.gridLayer,
            emptySummaryText = request.mode.emptySummaryText,
            observerAffiliation = observerAffiliation,
            identificationAnnouncements = displayed.visibleChecks.filter { it.unit.id !in request.previousState.identifiedIds },
            previouslyDetectedIds = request.previousState.sensorContactIds,
            lostContacts = newlyLost.filter { it.contactId !in matchedLostIds && !(it.wasIdentified && it.wasFriendly) },
            lowConfidenceContacts = lowConfidence,
            sensorConditionsPoor = request.observer.customProperties.booleanProperty("inNebula"),
            electronicWarfare = request.ecmIntensity > 0,
        )
        return OperationContactReportResult(displayed.copy(displayText = reportText, copyText = reportText), state)
    }

    private fun navrep(request: OperationContactReportRequest): OperationContactReportResult {
        val content = request.queryService.content
        val observerAffiliation = OperationUnitPresentation.affiliation(request.observer, content)
        val natural = assessment(request)
        val activeChecks = natural.trackableChecks().filterNot { it.unit.destroyed }
        val activeIds = activeChecks.mapTo(linkedSetOf()) { it.unit.id }
        val identifiedIds = (request.selectedIdentifiedIds + request.currentState.identifiedIds + natural.visibleChecks.map { it.unit.id })
            .intersect(activeIds)
        val numbers = request.currentState.sierraContactNumbers.filterKeys { it in activeIds }.toMutableMap()
        val used = (request.currentState.usedSierraContactNumbers + request.currentState.sierraContactNumbers.values).toMutableSet()
        var next = (used.maxOrNull() ?: 0) + 1
        activeChecks.forEach { check ->
            if (check.unit.id !in numbers) {
                while (next in used) next++
                numbers[check.unit.id] = next
                used += next++
            }
        }
        val report = OperationSensorService.visibilityReport(
            observer = request.observer,
            unitQueryService = request.queryService,
            gridLayer = request.gridLayer,
            includedIds = identifiedIds,
            unidentifiedIds = activeIds - identifiedIds,
            candidateFilter = { OperationUnitPresentation.affiliation(it, content) == observerAffiliation },
            emptySummaryText = request.mode.emptySummaryText,
            sensorReportMode = request.mode.sensorMode,
            ecmIntensity = request.ecmIntensity,
        )
        val lost = request.previousState.sensorContactIds.filter { it !in activeIds }.mapNotNull { id ->
            request.previousState.lastSeenContacts[id]?.withDestructionState(natural.trackableChecks().firstOrNull { it.unit.id == id }, observerAffiliation, content)
        }.filter { it.wasIdentified && it.wasFriendly }
        val identificationText = OperationSensorService.navrepIdentificationText(
            observer = request.observer,
            checks = report.visibleChecks,
            contactNumbers = numbers,
            content = content,
            mapWidth = request.queryService.mapWidth,
            mapHeight = request.queryService.mapHeight,
            gridLayer = request.gridLayer,
            previouslyDetectedIds = request.previousState.sensorContactIds,
            previouslyIdentifiedIds = request.previousState.identifiedIds,
        )
        val observerPosition = OperationSensorService.navrepObserverPositionText(
            request.observer, request.queryService.mapWidth, request.queryService.mapHeight, request.gridLayer,
        )
        val conditions = report.displayText.substringBefore(observerPosition).trim()
        val lostText = lost.joinToString("\n") { "${OperationSensorService.lostContactText(it)}." }
        val summary = if (report.visibleChecks.isEmpty() && lost.isEmpty()) report.displayText
        else listOf(conditions, observerPosition).filter(String::isNotBlank).joinToString("\n")
        val text = listOf(summary, identificationText, lostText).filter(String::isNotBlank).joinToString("\n")
        val state = request.currentState.copy(
            sensorContactIds = activeIds,
            identifiedIds = identifiedIds,
            sierraContactNumbers = numbers,
            usedSierraContactNumbers = used,
        )
        return OperationContactReportResult(report.copy(unidentifiedChecks = emptyList(), displayText = text, copyText = text), state)
    }

    private fun assessment(request: OperationContactReportRequest) = OperationSensorService.visibilityReport(
        observer = request.observer,
        unitQueryService = request.queryService,
        gridLayer = request.gridLayer,
        candidateFilter = when (request.mode) {
            OperationContactReportMode.SCIREP -> { _ -> true }
            OperationContactReportMode.NAVREP -> { candidate ->
                OperationUnitPresentation.affiliation(candidate, request.queryService.content) ==
                        OperationUnitPresentation.affiliation(request.observer, request.queryService.content)
            }
        },
        emptySummaryText = request.mode.emptySummaryText,
        sensorReportMode = request.mode.sensorMode,
        ecmIntensity = request.ecmIntensity,
    )

    private fun OperationSensorAssessment.trackableChecks() =
        (detectedChecks + ecmGhostChecks).distinctBy { it.unit.id }

    private fun OperationLostContact.withDestructionState(
        currentCheck: OperationSensorVisibilityCheck?,
        observerAffiliation: OperationUnitAffiliation?,
        content: OperationContent,
    ): OperationLostContact {
        val friendly = wasFriendly || currentCheck?.unit?.let { OperationUnitPresentation.affiliation(it, content) == observerAffiliation } == true
        val destroyedInRange = currentCheck?.let { it.unit.destroyed && it.distanceUnits <= it.identificationRangeUnits } == true
        return copy(
            wasFriendly = friendly,
            lostDueToEnemyAction = wasIdentified && friendly && destroyedInRange,
            destroyedByFriendlyAction = wasIdentified && !friendly && destroyedInRange,
        )
    }

    private fun affiliationDescription(
        affiliation: OperationUnitAffiliation?,
        observerAffiliation: OperationUnitAffiliation?,
    ) = when (affiliation) {
        OperationUnitAffiliation.FRIEND -> if (observerAffiliation == OperationUnitAffiliation.HOSTILE) "enemy" else "allied"
        OperationUnitAffiliation.HOSTILE -> "enemy"
        OperationUnitAffiliation.NEUTRAL -> "neutral"
        OperationUnitAffiliation.UNKNOWN -> "unknown"
        null -> "unaffiliated"
    }

    private fun Map<String, String>.booleanProperty(key: String): Boolean =
        entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value?.trim()?.lowercase() in setOf("true", "yes", "1", "on")

    private const val LOW_CONFIDENCE_DISTANCE_UNITS = 3.0
}
