package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot

/**
 * Providerneutrales Ergebnis der Emittentenauswahl für bereits brokerhandelbare Kandidaten.
 *
 * Die Emittentenauswahl ist ein Nutzerfilter nach der verpflichtenden
 * Broker-Verfügbarkeitsprüfung und vor späterer Bewertung. Kandidaten deaktivierter Emittenten
 * dürfen später weder bewertet noch angezeigt werden. Das Ergebnis ist keine Kauf- oder
 * Verkaufsempfehlung und enthält keine Marktdaten-, Data-Quality-, Freshness-, Zielhebel- oder
 * Rankingentscheidung sowie keine UI-Texte. Der Vertrag besitzt keine Android-, Compose-,
 * Netzwerk-, Provider- oder DTO-Abhängigkeit.
 *
 * Da der Filter auf keine externe Datenquelle zugreift, gibt es bewusst weder
 * `DataAccessFailure`, `InvalidData` noch `NotFound`.
 */
sealed interface KnockoutProductIssuerSelectionResult {

    /**
     * Ausschließlich ursprüngliche Eingabekandidaten, deren `specification.issuerId` exakt in der
     * aktivierten Emittentenmenge enthalten ist.
     *
     * Der Filter erzeugt diesen Zustand nur mit einer nichtleeren Kandidatenliste. Reihenfolge,
     * Duplikate und unterschiedliche Produkte desselben Emittenten bleiben erhalten. Dieselben
     * Snapshot- und Specification-Objektinstanzen sowie `sourceId`, `retrievedAtEpochMillis` und
     * `sourceTimestampEpochMillis` werden unverändert transportiert.
     *
     * Bestätigt wird ausschließlich, dass der Emittent aktiviert ist, nicht Broker-Handelbarkeit,
     * aktuelle Quotierung, akzeptabler Spread, geeigneter Zielhebel, Produktqualität oder
     * Orderausführung.
     */
    data class EnabledIssuerCandidates(
        val candidates: List<KnockoutProductSpecificationSnapshot>
    ) : KnockoutProductIssuerSelectionResult

    /**
     * Die übergebene Kandidatenliste war leer. Dies bedeutet weder, dass alle Emittenten
     * deaktiviert wurden, noch dass ein technischer Fehler aufgetreten ist.
     */
    data object NoInputCandidates :
        KnockoutProductIssuerSelectionResult

    /**
     * Die Eingabekandidatenliste war nicht leer, nach exakter Filterung blieb jedoch kein
     * Kandidat übrig. Dies tritt insbesondere bei leerer aktivierter Emittentenmenge oder ohne
     * exakt passende Emittenten-ID ein. Es bedeutet weder, dass generell keine KO-Produkte
     * existieren, noch dass beim Broker keine Produkte handelbar sind, und ist kein technischer
     * Fehler.
     */
    data object NoEnabledIssuerCandidates :
        KnockoutProductIssuerSelectionResult
}
