package de.konavigator.app.domain.model

/**
 * Beschreibt die statischen Produktparameter eines konkreten KO-Produkts.
 *
 * Basispreis und KO-Barriere sind getrennte fachliche Größen. Das Modell
 * enthält keine Marktpreise wie Bid, Ask, Last oder Spread, keinen Wechselkurs,
 * keine Bewertungsergebnisse und keinen UI-Zustand. Es besitzt keine Android-
 * oder Compose-Abhängigkeiten.
 *
 * Basiswert- und Produktwährung werden in Version 1 als dokumentierte
 * ISO-4217-Strings geführt. Die Validierung erfolgt später außerhalb des
 * Modells. Verfügbarkeit, Quelle und Gültigkeitszeitpunkt realer
 * Produktparameter bleiben ebenfalls Gegenstand späterer Entwicklungsschritte.
 */
data class KnockoutProductSpecification(
    val productIsin: String,
    val productWkn: String?,
    val issuerId: String,
    val underlyingId: String,
    val direction: TradeDirection,
    val basePrice: Double,
    val knockoutBarrier: Double,
    val ratio: Double,
    val underlyingCurrency: String,
    val productCurrency: String
)
