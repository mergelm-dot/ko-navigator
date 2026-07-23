package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryKnockoutProductSpecificationProviderTest {

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
    fun whitespaceInProductIsinIsSearchedExactly() {
        val spacedProductIsin = " $PRODUCT_ISIN "
        val dto = dto(productIsin = spacedProductIsin)
        val provider = provider(mapOf(spacedProductIsin to dto))

        assertSame(ProviderResult.NotFound, find(provider, PRODUCT_ISIN))
        assertSame(dto, successValue(find(provider, spacedProductIsin)))
    }

    @Test
    fun productIsinCaseIsNotChanged() {
        val lowerProductIsin = PRODUCT_ISIN.lowercase()
        val dto = dto(productIsin = lowerProductIsin)
        val provider = provider(mapOf(lowerProductIsin to dto))

        assertSame(ProviderResult.NotFound, find(provider, PRODUCT_ISIN))
        assertSame(dto, successValue(find(provider, lowerProductIsin)))
    }

    @Test
    fun multipleProductsRemainDistinct() {
        val first = dto(productIsin = PRODUCT_ISIN, issuerId = "issuer-a")
        val secondProductIsin = "DE000TEST002"
        val second = dto(productIsin = secondProductIsin, issuerId = "issuer-b")
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
        values: Map<String, KnockoutProductSpecificationDto>
    ) = InMemoryKnockoutProductSpecificationProvider(values)

    private fun find(
        provider: InMemoryKnockoutProductSpecificationProvider,
        productIsin: String
    ): ProviderResult<KnockoutProductSpecificationDto> = runSuspend {
        provider.findByProductIsin(productIsin)
    }

    private fun successValue(
        result: ProviderResult<KnockoutProductSpecificationDto>
    ) = (result as ProviderResult.Success).value

    private fun dto(
        productIsin: String? = PRODUCT_ISIN,
        issuerId: String? = "issuer-a"
    ) = KnockoutProductSpecificationDto(
        productIsin = productIsin,
        productWkn = "ABC123",
        issuerId = issuerId,
        underlyingId = "underlying-a",
        direction = "LONG",
        basePrice = 80.0,
        knockoutBarrier = 82.0,
        ratio = 0.1,
        underlyingCurrency = "EUR",
        productCurrency = "EUR"
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
