package de.konavigator.app.domain.orchestration

sealed interface MarketDataCalculationValue {

    data class PurchasePrice(
        val value: Double,
        val currency: String
    ) : MarketDataCalculationValue

    data class SalePrice(
        val value: Double,
        val currency: String
    ) : MarketDataCalculationValue

    /**
     * Enthält beide Version-1-Spreadwerte desselben freigegebenen Quotes.
     *
     * [absoluteSpread] lautet auf die Produktwährung in [currency].
     * [relativeSpreadToAskPercent] ist prozentual auf Ask bezogen.
     */
    data class Spread(
        val absoluteSpread: Double,
        val relativeSpreadToAskPercent: Double,
        val currency: String
    ) : MarketDataCalculationValue

    data class MidPrice(
        val value: Double,
        val currency: String
    ) : MarketDataCalculationValue
}
