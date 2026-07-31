package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot

/**
 * Providerneutrales Ergebnis der ersten beiden verpflichtenden Stufen der KO-Produktfindung.
 *
 * Broker-Verfügbarkeit ist ein verpflichtender Ausschluss vor späterer Bewertung und Anzeige;
 * nicht bestätigte Kandidaten werden nicht weitergereicht. Das Ergebnis ist keine Kauf- oder
 * Verkaufsempfehlung und enthält keine Marktdaten-, Data-Quality-, Freshness-, Zielhebel- oder
 * Rankingentscheidung sowie keine UI-Texte. Der Vertrag besitzt keine Android-, Compose-,
 * Netzwerk-, Provider- oder DTO-Abhängigkeit.
 */
sealed interface KnockoutProductDiscoveryApplicationResult {

    /**
     * Ausschließlich ursprüngliche Katalogkandidaten, deren Produkt-ISIN in der erfolgreich
     * bestätigten Broker-Verfügbarkeitsmenge enthalten ist.
     *
     * Dieser Zustand enthält stets mindestens einen Kandidaten. Reihenfolge und Duplikate
     * entsprechen exakt dem Katalog; mehrere Produkte desselben Emittenten bleiben erhalten.
     * Dieselben Snapshot- und Specification-Objektinstanzen sowie Quellen und Zeitwerte werden
     * unverändert transportiert. Bestätigt wird nur Broker-Handelbarkeit, nicht aktuelle
     * Quotierung, Spread, Stückzahl, Zielhebel oder Produktqualität.
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
