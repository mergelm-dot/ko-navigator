package de.konavigator.app.data.remote

import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.provider.KnockoutProductSpecificationProvider
import de.konavigator.app.data.remote.provider.ProviderResult
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKnockoutProductSpecificationRepositoryTest {

    @Test
    fun successfulDtoIsMappedToDomainModel() {
        val result = find(FakeProvider(ProviderResult.Success(dto())))

        assertTrue(result is RepositoryResult.Success)
        assertEquals(TradeDirection.LONG, successValue(result).direction)
    }

    @Test
    fun successfulMappingPreservesEveryField() {
        val result = successValue(
            find(
                FakeProvider(
                    ProviderResult.Success(
                        dto(
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
        )

        assertEquals(" de000test001 ", result.productIsin)
        assertEquals(null, result.productWkn)
        assertEquals("", result.issuerId)
        assertEquals(" underlying-a ", result.underlyingId)
        assertEquals(TradeDirection.SHORT, result.direction)
        assertEquals(-1.0, result.basePrice, 0.0)
        assertTrue(result.knockoutBarrier.isNaN())
        assertEquals(Double.POSITIVE_INFINITY, result.ratio, 0.0)
        assertEquals(" usd ", result.underlyingCurrency)
        assertEquals("eur", result.productCurrency)
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
            find(FakeProvider(ProviderResult.Success(dto(productIsin = null))))
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
        private val result: ProviderResult<KnockoutProductSpecificationDto>
    ) : KnockoutProductSpecificationProvider {

        var receivedProductIsin: String? = null
            private set

        override suspend fun findByProductIsin(
            productIsin: String
        ): ProviderResult<KnockoutProductSpecificationDto> {
            receivedProductIsin = productIsin
            return result
        }
    }

    private fun find(
        provider: KnockoutProductSpecificationProvider,
        productIsin: String = PRODUCT_ISIN
    ): RepositoryResult<KnockoutProductSpecification> = runSuspend {
        RemoteKnockoutProductSpecificationRepository(provider)
            .findByProductIsin(productIsin)
    }

    private fun successValue(
        result: RepositoryResult<KnockoutProductSpecification>
    ) = (result as RepositoryResult.Success).value

    private fun dto(
        productIsin: String? = PRODUCT_ISIN,
        productWkn: String? = "ABC123",
        issuerId: String? = "issuer-a",
        underlyingId: String? = "underlying-a",
        direction: String? = "LONG",
        basePrice: Double? = 80.0,
        knockoutBarrier: Double? = 82.0,
        ratio: Double? = 0.1,
        underlyingCurrency: String? = "EUR",
        productCurrency: String? = "EUR"
    ) = KnockoutProductSpecificationDto(
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
