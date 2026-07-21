package de.konavigator.app.presentation.tradeplanner

import androidx.lifecycle.ViewModel
import de.konavigator.app.application.tradeplanning.TradePlanningApplicationService
import de.konavigator.app.calculator.TradeCalculationInput
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.tradeplanning.EntryPriceRelationEvaluationError
import de.konavigator.app.domain.tradeplanning.EntryPriceRelationEvaluationResult
import de.konavigator.app.domain.tradeplanning.EntryPriceRelationEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Koordiniert den synchronen Presentation-Pfad der theoretischen Trade-Planung.
 *
 * Das ViewModel besitzt ausschließlich den Application-Service als
 * Konstruktorabhängigkeit. Es kennt weder Repositories, Marktdaten, Android
 * Context, Ressourcen, Compose, Systemzeit noch Broker-Ordertypen.
 */
class TradePlannerViewModel(
    private val applicationService: TradePlanningApplicationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TradePlannerUiState())

    val uiState: StateFlow<TradePlannerUiState> =
        _uiState.asStateFlow()

    fun onCurrentPriceChanged(value: String) {
        _uiState.update {
            it.copy(
                currentUnderlyingPriceInput = value,
                submission = TradePlannerUiSubmission.Idle
            )
        }
    }

    fun onPlannedEntryPriceChanged(value: String) {
        _uiState.update {
            it.copy(
                plannedEntryPriceInput = value,
                submission = TradePlannerUiSubmission.Idle
            )
        }
    }

    fun onTargetLeverageChanged(value: String) {
        _uiState.update {
            it.copy(
                targetLeverageInput = value,
                submission = TradePlannerUiSubmission.Idle
            )
        }
    }

    fun onDirectionChanged(direction: TradeDirection) {
        _uiState.update {
            it.copy(
                direction = direction,
                submission = TradePlannerUiSubmission.Idle
            )
        }
    }

    fun onCalculateClicked() {
        val state = _uiState.value
        val currentPrice = parseInput(
            value = state.currentUnderlyingPriceInput,
            requiredError = TradePlannerUiInputError.CURRENT_PRICE_REQUIRED,
            invalidError = TradePlannerUiInputError.CURRENT_PRICE_INVALID,
            isValid = { it > 0.0 }
        )
        val plannedEntryPrice = parseInput(
            value = state.plannedEntryPriceInput,
            requiredError = TradePlannerUiInputError.PLANNED_ENTRY_PRICE_REQUIRED,
            invalidError = TradePlannerUiInputError.PLANNED_ENTRY_PRICE_INVALID,
            isValid = { it > 0.0 }
        )
        val targetLeverage = parseInput(
            value = state.targetLeverageInput,
            requiredError = TradePlannerUiInputError.TARGET_LEVERAGE_REQUIRED,
            invalidError = TradePlannerUiInputError.TARGET_LEVERAGE_INVALID,
            isValid = { it > 1.0 }
        )
        val errors = listOfNotNull(
            currentPrice.error,
            plannedEntryPrice.error,
            targetLeverage.error
        )

        if (errors.isNotEmpty()) {
            updateSubmission(TradePlannerUiSubmission.InvalidInput(errors))
            return
        }

        val currentPriceValue = checkNotNull(currentPrice.value)
        val plannedEntryPriceValue = checkNotNull(plannedEntryPrice.value)
        val targetLeverageValue = checkNotNull(targetLeverage.value)
        val relation = when (
            val evaluation = EntryPriceRelationEvaluator.evaluate(
                currentPrice = currentPriceValue,
                plannedEntryPrice = plannedEntryPriceValue
            )
        ) {
            is EntryPriceRelationEvaluationResult.Success -> evaluation.relation
            is EntryPriceRelationEvaluationResult.Failure -> {
                val error = when (evaluation.error) {
                    EntryPriceRelationEvaluationError.INVALID_CURRENT_PRICE ->
                        TradePlannerUiInputError.CURRENT_PRICE_INVALID

                    EntryPriceRelationEvaluationError.INVALID_PLANNED_ENTRY_PRICE ->
                        TradePlannerUiInputError.PLANNED_ENTRY_PRICE_INVALID
                }
                updateSubmission(
                    TradePlannerUiSubmission.InvalidInput(listOf(error))
                )
                return
            }
        }

        val input = createTradeCalculationInput(
            currentUnderlyingPrice = currentPriceValue,
            plannedEntryPrice = plannedEntryPriceValue,
            targetLeverage = targetLeverageValue,
            direction = state.direction
        )
        val result = applicationService.execute(input)

        updateSubmission(
            TradePlannerUiSubmission.Completed(
                result.toTradePlannerUiResult(relation)
            )
        )
    }

    private fun updateSubmission(submission: TradePlannerUiSubmission) {
        _uiState.update { it.copy(submission = submission) }
    }

    private fun parseInput(
        value: String,
        requiredError: TradePlannerUiInputError,
        invalidError: TradePlannerUiInputError,
        isValid: (Double) -> Boolean
    ): ParsedInput {
        val parsingValue = value.trim()
        if (parsingValue.isEmpty()) {
            return ParsedInput(value = null, error = requiredError)
        }

        val parsedValue = parsingValue.replace(",", ".").toDoubleOrNull()
        if (parsedValue == null || !parsedValue.isFinite() || !isValid(parsedValue)) {
            return ParsedInput(value = null, error = invalidError)
        }

        return ParsedInput(value = parsedValue, error = null)
    }

    private data class ParsedInput(
        val value: Double?,
        val error: TradePlannerUiInputError?
    )
}

internal fun createTradeCalculationInput(
    currentUnderlyingPrice: Double,
    plannedEntryPrice: Double,
    targetLeverage: Double,
    direction: TradeDirection
) = TradeCalculationInput(
    underlyingPrice = currentUnderlyingPrice,
    plannedEntryPrice = plannedEntryPrice,
    leverage = targetLeverage,
    isLong = direction == TradeDirection.LONG,
    exchangeRate = 1.0,
    ratio = 0.01
)
