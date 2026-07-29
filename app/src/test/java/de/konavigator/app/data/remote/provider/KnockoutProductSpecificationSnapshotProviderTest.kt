package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductSpecificationSnapshotProviderTest {

    @Test
    fun successTransportsExactSnapshotInstance() {
        val snapshot = snapshot()
        val result = find(FakeProvider(ProviderResult.Success(snapshot)), PRODUCT_ISIN)

        assertTrue(result is ProviderResult.Success)
        assertSame(snapshot, (result as ProviderResult.Success).value)
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
        val productIsin = " De000Test001 "
        val provider = FakeProvider(ProviderResult.NotFound)

        find(provider, productIsin)

        assertEquals(productIsin, provider.receivedProductIsin)
    }

    @Test
    fun interfaceExposesExactlyOnePublicSuspendFunction() {
        val methods = publicMethods()

        assertEquals(1, methods.size)
        assertEquals(Continuation::class.java, methods.single().parameterTypes.last())
    }

    @Test
    fun suspendFunctionIsNamedFindByProductIsin() {
        val method = publicMethods().single()

        assertEquals("findByProductIsin", method.name)
        assertEquals(String::class.java, method.parameterTypes.first())
    }

    @Test
    fun publicContractHasNoMapperDomainOrAndroidDependency() {
        assertTrue(
            apiTypeNames(KnockoutProductSpecificationSnapshotProvider::class.java).none {
                it.contains(".mapper.") ||
                    it.contains(".domain.") ||
                    it.startsWith("android.") ||
                    it.startsWith("androidx.")
            }
        )
    }

    @Test
    fun successSnapshotFieldsRemainExactlyUnchanged() {
        val specification = specification()
        val snapshot = KnockoutProductSpecificationSnapshotDto(
            specification = specification,
            sourceId = " Source-Id ",
            retrievedAtEpochMillis = -1L,
            sourceTimestampEpochMillis = 0L
        )
        val result = find(FakeProvider(ProviderResult.Success(snapshot)), PRODUCT_ISIN)
        val transported = (result as ProviderResult.Success).value

        assertSame(snapshot, transported)
        assertSame(specification, transported.specification)
        assertEquals(" Source-Id ", transported.sourceId)
        assertEquals(-1L, transported.retrievedAtEpochMillis)
        assertEquals(0L, transported.sourceTimestampEpochMillis)
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
        productIsin: String
    ): ProviderResult<KnockoutProductSpecificationSnapshotDto> = runSuspend {
        provider.findByProductIsin(productIsin)
    }

    private fun publicMethods() =
        KnockoutProductSpecificationSnapshotProvider::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }

    private fun snapshot() = KnockoutProductSpecificationSnapshotDto(
        specification = specification(),
        sourceId = "test-source",
        retrievedAtEpochMillis = 100L,
        sourceTimestampEpochMillis = null
    )

    private fun specification() = KnockoutProductSpecificationDto(
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
