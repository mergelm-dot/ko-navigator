package de.konavigator.app.debug.marketdata

import de.konavigator.app.application.repository.adapter.SnapshotBackedKnockoutProductSpecificationRepository
import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult
import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryLoader
import de.konavigator.app.data.remote.RemoteKnockoutProductSpecificationSnapshotRepository
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductSpecificationSnapshotProvider
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseKnockoutProductMarketDataMapper
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseSnapshotProviderCreationError
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.presentation.marketdata.MarketDataCalculationViewModelFactory
import java.io.File

sealed interface DeutscheBoerseCompressedFileMarketDataDemoCompositionResult {

    data class Success(
        val viewModelFactory: MarketDataCalculationViewModelFactory
    ) : DeutscheBoerseCompressedFileMarketDataDemoCompositionResult

    data class Failure(
        val error: DeutscheBoerseSnapshotProviderCreationError
    ) : DeutscheBoerseCompressedFileMarketDataDemoCompositionResult
}

object DeutscheBoerseCompressedFileMarketDataDemoComposition {

    /**
     * Lädt Produktmarktdaten aus lokalen komprimierten Deutsche-Börse-Dateien und speist
     * Produktspezifikationen als providerneutrale lokale Snapshots ein.
     *
     * Der bestehende Application-Service wird über die dokumentierte Kompatibilitätsbrücke
     * angeschlossen. Quelle und Zeitbezug bleiben im Snapshot-Pfad erhalten, können vom alten
     * Service-Port jedoch noch nicht weitertransportiert werden. Eingabemengen und Snapshot-Map
     * werden vor dem suspendierenden Dateizugriff defensiv kopiert. Die Composition verwendet
     * keine Netzwerkverbindung und keine Live-Datenquelle.
     */
    suspend fun createFactory(
        dxscGzipFile: File,
        xfraZipFile: File,
        requestedProductIsins: Set<String>,
        specificationSnapshots: Map<String, KnockoutProductSpecificationSnapshotDto>,
        freshnessThresholds: MarketDataFreshnessThresholds,
        repositoryLoader: DeutscheBoerseCompressedFileMarketDataRepositoryLoader =
            DeutscheBoerseCompressedFileMarketDataRepositoryLoader()
    ): DeutscheBoerseCompressedFileMarketDataDemoCompositionResult {
        val requestedProductIsinsSnapshot = requestedProductIsins.toSet()
        val specificationSnapshotsSnapshot = specificationSnapshots.toMap()
        return when (
            val repositoryCreationResult = repositoryLoader.load(
                dxscGzipFile = dxscGzipFile,
                xfraZipFile = xfraZipFile,
                requestedProductIsins = requestedProductIsinsSnapshot
            )
        ) {
            is DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Failure ->
                DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.Failure(
                    error = repositoryCreationResult.error
                )

            is DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success -> {
                val specificationSnapshotProvider =
                    InMemoryKnockoutProductSpecificationSnapshotProvider(
                        specificationSnapshotsSnapshot
                    )
                val specificationSnapshotRepository =
                    RemoteKnockoutProductSpecificationSnapshotRepository(
                        specificationSnapshotProvider
                    )
                val specificationRepository =
                    SnapshotBackedKnockoutProductSpecificationRepository(
                        specificationSnapshotRepository
                    )
                val viewModelFactory =
                    MarketDataCalculationDemoViewModelFactoryComposer.create(
                        specificationRepository = specificationRepository,
                        marketDataRepository = repositoryCreationResult.repository,
                        freshnessThresholds = freshnessThresholds,
                        marketDataSourceId =
                            DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID
                    )

                DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.Success(
                    viewModelFactory = viewModelFactory
                )
            }
        }
    }
}
