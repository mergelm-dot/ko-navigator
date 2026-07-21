package de.konavigator.app.presentation.marketdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationRequest
import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationService
import de.konavigator.app.domain.availability.MarketDataCalculationType
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Koordiniert die Presentation-Schicht einer Marktdatenberechnung.
 *
 * Das ViewModel besitzt exakt eine Application-Service-Abhängigkeit, hält den
 * unveränderlichen UI-State als [StateFlow] und verarbeitet vier explizite
 * Eingabemethoden. Es enthält weder Repository- noch Domainlogik, liest keine
 * versteckte Systemzeit und kennt weder Android Context noch Ressourcen oder
 * Compose. Die Ergebnisse unterstützen die Planung, stellen aber keine Kauf-
 * oder Verkaufsempfehlung dar.
 */
class MarketDataCalculationViewModel(
    private val applicationService: MarketDataCalculationApplicationService
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(MarketDataCalculationUiState())

    val uiState: StateFlow<MarketDataCalculationUiState> =
        _uiState.asStateFlow()

    private var calculationJob: Job? = null
    private var activeRequestToken: Any? = null

    fun onProductIsinChanged(value: String) {
        invalidateActiveCalculation()
        _uiState.update {
            it.copy(
                productIsinInput = value,
                submission = MarketDataCalculationUiSubmission.Idle
            )
        }
    }

    fun onCalculationTypeChanged(value: MarketDataCalculationType) {
        invalidateActiveCalculation()
        _uiState.update {
            it.copy(
                selectedCalculationType = value,
                submission = MarketDataCalculationUiSubmission.Idle
            )
        }
    }

    fun onEvaluationTimeChanged(value: String) {
        invalidateActiveCalculation()
        _uiState.update {
            it.copy(
                evaluationTimeEpochMillisInput = value,
                submission = MarketDataCalculationUiSubmission.Idle
            )
        }
    }

    fun onCalculateClicked() {
        val state = _uiState.value
        if (state.submission is MarketDataCalculationUiSubmission.Loading) {
            return
        }

        val errors = inputErrors(state)
        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    submission = MarketDataCalculationUiSubmission.InvalidInput(errors)
                )
            }
            return
        }

        val request = MarketDataCalculationApplicationRequest(
            productIsin = state.productIsinInput,
            calculationType = state.selectedCalculationType,
            evaluationTimeEpochMillis = state.evaluationTimeEpochMillisInput.toLong()
        )
        val requestToken = Any()
        activeRequestToken = requestToken
        _uiState.update {
            it.copy(submission = MarketDataCalculationUiSubmission.Loading)
        }

        calculationJob = viewModelScope.launch {
            try {
                val result = applicationService.execute(request).toUiResult()
                completeIfCurrent(requestToken, result)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                completeIfCurrent(
                    requestToken = requestToken,
                    result = MarketDataCalculationUiResult.Failure(
                        MarketDataCalculationUiError.UNEXPECTED_FAILURE
                    )
                )
            }
        }
    }

    private fun inputErrors(
        state: MarketDataCalculationUiState
    ): List<MarketDataCalculationUiInputError> = buildList {
        if (state.productIsinInput.isEmpty()) {
            add(MarketDataCalculationUiInputError.PRODUCT_ISIN_REQUIRED)
        }
        if (state.evaluationTimeEpochMillisInput.isEmpty()) {
            add(MarketDataCalculationUiInputError.EVALUATION_TIME_REQUIRED)
        } else if (state.evaluationTimeEpochMillisInput.toLongOrNull() == null) {
            add(MarketDataCalculationUiInputError.EVALUATION_TIME_INVALID)
        }
    }

    private fun invalidateActiveCalculation() {
        activeRequestToken = null
        calculationJob?.cancel()
        calculationJob = null
    }

    private fun completeIfCurrent(
        requestToken: Any,
        result: MarketDataCalculationUiResult
    ) {
        if (activeRequestToken === requestToken) {
            _uiState.update {
                it.copy(
                    submission = MarketDataCalculationUiSubmission.Completed(result)
                )
            }
        }
    }
}
