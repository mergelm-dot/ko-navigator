package de.konavigator.app.data.remote

import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.data.remote.provider.KnockoutProductSpecificationSnapshotProvider
import de.konavigator.app.data.remote.provider.ProviderResult
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKnockoutProductSpecificationSnapshotRepositoryTest {

    @Test
    fun successfulProviderSnapshotMapsToRepositorySuccess() {
        val result = find(FakeProvider(ProviderResult.Success(snapshotDto())))

        assertTrue(result is RepositoryResult.Success)
        assertEquals(TradeDirection.LONG, successValue(result).specification.direction)
    }

    @Test
    fun successfulMappingPreservesEverySpecificationField() {
        val specification = successValue(
            find(
                FakeProvider(
                    ProviderResult.Success(
                        snapshotDto(
                            productIsin = " de000test001 ",
                            productWkn = null,
                            issuerId = "",
                            underlyingId = " underlying-a ",
                            direction = "SHORT",
                            basePrice = -1.0,
                            knockoutBarrier = Double.NaN,
                            ratio = Double.POSITIVE_INFINITY,
                            underlyingCurrency = " usd ",
                            productCurrency = "eur"
                        )
                    )
                )
            )
        ).specification

        assertEquals(" de000test001 ", specification.productIsin)
        assertNull(specification.productWkn)
        assertEquals("", specification.issuerId)
        assertEquals(" underlying-a ", specification.underlyingId)
        assertEquals(TradeDirection.SHORT, specification.direction)
        assertEquals(-1.0, specification.basePrice, 0.0)
        assertTrue(specification.knockoutBarrier.isNaN())
        assertEquals(Double.POSITIVE_INFINITY, specification.ratio, 0.0)
        assertEquals(" usd ", specification.underlyingCurrency)
        assertEquals("eur", specification.productCurrency)
    }

    @Test
    fun sourceIdPreservesWhitespaceAndCase() {
        val sourceId = "  Provider-Source Id  "

        assertEquals(
            sourceId,
            successValue(find(FakeProvider(ProviderResult.Success(snapshotDto(sourceId = sourceId)))))
                .sourceId
        )
    }

    @Test
    fun retrievedAtEpochMillisRemainsExact() {
        val retrievedAt = 9_223_372_036_854_775_000L

        assertEquals(
            retrievedAt,
            successValue(
                find(
                    FakeProvider(
                        ProviderResult.Success(
                            snapshotDto(retrievedAtEpochMillis = retrievedAt)
                        )
                    )
                )
            ).retrievedAtEpochMillis
        )
    }

    @Test
    fun nullSourceTimestampRemainsNull() {
        val result = successValue(
            find(
                FakeProvider(
                    ProviderResult.Success(snapshotDto(sourceTimestampEpochMillis = null))
                )
            )
        )

        assertNull(result.sourceTimestampEpochMillis)
    }

    @Test
    fun retrievedAndSourceTimestampsRemainSeparate() {
        val result = successValue(
            find(
                FakeProvider(
                    ProviderResult.Success(
                        snapshotDto(
                            retrievedAtEpochMillis = 123L,
                            sourceTimestampEpochMillis = 456L
                        )
                    )
                )
            )
        )

        assertEquals(123L, result.retrievedAtEpochMillis)
        assertEquals(456L, result.sourceTimestampEpochMillis)
        assertNotEquals(result.retrievedAtEpochMillis, result.sourceTimestampEpochMillis)
    }

    @Test
    fun emptySourceAndNegativeTimesRemainSuccessful() {
        val result = successValue(
            find(
                FakeProvider(
                    ProviderResult.Success(
                        snapshotDto(
                            sourceId = "",
                            retrievedAtEpochMillis = -1L,
                            sourceTimestampEpochMillis = -2L
                        )
                    )
                )
            )
        )

        assertEquals("", result.sourceId)
        assertEquals(-1L, result.retrievedAtEpochMillis)
        assertEquals(-2L, result.sourceTimestampEpochMillis)
    }

    @Test
    fun providerNotFoundBecomesRepositoryNotFound() {
        assertSame(RepositoryResult.NotFound, find(FakeProvider(ProviderResult.NotFound)))
    }

    @Test
    fun providerDataAccessFailureBecomesRepositoryDataAccessFailure() {
        assertSame(
            RepositoryResult.DataAccessFailure,
            find(FakeProvider(ProviderResult.DataAccessFailure))
        )
    }

    @Test
    fun invalidEmbeddedSpecificationBecomesRepositoryInvalidData() {
        assertSame(
            RepositoryResult.InvalidData,
            find(FakeProvider(ProviderResult.Success(snapshotDto(productIsin = null))))
        )
    }

    @Test
    fun multipleSpecificationMappingErrorsBecomeOnlyRepositoryInvalidData() {
        assertSame(
            RepositoryResult.InvalidData,
            find(
                FakeProvider(
                    ProviderResult.Success(
                        snapshotDto(
                            productIsin = null,
                            direction = null,
                            ratio = null,
                            productCurrency = null
                        )
                    )
                )
            )
        )
    }

    @Test
    fun productIsinReachesProviderExactlyUnchanged() {
        val productIsin = " De000Test001 "
        val provider = FakeProvider(ProviderResult.NotFound)

        find(provider, productIsin)

        assertEquals(productIsin, provider.receivedProductIsin)
    }

    @Test
    fun emptyProductIsinReachesProviderUnchanged() {
        val provider = FakeProvider(ProviderResult.NotFound)

        find(provider, "")

        assertEquals("", provider.receivedProductIsin)
    }

    @Test
    fun repositoryAdapterDoesNotMutateProviderDto() {
        val input = snapshotDto(
            productIsin = " de000test001 ",
            productWkn = null,
            issuerId = "",
            sourceId = " source ",
            retrievedAtEpochMillis = -1L,
            sourceTimestampEpochMillis = null
        )
        val embeddedBefore = input.specification.copy()
        val snapshotBefore = input.copy()

        find(FakeProvider(ProviderResult.Success(input)))

        assertEquals(snapshotBefore, input)
        assertEquals(embeddedBefore, input.specification)
    }

    @Test
    fun repeatedIdenticalCallsProduceEqualRepositoryResults() {
        val provider = FakeProvider(ProviderResult.Success(snapshotDto()))

        assertEquals(find(provider), find(provider))
    }

    private class FakeProvider(
        private val result: ProviderResult<KnockoutProductSpecificationSnapshotDto>
    ) : KnockoutProductSpecificationSnapshotProvider {

        var receivedProductIsin: String? = null
            private set

        override suspend fun findByProductIsin(
            productIsin: String
        ): ProviderResult<KnockoutProductSpecificationSnapshotDto> {
            receivedProductIsin = productIsin
            return result
        }
    }

    private fun find(
        provider: KnockoutProductSpecificationSnapshotProvider,
        productIsin: String = PRODUCT_ISIN
    ): RepositoryResult<KnockoutProductSpecificationSnapshot> = runSuspend {
        RemoteKnockoutProductSpecificationSnapshotRepository(provider)
            .findByProductIsin(productIsin)
    }

    private fun successValue(
        result: RepositoryResult<KnockoutProductSpecificationSnapshot>
    ) = (result as RepositoryResult.Success).value

    private fun snapshotDto(
        productIsin: String? = PRODUCT_ISIN,
        productWkn: String? = "ABC123",
        issuerId: String? = "issuer-a",
        underlyingId: String? = "underlying-a",
        direction: String? = "LONG",
        basePrice: Double? = 80.0,
        knockoutBarrier: Double? = 82.0,
        ratio: Double? = 0.1,
        underlyingCurrency: String? = "EUR",
        productCurrency: String? = "EUR",
        sourceId: String = "test-source",
        retrievedAtEpochMillis: Long = 100L,
        sourceTimestampEpochMillis: Long? = 90L
    ) = KnockoutProductSpecificationSnapshotDto(
        specification = KnockoutProductSpecificationDto(
            productIsin = productIsin,
            productWkn = productWkn,
            issuerId = issuerId,
            underlyingId = underlyingId,
            direction = direction,
            basePrice = basePrice,
            knockoutBarrier = knockoutBarrier,
            ratio = ratio,
            underlyingCurrency = underlyingCurrency,
            productCurrency = productCurrency
        ),
        sourceId = sourceId,
        retrievedAtEpochMillis = retrievedAtEpochMillis,
        sourceTimestampEpochMillis = sourceTimestampEpochMillis
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
    }
}
