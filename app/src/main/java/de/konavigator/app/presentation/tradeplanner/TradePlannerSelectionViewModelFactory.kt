package de.konavigator.app.presentation.tradeplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Erzeugt den parallelen ViewModel-Pfad für die vollständige Produktauswahl. */
class TradePlannerSelectionViewModelFactory(
    private val selectionExecutor: TradePlannerSelectionExecutor,
    private val executionSettings: TradePlannerSelectionExecutionSettings,
    private val evaluationTimeProvider: TradePlannerSelectionEvaluationTimeProvider
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass != TradePlannerSelectionViewModel::class.java) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return TradePlannerSelectionViewModel(
            selectionExecutor = selectionExecutor,
            executionSettings = executionSettings,
            evaluationTimeProvider = evaluationTimeProvider
        ) as T
    }
}
