package de.konavigator.app.debug.marketdata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.konavigator.app.R
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiError
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiInputError
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiResult
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiState
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiSubmission
import de.konavigator.app.presentation.marketdata.MarketDataCalculationViewModel
import de.konavigator.app.ui.theme.KONavigatorTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MarketDataCalculationDemoRoute(
    viewModel: MarketDataCalculationViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MarketDataCalculationDemoScreen(
        state = state,
        onProductIsinChanged = viewModel::onProductIsinChanged,
        onCalculationTypeChanged = viewModel::onCalculationTypeChanged,
        onEvaluationTimeChanged = viewModel::onEvaluationTimeChanged,
        onCalculateClicked = viewModel::onCalculateClicked,
        modifier = modifier
    )
}

@Composable
fun MarketDataCalculationDemoScreen(
    state: MarketDataCalculationUiState,
    onProductIsinChanged: (String) -> Unit,
    onCalculationTypeChanged: (MarketDataCalculationType) -> Unit,
    onEvaluationTimeChanged: (String) -> Unit,
    onCalculateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.market_data_demo_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.market_data_demo_internal_notice),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.market_data_demo_data_notice),
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = state.productIsinInput,
                onValueChange = onProductIsinChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PRODUCT_ISIN_TAG),
                label = {
                    Text(stringResource(R.string.market_data_demo_product_isin_label))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii
                )
            )

            Text(
                text = stringResource(R.string.market_data_demo_calculation_type_label),
                style = MaterialTheme.typography.titleMedium
            )
            MarketDataCalculationType.entries.forEach { calculationType ->
                CalculationTypeOption(
                    calculationType = calculationType,
                    selected = state.selectedCalculationType == calculationType,
                    onSelected = onCalculationTypeChanged
                )
            }

            OutlinedTextField(
                value = state.evaluationTimeEpochMillisInput,
                onValueChange = onEvaluationTimeChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(EVALUATION_TIME_TAG),
                label = {
                    Text(stringResource(R.string.market_data_demo_evaluation_time_label))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii
                )
            )
            Text(
                text = stringResource(R.string.market_data_demo_evaluation_time_example),
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onCalculateClicked()
                },
                enabled = state.isCalculateEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CALCULATE_BUTTON_TAG)
            ) {
                Text(stringResource(R.string.market_data_demo_calculate))
            }

            when (val submission = state.submission) {
                MarketDataCalculationUiSubmission.Idle -> Unit
                MarketDataCalculationUiSubmission.Loading -> LoadingIndicator()
                is MarketDataCalculationUiSubmission.InvalidInput -> {
                    MarketDataCalculationErrorCard(
                        messages = submission.errors.map { error ->
                            stringResource(inputErrorResource(error))
                        }
                    )
                }
                is MarketDataCalculationUiSubmission.Completed -> {
                    MarketDataCalculationResultCard(result = submission.result)
                }
            }
        }
    }
}

@Composable
private fun CalculationTypeOption(
    calculationType: MarketDataCalculationType,
    selected: Boolean,
    onSelected: (MarketDataCalculationType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = { onSelected(calculationType) },
                role = Role.RadioButton
            )
            .testTag(calculationTypeTag(calculationType))
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(text = stringResource(calculationTypeLabel(calculationType)))
    }
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier.testTag(LOADING_TAG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.market_data_demo_loading))
    }
}

@Composable
private fun MarketDataCalculationResultCard(
    result: MarketDataCalculationUiResult,
    modifier: Modifier = Modifier
) {
    if (result is MarketDataCalculationUiResult.Failure) {
        MarketDataCalculationErrorCard(
            messages = listOf(stringResource(uiErrorResource(result.error))),
            modifier = modifier
        )
        return
    }

    val priceFormatter = rememberNumberFormatter(
        minimumFractionDigits = 4,
        maximumFractionDigits = 8
    )
    val percentFormatter = rememberNumberFormatter(
        minimumFractionDigits = 2,
        maximumFractionDigits = 4
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(RESULT_CARD_TAG)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.market_data_demo_result_title),
                style = MaterialTheme.typography.titleMedium
            )
            when (result) {
                is MarketDataCalculationUiResult.PurchasePrice -> {
                    Text(stringResource(R.string.market_data_demo_purchase_price))
                    Text(
                        stringResource(
                            R.string.market_data_demo_price_value,
                            priceFormatter.format(result.value),
                            result.currency
                        )
                    )
                }
                is MarketDataCalculationUiResult.SalePrice -> {
                    Text(stringResource(R.string.market_data_demo_sale_price))
                    Text(
                        stringResource(
                            R.string.market_data_demo_price_value,
                            priceFormatter.format(result.value),
                            result.currency
                        )
                    )
                }
                is MarketDataCalculationUiResult.Spread -> {
                    Text(stringResource(R.string.market_data_demo_spread))
                    Text(
                        stringResource(
                            R.string.market_data_demo_absolute_spread,
                            priceFormatter.format(result.absoluteSpread),
                            result.currency
                        )
                    )
                    Text(
                        stringResource(
                            R.string.market_data_demo_relative_spread,
                            percentFormatter.format(result.relativeSpreadToAskPercent)
                        )
                    )
                }
                is MarketDataCalculationUiResult.MidPrice -> {
                    Text(stringResource(R.string.market_data_demo_mid_price))
                    Text(
                        stringResource(
                            R.string.market_data_demo_price_value,
                            priceFormatter.format(result.value),
                            result.currency
                        )
                    )
                }
                is MarketDataCalculationUiResult.Failure -> Unit
            }
        }
    }
}

@Composable
private fun MarketDataCalculationErrorCard(
    messages: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ERROR_CARD_TAG)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            messages.forEach { message ->
                Text(text = message)
            }
        }
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

private fun calculationTypeLabel(calculationType: MarketDataCalculationType): Int =
    when (calculationType) {
        MarketDataCalculationType.PURCHASE_PRICE -> R.string.market_data_demo_purchase_price
        MarketDataCalculationType.SALE_PRICE -> R.string.market_data_demo_sale_price
        MarketDataCalculationType.SPREAD -> R.string.market_data_demo_spread
        MarketDataCalculationType.MID -> R.string.market_data_demo_mid_price
    }

private fun calculationTypeTag(calculationType: MarketDataCalculationType): String =
    "market_data_demo_calculation_type_${calculationType.name.lowercase(Locale.ROOT)}"

private fun inputErrorResource(error: MarketDataCalculationUiInputError): Int =
    when (error) {
        MarketDataCalculationUiInputError.PRODUCT_ISIN_REQUIRED ->
            R.string.market_data_demo_input_product_isin_required
        MarketDataCalculationUiInputError.EVALUATION_TIME_REQUIRED ->
            R.string.market_data_demo_input_evaluation_time_required
        MarketDataCalculationUiInputError.EVALUATION_TIME_INVALID ->
            R.string.market_data_demo_input_evaluation_time_invalid
    }

private fun uiErrorResource(error: MarketDataCalculationUiError): Int =
    when (error) {
        MarketDataCalculationUiError.PRODUCT_NOT_FOUND ->
            R.string.market_data_demo_error_product_not_found
        MarketDataCalculationUiError.MARKET_DATA_NOT_FOUND ->
            R.string.market_data_demo_error_market_data_not_found
        MarketDataCalculationUiError.DATA_ACCESS_FAILURE ->
            R.string.market_data_demo_error_data_access_failure
        MarketDataCalculationUiError.INVALID_SPECIFICATION ->
            R.string.market_data_demo_error_invalid_specification
        MarketDataCalculationUiError.INVALID_MARKET_DATA ->
            R.string.market_data_demo_error_invalid_market_data
        MarketDataCalculationUiError.INCOMPATIBLE_PRODUCT_DATA ->
            R.string.market_data_demo_error_incompatible_product_data
        MarketDataCalculationUiError.REQUIRED_QUOTE_UNAVAILABLE ->
            R.string.market_data_demo_error_required_quote_unavailable
        MarketDataCalculationUiError.MARKET_DATA_NOT_FRESH ->
            R.string.market_data_demo_error_market_data_not_fresh
        MarketDataCalculationUiError.SOURCE_UNAVAILABLE ->
            R.string.market_data_demo_error_source_unavailable
        MarketDataCalculationUiError.CALCULATION_FAILED ->
            R.string.market_data_demo_error_calculation_failed
        MarketDataCalculationUiError.UNEXPECTED_FAILURE ->
            R.string.market_data_demo_error_unexpected_failure
    }

@Preview(showBackground = true)
@Composable
private fun MarketDataCalculationDemoScreenPreview() {
    KONavigatorTheme(dynamicColor = false) {
        MarketDataCalculationDemoScreen(
            state = MarketDataCalculationUiState(
                productIsinInput = "DE000DEMO001",
                evaluationTimeEpochMillisInput = "1700000000000",
                submission = MarketDataCalculationUiSubmission.Completed(
                    MarketDataCalculationUiResult.PurchasePrice(
                        value = 2.0,
                        currency = "EUR"
                    )
                )
            ),
            onProductIsinChanged = {},
            onCalculationTypeChanged = {},
            onEvaluationTimeChanged = {},
            onCalculateClicked = {}
        )
    }
}

private const val PRODUCT_ISIN_TAG = "market_data_demo_product_isin"
private const val EVALUATION_TIME_TAG = "market_data_demo_evaluation_time"
private const val CALCULATE_BUTTON_TAG = "market_data_demo_calculate"
private const val LOADING_TAG = "market_data_demo_loading"
private const val RESULT_CARD_TAG = "market_data_demo_result_card"
private const val ERROR_CARD_TAG = "market_data_demo_error_card"
