package io.hsar.mapgenerator.commandgui.operation

internal object OperationUnitPresentation {
    const val UNIT_CLASS_KEY = "unitClass"
    const val MASS_KEY = "mass"
    const val UNKNOWN_HULL_CODE = "UKN"

    val classNames = mapOf(
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
    )

    val classBrackets = mapOf(
        "F" to "lightweight",
        "FF" to "lightweight",
        "DD" to "midweight",
        "CL" to "midweight",
        "CA" to "heavyweight",
        "CVE" to "heavyweight",
        "CVL" to "heavyweight",
        "CV" to "heavyweight",
        "BC" to "heavyweight",
    )

    val navrepEligibleClasses = setOf("FF", "F", "DD", "CL", "CA", "CVE", "CVL", "CV", "BC")

    fun displayName(unit: OperationUnit, content: OperationContent): String =
        unit.instanceName.ifBlank { content.unitTemplate(unit.unitTemplateId)?.name.orEmpty() }

    fun unitClass(unit: OperationUnit, content: OperationContent): String? =
        (unit.customProperties.stringProperty(UNIT_CLASS_KEY)
            ?: content.unitTemplate(unit.unitTemplateId)?.customProperties?.stringProperty(UNIT_CLASS_KEY))
            ?.uppercase()

    fun affiliation(unit: OperationUnit, content: OperationContent): OperationUnitAffiliation? =
        unit.affiliation ?: content.unitTemplate(unit.unitTemplateId)?.affiliation

    fun mass(unit: OperationUnit, content: OperationContent): Int? =
        (unit.customProperties.stringProperty(MASS_KEY)
            ?: content.unitTemplate(unit.unitTemplateId)?.customProperties?.stringProperty(MASS_KEY))
            ?.toIntOrNull()

    fun hullCode(unit: OperationUnit, content: OperationContent): String = unitClass(unit, content) ?: UNKNOWN_HULL_CODE

    fun navrepMenuLabel(unit: OperationUnit, content: OperationContent): String =
        "${displayName(unit, content)} [${hullCode(unit, content)}]"

    fun navrepReportLabel(unit: OperationUnit, content: OperationContent): String =
        "${hullCode(unit, content)} ${displayName(unit, content)}"

    fun sitrepValidationError(unit: OperationUnit, content: OperationContent): String? {
        val unitClass = unitClass(unit, content)
        if (unitClass.needsMassFallback() && mass(unit, content)?.let { it >= 0 } != true) {
            return if (unitClass == null) "This unit has no unitClass setting."
            else "The unitClass value \"$unitClass\" is not recognised."
        }
        return null
    }

    fun isNavrepUnit(unit: OperationUnit, content: OperationContent): Boolean =
        unitClass(unit, content) in navrepEligibleClasses ||
                (unitClass(unit, content).needsMassFallback() && mass(unit, content)?.let { it >= 0 } == true)

    private fun String?.needsMassFallback(): Boolean =
        this == null || this == UNKNOWN_HULL_CODE || this == "AK" || this == "MV" || this !in classNames

    private fun Map<String, String>.stringProperty(key: String): String? =
        entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value?.trim()?.takeIf { it.isNotBlank() }
}
