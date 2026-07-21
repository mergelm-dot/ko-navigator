package de.konavigator.app.domain.tradeplanning

/**
 * Strukturierte Fehler der brokerneutralen Einstiegskursrelation.
 *
 * Die Codes enthalten keine UI-Texte und treffen keine Aussage über
 * Handelsrichtung, Broker-Order oder Handelsstrategie.
 */
enum class EntryPriceRelationEvaluationError {
    INVALID_CURRENT_PRICE,
    INVALID_PLANNED_ENTRY_PRICE
}
