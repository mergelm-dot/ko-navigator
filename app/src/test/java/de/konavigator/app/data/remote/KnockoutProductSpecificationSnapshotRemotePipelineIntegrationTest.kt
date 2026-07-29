package de.konavigator.app.data.remote

import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductMarketDataProvider
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductSpecificationSnapshotProvider
import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.dataquality.DataQualityStatus
import de.konavigator.app.domain.dataquality.KnockoutProductDataQualityValidator
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft den neuen parallelen Snapshot-Pfad über Provider, Repository und Mapper bis zum
 * Domain-Snapshot. Die bestehende Data-Quality-Prüfung bewertet dessen enthaltene
 * Produktspezifikation weiterhin gemeinsam mit Produktmarktdaten. Quelle und Zeitbezug werden
 * nicht von Data Quality ersetzt oder erzeugt. Alle Testdaten sind synthetisch und lokal.
 */
class KnockoutProductSpecificationSnapshotRemotePipelineIntegrationTest {

    @Test
    fun validCompatibleSnapshotAndMarketDataPassEntirePipeline() {
        val (snapshotResult, marketDataResult) = loadPair()

        assertTrue(snapshotResult is RepositoryResult.Success)
        assertTrue(marketDataResult is RepositoryResult.Success)
        val snapshot = snapshotValue(snapshotResult)
        val marketData = marketDataValue(marketDataResult)
        val assessment = KnockoutProductDataQualityValidator.assess(
            specification = snapshot.specification,
            marketData = marketData
        )

        assertEquals(DataQualityStatus.PASSED, assessment.status)
        assertTrue(assessment.findings.isEmpty())
    }

    @Test
    fun successfulPipelinePreservesSnapshotSourceAndTimes() {
        val sourceId = "  Provider-Source Id  "
        val snapshot = snapshotValue(
            loadPair(
                snapshotInput = snapshotDto(
                    sourceId = sourceId,
                    retrievedAtEpochMillis = 123L,
                    sourceTimestampEpochMillis = 456L
                )
            ).first
        )

        assertEquals(sourceId, snapshot.sourceId)
        assertEquals(123L, snapshot.retrievedAtEpochMillis)
        assertEquals(456L, snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun nullSourceTimestampRemainsNullThroughEntireRepositoryPipeline() {
        val snapshot = snapshotValue(
            loadPair(
                snapshotInput = snapshotDto(
                    retrievedAtEpochMillis = 123L,
                    sourceTimestampEpochMillis = null
                )
            ).first
        )

        assertEquals(123L, snapshot.retrievedAtEpochMillis)
        assertNull(snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun mappableButInvalidSpecificationReachesDataQualityAndIsBlocked() {
        val (snapshotResult, marketDataResult) = loadPair(
            snapshotInput = snapshotDto(basePrice = -1.0)
        )

        assertTrue(snapshotResult is RepositoryResult.Success)
        assertTrue(marketDataResult is RepositoryResult.Success)
        val assessment = KnockoutProductDataQualityValidator.assess(
            specification = snapshotValue(snapshotResult).specification,
            marketData = marketDataValue(marketDataResult)
        )

        assertEquals(DataQualityStatus.BLOCKED, assessment.status)
        assertTrue(assessment.findings.isNotEmpty())
    }

    @Test
    fun missingRequiredSpecificationFieldBecomesRepositoryInvalidData() {
        val repository = snapshotRepository(
            lookupIsin = PRODUCT_ISIN,
            snapshot = snapshotDto(productIsin = null)
        )

        assertSame(
            RepositoryResult.InvalidData,
            findSnapshot(repository, PRODUCT_ISIN)
        )
    }

    @Test
    fun unknownProductIsinBecomesRepositoryNotFound() {
        val snapshotRepository = snapshotRepository(PRODUCT_ISIN, snapshotDto())
        val marketDataRepository = marketDataRepository(PRODUCT_ISIN, marketDataDto())

        assertSame(
            RepositoryResult.NotFound,
            findSnapshot(snapshotRepository, UNKNOWN_ISIN)
        )
        assertSame(
            RepositoryResult.NotFound,
            findMarketData(marketDataRepository, UNKNOWN_ISIN)
        )
    }

    @Test
    fun incompatibleProductIsinsReachDataQualityAndAreBlocked() {
        val (snapshotResult, marketDataResult) = loadPair(
            marketDataInput = marketDataDto(productIsin = OTHER_PRODUCT_ISIN)
        )

        assertTrue(snapshotResult is RepositoryResult.Success)
        assertTrue(marketDataResult is RepositoryResult.Success)
        val assessment = KnockoutProductDataQualityValidator.assess(
            specification = snapshotValue(snapshotResult).specification,
            marketData = marketDataValue(marketDataResult)
        )

        assertEquals(DataQualityStatus.BLOCKED, assessment.status)
        assertTrue(
            assessment.findings.any {
                it.code == DataQualityFindingCode.COMPATIBILITY_PRODUCT_ISIN_MISMATCH
            }
        )
    }

    @Test
    fun lookupProductIsinReachesBothProvidersExactlyUnchanged() {
        val lookupIsin = " De000Test001 "
        val (snapshotResult, marketDataResult) = loadPair(
            snapshotInput = snapshotDto(productIsin = lookupIsin),
            marketDataInput = marketDataDto(productIsin = lookupIsin),
            lookupIsin = lookupIsin
        )

        assertTrue(snapshotResult is RepositoryResult.Success)
        assertTrue(marketDataResult is RepositoryResult.Success)
        assertEquals(lookupIsin, snapshotValue(snapshotResult).specification.productIsin)
        assertEquals(lookupIsin, marketDataValue(marketDataResult).productIsin)
    }

    private fun loadPair(
        snapshotInput: KnockoutProductSpecificationSnapshotDto = snapshotDto(),
        marketDataInput: KnockoutProductMarketDataDto = marketDataDto(),
        lookupIsin: String = PRODUCT_ISIN
    ): Pair<
        RepositoryResult<KnockoutProductSpecificationSnapshot>,
        RepositoryResult<KnockoutProductMarketData>
    > {
        val snapshotRepository = snapshotRepository(lookupIsin, snapshotInput)
        val marketDataRepository = marketDataRepository(lookupIsin, marketDataInput)

        return findSnapshot(snapshotRepository, lookupIsin) to
            findMarketData(marketDataRepository, lookupIsin)
    }

    private fun snapshotRepository(
        lookupIsin: String,
        snapshot: KnockoutProductSpecificationSnapshotDto
    ) = RemoteKnockoutProductSpecificationSnapshotRepository(
        InMemoryKnockoutProductSpecificationSnapshotProvider(
            mapOf(lookupIsin to snapshot)
        )
    )

    private fun marketDataRepository(
        lookupIsin: String,
        marketData: KnockoutProductMarketDataDto
    ) = RemoteKnockoutProductMarketDataRepository(
        InMemoryKnockoutProductMarketDataProvider(
            mapOf(lookupIsin to marketData)
        )
    )

    private fun findSnapshot(
        repository: RemoteKnockoutProductSpecificationSnapshotRepository,
        productIsin: String
    ): RepositoryResult<KnockoutProductSpecificationSnapshot> = runSuspend {
        repository.findByProductIsin(productIsin)
    }

    private fun findMarketData(
        repository: RemoteKnockoutProductMarketDataRepository,
        productIsin: String
    ): RepositoryResult<KnockoutProductMarketData> = runSuspend {
        repository.findByProductIsin(productIsin)
    }

    private fun snapshotValue(
        result: RepositoryResult<KnockoutProductSpecificationSnapshot>
    ): KnockoutProductSpecificationSnapshot =
        (result as RepositoryResult.Success).value

    private fun marketDataValue(
        result: RepositoryResult<KnockoutProductMarketData>
    ): KnockoutProductMarketData =
        (result as RepositoryResult.Success).value

    private fun snapshotDto(
        productIsin: String? = PRODUCT_ISIN,
        basePrice: Double? = 100.0,
        sourceId: String = "snapshot-source",
        retrievedAtEpochMillis: Long = 1_700_000_000_100L,
        sourceTimestampEpochMillis: Long? = 1_700_000_000_000L
    ) = KnockoutProductSpecificationSnapshotDto(
        specification = specificationDto(
            productIsin = productIsin,
            basePrice = basePrice
        ),
        sourceId = sourceId,
        retrievedAtEpochMillis = retrievedAtEpochMillis,
        sourceTimestampEpochMillis = sourceTimestampEpochMillis
    )

    private fun specificationDto(
        productIsin: String?,
        basePrice: Double?
    ) = KnockoutProductSpecificationDto(
        productIsin = productIsin,
        productWkn = "ABC123",
        issuerId = "issuer-a",
        underlyingId = "underlying-a",
        direction = "LONG",
        basePrice = basePrice,
        knockoutBarrier = 90.0,
        ratio = 0.1,
        underlyingCurrency = "EUR",
        productCurrency = "EUR"
    )

    private fun marketDataDto(
        productIsin: String? = PRODUCT_ISIN
    ) = KnockoutProductMarketDataDto(
        productIsin = productIsin,
        bid = 1.0,
        ask = 1.1,
        bidTimestampEpochMillis = QUOTE_TIMESTAMP,
        askTimestampEpochMillis = QUOTE_TIMESTAMP,
        currency = "EUR",
        sourceId = "market-data-source"
    )

    private fun <T> runSuspend(block: suspend () -> T): T {
        var completed: Result<T>? = null
        block.startCoroutine(
            object : Continuation<T> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<T>) {
                    completed = result
                }
            }
        )
        return (completed ?: error("Suspend repository call did not complete synchronously"))
            .getOrThrow()
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000TEST001"
        const val OTHER_PRODUCT_ISIN = "DE000TEST002"
        const val UNKNOWN_ISIN = "DE000UNKNOWN"
        const val QUOTE_TIMESTAMP = 1_700_000_000_000L
    }
}
