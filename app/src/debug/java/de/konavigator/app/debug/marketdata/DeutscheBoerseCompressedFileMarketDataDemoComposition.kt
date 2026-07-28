package de.konavigator.app.debug.marketdata

import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationService
import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult
import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryLoader
import de.konavigator.app.data.remote.RemoteKnockoutProductSpecificationRepository
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductSpecificationProvider
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseKnockoutProductMarketDataMapper
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseSnapshotProviderCreationError
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrator
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
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

    suspend fun createFactory(
        dxscGzipFile: File,
        xfraZipFile: File,
        requestedProductIsins: Set<String>,
        specificationDtos: Map<String, KnockoutProductSpecificationDto>,
        freshnessThresholds: MarketDataFreshnessThresholds,
        repositoryLoader: DeutscheBoerseCompressedFileMarketDataRepositoryLoader =
            DeutscheBoerseCompressedFileMarketDataRepositoryLoader()
    ): DeutscheBoerseCompressedFileMarketDataDemoCompositionResult {
        val requestedProductIsinsSnapshot = requestedProductIsins.toSet()
        val specificationDtosSnapshot = specificationDtos.toMap()
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
                val specificationRepository = RemoteKnockoutProductSpecificationRepository(
                    InMemoryKnockoutProductSpecificationProvider(specificationDtosSnapshot)
                )
                val freshnessPolicy = MarketDataFreshnessPolicy(freshnessThresholds)
                val sourcePolicy = MarketDataSourcePolicy(
                    MarketDataSourcePolicyConfig(
                        rules = listOf(
                            MarketDataSourceRule(
                                sourceId =
                                    DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID,
                                supportedCalculationTypes = setOf(
                                    MarketDataCalculationType.PURCHASE_PRICE,
                                    MarketDataCalculationType.SALE_PRICE,
                                    MarketDataCalculationType.SPREAD,
                                    MarketDataCalculationType.MID
                                )
                            )
                        )
                    )
                )
                val orchestrator = MarketDataCalculationOrchestrator(
                    freshnessPolicy = freshnessPolicy,
                    sourcePolicy = sourcePolicy
                )
                val applicationService = MarketDataCalculationApplicationService(
                    specificationRepository = specificationRepository,
                    marketDataRepository = repositoryCreationResult.repository,
                    orchestrator = orchestrator
                )

                DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.Success(
                    viewModelFactory = MarketDataCalculationViewModelFactory(applicationService)
                )
            }
        }
    }
}
