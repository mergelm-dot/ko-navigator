package de.konavigator.app.data.remote

import de.konavigator.app.application.productdiscovery.KnockoutProductBrokerAvailabilityQuery
import de.konavigator.app.application.productdiscovery.KnockoutProductBrokerAvailabilityResult
import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository
import de.konavigator.app.data.remote.provider.KnockoutProductBrokerAvailabilityProvider
import de.konavigator.app.data.remote.provider.KnockoutProductBrokerAvailabilityProviderResult

/**
 * Remote-Data-Adapter zwischen dem technischen Broker-Provider und dem Application-Port.
 *
 * Query-Werte werden exakt an den Provider delegiert. Eine erfolgreiche Provider-Antwort wird
 * nur akzeptiert, wenn jede bestätigte ISIN exakt in der angefragten Liste enthalten ist. Eine
 * unbekannte, anders geschriebene oder mit abweichendem Whitespace versehene ISIN macht das
 * gesamte Ergebnis fail-closed zu [KnockoutProductBrokerAvailabilityResult.InvalidData].
 *
 * Der Adapter besitzt keine Broker-Businessregeln, Produktsuche, Normalisierung, Marktdaten-,
 * Freshness-, Data-Quality-, FX-, Berechnungs-, Ranking-, Netzwerk-, Zeit- oder UI-Logik.
 */
class RemoteKnockoutProductBrokerAvailabilityRepository(
    private val provider: KnockoutProductBrokerAvailabilityProvider
) : KnockoutProductBrokerAvailabilityRepository {

    override suspend fun findTradableProductIsins(
        query: KnockoutProductBrokerAvailabilityQuery
    ): KnockoutProductBrokerAvailabilityResult =
        when (
            val providerResult = provider.findTradableProductIsins(
                brokerId = query.brokerId,
                productIsins = query.productIsins
            )
        ) {
            is KnockoutProductBrokerAvailabilityProviderResult.Success ->
                mapSuccess(query, providerResult)

            KnockoutProductBrokerAvailabilityProviderResult.DataAccessFailure ->
                KnockoutProductBrokerAvailabilityResult.DataAccessFailure

            KnockoutProductBrokerAvailabilityProviderResult.InvalidData ->
                KnockoutProductBrokerAvailabilityResult.InvalidData
        }

    private fun mapSuccess(
        query: KnockoutProductBrokerAvailabilityQuery,
        providerResult: KnockoutProductBrokerAvailabilityProviderResult.Success
    ): KnockoutProductBrokerAvailabilityResult =
        if (
            providerResult.tradableProductIsins.all { productIsin ->
                productIsin in query.productIsins
            }
        ) {
            KnockoutProductBrokerAvailabilityResult.Success(
                tradableProductIsins = providerResult.tradableProductIsins
            )
        } else {
            KnockoutProductBrokerAvailabilityResult.InvalidData
        }
}
