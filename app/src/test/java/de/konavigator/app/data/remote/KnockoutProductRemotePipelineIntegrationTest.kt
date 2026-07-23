package de.konavigator.app.data.remote

import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductMarketDataProvider
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductSpecificationProvider
import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.dataquality.DataQualityStatus
import de.konavigator.app.domain.dataquality.KnockoutProductDataQualityValidator
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductRemotePipelineIntegrationTest {

    @Test
    fun validCompatibleDtosPassThroughEntirePipeline() {
        val (specificationResult, marketDataResult) = loadPair()

        assertTrue(specificationResult is RepositoryResult.Success)
        assertTrue(marketDataResult is RepositoryResult.Success)

        val assessment = KnockoutProductDataQualityValidator.assess(
            specification = specificationValue(specificationResult),
            marketData = marketDataValue(marketDataResult)
        )

        assertEquals(DataQualityStatus.PASSED, assessment.status)
        assertTrue(assessment.findings.isEmpty())
    }

    @Test
    fun mappableButInvalidDataReachesDataQualityAndIsBlocked() {
        val (specificationResult, marketDataResult) = loadPair(
            specificationInput = specificationDto(basePrice = -1.0)
        )

        assertTrue(specificationResult is RepositoryResult.Success)
        assertTrue(marketDataResult is RepositoryResult.Success)

        val assessment = KnockoutProductDataQualityValidator.assess(
            specification = specificationValue(specificationResult),
            marketData = marketDataValue(marketDataResult)
        )

        assertEquals(DataQualityStatus.BLOCKED, assessment.status)
        assertTrue(assessment.findings.isNotEmpty())
    }

    @Test
    fun missingRequiredDtoFieldBecomesRepositoryInvalidData() {
        val provider = InMemoryKnockoutProductSpecificationProvider(
            mapOf(
                PRODUCT_ISIN to specificationDto(productIsin = null)
            )
        )
        val repository = RemoteKnockoutProductSpecificationRepository(provider)

        val result = findSpecification(repository, PRODUCT_ISIN)

        assertSame(RepositoryResult.InvalidData, result)
    }

    @Test
    fun unknownProductIsinBecomesRepositoryNotFound() {
        val specificationRepository = RemoteKnockoutProductSpecificationRepository(
            InMemoryKnockoutProductSpecificationProvider(
                mapOf(PRODUCT_ISIN to specificationDto())
            )
        )
        val marketDataRepository = RemoteKnockoutProductMarketDataRepository(
            InMemoryKnockoutProductMarketDataProvider(
                mapOf(PRODUCT_ISIN to marketDataDto())
            )
        )

        assertSame(
            RepositoryResult.NotFound,
            findSpecification(specificationRepository, UNKNOWN_ISIN)
        )
        assertSame(
            RepositoryResult.NotFound,
            findMarketData(marketDataRepository, UNKNOWN_ISIN)
        )
    }

    @Test
    fun incompatibleProductIsinsReachDataQualityAndAreBlocked() {
        val (specificationResult, marketDataResult) = loadPair(
            marketDataInput = marketDataDto(productIsin = OTHER_PRODUCT_ISIN)
        )

        assertTrue(specificationResult is RepositoryResult.Success)
        assertTrue(marketDataResult is RepositoryResult.Success)

        val assessment = KnockoutProductDataQualityValidator.assess(
            specification = specificationValue(specificationResult),
            marketData = marketDataValue(marketDataResult)
        )

        assertEquals(DataQualityStatus.BLOCKED, assessment.status)
        assertTrue(
            assessment.findings.any {
                it.code == DataQualityFindingCode.COMPATIBILITY_PRODUCT_ISIN_MISMATCH
            }
        )
    }

    private fun loadPair(
        specificationInput: KnockoutProductSpecificationDto = specificationDto(),
        marketDataInput: KnockoutProductMarketDataDto = marketDataDto(),
        lookupIsin: String = PRODUCT_ISIN
    ): Pair<
            RepositoryResult<KnockoutProductSpecification>,
            RepositoryResult<KnockoutProductMarketData>
            > {
        val specificationRepository = RemoteKnockoutProductSpecificationRepository(
            InMemoryKnockoutProductSpecificationProvider(
                mapOf(lookupIsin to specificationInput)
            )
        )
        val marketDataRepository = RemoteKnockoutProductMarketDataRepository(
            InMemoryKnockoutProductMarketDataProvider(
                mapOf(lookupIsin to marketDataInput)
            )
        )

        return findSpecification(specificationRepository, lookupIsin) to
                findMarketData(marketDataRepository, lookupIsin)
    }

    private fun findSpecification(
        repository: RemoteKnockoutProductSpecificationRepository,
        productIsin: String
    ): RepositoryResult<KnockoutProductSpecification> = runSuspend {
        repository.findByProductIsin(productIsin)
    }

    private fun findMarketData(
        repository: RemoteKnockoutProductMarketDataRepository,
        productIsin: String
    ): RepositoryResult<KnockoutProductMarketData> = runSuspend {
        repository.findByProductIsin(productIsin)
    }

    private fun specificationValue(
        result: RepositoryResult<KnockoutProductSpecification>
    ): KnockoutProductSpecification =
        (result as RepositoryResult.Success).value

    private fun marketDataValue(
        result: RepositoryResult<KnockoutProductMarketData>
    ): KnockoutProductMarketData =
        (result as RepositoryResult.Success).value

    private fun specificationDto(
        productIsin: String? = PRODUCT_ISIN,
        basePrice: Double? = 100.0
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
        sourceId = "mock-provider"
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

        return (
                completed
                    ?: error("Suspend repository call did not complete synchronously")
                ).getOrThrow()
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000TEST001"
        const val OTHER_PRODUCT_ISIN = "DE000TEST002"
        const val UNKNOWN_ISIN = "DE000UNKNOWN"
        const val QUOTE_TIMESTAMP = 1_700_000_000_000L
    }
}