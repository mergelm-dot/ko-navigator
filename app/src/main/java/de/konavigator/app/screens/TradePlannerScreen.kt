package de.konavigator.app.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.konavigator.app.R
import de.konavigator.app.components.IssuerSelector
import de.konavigator.app.components.UnderlyingSearchField
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.tradeplanning.EntryPriceRelation
import de.konavigator.app.models.IssuerOption
import de.konavigator.app.models.UnderlyingAsset
import de.konavigator.app.presentation.tradeplanner.TradePlannerUiCalculationError
import de.konavigator.app.presentation.tradeplanner.TradePlannerUiInputError
import de.konavigator.app.presentation.tradeplanner.TradePlannerUiResult
import de.konavigator.app.presentation.tradeplanner.TradePlannerUiState
import de.konavigator.app.presentation.tradeplanner.TradePlannerUiSubmission
import java.text.NumberFormat
import java.util.Locale

private val AppBackground = Color(0xFF040A0E)
private val CardBackground = Color(0xFF0C171D)
private val BorderColor = Color(0xFF283740)
private val PrimaryText = Color(0xFFF3F4F6)
private val SecondaryText = Color(0xFF9CA3AF)
private val AccentGreen = Color(0xFF20C967)
private val DangerRed = Color(0xFFFF4D4D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradePlannerScreen(
    state: TradePlannerUiState,
    onCurrentPriceChanged: (String) -> Unit,
    onPlannedEntryPriceChanged: (String) -> Unit,
    onTargetLeverageChanged: (String) -> Unit,
    onDirectionChanged: (TradeDirection) -> Unit,
    onCalculateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {

    var underlying by remember { mutableStateOf("NVIDIA") }
    var selectedAsset by remember { mutableStateOf<UnderlyingAsset?>(null) }
    var selectedBroker by remember {
        mutableStateOf("Scalable Capital")
    }

    var brokerMenuExpanded by remember {
        mutableStateOf(false)
    }

    val brokers = listOf(
        "Scalable Capital",
        "Trade Republic",
        "ING",
        "comdirect",
        "Consorsbank",
        "flatex",
        "Alle Broker"
    )
    val issuers = remember {
        mutableStateListOf(
            IssuerOption("hsbc", "HSBC"),
            IssuerOption("ubs", "UBS"),
            IssuerOption("societe_generale", "Société Générale")
        )
    }

    val accentColor = if (state.direction == TradeDirection.LONG) {
        AccentGreen
    } else {
        DangerRed
    }
    val selectedCurrency = selectedAsset?.currency
    val inputErrors = (state.submission as? TradePlannerUiSubmission.InvalidInput)
        ?.errors
        .orEmpty()
    val currentPriceError = inputErrors.firstOrNull {
        it == TradePlannerUiInputError.CURRENT_PRICE_REQUIRED ||
            it == TradePlannerUiInputError.CURRENT_PRICE_INVALID
    }
    val plannedEntryPriceError = inputErrors.firstOrNull {
        it == TradePlannerUiInputError.PLANNED_ENTRY_PRICE_REQUIRED ||
            it == TradePlannerUiInputError.PLANNED_ENTRY_PRICE_INVALID
    }
    val targetLeverageError = inputErrors.firstOrNull {
        it == TradePlannerUiInputError.TARGET_LEVERAGE_REQUIRED ||
            it == TradePlannerUiInputError.TARGET_LEVERAGE_INVALID
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {

        Text(
            text = "KO Navigator",
            color = PrimaryText,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Plane deinen Trade",
            color = SecondaryText,
            fontSize = 17.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = BorderColor
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "Trade-Setup",
                    color = PrimaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                UnderlyingSearchField(
                    value = underlying,
                    onValueChange = { underlying = it },
                    onAssetSelected = { asset ->
                        selectedAsset = asset
                        underlying = asset.displayName
                        val formattedPrice = asset.currentPrice?.let { price ->
                            String.format(Locale.GERMANY, "%.2f", price)
                        }.orEmpty()
                        onCurrentPriceChanged(formattedPrice)
                        onPlannedEntryPriceChanged(formattedPrice)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                selectedAsset?.let { asset ->
                    Text(
                        text = "Börse: ${asset.referenceExchange}",
                        color = SecondaryText
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Währung: ${asset.currency}",
                        color = SecondaryText
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
                ExposedDropdownMenuBox(
                    expanded = brokerMenuExpanded,
                    onExpandedChange = {
                        brokerMenuExpanded = !brokerMenuExpanded
                    }
                ) {
                    OutlinedTextField(
                        value = selectedBroker,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = {
                            Text("Broker")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = brokerMenuExpanded
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = SecondaryText,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = brokerMenuExpanded,
                        onDismissRequest = {
                            brokerMenuExpanded = false
                        }
                    ) {
                        brokers.forEach { broker ->
                            DropdownMenuItem(
                                text = {
                                    Text(broker)
                                },
                                onClick = {
                                    selectedBroker = broker
                                    brokerMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                IssuerSelector(
                    issuers = issuers
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Richtung",
                    color = SecondaryText,
                    fontSize = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    DirectionOption(
                        text = "Long",
                        selected = state.direction == TradeDirection.LONG,
                        onClick = { onDirectionChanged(TradeDirection.LONG) },
                        accentColor = accentColor
                    )

                    DirectionOption(
                        text = "Short",
                        selected = state.direction == TradeDirection.SHORT,
                        onClick = { onDirectionChanged(TradeDirection.SHORT) },
                        accentColor = accentColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                PlannerTextField(
                    label = "Aktueller Kurs",
                    value = state.currentUnderlyingPriceInput,
                    onValueChange = onCurrentPriceChanged,
                    numeric = true,
                    suffix = selectedCurrency,
                    accentColor = accentColor,
                    isError = currentPriceError != null,
                    supportingText = currentPriceError?.let {
                        stringResource(inputErrorResource(it))
                    },
                    modifier = Modifier.testTag(TRADE_PLANNER_CURRENT_PRICE_TAG)
                )

                PlannerTextField(
                    label = stringResource(R.string.trade_planner_planned_entry_price_label),
                    value = state.plannedEntryPriceInput,
                    onValueChange = onPlannedEntryPriceChanged,
                    numeric = true,
                    suffix = selectedCurrency,
                    accentColor = accentColor,
                    isError = plannedEntryPriceError != null,
                    supportingText = plannedEntryPriceError?.let {
                        stringResource(inputErrorResource(it))
                    },
                    modifier = Modifier.testTag(TRADE_PLANNER_PLANNED_ENTRY_PRICE_TAG)
                )

                PlannerTextField(
                    label = "Hebel",
                    value = state.targetLeverageInput,
                    onValueChange = onTargetLeverageChanged,
                    numeric = true,
                    accentColor = accentColor,
                    isError = targetLeverageError != null,
                    supportingText = targetLeverageError?.let {
                        stringResource(inputErrorResource(it))
                    },
                    modifier = Modifier.testTag(TRADE_PLANNER_TARGET_LEVERAGE_TAG)
                )

                Button(
                    onClick = onCalculateClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag(TRADE_PLANNER_CALCULATE_TAG),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = stringResource(R.string.trade_planner_calculate),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        TradePlannerSubmissionContent(state.submission)
    }
}

@Composable
private fun PlannerTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: Color,
    numeric: Boolean = false,
    suffix: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(label)
        },
        suffix = suffix?.let { valueSuffix ->
            { Text(valueSuffix) }
        },
        isError = isError,
        supportingText = supportingText?.let { text ->
            {
                Text(
                    text = text,
                    modifier = Modifier.semantics(mergeDescendants = true) {}
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) {
                KeyboardType.Decimal
            } else {
                KeyboardType.Text
            }
        ),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PrimaryText,
            unfocusedTextColor = PrimaryText,
            focusedLabelColor = accentColor,
            unfocusedLabelColor = SecondaryText,
            focusedBorderColor = accentColor,
            unfocusedBorderColor = BorderColor,
            cursorColor = accentColor,
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground
        )
    )
}

@Composable
private fun DirectionOption(
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
                    unselectedColor = SecondaryText
                )
            )
        }

        Text(
            text = text,
            color = PrimaryText,
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(end = 8.dp),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun TradePlannerSubmissionContent(
    submission: TradePlannerUiSubmission
) {
    when (submission) {
        TradePlannerUiSubmission.Idle,
        is TradePlannerUiSubmission.InvalidInput -> Unit

        is TradePlannerUiSubmission.Completed -> {
            Spacer(modifier = Modifier.height(20.dp))
            when (val result = submission.result) {
                is TradePlannerUiResult.Success -> TradePlannerResultCard(result)
                is TradePlannerUiResult.Failure -> TradePlannerFailureCard(result.error)
            }
        }
    }
}

@Composable
private fun TradePlannerResultCard(result: TradePlannerUiResult.Success) {
    val modelValueFormatter = rememberNumberFormatter(
        minimumFractionDigits = 4,
        maximumFractionDigits = 8
    )
    val priceFormatter = rememberNumberFormatter(
        minimumFractionDigits = 2,
        maximumFractionDigits = 8
    )
    val percentFormatter = rememberNumberFormatter(
        minimumFractionDigits = 2,
        maximumFractionDigits = 4
    )
    val leverageFormatter = rememberNumberFormatter(
        minimumFractionDigits = 0,
        maximumFractionDigits = 4
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TRADE_PLANNER_RESULT_TAG),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(width = 1.dp, color = BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.trade_planner_result_title),
                color = PrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            ResultValueRow(
                label = stringResource(R.string.trade_planner_result_relation),
                value = stringResource(relationResource(result.relation))
            )
            ResultValueRow(
                label = stringResource(R.string.trade_planner_result_model_value),
                value = modelValueFormatter.format(result.theoreticalProductValue)
            )
            ResultValueRow(
                label = stringResource(R.string.trade_planner_result_target_leverage),
                value = leverageFormatter.format(result.targetLeverage)
            )
            ResultValueRow(
                label = stringResource(
                    R.string.trade_planner_result_calculated_theoretical_leverage
                ),
                value = leverageFormatter.format(
                    result.calculatedTheoreticalLeverageAtEntry
                )
            )
            ResultValueRow(
                label = stringResource(R.string.trade_planner_result_knockout_price),
                value = priceFormatter.format(result.knockoutPrice)
            )
            ResultValueRow(
                label = stringResource(R.string.trade_planner_result_distance_absolute),
                value = priceFormatter.format(result.distanceToKnockoutAbsolute)
            )
            ResultValueRow(
                label = stringResource(R.string.trade_planner_result_distance_percent),
                value = stringResource(
                    R.string.trade_planner_result_percent_value,
                    percentFormatter.format(result.distanceToKnockoutPercent)
                )
            )
            ResultValueRow(
                label = stringResource(R.string.trade_planner_result_data_quality),
                value = stringResource(
                    R.string.trade_planner_result_data_quality_not_evaluated
                )
            )
            Text(
                text = stringResource(R.string.trade_planner_result_model_notice),
                color = SecondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun TradePlannerFailureCard(error: TradePlannerUiCalculationError) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TRADE_PLANNER_ERROR_TAG),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(width = 1.dp, color = BorderColor)
    ) {
        Text(
            text = stringResource(calculationErrorResource(error)),
            modifier = Modifier.padding(20.dp),
            color = DangerRed,
            fontSize = 15.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun ResultValueRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = SecondaryText, fontSize = 13.sp)
        Text(text = value, color = PrimaryText, fontSize = 15.sp)
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

@StringRes
private fun inputErrorResource(error: TradePlannerUiInputError): Int = when (error) {
    TradePlannerUiInputError.CURRENT_PRICE_REQUIRED ->
        R.string.trade_planner_error_current_price_required

    TradePlannerUiInputError.CURRENT_PRICE_INVALID ->
        R.string.trade_planner_error_current_price_invalid

    TradePlannerUiInputError.PLANNED_ENTRY_PRICE_REQUIRED ->
        R.string.trade_planner_error_planned_entry_price_required

    TradePlannerUiInputError.PLANNED_ENTRY_PRICE_INVALID ->
        R.string.trade_planner_error_planned_entry_price_invalid

    TradePlannerUiInputError.TARGET_LEVERAGE_REQUIRED ->
        R.string.trade_planner_error_target_leverage_required

    TradePlannerUiInputError.TARGET_LEVERAGE_INVALID ->
        R.string.trade_planner_error_target_leverage_invalid
}

@StringRes
private fun calculationErrorResource(error: TradePlannerUiCalculationError): Int = when (error) {
    TradePlannerUiCalculationError.INVALID_PLANNED_ENTRY_PRICE ->
        R.string.trade_planner_error_invalid_planned_entry_price

    TradePlannerUiCalculationError.INVALID_TARGET_LEVERAGE ->
        R.string.trade_planner_error_invalid_target_leverage

    TradePlannerUiCalculationError.INVALID_RATIO ->
        R.string.trade_planner_error_invalid_ratio

    TradePlannerUiCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE ->
        R.string.trade_planner_error_invalid_derived_knockout_price

    TradePlannerUiCalculationError.INVALID_EXCHANGE_RATE ->
        R.string.trade_planner_error_invalid_exchange_rate

    TradePlannerUiCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE ->
        R.string.trade_planner_error_invalid_theoretical_product_value

    TradePlannerUiCalculationError.INVALID_CALCULATED_LEVERAGE ->
        R.string.trade_planner_error_invalid_calculated_leverage

    TradePlannerUiCalculationError.INCONSISTENT_CALCULATION_RESULT ->
        R.string.trade_planner_error_inconsistent_calculation_result
}

@StringRes
private fun relationResource(relation: EntryPriceRelation): Int = when (relation) {
    EntryPriceRelation.BELOW_CURRENT -> R.string.trade_planner_relation_below_current
    EntryPriceRelation.AT_CURRENT -> R.string.trade_planner_relation_at_current
    EntryPriceRelation.ABOVE_CURRENT -> R.string.trade_planner_relation_above_current
}

private const val TRADE_PLANNER_CURRENT_PRICE_TAG = "trade_planner_current_price"
private const val TRADE_PLANNER_PLANNED_ENTRY_PRICE_TAG =
    "trade_planner_planned_entry_price"
private const val TRADE_PLANNER_TARGET_LEVERAGE_TAG = "trade_planner_target_leverage"
private const val TRADE_PLANNER_CALCULATE_TAG = "trade_planner_calculate"
private const val TRADE_PLANNER_RESULT_TAG = "trade_planner_result"
private const val TRADE_PLANNER_ERROR_TAG = "trade_planner_error"

