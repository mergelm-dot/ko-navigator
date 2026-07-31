package de.konavigator.app.application.productdiscovery

import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository
import de.konavigator.app.application.repository.KnockoutProductSpecificationCatalogRepository

/**
 * Providerneutraler Application-Service, der sequenziell und fail-fast genau zwei Stufen
 * koordiniert: Katalogsuche und verpflichtende Broker-Verfügbarkeitsprüfung.
 *
 * Das Katalogrepository wird immer zuerst aufgerufen. Das Brokerrepository wird ausschließlich
 * nach einer erfolgreichen, nichtleeren Katalogantwort aufgerufen. Request-Werte werden exakt in
 * die jeweiligen Querys übernommen. Die Produkt-ISIN-Liste entsteht ausschließlich aus den
 * Katalogkandidaten und in deren Reihenfolge; ISIN-Duplikate werden für die Brokerabfrage nicht
 * entfernt.
 *
 * Die Filterung verwendet ausschließlich exakte Set-Mitgliedschaft. Ursprüngliche
 * Katalogreihenfolge und Katalogduplikate bleiben erhalten. Der Service sortiert, gruppiert und
 * dedupliziert nicht und begrenzt die Kandidatenzahl nicht. Er validiert und normalisiert nicht
 * und bildet erwartbare Repository-Ergebnisse ohne Exceptions ab. Technische und ungültige
 * Datenzustände beider Repositories bleiben getrennt; bei Fehlern gibt es keine Teilresultate.
 *
 * Repository-Implementierungs-, Provider-, DTO- und Mappingdetails liegen außerhalb des
 * Services. Er enthält keine Marktdaten-, Berechnungs- oder Domainlogik, keine Systemzeit oder
 * Zeitumrechnung und keine Android-, Compose- oder UI-Abhängigkeit.
 */
class KnockoutProductDiscoveryApplicationService(
    private val catalogRepository:
        KnockoutProductSpecificationCatalogRepository,
    private val brokerAvailabilityRepository:
        KnockoutProductBrokerAvailabilityRepository
) {

    suspend fun execute(
        request: KnockoutProductDiscoveryApplicationRequest
    ): KnockoutProductDiscoveryApplicationResult {
        val catalogQuery =
            KnockoutProductSpecificationCatalogQuery(
                underlyingId = request.underlyingId,
                direction = request.direction
            )

        val catalogCandidates =
            when (
                val catalogResult =
                    catalogRepository.findCandidates(catalogQuery)
            ) {
                is KnockoutProductSpecificationCatalogResult.Success ->
                    catalogResult.candidates

                KnockoutProductSpecificationCatalogResult.DataAccessFailure ->
                    return KnockoutProductDiscoveryApplicationResult
                        .CatalogDataAccessFailure

                KnockoutProductSpecificationCatalogResult.InvalidData ->
                    return KnockoutProductDiscoveryApplicationResult
                        .CatalogInvalidData
            }

        if (catalogCandidates.isEmpty()) {
            return KnockoutProductDiscoveryApplicationResult
                .NoCatalogCandidates
        }

        val availabilityQuery =
            KnockoutProductBrokerAvailabilityQuery(
                brokerId = request.brokerId,
                productIsins = catalogCandidates.map { candidate ->
                    candidate.specification.productIsin
                }
            )

        val tradableProductIsins =
            when (
                val availabilityResult =
                    brokerAvailabilityRepository
                        .findTradableProductIsins(availabilityQuery)
            ) {
                is KnockoutProductBrokerAvailabilityResult.Success ->
                    availabilityResult.tradableProductIsins

                KnockoutProductBrokerAvailabilityResult.DataAccessFailure ->
                    return KnockoutProductDiscoveryApplicationResult
                        .BrokerAvailabilityDataAccessFailure

                KnockoutProductBrokerAvailabilityResult.InvalidData ->
                    return KnockoutProductDiscoveryApplicationResult
                        .BrokerAvailabilityInvalidData
            }

        val brokerTradableCandidates =
            catalogCandidates.filter { candidate ->
                candidate.specification.productIsin in tradableProductIsins
            }

        return if (brokerTradableCandidates.isEmpty()) {
            KnockoutProductDiscoveryApplicationResult
                .NoBrokerTradableCandidates
        } else {
            KnockoutProductDiscoveryApplicationResult
                .BrokerTradableCandidates(
                    candidates = brokerTradableCandidates
                )
        }
    }
}
