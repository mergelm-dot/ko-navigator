package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot

/**
 * Verbindet exakt einen ursprünglichen Spezifikations-Snapshot mit dem vom Repository gelieferten
 * Marktdatenobjekt.
 *
 * Beide Objektinstanzen werden unverändert und ohne Kopie transportiert. Der Vertrag verändert
 * keine Werte, führt keine Validierung oder Kompatibilitätsprüfung durch und bestätigt noch keine
 * Berechnungs- oder Rankingfreigabe.
 */
data class KnockoutProductCandidateWithMarketData(
    val specificationSnapshot: KnockoutProductSpecificationSnapshot,
    val marketData: KnockoutProductMarketData
)

/**
 * Providerneutrales Ergebnis der Marktdatenanreicherung von KO-Produktkandidaten.
 *
 * Das Ergebnis ist keine Kauf- oder Verkaufsempfehlung und enthält keine Data-Quality-,
 * Freshness-, Spread-, Zielhebel- oder Rankingentscheidung sowie keine UI-Texte. Der Vertrag
 * besitzt keine Android-, Compose-, Netzwerk-, Provider- oder DTO-Abhängigkeit.
 */
sealed interface KnockoutProductCandidateMarketDataResult {

    /**
     * Ausschließlich erfolgreich angereicherte ursprüngliche Kandidaten.
     *
     * Der Service erzeugt diesen Zustand nur mit einer nichtleeren Liste. Reihenfolge und
     * Kandidatenduplikate entsprechen exakt dem Request; unterschiedliche Kandidaten mit gleicher
     * Produkt-ISIN bleiben getrennte Einträge. Dieselben Snapshot- und Specification-Instanzen
     * bleiben erhalten. Für dieselbe exakte ISIN wird innerhalb eines Aufrufs dasselbe erfolgreich
     * geladene Marktdatenobjekt wiederverwendet; Instanz und Werte bleiben unverändert.
     *
     * Bestätigt wird ausschließlich erfolgreicher Repository-Zugriff, nicht strukturelle
     * Gültigkeit, ISIN- oder Währungskompatibilität, vollständige Bid-/Ask-Quotes, Aktualität,
     * akzeptabler Spread, geeigneter Zielhebel, Produktqualität, Rankingfreigabe oder
     * Orderausführung.
     */
    data class CandidatesWithMarketData(
        val candidates: List<KnockoutProductCandidateWithMarketData>
    ) : KnockoutProductCandidateMarketDataResult

    /**
     * Die übergebene Kandidatenliste war leer. Das Repository wird nicht aufgerufen. Dieser
     * Zustand ist weder ein technischer Fehler noch `NotFound` eines konkreten Produkts.
     */
    data object NoInputCandidates :
        KnockoutProductCandidateMarketDataResult

    /**
     * Bildet ausschließlich `RepositoryResult.NotFound` für die zuerst betroffene exakte
     * Produkt-ISIN ab. Es gibt keine Teilresultate und spätere Kandidaten werden nicht geladen.
     * Der Zustand bedeutet nicht automatisch, dass das Produkt beim Broker nicht handelbar ist.
     */
    data class MarketDataNotFound(
        val productIsin: String
    ) : KnockoutProductCandidateMarketDataResult

    /**
     * Bildet ausschließlich `RepositoryResult.DataAccessFailure` für die zuerst betroffene exakte
     * Produkt-ISIN ab. Es gibt keine Teilresultate, der Zustand ist nicht `MarketDataNotFound` und
     * spätere Kandidaten werden nicht geladen.
     */
    data class MarketDataDataAccessFailure(
        val productIsin: String
    ) : KnockoutProductCandidateMarketDataResult

    /**
     * Bildet ausschließlich `RepositoryResult.InvalidData` für die zuerst betroffene exakte
     * Produkt-ISIN ab. Es gibt keine Teilresultate, der Zustand ist weder `MarketDataNotFound` noch
     * `MarketDataDataAccessFailure`, und spätere Kandidaten werden nicht geladen.
     */
    data class MarketDataInvalidData(
        val productIsin: String
    ) : KnockoutProductCandidateMarketDataResult
}
