package de.konavigator.app.presentation.tradeplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.konavigator.app.application.tradeplanning.TradePlanningApplicationService

/**
 * Erzeugt ausschließlich ein [TradePlannerViewModel] mit dem übergebenen
 * [TradePlanningApplicationService].
 *
 * Die Factory besitzt keine Context-, Repository- oder UI-Abhängigkeit.
 */
class TradePlannerViewModelFactory(
    private val applicationService: TradePlanningApplicationService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass != TradePlannerViewModel::class.java) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return TradePlannerViewModel(applicationService) as T
    }
}
