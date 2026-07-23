package de.konavigator.app.data.remote

import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.data.remote.provider.KnockoutProductMarketDataProvider
import de.konavigator.app.data.remote.provider.ProviderResult
import de.konavigator.app.domain.model.KnockoutProductMarketData
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKnockoutProductMarketDataRepositoryTest {

    @Test
    fun successfulDtoIsMappedCompletelyAndWithoutCorrection() {
        val result = successValue(
            find(
                FakeProvider(
                    ProviderResult.Success(
                        dto(
                            productIsin = " de000test001 ",
                            bid = -1.0,
                            ask = Double.POSITIVE_INFINITY,
                            bidTimestampEpochMillis = -10L,
                            askTimestampEpochMillis = 20L,
                            currency = " eur ",
                            sourceId = " source-a "
                        )
                    )
                )
            )
        )

        assertEquals(" de000test001 ", result.productIsin)
        assertEquals(-1.0, result.bid!!, 0.0)
        assertEquals(Double.POSITIVE_INFINITY, result.ask!!, 0.0)
        assertEquals(-10L, result.bidTimestampEpochMillis)
        assertEquals(20L, result.askTimestampEpochMillis)
        assertEquals(" eur ", result.currency)
        assertEquals(" source-a ", result.sourceId)
    }

    @Test
    fun optionalQuotesAndTimestampsRemainNull() {
        val result = successValue(
            find(
                FakeProvider(
                    ProviderResult.Success(
                        dto(
                            bid = null,
                            ask = null,
                            bidTimestampEpochMillis = null,
                            askTimestampEpochMillis = null
                        )
                    )
                )
            )
        )

        assertNull(result.bid)
        assertNull(result.ask)
        assertNull(result.bidTimestampEpochMillis)
        assertNull(result.askTimestampEpochMillis)
    }

    @Test
    fun providerNotFoundRemainsRepositoryNotFound() {
        assertSame(
            RepositoryResult.NotFound,
            find(FakeProvider(ProviderResult.NotFound))
        )
    }

    @Test
    fun providerDataAccessFailureRemainsRepositoryDataAccessFailure() {
        assertSame(
            RepositoryResult.DataAccessFailure,
            find(FakeProvider(ProviderResult.DataAccessFailure))
        )
    }

    @Test
    fun mapperFailureBecomesRepositoryInvalidData() {
        assertSame(
            RepositoryResult.InvalidData,
            find(FakeProvider(ProviderResult.Success(dto(sourceId = null))))
        )
    }

    @Test
    fun productIsinReachesProviderExactlyUnchanged() {
        val productIsin = " de000Test001 "
        val provider = FakeProvider(ProviderResult.NotFound)

        find(provider, productIsin)

        assertEquals(productIsin, provider.receivedProductIsin)
    }

    private class FakeProvider(
        private val result: ProviderResult<KnockoutProductMarketDataDto>
    ) : KnockoutProductMarketDataProvider {

        var receivedProductIsin: String? = null
            private set

        override suspend fun findByProductIsin(
            productIsin: String
        ): ProviderResult<KnockoutProductMarketDataDto> {
            receivedProductIsin = productIsin
            return result
        }
    }

    private fun find(
        provider: KnockoutProductMarketDataProvider,
        productIsin: String = PRODUCT_ISIN
    ): RepositoryResult<KnockoutProductMarketData> = runSuspend {
        RemoteKnockoutProductMarketDataRepository(provider)
            .findByProductIsin(productIsin)
    }

    private fun successValue(
        result: RepositoryResult<KnockoutProductMarketData>
    ): KnockoutProductMarketData {
        assertTrue(result is RepositoryResult.Success)
        return (result as RepositoryResult.Success).value
    }

    private fun dto(
        productIsin: String? = PRODUCT_ISIN,
        bid: Double? = 1.8,
        ask: Double? = 2.0,
        bidTimestampEpochMillis: Long? = 1_000L,
        askTimestampEpochMillis: Long? = 1_001L,
        currency: String? = "EUR",
        sourceId: String? = "source-a"
    ) = KnockoutProductMarketDataDto(
        productIsin = productIsin,
        bid = bid,
        ask = ask,
        bidTimestampEpochMillis = bidTimestampEpochMillis,
        askTimestampEpochMillis = askTimestampEpochMillis,
        currency = currency,
        sourceId = sourceId
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
