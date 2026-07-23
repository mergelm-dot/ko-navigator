package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryKnockoutProductMarketDataProviderTest {

    @Test
    fun existingProductIsinReturnsSuccessWithExactDto() {
        val dto = dto()

        val result = find(provider(mapOf(PRODUCT_ISIN to dto)), PRODUCT_ISIN)

        assertTrue(result is ProviderResult.Success)
        assertSame(dto, (result as ProviderResult.Success).value)
    }

    @Test
    fun unknownProductIsinReturnsNotFound() {
        assertSame(
            ProviderResult.NotFound,
            find(provider(mapOf(PRODUCT_ISIN to dto())), "unknown")
        )
    }

    @Test
    fun optionalNullQuotesAndTimestampsRemainUnchanged() {
        val dto = dto(
            bid = null,
            ask = null,
            bidTimestampEpochMillis = null,
            askTimestampEpochMillis = null
        )

        val result = successValue(find(provider(mapOf(PRODUCT_ISIN to dto)), PRODUCT_ISIN))

        assertSame(dto, result)
        assertNull(result.bid)
        assertNull(result.ask)
        assertNull(result.bidTimestampEpochMillis)
        assertNull(result.askTimestampEpochMillis)
    }

    @Test
    fun productIsinLookupRemainsWhitespaceSensitive() {
        val spacedProductIsin = " $PRODUCT_ISIN "
        val dto = dto(productIsin = spacedProductIsin)
        val provider = provider(mapOf(spacedProductIsin to dto))

        assertSame(ProviderResult.NotFound, find(provider, PRODUCT_ISIN))
        assertSame(dto, successValue(find(provider, spacedProductIsin)))
    }

    @Test
    fun productIsinLookupRemainsCaseSensitive() {
        val lowerProductIsin = PRODUCT_ISIN.lowercase()
        val dto = dto(productIsin = lowerProductIsin)
        val provider = provider(mapOf(lowerProductIsin to dto))

        assertSame(ProviderResult.NotFound, find(provider, PRODUCT_ISIN))
        assertSame(dto, successValue(find(provider, lowerProductIsin)))
    }

    @Test
    fun multipleProductsRemainDistinct() {
        val first = dto(productIsin = PRODUCT_ISIN, sourceId = "source-a")
        val secondProductIsin = "DE000TEST002"
        val second = dto(productIsin = secondProductIsin, sourceId = "source-b")
        val provider = provider(
            mapOf(
                PRODUCT_ISIN to first,
                secondProductIsin to second
            )
        )

        assertSame(first, successValue(find(provider, PRODUCT_ISIN)))
        assertSame(second, successValue(find(provider, secondProductIsin)))
    }

    @Test
    fun equalLookupsAreDeterministicAndInputMapIsDefensivelyCopied() {
        val dto = dto()
        val input = mutableMapOf(PRODUCT_ISIN to dto)
        val provider = provider(input)

        val first = find(provider, PRODUCT_ISIN)
        input.clear()
        val second = find(provider, PRODUCT_ISIN)

        assertSame(dto, successValue(first))
        assertSame(dto, successValue(second))
    }

    private fun provider(
        values: Map<String, KnockoutProductMarketDataDto>
    ) = InMemoryKnockoutProductMarketDataProvider(values)

    private fun find(
        provider: InMemoryKnockoutProductMarketDataProvider,
        productIsin: String
    ): ProviderResult<KnockoutProductMarketDataDto> = runSuspend {
        provider.findByProductIsin(productIsin)
    }

    private fun successValue(
        result: ProviderResult<KnockoutProductMarketDataDto>
    ) = (result as ProviderResult.Success).value

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
        return (completed ?: error("Suspend provider call did not complete synchronously"))
            .getOrThrow()
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000TEST001"
    }
}
