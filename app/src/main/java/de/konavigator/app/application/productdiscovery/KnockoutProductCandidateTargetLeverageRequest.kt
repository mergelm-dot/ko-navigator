package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.currency.CurrencyConversion

/**
 * Verbindet einen unveränderten erfolgreich berechneten Kandidaten mit seiner
 * außerhalb des Services bereitgestellten Währungsbeziehung. Beide Instanzen
 * bleiben erhalten; der Service konstruiert oder errät keine FX-Beziehung.
 */
data class KnockoutProductCandidateTargetLeverageInput(
    val candidateWithCalculation: KnockoutProductCandidateWithCalculation,
    val currencyConversion: CurrencyConversion
)

/**
 * Providerneutraler Auftrag für theoretische Zielhebel-Pläne. Kandidatenliste
 * und Zahlenwerte werden exakt übernommen; dieselbe Listenreferenz, Reihenfolge
 * und Duplikate bleiben erhalten. Eine leere Liste ist zulässig.
 *
 * Richtung und Ratio stammen ausschließlich aus der jeweiligen Spezifikation.
 * Der Auftrag enthält keine separate Barriere, Produktkennungen, Broker-ID,
 * Systemzeit, Ranking- oder Qualitätsparameter und validiert keine Werte.
 */
data class KnockoutProductCandidateTargetLeverageRequest(
    val candidates: List<KnockoutProductCandidateTargetLeverageInput>,
    val underlyingPrice: Double,
    val plannedEntryPrice: Double,
    val targetLeverage: Double
)
