package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot

/**
 * Providerneutrales Ergebnis von Katalogsuche, Broker-Verfügbarkeit und Emittentenauswahl.
 *
 * Broker-Verfügbarkeit bleibt ein verpflichtender Ausschluss. Deaktivierte Emittenten sind
 * ebenfalls ein verpflichtender Nutzerfilter vor späterer Bewertung und Anzeige. Nicht
 * bestätigte beziehungsweise deaktivierte Kandidaten werden nicht weitergereicht. Das Ergebnis
 * ist keine Kauf- oder Verkaufsempfehlung und enthält keine Marktdaten-, Data-Quality-,
 * Freshness-, Zielhebel- oder Rankingentscheidung sowie keine UI-Texte. Der Vertrag besitzt keine
 * Android-, Compose-, Netzwerk-, Provider- oder DTO-Abhängigkeit.
 */
sealed interface KnockoutProductDiscoveryApplicationResult {

    /**
     * Ausschließlich ursprüngliche Katalogkandidaten, die brokerhandelbar bestätigt sind und
     * einem ausdrücklich aktivierten Emittenten angehören.
     *
     * Dieser Zustand enthält stets mindestens einen Kandidaten. Reihenfolge und Duplikate
     * entsprechen exakt dem Katalog; mehrere Produkte desselben Emittenten bleiben erhalten.
     * Dieselben Snapshot- und Specification-Objektinstanzen sowie Quellen und Zeitwerte werden
     * unverändert transportiert. Bestätigt werden Broker-Handelbarkeit und aktivierte
     * Emittentenauswahl, nicht Marktdaten, aktuelle Quotierung, Spreadqualität, Zielhebel,
     * Produktqualität oder Orderausführung.
     */
    data class BrokerTradableCandidates(
        val candidates: List<KnockoutProductSpecificationSnapshot>
    ) : KnockoutProductDiscoveryApplicationResult

    /**
     * Der Katalogaufruf war erfolgreich, seine Kandidatenliste jedoch leer. Das
     * Broker-Verfügbarkeitsrepository wird in diesem Zustand nicht aufgerufen.
     */
    data object NoCatalogCandidates :
        KnockoutProductDiscoveryApplicationResult

    /**
     * Der Katalog enthielt mindestens einen Kandidaten und die Broker-Verfügbarkeitsprüfung war
     * erfolgreich, aber nach der Mitgliedschaftsfilterung blieb kein Kandidat übrig. Dies
     * bedeutet nicht, dass generell keine KO-Produkte existieren.
     */
    data object NoBrokerTradableCandidates :
        KnockoutProductDiscoveryApplicationResult

    /**
     * Katalogsuche und Broker-Verfügbarkeitsprüfung waren erfolgreich und mindestens ein
     * brokerhandelbarer Kandidat war vorhanden, aber nach exakter Filterung anhand der aktivierten
     * Emittenten blieb kein Kandidat übrig. Dies tritt insbesondere bei leerer
     * `enabledIssuerIds`-Menge oder ohne exakt passende Emittenten-ID ein.
     *
     * Dieser Zustand bedeutet weder, dass generell keine KO-Produkte existieren, noch dass beim
     * Broker keine Produkte handelbar sind oder externe Daten technisch beziehungsweise
     * inhaltlich fehlerhaft waren. Er ist weder `DataAccessFailure`, `InvalidData` noch
     * `NotFound`.
     */
    data object NoEnabledIssuerCandidates :
        KnockoutProductDiscoveryApplicationResult

    /**
     * Bildet ausschließlich
     * [KnockoutProductSpecificationCatalogResult.DataAccessFailure] ab. Das
     * Broker-Verfügbarkeitsrepository wird nicht aufgerufen.
     */
    data object CatalogDataAccessFailure :
        KnockoutProductDiscoveryApplicationResult

    /**
     * Bildet ausschließlich [KnockoutProductSpecificationCatalogResult.InvalidData] ab. Das
     * Broker-Verfügbarkeitsrepository wird nicht aufgerufen.
     */
    data object CatalogInvalidData :
        KnockoutProductDiscoveryApplicationResult

    /**
     * Bildet ausschließlich [KnockoutProductBrokerAvailabilityResult.DataAccessFailure] ab. Der
     * Zustand darf nicht als [NoBrokerTradableCandidates] behandelt werden; Teilkandidaten werden
     * nicht zurückgegeben.
     */
    data object BrokerAvailabilityDataAccessFailure :
        KnockoutProductDiscoveryApplicationResult

    /**
     * Bildet ausschließlich [KnockoutProductBrokerAvailabilityResult.InvalidData] ab. Der Zustand
     * darf nicht als [NoBrokerTradableCandidates] behandelt werden; Teilkandidaten werden nicht
     * zurückgegeben.
     */
    data object BrokerAvailabilityInvalidData :
        KnockoutProductDiscoveryApplicationResult
}
