package io.hsar.mapgenerator.commandgui.korge.interaction

import io.hsar.mapgenerator.grid.EpsilonCoord
import io.hsar.mapgenerator.grid.GridLayer
import io.hsar.mapgenerator.grid.ReferenceSystem
import korlibs.image.bitmap.Bitmap32
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.*

class SensorDetectionTest {
    @Test
    fun `sitrep validation requires a recognised unit class from the unit or type`() {
        val missingUnitClass = unit(
            id = "00000000-0000-0000-0000-0000000000a1",
            instanceName = "Missing",
            unitClass = null,
            x = 0.5,
            y = 0.5,
        )
        val invalidUnitClass = unit(
            id = "00000000-0000-0000-0000-0000000000a2",
            instanceName = "Invalid",
            unitClass = "XX",
            x = 0.5,
            y = 0.5,
        )
        val typeUnitClass = unit(
            id = "00000000-0000-0000-0000-0000000000a3",
            instanceName = "Type default",
            unitClass = null,
            templateUnitClass = "CA",
            x = 0.5,
            y = 0.5,
        )

        assertThat(missingUnitClass.sitrepValidationError(), equalTo("This unit has no unitClass setting."))
        assertThat(invalidUnitClass.sitrepValidationError(), equalTo("The unitClass value \"XX\" is not recognised."))
        assertThat(typeUnitClass.sitrepValidationError(), equalTo(null))
    }


    @Test
    fun `navrep contact detection uses the target class range and distance`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000001",
            instanceName = "FF 1",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val nearbyFF = unit(
            id = "00000000-0000-0000-0000-000000000002",
            instanceName = "Escort",
            unitClass = "FF",
            x = 0.5035,
            y = 0.5,
        )
        val farCL = unit(
            id = "00000000-0000-0000-0000-000000000003",
            instanceName = "Cruiser",
            unitClass = "CL",
            x = 0.513,
            y = 0.5,
        )

        val contacts = SensorDetection.visibleContacts(
            observer = observer,
            candidates = listOf(observer, nearbyFF, farCL),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
        )

        assertThat(contacts.map { it.unit.id }, equalTo(listOf(nearbyFF.id, farCL.id)))
        assertThat(contacts.first().distanceUnits, closeTo(3.5, 0.0001))
        assertThat(contacts.first().detectionRangeUnits, equalTo(8.0))
    }

    @Test
    fun `unknown and cargo hulls use mass bands while recognised classes win`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-0000000000b1",
            instanceName = "Observer",
            unitClass = "XX",
            x = 0.5,
            y = 0.5,
            customProperties = mapOf(OlympiadDawnShip.MASS_KEY to "70"),
        )
        val lightweightUnknown = unit(
            id = "00000000-0000-0000-0000-0000000000b2",
            instanceName = "Unknown light",
            unitClass = "XX",
            x = 0.51,
            y = 0.5,
            customProperties = mapOf(OlympiadDawnShip.MASS_KEY to "20"),
        )
        val midweightCargo = unit(
            id = "00000000-0000-0000-0000-0000000000b3",
            instanceName = "Cargo mid",
            unitClass = "AK",
            x = 0.52,
            y = 0.5,
            customProperties = mapOf(OlympiadDawnShip.MASS_KEY to "40"),
        )
        val heavyweightMerchant = unit(
            id = "00000000-0000-0000-0000-0000000000b4",
            instanceName = "Merchant heavy",
            unitClass = "MV",
            x = 0.53,
            y = 0.5,
            customProperties = mapOf(OlympiadDawnShip.MASS_KEY to "70"),
        )
        val recognisedClass = unit(
            id = "00000000-0000-0000-0000-0000000000b5",
            instanceName = "Class wins",
            unitClass = "FF",
            x = 0.54,
            y = 0.5,
            customProperties = mapOf(OlympiadDawnShip.MASS_KEY to "70"),
        )

        val checks = SensorDetection.visibilityChecks(
            observer = observer,
            candidates = listOf(observer, lightweightUnknown, midweightCargo, heavyweightMerchant, recognisedClass),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
        ).associateBy { it.unit.id }

        assertThat(checks[lightweightUnknown.id]!!.baseDetectionRangeUnits, equalTo(6.0))
        assertThat(checks[lightweightUnknown.id]!!.identificationRangeUnits, equalTo(9.0))
        assertThat(checks[midweightCargo.id]!!.baseDetectionRangeUnits, equalTo(12.0))
        assertThat(checks[heavyweightMerchant.id]!!.baseDetectionRangeUnits, equalTo(24.0))
        assertThat(checks[recognisedClass.id]!!.baseDetectionRangeUnits, equalTo(6.0))
    }

    @Test
    fun `missing hull code uses UKN and unknown full text`() {
        val missing = unit(
            id = "00000000-0000-0000-0000-0000000000b6",
            instanceName = "Uncoded",
            unitClass = null,
            templateUnitClass = null,
            x = 0.5,
            y = 0.5,
            customProperties = mapOf(OlympiadDawnShip.MASS_KEY to "40"),
        )

        assertThat(missing.navrepMenuLabel(), equalTo("Uncoded [UKN]"))
        assertThat(missing.navrepReportLabel(), equalTo("UKN Uncoded"))
        assertThat(missing.sitrepValidationError(), equalTo(null))
        assertThat(missing.isNavrepUnit(), equalTo(true))

        val observer = unit(
            id = "00000000-0000-0000-0000-0000000000b7",
            instanceName = "Observer",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val check = SensorDetection.visibilityChecks(
            observer = observer,
            candidates = listOf(observer, missing),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
        ).single()
        assertThat(
            SensorDetection.scirepText(
                checks = listOf(check),
                identifiedIds = setOf(missing.id),
                contactNumbers = mapOf(missing.id to 1),
                mapWidth = 1_000,
                mapHeight = 1_000,
                emptySummaryText = "No hostile contacts to report.",
            ),
            equalTo("Contact Master 1 at 500 500, range 0U is unaffiliated Uncoded-class UKN."),
        )
    }

    @Test
    fun `invalid fallback mass keeps the default lightweight band`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-0000000000b9",
            instanceName = "Observer",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val target = unit(
            id = "00000000-0000-0000-0000-0000000000ba",
            instanceName = "Unknown",
            unitClass = "XX",
            x = 0.51,
            y = 0.5,
            customProperties = mapOf(OlympiadDawnShip.MASS_KEY to "not-a-number"),
        )

        assertThat(target.sitrepValidationError(), equalTo("The unitClass value \"XX\" is not recognised."))
        assertThat(
            SensorDetection.visibilityChecks(
                observer = observer,
                candidates = listOf(observer, target),
                mapWidth = 1_000,
                mapHeight = 1_000,
                mapUnitsPerPixel = 1.0,
            ).single().baseDetectionRangeUnits,
            equalTo(6.0),
        )
    }

    @Test
    fun `navrep contact detection respects sensor modifiers from custom properties`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000010",
            instanceName = "FF 1",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val activeSensorsContact = unit(
            id = "00000000-0000-0000-0000-000000000011",
            instanceName = "Runner",
            unitClass = "CL",
            x = 0.515,
            y = 0.5,
            customProperties = mapOf("activeSensors" to "true"),
        )
        val silentContact = unit(
            id = "00000000-0000-0000-0000-000000000012",
            instanceName = "Sneak",
            unitClass = "CA",
            x = 0.52,
            y = 0.5,
            customProperties = mapOf("silentRunning" to "true"),
        )

        val contacts = SensorDetection.visibleContacts(
            observer = observer,
            candidates = listOf(observer, activeSensorsContact, silentContact),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
        )

        assertThat(contacts.map { it.unit.id }, equalTo(listOf(activeSensorsContact.id)))
        assertThat(contacts.single().detectionRangeUnits, equalTo(32.0))
    }

    @Test
    fun `active sensors on the observer doubles detection and identification ranges`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000013",
            instanceName = "Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
            customProperties = mapOf("activeSensors" to "true"),
        )
        val target = unit(
            id = "00000000-0000-0000-0000-000000000014",
            instanceName = "Target",
            unitClass = "CL",
            x = 0.53,
            y = 0.5,
        )

        val check = SensorDetection.visibilityChecks(
            observer = observer,
            unitQueryService = UnitQueryService(listOf(observer, target), mapWidth = 1_000, mapHeight = 1_000, mapUnitsPerPixel = 1.0),
        ).single()

        assertThat(check.identificationRangeUnits, equalTo(36.0))
        assertThat(check.detectionRangeUnits, equalTo(72.0))
        assertThat(check.visible, equalTo(true))
        assertThat(check.detected, equalTo(true))
    }

    @Test
    fun `active sensors are ignored by navrep visibility`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000017",
            instanceName = "Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
            customProperties = mapOf("activeSensors" to "true"),
        )
        val target = unit(
            id = "00000000-0000-0000-0000-000000000018",
            instanceName = "Target",
            unitClass = "CL",
            x = 0.53,
            y = 0.5,
        )

        val check = SensorDetection.visibilityChecks(
            observer = observer,
            candidates = listOf(observer, target),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            sensorReportMode = SensorReportMode.NAVREP,
        ).single()

        assertThat(check.identificationRangeUnits, equalTo(18.0))
        assertThat(check.detectionRangeUnits, equalTo(36.0))
    }

    @Test
    fun `nebula affects observer range while warp affects target signature in navrep`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000019",
            instanceName = "Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
            customProperties = mapOf("inNebula" to "true"),
        )
        val target = unit(
            id = "00000000-0000-0000-0000-00000000001a",
            instanceName = "Target",
            unitClass = "CL",
            x = 0.53,
            customProperties = mapOf("activeWarp" to "true"),
            y = 0.5,
        )

        val check = SensorDetection.visibilityChecks(
            observer = observer,
            candidates = listOf(observer, target),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            sensorReportMode = SensorReportMode.NAVREP,
        ).single()

        assertThat(check.identificationRangeUnits, equalTo(36.0))
        assertThat(check.detectionRangeUnits, equalTo(72.0))
    }

    @Test
    fun `maximum warp is detected at any range but identified normally in both reports`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-00000000001b",
            instanceName = "Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
        )
        val target = unit(
            id = "00000000-0000-0000-0000-00000000001c",
            instanceName = "Target",
            unitClass = "CL",
            x = 0.7,
            y = 0.5,
            customProperties = mapOf("maximumWarp" to "true"),
        )

        SensorReportMode.entries.forEach { reportMode ->
            val check = SensorDetection.visibilityChecks(
                observer = observer,
                candidates = listOf(observer, target),
                mapWidth = 1_000,
                mapHeight = 1_000,
                mapUnitsPerPixel = 1.0,
                sensorReportMode = reportMode,
            ).single()

            assertThat(check.detected, equalTo(true))
            assertThat(check.visible, equalTo(false))
            assertThat(check.detectionRangeUnits.isInfinite(), equalTo(true))
            assertThat(check.identificationRangeUnits, equalTo(18.0))

            val nearbyCheck = SensorDetection.visibilityChecks(
                observer = observer,
                candidates = listOf(observer, target.copy(mapCoordinate = MapCoordinate(0.51, 0.5))),
                mapWidth = 1_000,
                mapHeight = 1_000,
                mapUnitsPerPixel = 1.0,
                sensorReportMode = reportMode,
            ).single()

            assertThat(nearbyCheck.visible, equalTo(true))
        }
    }

    @Test
    fun `maximum warp is detected-only in scirep and remains informational in navrep`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-00000000001d",
            instanceName = "Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
            affiliation = UnitAffiliation.Friend,
        )
        val target = unit(
            id = "00000000-0000-0000-0000-00000000001e",
            instanceName = "Target",
            unitClass = "CL",
            x = 0.7,
            y = 0.5,
            affiliation = UnitAffiliation.Friend,
            customProperties = mapOf("maximumWarp" to "true"),
        )

        val scirep = SensorDetection.visibilityReport(
            observer = observer,
            candidates = listOf(observer, target),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            includedIds = emptySet(),
            unidentifiedIds = setOf(target.id),
            candidateFilter = { true },
            sensorReportMode = SensorReportMode.SCIREP,
        )
        val navrep = SensorDetection.visibilityReport(
            observer = observer,
            candidates = listOf(observer, target),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            sensorReportMode = SensorReportMode.NAVREP,
        )

        assertThat(scirep.unidentifiedChecks.map { it.unit.id }, equalTo(listOf(target.id)))
        assertThat(navrep.unidentifiedChecks, equalTo(emptyList<SensorVisibilityCheck>()))
        assertThat(navrep.hiddenChecks.map { it.unit.id }, equalTo(listOf(target.id)))
        assertThat(
            SensorDetection.hiddenContactLabel(observer, navrep.hiddenChecks.single(), 1_000, 1_000),
            equalTo("CL Target contact ranges are ∞ / 18, actual range is 200"),
        )
    }

    @Test
    fun `navrep reports nebular conditions before electronic warfare`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-0000000000c1",
            instanceName = "Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
            customProperties = mapOf("inNebula" to "true"),
        )

        val report = SensorDetection.visibilityReport(
            observer = observer,
            candidates = listOf(observer),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            emptySummaryText = "No contacts.",
            sensorReportMode = SensorReportMode.NAVREP,
            ecmIntensity = 1,
        )

        assertThat(
            report.displayText,
            equalTo(
                "Poor sensor conditions due to nebular interference. Poor sensor conditions due to electronic warfare. " +
                        "Position 500 500 bearing unknown. No contacts.",
            ),
        )
    }

    @Test
    fun `scirep reports poor nebula conditions and active radiation`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-00000000001b",
            instanceName = "Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
            customProperties = mapOf("inNebula" to "true"),
        )
        val target = unit(
            id = "00000000-0000-0000-0000-00000000001c",
            instanceName = "Target",
            unitClass = "CL",
            x = 0.51,
            y = 0.5,
            customProperties = mapOf("activeSensors" to "true"),
        )
        val check = SensorDetection.visibilityChecks(
            observer = observer,
            candidates = listOf(observer, target),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            sensorReportMode = SensorReportMode.SCIREP,
        ).single()

        assertThat(
            SensorDetection.scirepText(
                checks = listOf(check),
                identifiedIds = emptySet(),
                contactNumbers = mapOf(target.id to 1),
                mapWidth = 1_000,
                mapHeight = 1_000,
                emptySummaryText = "No hostile contacts to report.",
                sensorConditionsPoor = true,
            ),
            equalTo("Poor sensor conditions due to nebular interference. Contact actively radiating at extreme range as Sierra 1 at 510 500, range 10U has unknown midweight classification."),
        )
    }

    @Test
    fun `ECM reduces passive sensors by two initial bands and active sensors by one`() {
        val passive = unit("00000000-0000-0000-0000-0000000000b1", "Passive", "DD", 0.5, 0.5)
        val active = unit(
            "00000000-0000-0000-0000-0000000000b2", "Active", "DD", 0.5, 0.5,
            customProperties = mapOf("activeSensors" to "true"),
        )
        val target = unit("00000000-0000-0000-0000-0000000000b3", "Target", "CL", 0.5, 0.5)

        val passiveCheck = SensorDetection.visibilityCheck(passive, target, 1.0, ecmIntensity = 1)!!
        val activeCheck = SensorDetection.visibilityCheck(active, target, 1.0, ecmIntensity = 1)!!

        assertThat(passiveCheck.identificationRangeUnits, closeTo(3.0, 0.0001))
        assertThat(passiveCheck.detectionRangeUnits, closeTo(6.0, 0.0001))
        assertThat(activeCheck.identificationRangeUnits, closeTo(12.0, 0.0001))
        assertThat(activeCheck.detectionRangeUnits, closeTo(24.0, 0.0001))
    }

    @Test
    fun `ECM transitions consume levels before advancing legacy combat penalties`() {
        val observer = unit("00000000-0000-0000-0000-0000000000b4", "Observer", "DD", 0.5, 0.5)
        val target = unit("00000000-0000-0000-0000-0000000000b5", "Target", "CL", 0.5, 0.5)

        val faint = SensorDetection.visibilityCheck(observer, target, 5.0, ecmIntensity = 1)!!
        val ghost = SensorDetection.visibilityCheck(observer, target, 5.0, ecmIntensity = 3)!!

        assertThat(faint.ecmContact, equalTo(EcmSensorContact.FAINT_TRACK))
        assertThat(faint.ecmTransitionIntensity, equalTo(1))
        assertThat(faint.legacyEcmLevel, equalTo(1))
        assertThat(ghost.ecmContact, equalTo(EcmSensorContact.SENSOR_GHOST))
        assertThat(ghost.ecmTransitionIntensity, equalTo(2))
        assertThat(ghost.legacyEcmLevel, equalTo(3))
    }

    @Test
    fun `ordnance seeker uses passive lowest fighter sensor profile`() {
        val launcher = unit(
            "00000000-0000-0000-0000-0000000000b6", "Launcher", "CA", 0.5, 0.5,
            customProperties = mapOf("activeSensors" to "true"),
        )
        val target = unit("00000000-0000-0000-0000-0000000000b7", "Target", "CL", 0.5, 0.5)

        val check = SensorDetection.visibilityCheck(
            launcher, target, 10.0, ecmIntensity = 1,
            observerProfile = SensorObserverProfile.LOWEST_FIGHTER_SEEKER,
        )!!

        assertThat(check.naturalVisible, equalTo(false))
        assertThat(check.naturalDetected, equalTo(true))
        assertThat(check.ecmContact, equalTo(EcmSensorContact.SENSOR_GHOST))
        assertThat(check.legacyEcmLevel, equalTo(2))
    }

    @Test
    fun `SCIREP reports electronic warfare faint tracks and ghosts exactly`() {
        val faintUnit = unit("00000000-0000-0000-0000-0000000000b8", "Faint", "CL", 0.5, 0.5)
        val ghostUnit = unit("00000000-0000-0000-0000-0000000000b9", "Ghost", "CL", 0.5, 0.5)
        val faint = SensorVisibilityCheck(faintUnit, false, true, 5.0, 3.0, 6.0, 12.0, emptyList(), ecmContact = EcmSensorContact.FAINT_TRACK)
        val ghost = SensorVisibilityCheck(ghostUnit, false, false, 7.0, 1.5, 3.0, 12.0, emptyList(), ecmContact = EcmSensorContact.SENSOR_GHOST)

        val text = SensorDetection.scirepText(
            listOf(faint, ghost), emptySet(), mapOf(faintUnit.id to 1, ghostUnit.id to 2),
            1_000, 1_000, emptySummaryText = "No hostile contacts to report.",
            sensorConditionsPoor = true, electronicWarfare = true,
        )

        assertThat(
            text,
            equalTo(
                "Poor sensor conditions due to nebular interference. Poor sensor conditions due to electronic warfare. " +
                        "Contact detected at extreme range as Sierra 1 at 500 500, range 5U is faint sensor track. " +
                        "Contact detected at extreme range as Sierra 2 at 500 500, range 7U is sensor ghost.",
            ),
        )
    }

    @Test
    fun `silent running on a target reduces its detection and identification ranges by one band`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000015",
            instanceName = "Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
        )
        val target = unit(
            id = "00000000-0000-0000-0000-000000000016",
            instanceName = "Target",
            unitClass = "CL",
            x = 0.53,
            y = 0.5,
            customProperties = mapOf("silentRunning" to "true"),
        )

        val check = SensorDetection.visibilityChecks(
            observer = observer,
            candidates = listOf(observer, target),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
        ).single()

        assertThat(check.identificationRangeUnits, equalTo(9.0))
        assertThat(check.detectionRangeUnits, equalTo(18.0))
    }

    @Test
    fun `navrep labels use the unit class when present`() {
        val snapshot = unit(
            id = "00000000-0000-0000-0000-000000000020",
            instanceName = "FF 1",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )

        assertThat(snapshot.isNavrepUnit(), equalTo(true))
        assertThat(snapshot.navrepMenuLabel(), equalTo("FF 1 [FF]"))
    }

    @Test
    fun `navrep labels fall back to the template class when the unit has no class`() {
        val snapshot = unit(
            id = "00000000-0000-0000-0000-000000000021",
            instanceName = "FF 2",
            unitClass = null,
            x = 0.5,
            y = 0.5,
            templateUnitClass = "FF",
        )

        assertThat(snapshot.isNavrepUnit(), equalTo(true))
        assertThat(snapshot.navrepMenuLabel(), equalTo("FF 2 [FF]"))
    }

    @Test
    fun `navrep report includes visible and hidden checks`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000030",
            instanceName = "FF 1",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val report = SensorDetection.visibilityReport(
            observer = observer,
            candidates = listOf(
                observer,
                unit(
                    id = "00000000-0000-0000-0000-000000000031",
                    instanceName = "Escort",
                    unitClass = "FF",
                    x = 0.5035,
                    y = 0.5,
                ),
                unit(
                    id = "00000000-0000-0000-0000-000000000032",
                    instanceName = "Cruiser",
                    unitClass = "CL",
                    x = 0.513,
                    y = 0.5,
                ),
            ),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            gridLayer = epsilonGridLayer(),
        )

        assertThat(report.displayText.startsWith("Position "), equalTo(true))
        assertThat(report.displayText.contains("FF Escort has position"), equalTo(true))
        assertThat(report.displayText.contains("[HIDDEN]"), equalTo(false))
        assertThat(report.displayText.contains("- CL Cruiser contact ranges are"), equalTo(false))
        assertThat(report.hiddenChecks.map { it.unit.id }, equalTo(listOf(MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000032")))))
        assertThat(report.copyText.startsWith("Position "), equalTo(true))
        assertThat(report.copyText, equalTo(report.displayText))
    }

    @Test
    fun `navrep report force includes hidden contacts`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000040",
            instanceName = "FF 1",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val forced = unit(
            id = "00000000-0000-0000-0000-000000000041",
            instanceName = "Cruiser",
            unitClass = "CL",
            x = 0.513,
            y = 0.5,
        )

        val report = SensorDetection.visibilityReport(
            observer = observer,
            candidates = listOf(observer, forced),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            includedIds = setOf(forced.id),
        )

        assertThat(report.visibleChecks.map { it.unit.id }, equalTo(listOf(forced.id)))
        assertThat(report.hiddenChecks, equalTo(emptyList<SensorVisibilityCheck>()))
        assertThat(report.displayText.contains("CL Cruiser has position"), equalTo(true))
        assertThat(report.copyText, equalTo(report.displayText))
    }

    @Test
    fun `navrep report can exclude every contact from the identified section`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000041",
            instanceName = "FF 1",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val contact = unit(
            id = "00000000-0000-0000-0000-000000000042",
            instanceName = "Cruiser",
            unitClass = "CL",
            x = 0.513,
            y = 0.5,
        )

        val report = SensorDetection.visibilityReport(
            observer = observer,
            candidates = listOf(observer, contact),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            includedIds = emptySet(),
        )

        assertThat(report.visibleChecks, equalTo(emptyList()))
        assertThat(report.hiddenChecks.map { it.unit.id }, equalTo(listOf(contact.id)))
        assertThat(report.displayText.contains("No allied contacts to report."), equalTo(true))
    }

    @Test
    fun `navrep report splits identified unidentified and hidden contacts in distance order`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000061",
            instanceName = "CA Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
        )
        val identified = unit(
            id = "00000000-0000-0000-0000-000000000062",
            instanceName = "Close",
            unitClass = "FF",
            x = 0.5035,
            y = 0.5,
        )
        val unidentifiedNear = unit(
            id = "00000000-0000-0000-0000-000000000063",
            instanceName = "Unknown Near",
            unitClass = "DD",
            x = 0.514,
            y = 0.5,
        )
        val unidentifiedFar = unit(
            id = "00000000-0000-0000-0000-000000000064",
            instanceName = "Unknown Far",
            unitClass = "CA",
            x = 0.527,
            y = 0.5,
        )
        val hidden = unit(
            id = "00000000-0000-0000-0000-000000000065",
            instanceName = "Hidden",
            unitClass = "CA",
            x = 0.560,
            y = 0.5,
        )

        val report = SensorDetection.visibilityReport(
            observer = observer,
            candidates = listOf(observer, hidden, unidentifiedFar, identified, unidentifiedNear),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            includedIds = setOf(identified.id),
            unidentifiedIds = setOf(unidentifiedNear.id, unidentifiedFar.id),
        )

        assertThat(report.visibleChecks.map { it.unit.id }, equalTo(listOf(identified.id)))
        assertThat(report.unidentifiedChecks.map { it.unit.id }, equalTo(listOf(unidentifiedNear.id, unidentifiedFar.id)))
        assertThat(report.hiddenChecks.map { it.unit.id }, equalTo(listOf(hidden.id)))
        assertThat(report.displayText.contains("FF Close has position"), equalTo(true))
        assertThat(report.displayText.contains("Unknown Near has position"), equalTo(false))
    }

    @Test
    fun `scirep report includes only non matching affiliations in distance order`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000043",
            instanceName = "CA Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
            affiliation = UnitAffiliation.Friend,
        )
        val friendly = unit(
            id = "00000000-0000-0000-0000-000000000044",
            instanceName = "Escort",
            unitClass = "FF",
            x = 0.5035,
            y = 0.5,
            affiliation = UnitAffiliation.Friend,
        )
        val hostileNear = unit(
            id = "00000000-0000-0000-0000-000000000045",
            instanceName = "Raider",
            unitClass = "FF",
            x = 0.5035,
            y = 0.5,
            affiliation = UnitAffiliation.Hostile,
        )
        val hostileMid = unit(
            id = "00000000-0000-0000-0000-000000000046",
            instanceName = "Picket",
            unitClass = "DD",
            x = 0.514,
            y = 0.5,
            affiliation = UnitAffiliation.Hostile,
        )
        val hostileFar = unit(
            id = "00000000-0000-0000-0000-000000000047",
            instanceName = "Cruiser",
            unitClass = "CA",
            x = 0.527,
            y = 0.5,
            affiliation = UnitAffiliation.Hostile,
        )
        val hostileHidden = unit(
            id = "00000000-0000-0000-0000-000000000048",
            instanceName = "Distant",
            unitClass = "CA",
            x = 0.560,
            y = 0.5,
            affiliation = UnitAffiliation.Hostile,
        )

        val report = SensorDetection.visibilityReport(
            observer = observer,
            candidates = listOf(observer, friendly, hostileFar, hostileHidden, hostileNear, hostileMid),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            candidateFilter = { candidate -> candidate.template.affiliation != observer.template.affiliation },
            emptySummaryText = "No hostile contacts to report.",
        )

        assertThat(report.visibleChecks.map { it.unit.id }, equalTo(listOf(hostileNear.id, hostileMid.id, hostileFar.id)))
        assertThat(report.hiddenChecks.map { it.unit.id }, equalTo(listOf(hostileHidden.id)))
        assertThat(report.displayText.contains("No hostile contacts to report."), equalTo(false))
        assertThat(report.displayText.contains("CA Cruiser has position"), equalTo(true))
    }

    @Test
    fun `navrep detection range follows the class band rules`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000050",
            instanceName = "CA Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
        )
        val ffTarget = unit(
            id = "00000000-0000-0000-0000-000000000051",
            instanceName = "FF Target",
            unitClass = "FF",
            x = 0.505,
            y = 0.5,
        )
        val ddTarget = unit(
            id = "00000000-0000-0000-0000-000000000052",
            instanceName = "DD Target",
            unitClass = "DD",
            x = 0.514,
            y = 0.5,
        )
        val caTarget = unit(
            id = "00000000-0000-0000-0000-000000000053",
            instanceName = "CA Target",
            unitClass = "CA",
            x = 0.527,
            y = 0.5,
        )

        val contacts = SensorDetection.visibleContacts(
            observer = observer,
            candidates = listOf(observer, ffTarget, ddTarget, caTarget),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
        )

        assertThat(contacts.map { it.unit.id }, equalTo(listOf(ffTarget.id, ddTarget.id, caTarget.id)))
        assertThat(contacts[0].baseDetectionRangeUnits, equalTo(6.0))
        assertThat(contacts[0].detectionRangeUnits, equalTo(18.0))
        assertThat(contacts[1].baseDetectionRangeUnits, equalTo(12.0))
        assertThat(contacts[1].detectionRangeUnits, equalTo(36.0))
        assertThat(contacts[2].baseDetectionRangeUnits, equalTo(24.0))
        assertThat(contacts[2].detectionRangeUnits, equalTo(72.0))
    }

    @Test
    fun `contacts in the outer band are detected but not identified`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000070",
            instanceName = "FF Observer",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val contact = unit(
            id = "00000000-0000-0000-0000-000000000071",
            instanceName = "DD Contact",
            unitClass = "DD",
            x = 0.51,
            y = 0.5,
            affiliation = UnitAffiliation.Hostile,
        )

        val check = SensorDetection.visibilityChecks(
            observer = observer,
            candidates = listOf(observer, contact),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            candidateFilter = { true },
        ).single()

        assertThat(check.distanceUnits, closeTo(10.0, 0.0001))
        assertThat(check.identificationRangeUnits, equalTo(8.0))
        assertThat(check.detectionRangeUnits, equalTo(16.0))
        assertThat(check.visible, equalTo(false))
        assertThat(check.detected, equalTo(true))

        val text = SensorDetection.scirepText(
            checks = listOf(check),
            identifiedIds = emptySet(),
            contactNumbers = mapOf(contact.id to 1),
            mapWidth = 1_000,
            mapHeight = 1_000,
            emptySummaryText = "No hostile contacts to report.",
        )

        assertThat(text, equalTo("Contact detected at extreme range as Sierra 1 at 510 500, range 10U has unknown midweight classification."))

        val identificationText = SensorDetection.scirepText(
            checks = listOf(check),
            identifiedIds = setOf(contact.id),
            contactNumbers = mapOf(contact.id to 1),
            mapWidth = 1_000,
            mapHeight = 1_000,
            emptySummaryText = "No hostile contacts to report.",
            identificationAnnouncements = listOf(check),
        )

        assertThat(
            identificationText,
            equalTo("Contact detected at extreme range as Master 1 at 510 500 is identified as enemy DD Contact-class DD."),
        )

        assertThat(
            SensorDetection.scirepText(
                checks = listOf(check),
                identifiedIds = emptySet(),
                contactNumbers = mapOf(contact.id to 1),
                mapWidth = 1_000,
                mapHeight = 1_000,
                emptySummaryText = "No hostile contacts to report.",
                previouslyDetectedIds = setOf(contact.id),
            ),
            equalTo("Contact Sierra 1 has position 510 500, range 10U."),
        )
    }

    @Test
    fun `scirep uses imported mass for rough classification`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000074",
            instanceName = "FF Observer",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val contact = unit(
            id = "00000000-0000-0000-0000-000000000075",
            instanceName = "FF Contact",
            unitClass = "FF",
            x = 0.51,
            y = 0.5,
            customProperties = mapOf(OlympiadDawnShip.MASS_KEY to "55"),
        )
        val check = SensorDetection.visibilityChecks(
            observer = observer,
            candidates = listOf(observer, contact),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
        ).single()

        assertThat(
            SensorDetection.scirepText(
                checks = listOf(check),
                identifiedIds = emptySet(),
                contactNumbers = mapOf(contact.id to 1),
                mapWidth = 1_000,
                mapHeight = 1_000,
                emptySummaryText = "No hostile contacts to report.",
            ),
            equalTo("Contact detected at extreme range as Sierra 1 at 510 500, range 10U has unknown lightweight classification."),
        )
    }

    @Test
    fun `identified contact keeps its detected number and reports its ship name`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000072",
            instanceName = "Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
        )
        val contact = unit(
            id = "00000000-0000-0000-0000-000000000073",
            instanceName = "Zweihander",
            unitClass = "CVE",
            templateName = "Sword",
            x = 0.5034,
            y = 0.5,
        )
        val check = SensorDetection.visibilityChecks(
            observer = observer,
            candidates = listOf(observer, contact),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
        ).single()

        assertThat(
            SensorDetection.scirepText(
                checks = listOf(check),
                identifiedIds = setOf(contact.id),
                contactNumbers = mapOf(contact.id to 9),
                mapWidth = 1_000,
                mapHeight = 1_000,
                emptySummaryText = "No hostile contacts to report.",
                identificationAnnouncements = listOf(check),
            ),
            equalTo("Contact detected at extreme range as Master 9 at 503.4 500 is identified as unaffiliated Sword-class CVE Zweihander."),
        )
    }

    @Test
    fun `navrep reports first-time allied identification with Sierra number and class data`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000076",
            instanceName = "Observer",
            unitClass = "CA",
            x = 0.5,
            y = 0.5,
            affiliation = UnitAffiliation.Hostile,
        )
        val contact = unit(
            id = "00000000-0000-0000-0000-000000000077",
            instanceName = "Florence",
            unitClass = "BC",
            templateName = "Roma",
            x = 0.5034,
            y = 0.5,
            affiliation = UnitAffiliation.Hostile,
        )
        val check = SensorDetection.visibilityChecks(
            observer = observer,
            candidates = listOf(observer, contact),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
            sensorReportMode = SensorReportMode.NAVREP,
        ).single()

        assertThat(
            SensorDetection.navrepIdentificationText(
                observer = observer,
                checks = listOf(check),
                contactNumbers = mapOf(contact.id to 4),
                mapWidth = 1_000,
                mapHeight = 1_000,
            ),
            equalTo("Contact detected at 503.4 500 is identified as allied Roma-class BC Florence."),
        )

        assertThat(
            SensorDetection.navrepIdentificationText(
                observer = observer,
                checks = listOf(check),
                contactNumbers = mapOf(contact.id to 4),
                mapWidth = 1_000,
                mapHeight = 1_000,
                previouslyDetectedIds = setOf(contact.id),
            ),
            equalTo("Contact Sierra 4 at 503.4 500 bearing unknown is identified as allied Roma-class BC Florence."),
        )
    }

    @Test
    fun `sensor unit class names cover the supported hull codes`() {
        assertThat(
            SENSOR_UNIT_CLASS_NAMES,
            equalTo(
                mapOf(
                    "FF" to "frigate",
                    "F" to "fighter",
                    "DD" to "destroyer",
                    "CL" to "light cruiser",
                    "CA" to "heavy cruiser",
                    "CVE" to "escort carrier",
                    "CVL" to "light carrier",
                    "CV" to "carrier",
                    "BC" to "battlecruiser",
                    "AK" to "Cargo Ship",
                    "MV" to "Cargo Ship",
                ),
            ),
        )
    }

    @Test
    fun `unknown contacts use the correct rough classification bracket`() {
        assertThat(
            SENSOR_UNIT_CLASS_BRACKETS,
            equalTo(
                mapOf(
                    "F" to "lightweight",
                    "FF" to "lightweight",
                    "DD" to "midweight",
                    "CL" to "midweight",
                    "CA" to "heavyweight",
                    "CVE" to "heavyweight",
                    "CVL" to "heavyweight",
                    "CV" to "heavyweight",
                    "BC" to "heavyweight",
                ),
            ),
        )
    }

    @Test
    fun `scirep combines any number of segments for each contact into one sentence`() {
        val firstContact = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000080"))
        val secondContact = MapPlacedObjectId(UUID.fromString("00000000-0000-0000-0000-000000000081"))

        val text = SensorDetection.combineScirepContactSegments(
            listOf(
                ScirepContactSegment(firstContact, 1, "Contact Sierra 1", listOf("at D8 39-48", "range 18U")),
                ScirepContactSegment(firstContact, 1, "ignored opening", listOf("is identified as a Sword-class heavy cruiser")),
                ScirepContactSegment(secondContact, 2, "Contact Sierra 2", listOf("has unknown classification")),
            ),
        )

        assertThat(
            text,
            equalTo(
                "Contact Sierra 1 at D8 39-48 range 18U is identified as a Sword-class heavy cruiser. " +
                        "Contact Sierra 2 has unknown classification.",
            ),
        )
    }

    @Test
    fun `scirep reports lost and low confidence contacts as single sentences`() {
        val observer = unit(
            id = "00000000-0000-0000-0000-000000000090",
            instanceName = "FF Observer",
            unitClass = "FF",
            x = 0.5,
            y = 0.5,
        )
        val contact = unit(
            id = "00000000-0000-0000-0000-000000000091",
            instanceName = "DD Contact",
            unitClass = "DD",
            x = 0.51,
            y = 0.5,
        )
        val check = SensorDetection.visibilityChecks(
            observer = observer,
            candidates = listOf(observer, contact),
            mapWidth = 1_000,
            mapHeight = 1_000,
            mapUnitsPerPixel = 1.0,
        ).single()
        val lost = SitrepLostContact(
            contactId = observer.id,
            sierraNumber = 6,
            lastSeenPosition = MapCoordinate(0.52, 0.5),
            lastSeenRangeUnits = 28.3,
            wasIdentified = true,
        )

        val text = SensorDetection.scirepText(
            checks = listOf(check),
            identifiedIds = setOf(contact.id),
            contactNumbers = mapOf(contact.id to 4),
            mapWidth = 1_000,
            mapHeight = 1_000,
            emptySummaryText = "No hostile contacts to report.",
            lowConfidenceContacts = mapOf(contact.id to lost),
            lostContacts = listOf(lost),
        )

        assertThat(
            text,
            equalTo(
                "New contact tentatively designated Master 4 from last known position " +
                        "at 520 500, range 28.3U was identified as unaffiliated DD Contact-class DD. " +
                        "Contact Sierra 6 lost.",
            ),
        )
    }

    private fun unit(
        id: String,
        instanceName: String,
        unitClass: String?,
        x: Double,
        y: Double,
        affiliation: UnitAffiliation? = null,
        customProperties: Map<String, String> = emptyMap(),
        templateUnitClass: String? = unitClass,
        templateName: String = instanceName,
    ): UnitSnapshot =
        UnitSnapshot(
            id = MapPlacedObjectId(UUID.fromString(id)),
            unitTemplateId = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-0000000000ab")),
            template = MapViewerUnitTemplate(
                id = UnitTemplateId(UUID.fromString("00000000-0000-0000-0000-0000000000ab")),
                name = templateName,
                bitmap = Bitmap32(4, 4),
                customProperties = templateUnitClass?.let { mapOf("unitClass" to it) } ?: emptyMap(),
                affiliation = affiliation,
            ),
            instanceName = instanceName,
            destroyed = false,
            facingDegrees = null,
            mapCoordinate = MapCoordinate(x = x, y = y),
            normalizedWidth = 0.01,
            normalizedHeight = 0.01,
            customProperties = customProperties + (unitClass?.let { mapOf("unitClass" to it) } ?: emptyMap()),
        )

    private fun epsilonGridLayer(): GridLayer =
        GridLayer(
            mapId = "EPSILON",
            referenceSystem = ReferenceSystem.EPSILON,
            gridScale = ReferenceSystem.EPSILON.gridScale,
            unitsPerPixel = 1.0,
            detailLevel = 2,
            pixelsPerLine = 10.0,
            verticalLineNumberStart = EpsilonCoord.fromUnits(0),
            horizontalLineNumberStart = EpsilonCoord.fromUnits(1_000),
            verticalLineCount = 10,
            horizontalLineCount = 10,
        )
}
