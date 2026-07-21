package de.konavigator.app.domain.tradeplanning

/**
 * Brokerneutrale Relation eines geplanten Einstiegskurses zum aktuellen
 * Basiswertkurs.
 *
 * Die Relation beschreibt weder Kauf oder Verkauf eines Zertifikats noch eine
 * Broker-Order, Handelsstrategie oder Empfehlung. Sie enthält keine UI- oder
 * Android-Verantwortung.
 */
enum class EntryPriceRelation {
    BELOW_CURRENT,
    AT_CURRENT,
    ABOVE_CURRENT
}
