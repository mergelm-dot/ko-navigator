package de.konavigator.app.screens

import android.content.ClipData

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.konavigator.app.R
import de.konavigator.app.components.UnderlyingSearchField
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.models.UnderlyingAsset
import de.konavigator.app.presentation.tradeplanner.TradePlannerBrokerUiOption
import de.konavigator.app.presentation.tradeplanner.TradePlannerIssuerUiOption
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectedProductUiModel
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionCurrencyEvidence
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiDiagnostics
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiInputError
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiResult
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiState
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiSubmission
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiText
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

private val SelectionAppBackground = Color(0xFF040A0E)
private val SelectionCardBackground = Color(0xFF0C171D)
private val SelectionBorderColor = Color(0xFF283740)
private val SelectionPrimaryText = Color(0xFFF3F4F6)
private val SelectionSecondaryText = Color(0xFF9CA3AF)
private val SelectionAccentGreen = Color(0xFF20C967)
private val SelectionDangerRed = Color(0xFFFF4D4D)
private const val SelectionUnderlyingInputTestTag = "trade_planner_selection_underlying_input"
private const val SelectionPrimaryCandidateTestTag = "trade_planner_selection_primary_candidate"
private const val SelectionAlternativeCandidateTestTagPrefix =
    "trade_planner_selection_alternative_candidate_"

/** Parallele Compose-Ansicht für den vollständigen, providerneutralen Selection-Pfad. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradePlannerSelectionScreen(
    state: TradePlannerSelectionUiState,
    brokerOptions: List<TradePlannerBrokerUiOption>,
    issuerOptions: List<TradePlannerIssuerUiOption>,
    onUnderlyingSelected: (String?) -> Unit,
    onBrokerSelected: (String?) -> Unit,
    onEnabledIssuerIdsChanged: (Set<String>) -> Unit,
    onCurrentPriceChanged: (String) -> Unit,
    onPlannedEntryPriceChanged: (String) -> Unit,
    onTargetLeverageChanged: (String) -> Unit,
    onDirectionChanged: (TradeDirection) -> Unit,
    onCalculateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var underlyingQuery by remember { mutableStateOf("") }
    var selectedAsset by remember { mutableStateOf<UnderlyingAsset?>(null) }
    var brokerMenuExpanded by remember { mutableStateOf(false) }
    val accentColor = if (state.direction == TradeDirection.LONG) {
        SelectionAccentGreen
    } else {
        SelectionDangerRed
    }
    val inputErrors = (state.submission as? TradePlannerSelectionUiSubmission.InvalidInput)
        ?.errors
        .orEmpty()
    val isLoading = state.submission is TradePlannerSelectionUiSubmission.Loading
    val selectedBrokerName = brokerOptions.firstOrNull {
        it.id == state.selectedBrokerId
    }?.displayName.orEmpty()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.submission) {
        val completedResult =
            (state.submission as? TradePlannerSelectionUiSubmission.Completed)?.result
        if (completedResult is TradePlannerSelectionUiResult.Selected) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SelectionAppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.trade_planner_selection_title),
            color = SelectionPrimaryText,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SelectionCardBackground),
            border = BorderStroke(width = 1.dp, color = SelectionBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.trade_planner_selection_setup_title),
                    color = SelectionPrimaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                UnderlyingSearchField(
                    value = underlyingQuery,
                    onValueChange = { value ->
                        underlyingQuery = value
                        if (selectedAsset != null) {
                            selectedAsset = null
                            onUnderlyingSelected(null)
                        }
                    },
                    onAssetSelected = { asset ->
                        selectedAsset = asset
                        underlyingQuery = asset.displayName
                        onUnderlyingSelected(asset.id)
                        asset.currentPrice?.let { currentPrice ->
                            val priceInput = currentPrice.toString()
                            onCurrentPriceChanged(priceInput)
                            onPlannedEntryPriceChanged(priceInput)
                        } ?: run {
                            onCurrentPriceChanged("")
                            onPlannedEntryPriceChanged("")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(SelectionUnderlyingInputTestTag)
                )
                SelectionFieldError(
                    errors = inputErrors,
                    acceptedErrors = setOf(TradePlannerSelectionUiInputError.UNDERLYING_REQUIRED)
                )

                selectedAsset?.let { asset ->
                    Text(
                        text = "${asset.referenceExchange} · ${asset.currency}",
                        color = SelectionSecondaryText,
                        fontSize = 14.sp
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = brokerMenuExpanded,
                    onExpandedChange = { brokerMenuExpanded = !brokerMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBrokerName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            ),
                        label = { Text(stringResource(R.string.trade_planner_selection_broker_label)) },
                        placeholder = {
                            Text(stringResource(R.string.trade_planner_selection_broker_placeholder))
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = brokerMenuExpanded
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = selectionTextFieldColors(accentColor)
                    )

                    ExposedDropdownMenu(
                        expanded = brokerMenuExpanded,
                        onDismissRequest = { brokerMenuExpanded = false }
                    ) {
                        brokerOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    brokerMenuExpanded = false
                                    onBrokerSelected(option.id)
                                }
                            )
                        }
                    }
                }
                SelectionFieldError(
                    errors = inputErrors,
                    acceptedErrors = setOf(TradePlannerSelectionUiInputError.BROKER_REQUIRED)
                )

                ControlledIssuerSelection(
                    issuerOptions = issuerOptions,
                    enabledIssuerIds = state.enabledIssuerIds,
                    onEnabledIssuerIdsChanged = onEnabledIssuerIdsChanged
                )

                Text(
                    text = stringResource(R.string.trade_planner_selection_direction_label),
                    color = SelectionSecondaryText,
                    fontSize = 15.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    SelectionDirectionOption(
                        text = stringResource(R.string.trade_planner_selection_direction_long),
                        selected = state.direction == TradeDirection.LONG,
                        onClick = { onDirectionChanged(TradeDirection.LONG) },
                        accentColor = accentColor
                    )
                    SelectionDirectionOption(
                        text = stringResource(R.string.trade_planner_selection_direction_short),
                        selected = state.direction == TradeDirection.SHORT,
                        onClick = { onDirectionChanged(TradeDirection.SHORT) },
                        accentColor = accentColor
                    )
                }

                SelectionNumericField(
                    label = stringResource(R.string.trade_planner_selection_current_price_label),
                    value = state.currentUnderlyingPriceInput,
                    onValueChange = onCurrentPriceChanged,
                    suffix = selectedAsset?.currency,
                    errors = inputErrors,
                    acceptedErrors = setOf(
                        TradePlannerSelectionUiInputError.CURRENT_PRICE_REQUIRED,
                        TradePlannerSelectionUiInputError.CURRENT_PRICE_INVALID
                    ),
                    accentColor = accentColor
                )
                SelectionNumericField(
                    label = stringResource(R.string.trade_planner_selection_planned_entry_price_label),
                    value = state.plannedEntryPriceInput,
                    onValueChange = onPlannedEntryPriceChanged,
                    suffix = selectedAsset?.currency,
                    errors = inputErrors,
                    acceptedErrors = setOf(
                        TradePlannerSelectionUiInputError.PLANNED_ENTRY_PRICE_REQUIRED,
                        TradePlannerSelectionUiInputError.PLANNED_ENTRY_PRICE_INVALID
                    ),
                    accentColor = accentColor
                )
                SelectionNumericField(
                    label = stringResource(R.string.trade_planner_selection_target_leverage_label),
                    value = state.targetLeverageInput,
                    onValueChange = onTargetLeverageChanged,
                    errors = inputErrors,
                    acceptedErrors = setOf(
                        TradePlannerSelectionUiInputError.TARGET_LEVERAGE_REQUIRED,
                        TradePlannerSelectionUiInputError.TARGET_LEVERAGE_INVALID
                    ),
                    accentColor = accentColor
                )

                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.height(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.trade_planner_selection_loading),
                            color = SelectionSecondaryText
                        )
                    }
                }

                Button(
                    onClick = onCalculateClicked,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = stringResource(R.string.trade_planner_selection_calculate),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        SelectionSubmissionContent(
            submission = state.submission,
            issuerOptions = issuerOptions
        )
    }
}

@Composable
private fun ControlledIssuerSelection(
    issuerOptions: List<TradePlannerIssuerUiOption>,
    enabledIssuerIds: Set<String>,
    onEnabledIssuerIdsChanged: (Set<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.trade_planner_selection_issuer_title),
            color = SelectionSecondaryText,
            fontSize = 15.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    onEnabledIssuerIdsChanged(issuerOptions.map { it.id }.toSet())
                }
            ) {
                Text(stringResource(R.string.trade_planner_selection_select_all_issuers))
            }
            TextButton(onClick = { onEnabledIssuerIdsChanged(emptySet()) }) {
                Text(stringResource(R.string.trade_planner_selection_clear_all_issuers))
            }
        }
        issuerOptions.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = option.id in enabledIssuerIds,
                    onCheckedChange = { checked ->
                        val nextIssuerIds = if (checked) {
                            enabledIssuerIds + option.id
                        } else {
                            enabledIssuerIds - option.id
                        }
                        onEnabledIssuerIdsChanged(nextIssuerIds)
                    }
                )
                Text(text = option.displayName, color = SelectionPrimaryText)
            }
        }
    }
}

@Composable
private fun SelectionNumericField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    errors: List<TradePlannerSelectionUiInputError>,
    acceptedErrors: Set<TradePlannerSelectionUiInputError>,
    accentColor: Color,
    suffix: String? = null
) {
    val error = errors.firstOrNull { it in acceptedErrors }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        suffix = suffix?.let { currency -> { Text(currency) } },
        isError = error != null,
        supportingText = error?.let { inputError ->
            { Text(stringResource(TradePlannerSelectionUiText.inputErrorResource(inputError))) }
        },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        shape = RoundedCornerShape(16.dp),
        colors = selectionTextFieldColors(accentColor)
    )
}

@Composable
private fun SelectionFieldError(
    errors: List<TradePlannerSelectionUiInputError>,
    acceptedErrors: Set<TradePlannerSelectionUiInputError>
) {
    errors.firstOrNull { it in acceptedErrors }?.let { error ->
        Text(
            text = stringResource(TradePlannerSelectionUiText.inputErrorResource(error)),
            color = SelectionDangerRed,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SelectionDirectionOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier.selectable(
            selected = selected,
            onClick = onClick,
            role = Role.RadioButton
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.clearAndSetSemantics {}) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accentColor,
                    unselectedColor = SelectionSecondaryText
                )
            )
        }
        Text(
            text = text,
            color = SelectionPrimaryText,
            modifier = Modifier.padding(end = 8.dp),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun SelectionSubmissionContent(
    submission: TradePlannerSelectionUiSubmission,
    issuerOptions: List<TradePlannerIssuerUiOption>
) {
    when (submission) {
        TradePlannerSelectionUiSubmission.Idle,
        is TradePlannerSelectionUiSubmission.InvalidInput,
        TradePlannerSelectionUiSubmission.Loading -> Unit

        is TradePlannerSelectionUiSubmission.Completed -> {
            Spacer(modifier = Modifier.height(20.dp))
            when (val result = submission.result) {
                is TradePlannerSelectionUiResult.Selected -> SelectionResultContent(
                    result = result,
                    issuerOptions = issuerOptions
                )

                is TradePlannerSelectionUiResult.NoSelection -> NoSelectionContent(result)
                is TradePlannerSelectionUiResult.InconsistentData -> InconsistentDataContent(result)
            }
        }
    }
}

@Composable
private fun SelectionResultContent(
    result: TradePlannerSelectionUiResult.Selected,
    issuerOptions: List<TradePlannerIssuerUiOption>
) {
    var detailsExpanded by rememberSaveable(result.primaryCandidate.productIsin) {
        mutableStateOf(false)
    }
    var alternativesExpanded by rememberSaveable(
        result.primaryCandidate.productIsin,
        result.alternativeCandidates.map { it.productIsin }
    ) {
        mutableStateOf(false)
    }

    SelectionPrimaryCandidateCard(
        candidate = result.primaryCandidate,
        issuerOptions = issuerOptions,
        detailsExpanded = detailsExpanded,
        onDetailsExpandedChange = { detailsExpanded = it }
    )

    if (result.alternativeCandidates.isNotEmpty()) {
        SelectionExpansionButton(
            expanded = alternativesExpanded,
            showText = stringResource(R.string.trade_planner_selection_show_alternatives),
            hideText = stringResource(R.string.trade_planner_selection_hide_alternatives),
            onExpandedChange = { alternativesExpanded = it }
        )
    }

    if (alternativesExpanded) {
        result.alternativeCandidates.forEachIndexed { index, candidate ->
            Spacer(modifier = Modifier.height(12.dp))
            SelectionCompactAlternativeCard(
                title = stringResource(
                    R.string.trade_planner_selection_alternative_title,
                    index + 1
                ),
                candidate = candidate,
                issuerOptions = issuerOptions
            )
        }
    }
    SelectionDiagnostics(result.diagnostics)
}

@Composable
private fun SelectionPrimaryCandidateCard(
    candidate: TradePlannerSelectedProductUiModel,
    issuerOptions: List<TradePlannerIssuerUiOption>,
    detailsExpanded: Boolean,
    onDetailsExpandedChange: (Boolean) -> Unit
) {
    val priceFormatter = rememberNumberFormatter(2, 4)
    val leverageFormatter = rememberNumberFormatter(2, 4)
    val barrierFormatter = rememberNumberFormatter(2, 4)
    val percentFormatter = rememberNumberFormatter(2, 4)
    val issuerName = issuerOptions.firstOrNull { it.id == candidate.issuerId }
        ?.displayName
        ?: candidate.issuerId
    val formattedProductPrice =
        priceFormatter.format(candidate.calculatedProductPriceAtPlannedEntry)
    val productPriceText = "$formattedProductPrice ${candidate.productCurrency}"
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val wknLabel = stringResource(R.string.trade_planner_selection_wkn_label)
    val priceLabel = stringResource(R.string.trade_planner_selection_product_price_label)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SelectionPrimaryCandidateTestTag),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SelectionCardBackground),
        border = BorderStroke(width = 1.dp, color = SelectionBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = if (detailsExpanded) 20.dp else 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(
                if (detailsExpanded) 12.dp else 6.dp
            )
        ) {
            Text(
                text = stringResource(R.string.trade_planner_selection_primary_title),
                color = SelectionPrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            SelectionValueRow(
                label = stringResource(R.string.trade_planner_selection_issuer_label),
                value = issuerName
            )
            SelectionCopyableValueRow(
                label = wknLabel,
                value = candidate.productWkn
                    ?: stringResource(R.string.trade_planner_selection_not_available),
                copyContentDescription = candidate.productWkn?.let {
                    stringResource(R.string.trade_planner_selection_copy_value, wknLabel)
                },
                onCopy = candidate.productWkn?.let { productWkn ->
                    {
                        coroutineScope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText(wknLabel, productWkn))
                            )
                        }
                    }
                }
            )
            SelectionCopyableValueRow(
                label = priceLabel,
                value = productPriceText,
                copyContentDescription = stringResource(
                    R.string.trade_planner_selection_copy_value,
                    priceLabel
                ),
                onCopy = {
                    coroutineScope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText(priceLabel, formattedProductPrice))
                        )
                    }
                }
            )
            SelectionExpansionButton(
                expanded = detailsExpanded,
                showText = stringResource(R.string.trade_planner_selection_show_details),
                hideText = stringResource(R.string.trade_planner_selection_hide_details),
                onExpandedChange = onDetailsExpandedChange
            )
            if (detailsExpanded) {
                SelectionCandidateDetails(
                    candidate = candidate,
                    leverageFormatter = leverageFormatter,
                    barrierFormatter = barrierFormatter,
                    percentFormatter = percentFormatter
                )
            }
        }
    }
}

@Composable
private fun SelectionCandidateDetails(
    candidate: TradePlannerSelectedProductUiModel,
    leverageFormatter: NumberFormat,
    barrierFormatter: NumberFormat,
    percentFormatter: NumberFormat
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectionValueRow(
            label = stringResource(R.string.trade_planner_selection_isin_label),
            value = candidate.productIsin
        )
        SelectionValueRow(
            label = stringResource(R.string.trade_planner_selection_product_currency_label),
            value = candidate.productCurrency
        )
        SelectionValueRow(
            label = stringResource(R.string.trade_planner_selection_leverage_label),
            value = stringResource(
                R.string.trade_planner_selection_leverage_value,
                leverageFormatter.format(candidate.calculatedLeverageAtPlannedEntry)
            )
        )
        SelectionValueRow(
            label = stringResource(R.string.trade_planner_selection_knockout_barrier_label),
            value = barrierFormatter.format(candidate.knockoutBarrier)
        )
        SelectionValueRow(
            label = stringResource(
                R.string.trade_planner_selection_knockout_distance_percent_label
            ),
            value = stringResource(
                R.string.trade_planner_selection_percent_value,
                percentFormatter.format(candidate.knockoutDistancePercent)
            )
        )
        SelectionValueRow(
            label = stringResource(
                R.string.trade_planner_selection_knockout_distance_absolute_label
            ),
            value = barrierFormatter.format(candidate.knockoutDistanceAbsolute)
        )
        SelectionValueRow(
            label = stringResource(
                R.string.trade_planner_selection_relative_leverage_deviation_label
            ),
            value = stringResource(
                R.string.trade_planner_selection_percent_value,
                percentFormatter.format(candidate.relativeLeverageDeviationPercent)
            )
        )
        SelectionValueRow(
            label = stringResource(R.string.trade_planner_selection_barrier_deviation_label),
            value = stringResource(
                R.string.trade_planner_selection_percent_value,
                percentFormatter.format(candidate.barrierDeviationPercentOfPlannedEntry)
            )
        )
        SelectionBooleanValueRow(
            label = stringResource(
                R.string.trade_planner_selection_leverage_within_tolerance_label
            ),
            value = candidate.leverageWithinTolerance
        )
        SelectionBooleanValueRow(
            label = stringResource(
                R.string.trade_planner_selection_barrier_within_tolerance_label
            ),
            value = candidate.barrierWithinTolerance
        )
        SelectionBooleanValueRow(
            label = stringResource(R.string.trade_planner_selection_all_tolerances_label),
            value = candidate.withinAllTargetTolerances
        )
        SelectionCurrencyEvidence(candidate.currencyEvidence)
    }
}

@Composable
private fun SelectionCompactAlternativeCard(
    title: String,
    candidate: TradePlannerSelectedProductUiModel,
    issuerOptions: List<TradePlannerIssuerUiOption>
) {
    val priceFormatter = rememberNumberFormatter(2, 4)
    val issuerName = issuerOptions.firstOrNull { it.id == candidate.issuerId }
        ?.displayName
        ?: candidate.issuerId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SelectionAlternativeCandidateTestTagPrefix + candidate.productIsin),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SelectionCardBackground),
        border = BorderStroke(width = 1.dp, color = SelectionBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = SelectionPrimaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            SelectionValueRow(
                label = stringResource(R.string.trade_planner_selection_issuer_label),
                value = issuerName
            )
            SelectionValueRow(
                label = stringResource(R.string.trade_planner_selection_wkn_label),
                value = candidate.productWkn
                    ?: stringResource(R.string.trade_planner_selection_not_available)
            )
            SelectionValueRow(
                label = stringResource(R.string.trade_planner_selection_product_price_label),
                value =
                    "${priceFormatter.format(candidate.calculatedProductPriceAtPlannedEntry)} " +
                        candidate.productCurrency
            )
        }
    }
}

@Composable
private fun SelectionExpansionButton(
    expanded: Boolean,
    showText: String,
    hideText: String,
    onExpandedChange: (Boolean) -> Unit
) {
    TextButton(
        onClick = { onExpandedChange(!expanded) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = if (expanded) hideText else showText)
    }
}

@Composable
private fun SelectionCopyableValueRow(
    label: String,
    value: String,
    copyContentDescription: String?,
    onCopy: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, color = SelectionSecondaryText, fontSize = 13.sp)
            Text(
                text = value,
                color = SelectionPrimaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (onCopy != null && copyContentDescription != null) {
            TextButton(
                onClick = onCopy,
                modifier = Modifier.semantics {
                    contentDescription = copyContentDescription
                }
            ) {
                Text(stringResource(R.string.trade_planner_selection_copy))
            }
        }
    }
}

@Composable
private fun SelectionBooleanValueRow(
    label: String,
    value: Boolean
) {
    SelectionValueRow(
        label = label,
        value = stringResource(
            if (value) {
                R.string.trade_planner_selection_yes
            } else {
                R.string.trade_planner_selection_no
            }
        )
    )
}

@Composable
private fun SelectionCurrencyEvidence(evidence: TradePlannerSelectionCurrencyEvidence) {
    val text = when (evidence) {
        TradePlannerSelectionCurrencyEvidence.SameCurrency ->
            stringResource(R.string.trade_planner_selection_same_currency)

        is TradePlannerSelectionCurrencyEvidence.CrossCurrency -> stringResource(
            R.string.trade_planner_selection_cross_currency,
            evidence.sourceId
        )
    }
    Text(text = text, color = SelectionSecondaryText, fontSize = 13.sp)
}

@Composable
private fun NoSelectionContent(result: TradePlannerSelectionUiResult.NoSelection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SelectionCardBackground),
        border = BorderStroke(width = 1.dp, color = SelectionBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.trade_planner_selection_no_selection_title),
                color = SelectionPrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    TradePlannerSelectionUiText.noSelectionReasonResource(result.reason)
                ),
                color = SelectionSecondaryText,
                fontSize = 15.sp
            )
        }
    }
    SelectionDiagnostics(result.diagnostics)
}

@Composable
private fun InconsistentDataContent(result: TradePlannerSelectionUiResult.InconsistentData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SelectionCardBackground),
        border = BorderStroke(width = 1.dp, color = SelectionBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.trade_planner_selection_inconsistent_title),
                color = SelectionDangerRed,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.trade_planner_selection_inconsistent_description),
                color = SelectionSecondaryText,
                fontSize = 15.sp
            )
            Text(
                text = stringResource(
                    TradePlannerSelectionUiText.mappingErrorResource(result.error)
                ),
                color = SelectionSecondaryText,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SelectionDiagnostics(diagnostics: TradePlannerSelectionUiDiagnostics) {
    val hasVisibleDiagnostics = listOf(
        diagnostics.dataQualityBlockedCount,
        diagnostics.calculationUnavailableCount,
        diagnostics.notFreshCount,
        diagnostics.sourceBlockedCount,
        diagnostics.calculationFailedCount,
        diagnostics.currencyConversionFailedCount,
        diagnostics.invalidTargetLeveragePlanCount,
        diagnostics.existingEntryFailedCount,
        diagnostics.targetDeviationFailedCount,
        diagnostics.nonMatchingTargetFitCount,
        diagnostics.targetFitFailedCount
    ).any { it > 0 }
    if (!hasVisibleDiagnostics) {
        return
    }

    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SelectionCardBackground),
        border = BorderStroke(width = 1.dp, color = SelectionBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.trade_planner_selection_diagnostics_title),
                color = SelectionPrimaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            SelectionDiagnosticValue(
                R.string.trade_planner_selection_diagnostic_data_quality_blocked,
                diagnostics.dataQualityBlockedCount
            )
            SelectionDiagnosticValue(
                R.string.trade_planner_selection_diagnostic_calculation_unavailable,
                diagnostics.calculationUnavailableCount
            )
            SelectionDiagnosticValue(
                R.string.trade_planner_selection_diagnostic_not_fresh,
                diagnostics.notFreshCount
            )
            SelectionDiagnosticValue(
                R.string.trade_planner_selection_diagnostic_source_blocked,
                diagnostics.sourceBlockedCount
            )
            SelectionDiagnosticValue(
                R.string.trade_planner_selection_diagnostic_calculation_failed,
                diagnostics.calculationFailedCount
            )
            SelectionDiagnosticValue(
                R.string.trade_planner_selection_diagnostic_currency_failed,
                diagnostics.currencyConversionFailedCount
            )
            SelectionDiagnosticValue(
                R.string.trade_planner_selection_diagnostic_target_plan_invalid,
                diagnostics.invalidTargetLeveragePlanCount
            )
            SelectionDiagnosticValue(
                R.string.trade_planner_selection_diagnostic_existing_entry_failed,
                diagnostics.existingEntryFailedCount
            )
            SelectionDiagnosticValue(
                R.string.trade_planner_selection_diagnostic_target_deviation_failed,
                diagnostics.targetDeviationFailedCount
            )
            SelectionDiagnosticValue(
                R.string.trade_planner_selection_diagnostic_target_fit_non_matching,
                diagnostics.nonMatchingTargetFitCount
            )
            SelectionDiagnosticValue(
                R.string.trade_planner_selection_diagnostic_target_fit_failed,
                diagnostics.targetFitFailedCount
            )
        }
    }
}

@Composable
private fun SelectionDiagnosticValue(
    labelResource: Int,
    count: Int
) {
    if (count > 0) {
        SelectionValueRow(
            label = stringResource(labelResource),
            value = count.toString()
        )
    }
}

@Composable
private fun SelectionValueRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = SelectionSecondaryText, fontSize = 13.sp)
        Text(text = value, color = SelectionPrimaryText, fontSize = 15.sp)
    }
}

@Composable
private fun rememberNumberFormatter(
    minimumFractionDigits: Int,
    maximumFractionDigits: Int
): NumberFormat {
    val locale = Locale.getDefault()
    return remember(locale, minimumFractionDigits, maximumFractionDigits) {
        NumberFormat.getNumberInstance(locale).apply {
            this.minimumFractionDigits = minimumFractionDigits
            this.maximumFractionDigits = maximumFractionDigits
            isGroupingUsed = false
        }
    }
}

@Composable
private fun selectionTextFieldColors(accentColor: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SelectionPrimaryText,
    unfocusedTextColor = SelectionPrimaryText,
    focusedLabelColor = accentColor,
    unfocusedLabelColor = SelectionSecondaryText,
    focusedBorderColor = accentColor,
    unfocusedBorderColor = SelectionBorderColor,
    cursorColor = accentColor,
    focusedContainerColor = SelectionCardBackground,
    unfocusedContainerColor = SelectionCardBackground
)
