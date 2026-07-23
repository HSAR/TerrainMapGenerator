package io.hsar.mapgenerator.commandgui.korge.ui

import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlag
import io.hsar.mapgenerator.commandgui.MapViewerFeatureFlags
import io.hsar.mapgenerator.commandgui.korge.interaction.MapViewerUnitTemplate
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnFighterLaunchStatus
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShip
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnShipDamage
import io.hsar.mapgenerator.commandgui.korge.interaction.OlympiadDawnSquadronStatus
import io.hsar.mapgenerator.commandgui.korge.interaction.UnitAffiliation
import io.hsar.mapgenerator.commandgui.save.OperationSaveJson
import io.hsar.mapgenerator.image.Palette
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.xy
import kotlin.math.max

@OptIn(KorgeExperimental::class)
internal class MapViewerUnitEditDialog(
    private val root: Container,
    private val dialogManager: MapViewerDialogManager,
    private val dialogKey: Any,
    private val persistenceTargetId: String? = null,
    private val unitType: MapViewerUnitTemplate,
    initialName: String,
    initialPositionX: String,
    initialPositionY: String,
    private val initialDestroyed: Boolean,
    private val initialFacing: String,
    initialAffiliation: UnitAffiliation?,
    initialCustomProperties: Map<String, String>,
    private val fighterLaunchStatusesProvider: () -> Map<String, OlympiadDawnFighterLaunchStatus> = { emptyMap() },
    private val squadronStatusProvider: () -> OlympiadDawnSquadronStatus? = { null },
    private val onSquadronRemainingChanged: (Int) -> Unit = {},
    private val onShipDamageChanged: (OlympiadDawnShipDamage) -> Unit = {},
    private val onPositionPreviewChanged: (String, String) -> Unit,
    private val onDestroyedPreviewChanged: (Boolean) -> Unit,
    private val onFacingPreviewChanged: (String) -> Unit,
    private val onAffiliationPreviewChanged: (UnitAffiliation?) -> Unit,
    private val focusTargetBoundsProvider: () -> List<ScreenBounds> = { emptyList() },
    private val isPositionValid: (String, String) -> Boolean = { _, _ -> true },
    private val isFacingValid: (String) -> Boolean = { true },
    private val onSave: (String, String, String, Boolean, String, UnitAffiliation?, Map<String, String>) -> Boolean,
    private val onDismiss: () -> Unit,
    private val restored: MapViewerPersistedDialog? = null,
) {
    private var nameValue = initialName
    private var positionXValue = initialPositionX
    private var positionYValue = initialPositionY
    private var destroyedValue = initialDestroyed
    private var facingValue = initialFacing
    private var affiliationValue = initialAffiliation
    private val initialAffiliationValue = initialAffiliation
    private var nameInput: MapViewerInputField? = null
    private var positionXInput: MapViewerInputField? = null
    private var positionYInput: MapViewerInputField? = null
    private var facingInput: MapViewerInputField? = null
    private val affiliationDropdown = MapViewerDropdownMenu(
        root = root,
        options = UnitAffiliationUi.options,
        onSelected = ::updateAffiliation,
        labelForValue = UnitAffiliationUi::label,
        swatchForValue = UnitAffiliationUi::swatchColor,
    )
    private var isOpen = false
    private var window: MapViewerDialogWindow? = null
    private var initialFocusApplied = false
    private var nameWasValid = MapViewerTextInputValidation.isPresent(nameValue)
    private var positionWasValid = isPositionValid(positionXValue, positionYValue)
    private var facingWasValid = isFacingValid(facingValue)
    private var pendingNameFocus = false
    private val shipDefinition = OlympiadDawnShip.definition(initialCustomProperties) ?: OlympiadDawnShip.definition(unitType.customProperties)
    private var shipDamage = shipDefinition?.let { OlympiadDawnShip.instanceDamage(initialCustomProperties, it) }
    private var shipPanelCollapsed = true
    private var shipComponentsCollapsed = true
    private var shipWeaponsCollapsed = true
    private var shipFightersCollapsed = true
    private var squadronPanelCollapsed = true
    private var customPropertiesExpanded = false
    private val restoredCustomProperties = restored?.draft?.path("customProperties")
        ?.takeIf { it.isObject }
        ?.fields()
        ?.asSequence()
        ?.mapNotNull { (key, value) -> value.textValue()?.let { key to it } }
        ?.toMap()
    private val initialShipProperties = initialCustomProperties.filterKeys { it in SHIP_PROPERTY_KEYS }
    private val customPropertiesEditor = MapViewerCustomPropertiesEditor(
        initialProperties = (restoredCustomProperties ?: initialCustomProperties).filterKeys { it !in SHIP_PROPERTY_KEYS },
        inheritedProperties = unitType.customProperties.filterKeys { it !in SHIP_PROPERTY_KEYS },
        reservedKeys = UNIT_FIELD_KEYS,
        onRowsChanged = { window?.refresh() },
        onValidationChanged = { window?.refresh() },
        tooltipRoot = root,
    )

    init {
        restored?.draft?.let { draft ->
            nameValue = draft.path("name").textValue() ?: nameValue
            positionXValue = draft.path("positionX").textValue() ?: positionXValue
            positionYValue = draft.path("positionY").textValue() ?: positionYValue
            destroyedValue = draft.path("destroyed").takeIf { it.isBoolean }?.booleanValue() ?: destroyedValue
            facingValue = draft.path("facing").textValue() ?: facingValue
            affiliationValue = draft.path("affiliation").textValue()
                ?.let { value -> UnitAffiliation.entries.firstOrNull { it.name == value } }
                ?: affiliationValue
            customPropertiesExpanded = draft.path("customPropertiesExpanded").booleanValue()
        }
    }

    fun show(viewportWidth: Int, viewportHeight: Int) {
        if (isOpen) {
            return
        }

        isOpen = true
        window = MapViewerDialogWindow(
            root = root,
            title = "Edit Unit",
            titleProvider = { MapViewerDialogTitles.unit("Edit Unit", nameValue) },
            preferredWidth = PREFERRED_WIDTH,
            preferredHeight = PREFERRED_HEIGHT,
            contentMinimumWidth = ::minimumContentWidth,
            contentHeight = ::contentHeight,
            actions = listOf(
                MapViewerDialogAction("Save", enabled = { canSave() }) { saveAndClose() },
                MapViewerDialogAction("Cancel") { close() },
            ),
            onClose = { close() },
            renderContent = ::renderContent,
            focusTargetBoundsProvider = focusTargetBoundsProvider,
            persistence = MapViewerDialogPersistence(
                type = TYPE,
                targetId = persistenceTargetId,
                restored = restored,
                draft = {
                    OperationSaveJson.objectMapper.createObjectNode().apply {
                        put("name", nameValue)
                        put("positionX", positionXValue)
                        put("positionY", positionYValue)
                        put("destroyed", destroyedValue)
                        put("facing", facingValue)
                        affiliationValue?.let { affiliation -> put("affiliation", affiliation.name) }
                        put("customPropertiesExpanded", customPropertiesExpanded)
                        putObject("customProperties").also { properties ->
                            unitProperties().forEach { (key, value) -> properties.put(key, value) }
                        }
                    }
                },
            ),
            mode = MapViewerDialogMode.FLOATING,
            dialogManager = dialogManager,
        ).also {
            it.show(viewportWidth, viewportHeight)
            dialogManager.register(
                owner = this,
                targetKey = dialogKey,
                blocksScreenPoint = { point -> it.blocksScreenPoint(point) },
                isTextInputActive = { isTextInputActive },
                sync = { payload ->
                    (payload as? MapViewerDialogPositionSync)?.let(::syncFields)
                },
                persistedDialog = { it.persistedDialog() },
                close = { close() },
            )
        }
    }

    fun close() {
        if (!isOpen) {
            return
        }

        onDestroyedPreviewChanged(initialDestroyed)
        onFacingPreviewChanged(initialFacing)
        onAffiliationPreviewChanged(initialAffiliationValue)
        dismiss()
    }

    fun refreshShipStatus() {
        if (isOpen) window?.refresh()
    }

    fun syncShipDamage(damage: OlympiadDawnShipDamage) {
        shipDamage = damage
        window?.refresh()
    }

    private fun dismiss() {
        if (!isOpen) {
            return
        }

        isOpen = false
        dialogManager.unregister(this)
        customPropertiesEditor.hideTooltip()
        window?.close()
        window = null
        onDismiss()
    }

    fun blocksScreenPoint(point: korlibs.math.geom.Vector2D): Boolean =
        window?.blocksScreenPoint(point) == true

    val isTextInputActive: Boolean
        get() = nameInput?.isFocused == true ||
                positionXInput?.isFocused == true ||
                positionYInput?.isFocused == true ||
                facingInput?.isFocused == true ||
                customPropertiesEditor.isTextInputActive

    private fun renderContent(parent: Container, width: Double) {
        val fieldWidth = width
        val typePreviewWidth = fieldWidth
        val positionFieldWidth = (fieldWidth - MapViewerDialogSpacing.FIELD_GAP) / 2.0
        val inlineBlock = MapViewerInlineInputs.block(UNIT_INLINE_LABELS)
        val twoColumn = width >= splitRowMinimumWidth()
        val splitWidth = if (twoColumn) (width - MapViewerDialogSpacing.FIELD_GAP) / 2.0 else width
        val nameTop = NAME_TOP
        val affiliationTop = if (twoColumn) NAME_TOP else AFFILIATION_TOP
        val facingTop = if (twoColumn) AFFILIATION_TOP else FACING_TOP
        val destroyedTop = if (twoColumn) {
            facingTop
        } else {
            facingTop + MapViewerDialogSpacing.INPUT_HEIGHT + MapViewerDialogSpacing.SECTION_GAP
        }
        val customPropertiesTop = destroyedTop + MapViewerDialogSpacing.INPUT_HEIGHT + MapViewerDialogSpacing.SECTION_GAP

        val typePreview = parent.container()
        typePreview.xy(0.0, TYPE_PREVIEW_TOP)
        MapViewerUiBoxes.imageBox(typePreview, unitType.bitmap, typePreviewWidth, MapViewerDialogSpacing.PREVIEW_HEIGHT)
        MapViewerTextViews.leftBox(
            parent = typePreview,
            text = unitType.name,
            width = typePreviewWidth - ICON_SLOT_WIDTH - 24.0,
            height = MapViewerDialogSpacing.PREVIEW_HEIGHT,
            color = Palette.Colours.WHITE,
        ) {
            x = ICON_SLOT_WIDTH + 12.0
        }

        MapViewerTextViews.box(
            parent = parent,
            text = "X",
            width = positionFieldWidth,
            height = POSITION_LABEL_HEIGHT,
            color = Palette.Colours.WHITE,
        ) {
            x = 0.0
            y = POSITION_LABEL_TOP
        }
        MapViewerTextViews.box(
            parent = parent,
            text = "Y",
            width = positionFieldWidth,
            height = POSITION_LABEL_HEIGHT,
            color = Palette.Colours.WHITE,
        ) {
            x = positionFieldWidth + MapViewerDialogSpacing.FIELD_GAP
            y = POSITION_LABEL_TOP
        }

        positionXInput = MapViewerTextInputs.input(
            parent = parent,
            text = positionXValue,
            x = 0.0,
            y = POSITION_INPUT_TOP,
            width = positionFieldWidth,
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
            frameStyle = MapViewerTextInputValidation.frameStyle(isPositionValid(positionXValue, positionYValue)),
        ).also { input ->
            input.controller.onTextUpdated.add {
                positionXValue = input.text
                refreshPositionPreview()
                refreshIfPositionValidityChanged()
            }
        }

        positionYInput = MapViewerTextInputs.input(
            parent = parent,
            text = positionYValue,
            x = positionFieldWidth + MapViewerDialogSpacing.FIELD_GAP,
            y = POSITION_INPUT_TOP,
            width = positionFieldWidth,
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
            frameStyle = MapViewerTextInputValidation.frameStyle(isPositionValid(positionXValue, positionYValue)),
        ).also { input ->
            input.controller.onTextUpdated.add {
                positionYValue = input.text
                refreshPositionPreview()
                refreshIfPositionValidityChanged()
            }
        }

        nameInput = inlineBlock.text(
            parent = parent,
            label = "Name",
            text = nameValue,
            x = 0.0,
            y = nameTop,
            width = splitWidth,
            inputMinimumWidth = nameTextInputMinimumWidth(),
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
            frameStyle = MapViewerTextInputValidation.frameStyle(MapViewerTextInputValidation.isPresent(nameValue)),
        )
        nameInput?.controller?.onTextUpdated?.add {
            val wasValid = nameWasValid
            nameValue = nameInput?.text.orEmpty()
            nameWasValid = MapViewerTextInputValidation.isPresent(nameValue)
            if (nameWasValid != wasValid) {
                pendingNameFocus = true
                window?.refresh()
            }
        }

        MapViewerInlineInputs.block(emptyList()).controlWithoutLabel(
            parent = parent,
            x = if (twoColumn) splitWidth + MapViewerDialogSpacing.FIELD_GAP else 0.0,
            y = affiliationTop,
            width = splitWidth,
            inputMinimumWidth = affiliationMinimumWidth(),
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
        ) { input, inputWidth ->
            affiliationDropdown.renderTrigger(
                parent = input,
                value = affiliationValue,
                width = inputWidth,
                height = MapViewerDialogSpacing.INPUT_HEIGHT,
            )
        }

        facingInput = inlineBlock.text(
            parent = parent,
            label = "Facing",
            text = facingValue,
            x = 0.0,
            y = facingTop,
            width = splitWidth,
            inputMinimumWidth = facingTextInputMinimumWidth(),
            frameStyle = MapViewerTextInputValidation.frameStyle(isFacingValid(facingValue)),
        ).also { input ->
            input.controller.onTextUpdated.add {
                facingValue = input.text
                onFacingPreviewChanged(facingValue)
                refreshIfFacingValidityChanged()
            }
        }

        inlineBlock.control(
            parent = parent,
            label = "KIA",
            x = if (twoColumn) splitWidth + MapViewerDialogSpacing.FIELD_GAP else 0.0,
            y = destroyedTop,
            width = splitWidth,
            inputMinimumWidth = DESTROYED_CHECKBOX_SIZE,
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
        ) { input, _ ->
            renderDestroyedToggle(input)
        }

        val customPropertiesContainer = parent.container()
        customPropertiesContainer.xy(0.0, customPropertiesTop)
        renderCustomPropertiesPanel(customPropertiesContainer, fieldWidth)

        if (shipPanelEnabled) {
            shipDefinition?.let { definition ->
                val shipPanel = parent.container()
                shipPanel.xy(0.0, customPropertiesTop + customPropertiesPanelHeight() + MapViewerDialogSpacing.SECTION_GAP)
                OlympiadDawnShipStatusPanel.render(
                    parent = shipPanel,
                    width = fieldWidth,
                    definition = definition,
                    damage = shipDamage ?: OlympiadDawnShipDamage(),
                    collapsed = shipPanelCollapsed,
                    editable = true,
                    onCollapsedChanged = {
                        shipPanelCollapsed = !shipPanelCollapsed
                        window?.refresh()
                    },
                    componentsCollapsed = shipComponentsCollapsed,
                    onComponentsCollapsedChanged = {
                        shipComponentsCollapsed = !shipComponentsCollapsed
                        window?.refresh()
                    },
                    weaponsCollapsed = shipWeaponsCollapsed,
                    onWeaponsCollapsedChanged = {
                        shipWeaponsCollapsed = !shipWeaponsCollapsed
                        window?.refresh()
                    },
                    fightersCollapsed = shipFightersCollapsed,
                    onFightersCollapsedChanged = {
                        shipFightersCollapsed = !shipFightersCollapsed
                        window?.refresh()
                    },
                    fighterLaunchStatuses = fighterLaunchStatusesProvider(),
                    onDamageChanged = { damage ->
                        shipDamage = damage
                        onShipDamageChanged(damage)
                        window?.refresh()
                    },
                )
            }
        }

        if (squadronPanelEnabled) {
            squadronStatusProvider()?.let { status ->
                val squadronPanel = parent.container()
                squadronPanel.xy(0.0, customPropertiesTop + customPropertiesPanelHeight() + MapViewerDialogSpacing.SECTION_GAP + shipPanelHeight())
                OlympiadDawnSquadronStatusPanel.render(
                    parent = squadronPanel,
                    width = fieldWidth,
                    status = status,
                    collapsed = squadronPanelCollapsed,
                    editable = true,
                    onCollapsedChanged = {
                        squadronPanelCollapsed = !squadronPanelCollapsed
                        window?.refresh()
                    },
                    onRemainingChanged = onSquadronRemainingChanged,
                )
            }
        }

        if (!initialFocusApplied) {
            nameInput?.focus()
            initialFocusApplied = true
        }
        if (pendingNameFocus) {
            nameInput?.focus()
            pendingNameFocus = false
        }
    }

    private fun syncFields(sync: MapViewerDialogPositionSync) {
        positionXValue = sync.positionX
        positionYValue = sync.positionY
        positionXInput?.text = sync.positionX
        positionYInput?.text = sync.positionY
        window?.refresh()
    }

    private fun updateAffiliation(value: UnitAffiliation?) {
        affiliationValue = value
        onAffiliationPreviewChanged(value)
        window?.refresh()
    }

    private fun renderDestroyedToggle(parent: Container) {
        parent.removeChildren()
        MapViewerCheckbox.render(
            parent = parent,
            x = 0.0,
            y = (MapViewerDialogSpacing.INPUT_HEIGHT - DESTROYED_CHECKBOX_SIZE) / 2.0,
            size = DESTROYED_CHECKBOX_SIZE,
            checked = destroyedValue,
        ) {
            destroyedValue = !destroyedValue
            onDestroyedPreviewChanged(destroyedValue)
            renderDestroyedToggle(parent)
        }
    }

    private fun affiliationMinimumWidth(): Double =
        maxOf(
            MIN_FIELD_WIDTH,
            MapViewerDropdownLayoutBuilder.minimumMenuWidth(
                labels = UnitAffiliationUi.options.map { it.label },
            ) + 18.0,
        )

    private fun refreshPositionPreview() {
        onPositionPreviewChanged(positionXValue, positionYValue)
        window?.refresh()
    }

    private fun refreshIfPositionValidityChanged() {
        val isValid = isPositionValid(positionXValue, positionYValue)
        if (isValid != positionWasValid) {
            positionWasValid = isValid
            window?.refresh()
        }
    }

    private fun refreshIfFacingValidityChanged() {
        val isValid = isFacingValid(facingValue)
        if (isValid != facingWasValid) {
            facingWasValid = isValid
            window?.refresh()
        }
    }

    private fun saveAndClose() {
        if (!canSave()) {
            return
        }

        if (onSave(
                nameValue.trim(),
                positionXValue.trim(),
                positionYValue.trim(),
                destroyedValue,
                facingValue.trim(),
                affiliationValue,
                unitProperties(),
            )
        ) {
            dismiss()
        }
    }

    private fun contentHeight(width: Double): Double =
        (width.let {
            val twoColumn = it >= splitRowMinimumWidth()
            val facingTop = if (twoColumn) AFFILIATION_TOP else FACING_TOP
            val destroyedTop = if (twoColumn) {
                facingTop
            } else {
                facingTop + MapViewerDialogSpacing.INPUT_HEIGHT + MapViewerDialogSpacing.SECTION_GAP
            }
            destroyedTop + MapViewerDialogSpacing.INPUT_HEIGHT + MapViewerDialogSpacing.SECTION_GAP + customPropertiesPanelHeight() + shipPanelHeight() + squadronPanelHeight()
        })

    private fun renderCustomPropertiesPanel(parent: Container, width: Double) {
        MapViewerCollapsiblePanel.header(
            parent = parent,
            width = width,
            height = MapViewerDialogSpacing.INPUT_HEIGHT,
            label = MapViewerCountText.of(
                customPropertiesEditor.propertyCount(),
                "additional property",
                "additional properties",
            ),
            expanded = customPropertiesExpanded,
            onToggle = {
                customPropertiesExpanded = !customPropertiesExpanded
                window?.refresh()
            },
        )
        if (customPropertiesExpanded) {
            val body = parent.container().xy(0.0, MapViewerDialogSpacing.INPUT_HEIGHT)
            customPropertiesEditor.renderBody(body, width)
        }
    }

    private fun customPropertiesPanelHeight(): Double =
        MapViewerDialogSpacing.INPUT_HEIGHT +
                if (customPropertiesExpanded) customPropertiesEditor.bodyHeight() else 0.0

    private fun canSave(): Boolean =
        MapViewerTextInputValidation.isPresent(nameValue) &&
                isPositionValid(positionXValue, positionYValue) &&
                isFacingValid(facingValue) &&
                customPropertiesEditor.isValid

    private val shipPanelEnabled: Boolean
        get() = shipDefinition != null && MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL in MapViewerFeatureFlags.enabled

    private fun shipPanelHeight(): Double =
        if (shipPanelEnabled) shipDefinition?.let {
            OlympiadDawnShipStatusPanel.height(
                it,
                shipPanelCollapsed,
                shipComponentsCollapsed,
                shipWeaponsCollapsed,
                shipFightersCollapsed,
            ) + MapViewerDialogSpacing.SECTION_GAP
        } ?: 0.0 else 0.0

    private val squadronPanelEnabled: Boolean
        get() = squadronStatusProvider() != null && MapViewerFeatureFlag.OLYMPIAD_DAWN_EXPERIMENTAL in MapViewerFeatureFlags.enabled

    private fun squadronPanelHeight(): Double =
        if (squadronPanelEnabled) squadronStatusProvider()?.let {
            OlympiadDawnSquadronStatusPanel.height(it, squadronPanelCollapsed) + MapViewerDialogSpacing.SECTION_GAP
        } ?: 0.0 else 0.0

    private fun unitProperties(): Map<String, String> =
        shipDefinition?.let { definition ->
            customPropertiesEditor.properties() + initialShipProperties +
                    (OlympiadDawnShip.DAMAGE_KEY to OlympiadDawnShip.encodeDamage(shipDamage ?: OlympiadDawnShip.instanceDamage(initialShipProperties, definition)))
        } ?: customPropertiesEditor.properties() + initialShipProperties

    private fun fieldWidth(width: Double): Double =
        width

    private fun minimumContentWidth(): Double =
        maxOf(
            minimumTypePreviewWidth(),
            MapViewerInlineInputs.block(listOf("Name")).minimumWidth(nameTextInputMinimumWidth()),
            affiliationMinimumWidth(),
            positionInputMinimumWidth(),
            inlineBlockMinimumWidth(facingTextInputMinimumWidth()),
            inlineBlockMinimumWidth(DESTROYED_CHECKBOX_SIZE),
            customPropertiesEditor.minimumWidth(),
            MapViewerDialogActions.minimumWidth(2),
        )

    private fun minimumFieldWidth(): Double =
        maxOf(
            minimumTypePreviewWidth(),
            MapViewerInlineInputs.block(listOf("Name")).minimumWidth(nameTextInputMinimumWidth()),
            affiliationMinimumWidth(),
            positionInputMinimumWidth(),
            inlineBlockMinimumWidth(facingTextInputMinimumWidth()),
            inlineBlockMinimumWidth(DESTROYED_CHECKBOX_SIZE),
            customPropertiesEditor.minimumWidth(),
        )

    private fun minimumTypePreviewWidth(): Double =
        max(
            ICON_SLOT_WIDTH + 24.0 + MapViewerTextViews.measure(unitType.name).width,
            MapViewerTextViews.measure(MapViewerDialogTitles.unit("Edit Unit", nameValue)).width + 24.0,
        )

    private fun nameTextInputMinimumWidth(): Double =
        maxOf(MIN_FIELD_WIDTH, MapViewerTextViews.measure(nameValue.ifBlank { "Unit Name" }).width + 24.0)

    private fun positionInputMinimumWidth(): Double =
        positionAxisInputMinimumWidth(positionXValue, "X") +
                MapViewerDialogSpacing.FIELD_GAP +
                positionAxisInputMinimumWidth(positionYValue, "Y")

    private fun positionAxisInputMinimumWidth(value: String, fallback: String): Double =
        maxOf(POSITION_INPUT_MINIMUM_WIDTH, MapViewerTextViews.measure(value.ifBlank { fallback }).width + 24.0)

    private fun facingTextInputMinimumWidth(): Double =
        maxOf(POSITION_INPUT_MINIMUM_WIDTH, MapViewerTextViews.measure(facingValue.ifBlank { "Facing" }).width + 24.0)

    private fun inlineBlockMinimumWidth(inputMinimumWidth: Double): Double =
        MapViewerInlineInputs.block(UNIT_INLINE_LABELS).minimumWidth(inputMinimumWidth)

    private fun splitRowMinimumWidth(): Double =
        MapViewerInlineInputs.minimumWidth("Name", nameTextInputMinimumWidth()) +
                MapViewerDialogSpacing.FIELD_GAP +
                affiliationMinimumWidth()

    private companion object {
        const val TYPE = "unit-edit"
        const val PREFERRED_WIDTH = 560.0
        const val PREFERRED_HEIGHT = 540.0
        const val ICON_SLOT_WIDTH = 34.0
        const val ICON_SLOT_HEIGHT = 26.0
        const val MIN_FIELD_WIDTH = 120.0
        const val POSITION_INPUT_MINIMUM_WIDTH = 90.0
        const val DESTROYED_CHECKBOX_SIZE = 20.0
        const val TYPE_PREVIEW_TOP = 0.0
        const val POSITION_LABEL_TOP = 68.0
        const val POSITION_LABEL_HEIGHT = 22.0
        const val POSITION_INPUT_TOP = 96.0
        const val NAME_TOP = 150.0
        const val AFFILIATION_TOP = 204.0
        const val FACING_TOP = 258.0
        val UNIT_INLINE_LABELS = listOf("Name", "Facing", "KIA")
        val UNIT_FIELD_KEYS = setOf(
            "type",
            "unitTemplateId",
            "instanceName",
            "destroyed",
            "facingDegrees",
            "mapCoordinate",
            "normalizedWidth",
            "normalizedHeight",
        )
        val SHIP_PROPERTY_KEYS = setOf(OlympiadDawnShip.DAMAGE_KEY)
    }
}
