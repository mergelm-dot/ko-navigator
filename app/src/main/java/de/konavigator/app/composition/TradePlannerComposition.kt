package de.konavigator.app.composition

import de.konavigator.app.application.tradeplanning.TradePlanningApplicationService
import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.presentation.tradeplanner.TradePlannerViewModelFactory

/**
 * Produktiver Composition-Einstieg für Trade Planning.
 *
 * Der Android-freie, zustandslose Aufbau erzeugt den Objektgraphen aus Engine,
 * Service und Factory. Er besitzt keine MarketData- oder Debug-Abhängigkeit
 * und ist noch nicht an MainActivity oder TradePlannerScreen angebunden.
 */
object TradePlannerComposition {

    fun createViewModelFactory(): TradePlannerViewModelFactory {
        val applicationService =
            TradePlanningApplicationService(
                TradeCalculationEngine
            )

        return TradePlannerViewModelFactory(
            applicationService
        )
    }
}
