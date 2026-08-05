package de.konavigator.app.calculator

import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.model.TradeDirection

/**
 * Unveränderter Eingabevertrag für den theoretischen Zustand eines bereits
 * existierenden KO-Produkts am geplanten Basiswert-Einstieg. Basispreis und
 * KO-Barriere bleiben bewusst getrennte feste Produktwerte.
 *
 * Die CurrencyConversion wird außerhalb erzeugt; dieser Vertrag enthält keine
 * FX-Herleitung, Defaults, Zielhebel, neu abgeleitete Barriere, Quotes, Spread,
 * Kennungen, Systemzeit oder Infrastrukturabhängigkeit. Er validiert,
 * normalisiert und berechnet nichts.
 */
data class ExistingKnockoutProductEntryCalculationInput(
    val plannedEntryPrice: Double,
    val basePrice: Double,
    val knockoutBarrier: Double,
    val direction: TradeDirection,
    val ratio: Double,
    val currencyConversion: CurrencyConversion
)
