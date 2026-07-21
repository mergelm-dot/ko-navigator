package de.konavigator.app.domain.tradeplanning

/**
 * Ergebnis der brokerneutralen Auswertung zweier Basiswertkurse.
 *
 * Erwartbare ungültige Werte werden strukturiert und ohne Exception oder
 * Nullable-Ergebnis abgebildet. Der Vertrag enthält keine UI-, Android- oder
 * Broker-Verantwortung.
 */
sealed interface EntryPriceRelationEvaluationResult {

    data class Success(
        val relation: EntryPriceRelation
    ) : EntryPriceRelationEvaluationResult

    data class Failure(
        val error: EntryPriceRelationEvaluationError
    ) : EntryPriceRelationEvaluationResult
}
