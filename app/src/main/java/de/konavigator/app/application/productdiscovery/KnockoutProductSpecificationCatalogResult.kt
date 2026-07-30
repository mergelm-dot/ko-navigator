package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot

/**
 * Providerneutrales Ergebnis einer Katalogabfrage nach KO-Produktspezifikationen.
 *
 * Das Ergebnis ist keine Kauf- oder Verkaufsempfehlung und bestätigt noch keine Handelbarkeit
 * bei einem Broker. Es enthält keine Data-Quality-, Freshness-, Broker- oder
 * Rankingentscheidung und keine UI-Texte. Der Vertrag besitzt keine Android-, Compose-,
 * Netzwerk-, Provider- oder DTO-Abhängigkeit.
 *
 * Es gibt bewusst keinen `NotFound`-Zustand: Ein erfolgreicher Suchlauf ohne Treffer wird als
 * [Success] mit leerer Kandidatenliste dargestellt. So bleibt „keine Kandidaten gefunden“ von
 * Datenzugriffs- und Datenqualitätsfehlern getrennt.
 */
sealed interface KnockoutProductSpecificationCatalogResult {

    /**
     * Die Katalogabfrage wurde fachlich und technisch erfolgreich ausgeführt.
     *
     * [candidates] darf leer sein. Eine leere Liste bedeutet ausschließlich, dass für die
     * exakte Kombination aus `underlyingId` und `direction` keine Kandidaten gefunden wurden;
     * sie ist weder ein technischer Fehler noch ungültige Daten. Der Vertrag verändert weder
     * Reihenfolge noch Duplikate, gruppiert nicht nach Emittent und sortiert nicht automatisch.
     * Die Snapshot-Objekte werden unverändert transportiert, sodass insbesondere `sourceId`,
     * `retrievedAtEpochMillis` und `sourceTimestampEpochMillis` erhalten bleiben.
     */
    data class Success(
        val candidates: List<KnockoutProductSpecificationSnapshot>
    ) : KnockoutProductSpecificationCatalogResult

    /**
     * Der Katalog oder seine Datenquelle konnte technisch nicht zuverlässig gelesen werden.
     * Dieser Zustand bedeutet ausdrücklich nicht, dass keine Produkte existieren.
     */
    data object DataAccessFailure :
        KnockoutProductSpecificationCatalogResult

    /**
     * Geladene externe Katalogdaten konnten nicht zuverlässig als gültige Kandidaten
     * bereitgestellt werden. Dieser Zustand bedeutet ausdrücklich nicht, dass keine Produkte
     * existieren.
     */
    data object InvalidData :
        KnockoutProductSpecificationCatalogResult
}
