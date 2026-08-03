package de.konavigator.app.application.productdiscovery

import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.domain.model.KnockoutProductMarketData

/**
 * Providerneutraler Application-Service zum Laden von Marktdaten für bereits broker- und
 * emittentengefilterte Kandidaten.
 *
 * Der Service prüft Broker und Emittenten nicht erneut. Er arbeitet sequenziell in der
 * ursprünglichen Kandidatenreihenfolge, liest Produkt-ISINs ausschließlich aus
 * `candidate.specification.productIsin` und übergibt jede ISIN exakt an das Repository. Es gibt
 * keine Normalisierung, kein `trim()`, keine Änderung der Groß-/Kleinschreibung und keine
 * Validierung.
 *
 * Dieselbe exakte ISIN wird innerhalb eines [execute]-Aufrufs höchstens einmal geladen. Der Cache
 * ist ausschließlich lokal für diesen Aufruf; es gibt keine globale oder zustandsübergreifende
 * Zwischenspeicherung. Kandidaten mit derselben ISIN bleiben separate Ergebnislisteneinträge und
 * verwenden denselben Marktdatenobjektverweis.
 *
 * Repository-Fehler werden beim ersten Auftreten fail-fast abgebildet. Danach erfolgen keine
 * weiteren Repository-Aufrufe, und bereits geladene Werte werden nicht als Teilresultat
 * zurückgegeben. Erwartbare [RepositoryResult]-Zustände erzeugen keine Exceptions.
 *
 * Ein erfolgreicher Wert wird weder auf ISIN-Übereinstimmung noch auf Datenqualität oder
 * Kompatibilität geprüft. Der Service führt keine Freshness-Prüfung, Spread- oder
 * Zielhebelberechnung und kein Ranking durch. Er sortiert, gruppiert und dedupliziert Kandidaten
 * nicht und begrenzt ihre Zahl nicht. Er verwendet keine Systemzeit oder Zeitumrechnung und
 * enthält keine Repository-Implementierungs-, Provider-, DTO- oder Mappingdetails sowie keine
 * Android-, Compose- oder UI-Abhängigkeit.
 */
class KnockoutProductCandidateMarketDataApplicationService(
    private val marketDataRepository:
        KnockoutProductMarketDataRepository
) {

    suspend fun execute(
        request: KnockoutProductCandidateMarketDataRequest
    ): KnockoutProductCandidateMarketDataResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateMarketDataResult
                .NoInputCandidates
        }

        val marketDataByProductIsin =
            linkedMapOf<String, KnockoutProductMarketData>()

        val candidatesWithMarketData =
            ArrayList<KnockoutProductCandidateWithMarketData>(
                request.candidates.size
            )

        for (candidate in request.candidates) {
            val productIsin =
                candidate.specification.productIsin

            val marketData =
                marketDataByProductIsin[productIsin]
                    ?: when (
                        val repositoryResult =
                            marketDataRepository
                                .findByProductIsin(productIsin)
                    ) {
                        is RepositoryResult.Success -> {
                            repositoryResult.value.also { loadedMarketData ->
                                marketDataByProductIsin[productIsin] =
                                    loadedMarketData
                            }
                        }

                        RepositoryResult.NotFound ->
                            return KnockoutProductCandidateMarketDataResult
                                .MarketDataNotFound(
                                    productIsin = productIsin
                                )

                        RepositoryResult.DataAccessFailure ->
                            return KnockoutProductCandidateMarketDataResult
                                .MarketDataDataAccessFailure(
                                    productIsin = productIsin
                                )

                        RepositoryResult.InvalidData ->
                            return KnockoutProductCandidateMarketDataResult
                                .MarketDataInvalidData(
                                    productIsin = productIsin
                                )
                    }

            candidatesWithMarketData +=
                KnockoutProductCandidateWithMarketData(
                    specificationSnapshot = candidate,
                    marketData = marketData
                )
        }

        return KnockoutProductCandidateMarketDataResult
            .CandidatesWithMarketData(
                candidates = candidatesWithMarketData
            )
    }
}
