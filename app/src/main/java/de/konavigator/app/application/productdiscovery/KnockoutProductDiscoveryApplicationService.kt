package de.konavigator.app.application.productdiscovery

import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository
import de.konavigator.app.application.repository.KnockoutProductSpecificationCatalogRepository

/**
 * Providerneutraler Application-Service, der sequenziell und fail-fast genau drei Stufen
 * koordiniert: Katalogsuche, verpflichtende Broker-Verfügbarkeitsprüfung und aktivierte
 * Emittentenauswahl – in exakt dieser Reihenfolge.
 *
 * Das Katalogrepository wird immer zuerst aufgerufen. Das Brokerrepository wird ausschließlich
 * nach einer erfolgreichen, nichtleeren Katalogantwort aufgerufen. Der Emittentenfilter wird nur
 * nach einer erfolgreichen Brokerprüfung mit mindestens einem brokerhandelbaren Kandidaten
 * aufgerufen. Request-Werte werden exakt in die jeweiligen Querys übernommen; insbesondere wird
 * `enabledIssuerIds` exakt an [KnockoutProductIssuerSelectionRequest] weitergegeben. Es erfolgen
 * keine automatische Aktivierung und keine Standardauswahl. Die Produkt-ISIN-Liste entsteht
 * ausschließlich aus den Katalogkandidaten und in deren Reihenfolge; ISIN-Duplikate werden für
 * die Brokerabfrage nicht entfernt.
 *
 * Die Filterung verwendet ausschließlich exakte Set-Mitgliedschaft. Ursprüngliche
 * Katalogreihenfolge und Katalogduplikate bleiben über alle drei Stufen erhalten. Der Service
 * sortiert, gruppiert und dedupliziert nicht und begrenzt die Kandidatenzahl nicht. Er validiert
 * und normalisiert nicht und bildet erwartbare Repository-Ergebnisse ohne Exceptions ab.
 * Technische und ungültige Datenzustände beider Repositories bleiben getrennt; bei Fehlern gibt
 * es keine Teilresultate.
 *
 * Regulär wird der Emittentenfilter nur mit einer nichtleeren brokerhandelbaren Kandidatenliste
 * aufgerufen, sodass `NoInputCandidates` nicht erreichbar ist. Der vollständige Ergebniszweig
 * bildet diesen Zustand dennoch ohne Exception und ohne Teilresultat fail-closed auf
 * `NoBrokerTradableCandidates` ab.
 *
 * Repository-Implementierungs-, Provider-, DTO- und Mappingdetails liegen außerhalb des
 * Services. Er enthält keine Marktdaten-, Berechnungs- oder Domainlogik, keine Systemzeit oder
 * Zeitumrechnung und keine Android-, Compose- oder UI-Abhängigkeit.
 */
class KnockoutProductDiscoveryApplicationService(
    private val catalogRepository:
        KnockoutProductSpecificationCatalogRepository,
    private val brokerAvailabilityRepository:
        KnockoutProductBrokerAvailabilityRepository,
    private val issuerSelectionFilter:
        KnockoutProductIssuerSelectionFilter
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

        if (brokerTradableCandidates.isEmpty()) {
            return KnockoutProductDiscoveryApplicationResult
                .NoBrokerTradableCandidates
        }

        val issuerSelectionRequest =
            KnockoutProductIssuerSelectionRequest(
                candidates = brokerTradableCandidates,
                enabledIssuerIds = request.enabledIssuerIds
            )

        return when (
            val issuerSelectionResult =
                issuerSelectionFilter.filter(issuerSelectionRequest)
        ) {
            is KnockoutProductIssuerSelectionResult.EnabledIssuerCandidates ->
                KnockoutProductDiscoveryApplicationResult
                    .BrokerTradableCandidates(
                        candidates = issuerSelectionResult.candidates
                    )

            KnockoutProductIssuerSelectionResult.NoEnabledIssuerCandidates ->
                KnockoutProductDiscoveryApplicationResult
                    .NoEnabledIssuerCandidates

            KnockoutProductIssuerSelectionResult.NoInputCandidates ->
                KnockoutProductDiscoveryApplicationResult
                    .NoBrokerTradableCandidates
        }
    }
}
