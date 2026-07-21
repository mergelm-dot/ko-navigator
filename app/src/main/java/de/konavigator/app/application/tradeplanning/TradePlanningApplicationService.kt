package de.konavigator.app.application.tradeplanning

import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.calculator.TradeCalculationInput
import de.konavigator.app.calculator.TradeCalculationResult

/**
 * Synchrone Application-Grenze für die theoretische Trade-Planung.
 *
 * Der Service reicht den vollständigen [TradeCalculationInput] unverändert an
 * die [TradeCalculationEngine] weiter und gibt deren [TradeCalculationResult]
 * unverändert zurück. Er enthält weder eigene Validierung noch Berechnung und
 * besitzt keine Repository- oder Marktdatenabhängigkeit.
 *
 * Die Komponente kennt weder UI, Android noch Compose und bleibt ausdrücklich
 * vom produktbezogenen `MarketDataCalculationApplicationService` getrennt.
 * Das Ergebnis unterstützt ausschließlich die Planung und stellt keine Kauf-
 * oder Verkaufsempfehlung dar.
 */
class TradePlanningApplicationService(
    private val tradeCalculationEngine: TradeCalculationEngine
) {
    fun execute(
        request: TradeCalculationInput
    ): TradeCalculationResult = tradeCalculationEngine.calculateTrade(request)
}
