package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductSpecificationProviderTest {

    @Test
    fun successTransportsExactDto() {
        val dto = dto()
        val provider = FakeProvider(ProviderResult.Success(dto))

        val result = find(provider, PRODUCT_ISIN)

        assertTrue(result is ProviderResult.Success)
        assertSame(dto, (result as ProviderResult.Success).value)
    }

    @Test
    fun notFoundRemainsDistinctState() {
        assertSame(
            ProviderResult.NotFound,
            find(FakeProvider(ProviderResult.NotFound), PRODUCT_ISIN)
        )
    }

    @Test
    fun dataAccessFailureRemainsDistinctState() {
        assertSame(
            ProviderResult.DataAccessFailure,
            find(FakeProvider(ProviderResult.DataAccessFailure), PRODUCT_ISIN)
        )
    }

    @Test
    fun productIsinReachesProviderExactlyUnchanged() {
        val productIsin = " de000test001 "
        val provider = FakeProvider(ProviderResult.NotFound)

        find(provider, productIsin)

        assertEquals(productIsin, provider.receivedProductIsin)
    }

    @Test
    fun exposesExactlyOneSuspendFunction() {
        val methods = KnockoutProductSpecificationProvider::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }

        assertEquals(1, methods.size)
        assertEquals("findByProductIsin", methods.single().name)
        assertEquals(String::class.java, methods.single().parameterTypes.first())
        assertEquals(Continuation::class.java, methods.single().parameterTypes.last())
    }

    @Test
    fun publicApiHasNoMapperOrDomainDependency() {
        assertTrue(
            apiTypeNames(KnockoutProductSpecificationProvider::class.java).none {
                it.contains(".mapper.") || it.contains(".domain.")
            }
        )
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
        productIsin: String
    ): ProviderResult<KnockoutProductSpecificationDto> = runSuspend {
        provider.findByProductIsin(productIsin)
    }

    private fun dto() = KnockoutProductSpecificationDto(
        productIsin = PRODUCT_ISIN,
        productWkn = "ABC123",
        issuerId = "issuer-a",
        underlyingId = "underlying-a",
        direction = "LONG",
        basePrice = 80.0,
        knockoutBarrier = 82.0,
        ratio = 0.1,
        underlyingCurrency = "EUR",
        productCurrency = "EUR"
    )

    private fun apiTypeNames(type: Class<*>): List<String> = buildList {
        add(type.name)
        type.declaredFields.forEach { add(it.type.name) }
        type.declaredMethods.forEach { method ->
            add(method.returnType.name)
            method.parameterTypes.forEach { add(it.name) }
            method.genericParameterTypes.forEach { add(it.typeName) }
            add(method.genericReturnType.typeName)
        }
    }

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
