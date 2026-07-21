package de.konavigator.app.presentation.marketdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationService

/**
 * Erzeugt ausschließlich ein [MarketDataCalculationViewModel] mit dem
 * ausdrücklich übergebenen Application-Service.
 *
 * Die Factory kennt weder Repositories und Domain-Policies noch Android
 * Context, Ressourcen oder Demo-Daten. Sie ist ein kleiner allgemeiner
 * Presentation-Adapter und keine Dependency-Injection-Infrastruktur.
 */
class MarketDataCalculationViewModelFactory(
    private val applicationService: MarketDataCalculationApplicationService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass != MarketDataCalculationViewModel::class.java) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return MarketDataCalculationViewModel(applicationService) as T
    }
}
