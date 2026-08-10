package de.konavigator.app.presentation.tradeplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateSelectionPipelineApplicationRequest
import de.konavigator.app.domain.model.TradeDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Koordiniert ausschließlich Eingaben, 21f-Ausführung und das bestehende 22a-Mapping. */
class TradePlannerSelectionViewModel(
    private val selectionExecutor: TradePlannerSelectionExecutor,
    private val executionSettings: TradePlannerSelectionExecutionSettings,
    private val evaluationTimeProvider: TradePlannerSelectionEvaluationTimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(TradePlannerSelectionUiState())

    val uiState: StateFlow<TradePlannerSelectionUiState> = _uiState.asStateFlow()

    private var calculationJob: Job? = null
    private var requestGeneration = 0L

    fun onUnderlyingSelected(underlyingId: String?) {
        updateInput { it.copy(selectedUnderlyingId = underlyingId) }
    }

    fun onBrokerSelected(brokerId: String?) {
        updateInput { it.copy(selectedBrokerId = brokerId) }
    }

    fun onEnabledIssuerIdsChanged(enabledIssuerIds: Set<String>) {
        val issuerIdsSnapshot = enabledIssuerIds.toSet()
        updateInput { it.copy(enabledIssuerIds = issuerIdsSnapshot) }
    }

    fun onCurrentPriceChanged(value: String) {
        updateInput { it.copy(currentUnderlyingPriceInput = value) }
    }

    fun onPlannedEntryPriceChanged(value: String) {
        updateInput { it.copy(plannedEntryPriceInput = value) }
    }

    fun onTargetLeverageChanged(value: String) {
        updateInput { it.copy(targetLeverageInput = value) }
    }

    fun onDirectionChanged(direction: TradeDirection) {
        updateInput { it.copy(direction = direction) }
    }

    fun onCalculateClicked() {
        calculationJob?.cancel()
        calculationJob = null
        val generation = ++requestGeneration
        val state = _uiState.value
        val currentPrice = parseInput(
            value = state.currentUnderlyingPriceInput,
            requiredError = TradePlannerSelectionUiInputError.CURRENT_PRICE_REQUIRED,
            invalidError = TradePlannerSelectionUiInputError.CURRENT_PRICE_INVALID,
            isValid = { it > 0.0 }
        )
        val plannedEntryPrice = parseInput(
            value = state.plannedEntryPriceInput,
            requiredError = TradePlannerSelectionUiInputError.PLANNED_ENTRY_PRICE_REQUIRED,
            invalidError = TradePlannerSelectionUiInputError.PLANNED_ENTRY_PRICE_INVALID,
            isValid = { it > 0.0 }
        )
        val targetLeverage = parseInput(
            value = state.targetLeverageInput,
            requiredError = TradePlannerSelectionUiInputError.TARGET_LEVERAGE_REQUIRED,
            invalidError = TradePlannerSelectionUiInputError.TARGET_LEVERAGE_INVALID,
            isValid = { it > 1.0 }
        )
        val errors = listOfNotNull(
            if (state.selectedUnderlyingId.isNullOrBlank()) {
                TradePlannerSelectionUiInputError.UNDERLYING_REQUIRED
            } else {
                null
            },
            if (state.selectedBrokerId.isNullOrBlank()) {
                TradePlannerSelectionUiInputError.BROKER_REQUIRED
            } else {
                null
            },
            currentPrice.error,
            plannedEntryPrice.error,
            targetLeverage.error
        )
        if (errors.isNotEmpty()) {
            updateSubmission(TradePlannerSelectionUiSubmission.InvalidInput(errors))
            return
        }

        val underlyingId = state.selectedUnderlyingId ?: return
        val brokerId = state.selectedBrokerId ?: return
        val currentPriceValue = currentPrice.value ?: return
        val plannedEntryPriceValue = plannedEntryPrice.value ?: return
        val targetLeverageValue = targetLeverage.value ?: return
        val evaluationTimeEpochMillis =
            evaluationTimeProvider.evaluationTimeEpochMillis()
        val request = KnockoutProductCandidateSelectionPipelineApplicationRequest(
            underlyingId = underlyingId,
            direction = state.direction,
            brokerId = brokerId,
            enabledIssuerIds = state.enabledIssuerIds,
            calculationType = executionSettings.calculationType,
            evaluationTimeEpochMillis = evaluationTimeEpochMillis,
            maxFxAgeMillis = executionSettings.maxFxAgeMillis,
            underlyingPrice = currentPriceValue,
            plannedEntryPrice = plannedEntryPriceValue,
            targetLeverage = targetLeverageValue,
            maxRelativeLeverageDeviationPercent =
                executionSettings.maxRelativeLeverageDeviationPercent,
            maxBarrierDeviationPercentOfPlannedEntry =
                executionSettings.maxBarrierDeviationPercentOfPlannedEntry
        )

        updateSubmission(TradePlannerSelectionUiSubmission.Loading)
        calculationJob = viewModelScope.launch {
            val applicationResult = selectionExecutor.execute(request)
            val mappedResult = TradePlannerSelectionUiMapper.map(applicationResult)
            if (generation == requestGeneration) {
                updateSubmission(TradePlannerSelectionUiSubmission.Completed(mappedResult))
            }
        }
    }

    private fun updateInput(
        transform: (TradePlannerSelectionUiState) -> TradePlannerSelectionUiState
    ) {
        requestGeneration++
        calculationJob?.cancel()
        calculationJob = null
        _uiState.update {
            transform(it).copy(submission = TradePlannerSelectionUiSubmission.Idle)
        }
    }

    private fun updateSubmission(submission: TradePlannerSelectionUiSubmission) {
        _uiState.update { it.copy(submission = submission) }
    }

    private fun parseInput(
        value: String,
        requiredError: TradePlannerSelectionUiInputError,
        invalidError: TradePlannerSelectionUiInputError,
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
        val error: TradePlannerSelectionUiInputError?
    )
}
