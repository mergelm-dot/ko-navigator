package de.konavigator.app.data.remote.provider

/**
 * Providerneutraler technischer Vertrag zur Prüfung konkreter Produkt-ISINs bei einem Broker.
 *
 * Broker-ID und Produkt-ISIN-Liste werden exakt und ohne Normalisierung übergeben. Der Vertrag
 * sucht keine Produkte und beschreibt weder einen konkreten Broker noch einen Transport oder
 * eine UI. Eine erfolgreiche Prüfung ohne bestätigte Handelbarkeit wird durch [Success] mit
 * leerer Menge dargestellt.
 */
interface KnockoutProductBrokerAvailabilityProvider {

    suspend fun findTradableProductIsins(
        brokerId: String,
        productIsins: List<String>
    ): KnockoutProductBrokerAvailabilityProviderResult
}

/**
 * Technisches Ergebnis einer Broker-Verfügbarkeitsprüfung.
 *
 * Es gibt bewusst keinen `NotFound`-Zustand. [Success.tradableProductIsins] enthält
 * ausschließlich die vom Provider als handelbar bestätigten angefragten ISINs und darf leer
 * sein. Die Menge definiert keine Ranking- oder Anzeigereihenfolge.
 */
sealed interface KnockoutProductBrokerAvailabilityProviderResult {

    data class Success(
        val tradableProductIsins: Set<String>
    ) : KnockoutProductBrokerAvailabilityProviderResult

    data object DataAccessFailure : KnockoutProductBrokerAvailabilityProviderResult

    data object InvalidData : KnockoutProductBrokerAvailabilityProviderResult
}
