package de.konavigator.app.application.productdiscovery

/**
 * Providerneutrales Ergebnis der verpflichtenden Broker-Verfügbarkeitsprüfung.
 *
 * Broker-Verfügbarkeit ist ein verpflichtendes Ausschlusskriterium vor dem späteren Ranking.
 * Nicht handelbare Kandidaten dürfen später weder bewertet noch angezeigt werden. Das Ergebnis
 * ist keine Kauf- oder Verkaufsempfehlung und enthält keine Data-Quality-, Freshness-,
 * Marktdaten- oder Rankingentscheidung sowie keine UI-Texte. Der Vertrag besitzt keine Android-,
 * Compose-, Netzwerk-, Provider- oder DTO-Abhängigkeit.
 *
 * Es gibt bewusst keinen `NotFound`-Zustand: Eine erfolgreiche Prüfung ohne brokerhandelbare
 * Produkte wird ausschließlich als [Success] mit leerer Menge dargestellt. Dadurch bleibt „bei
 * diesem Broker keine Produkte verfügbar“ von technischen und ungültigen Daten getrennt.
 */
sealed interface KnockoutProductBrokerAvailabilityResult {

    /**
     * Die Verfügbarkeitsprüfung wurde fachlich und technisch erfolgreich ausgeführt.
     *
     * [tradableProductIsins] enthält exakt und ohne Normalisierung die Produkt-ISINs, die beim
     * angefragten Broker als handelbar bestätigt wurden. Die Menge darf leer sein; dies bedeutet,
     * dass keines der angefragten Produkte als handelbar bestätigt wurde, und ist weder ein
     * technischer Fehler noch ungültige Daten. Die Ergebnismenge besitzt bewusst keine Ranking-
     * oder Anzeigereihenfolge. Eine spätere Filterung muss Reihenfolge und Duplikate der
     * ursprünglichen Katalogkandidaten erhalten.
     *
     * Das Ergebnis bestätigt ausschließlich Broker-Handelbarkeit, nicht automatisch aktuelle
     * Quotierung, verfügbare Stückzahl, akzeptablen Spread, geeigneten Zielhebel, Produktqualität,
     * Orderausführung, Handelszeit, Börsenplatz oder OTC-Partnerdetails.
     */
    data class Success(
        val tradableProductIsins: Set<String>
    ) : KnockoutProductBrokerAvailabilityResult

    /**
     * Broker-Verfügbarkeitsdaten konnten technisch nicht zuverlässig gelesen werden. Dies
     * bedeutet ausdrücklich nicht, dass kein Produkt beim Broker handelbar ist, und darf später
     * nicht als leere Verfügbarkeit behandelt werden.
     */
    data object DataAccessFailure :
        KnockoutProductBrokerAvailabilityResult

    /**
     * Geladene Broker-Verfügbarkeitsdaten konnten nicht zuverlässig als gültige
     * Verfügbarkeitsaussage bereitgestellt werden. Dies bedeutet ausdrücklich nicht, dass kein
     * Produkt beim Broker handelbar ist, und darf später nicht als leere Verfügbarkeit behandelt
     * werden.
     */
    data object InvalidData :
        KnockoutProductBrokerAvailabilityResult
}
