package de.konavigator.app.debug.marketdata

import de.konavigator.app.application.repository.adapter.SnapshotBackedKnockoutProductSpecificationRepository
import de.konavigator.app.data.remote.RemoteKnockoutProductMarketDataRepository
import de.konavigator.app.data.remote.RemoteKnockoutProductSpecificationSnapshotRepository
import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductMarketDataProvider
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductSpecificationSnapshotProvider
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.presentation.marketdata.MarketDataCalculationViewModelFactory

/**
 * Erzeugt bei jedem Aufruf einen vollständig neuen, deterministischen Objektgraphen für die
 * Debug-Engine-Demo. Alle Daten sind ausschließlich lokale, neutrale Testdaten.
 *
 * Die Produktspezifikation wird über den Snapshot-Pfad geladen. Der bestehende
 * Application-Service wird über die dokumentierte Kompatibilitätsbrücke angeschlossen.
 * Quelle und Zeitbezug bleiben im Snapshot-Pfad vorhanden, können über den alten Service-Port
 * jedoch noch nicht weitertransportiert werden. Die Abschlussverdrahtung erfolgt
 * ausschließlich durch den bestehenden [MarketDataCalculationDemoViewModelFactoryComposer].
 * Die Composition enthält keine eigene Policy-, Orchestrator-, Service- oder
 * Factory-Verdrahtung, keine Datei- oder Netzwerkverbindung und keine Live-Datenquelle. Sie ist
 * ausschließlich Bestandteil des Debug-Source-Sets.
 */
object MarketDataCalculationDemoComposition {

    fun createFactory(): MarketDataCalculationViewModelFactory {
        val specificationSnapshot = KnockoutProductSpecificationSnapshotDto(
            specification = KnockoutProductSpecificationDto(
                productIsin = "DE000DEMO001",
                productWkn = null,
                issuerId = "demo-issuer",
                underlyingId = "demo-underlying",
                direction = "LONG",
                basePrice = 80.0,
                knockoutBarrier = 82.0,
                ratio = 0.1,
                underlyingCurrency = "EUR",
                productCurrency = "EUR"
            ),
            sourceId = "demo-specification-source",
            retrievedAtEpochMillis = 1_700_000_000_500L,
            sourceTimestampEpochMillis = 1_700_000_000_250L
        )
        val marketData = KnockoutProductMarketDataDto(
            productIsin = "DE000DEMO001",
            bid = 1.8,
            ask = 2.0,
            bidTimestampEpochMillis = 1_700_000_000_000L,
            askTimestampEpochMillis = 1_700_000_000_000L,
            currency = "EUR",
            sourceId = "demo-source"
        )
        val specificationSnapshotProvider =
            InMemoryKnockoutProductSpecificationSnapshotProvider(
                mapOf("DE000DEMO001" to specificationSnapshot)
            )
        val specificationSnapshotRepository =
            RemoteKnockoutProductSpecificationSnapshotRepository(
                specificationSnapshotProvider
            )
        val specificationRepository = SnapshotBackedKnockoutProductSpecificationRepository(
            specificationSnapshotRepository
        )
        val marketDataProvider = InMemoryKnockoutProductMarketDataProvider(
            mapOf("DE000DEMO001" to marketData)
        )
        val marketDataRepository = RemoteKnockoutProductMarketDataRepository(
            marketDataProvider
        )
        return MarketDataCalculationDemoViewModelFactoryComposer.create(
            specificationRepository = specificationRepository,
            marketDataRepository = marketDataRepository,
            freshnessThresholds = MarketDataFreshnessThresholds(
                maxBidAgeMillis = 0L,
                maxAskAgeMillis = 0L,
                maxBidAskDifferenceMillis = 0L,
                allowedFutureSkewMillis = 0L
            ),
            marketDataSourceId = "demo-source"
        )
    }
}
