package de.konavigator.app.application.repository

import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductSpecificationSnapshotRepositoryTest {

    @Test
    fun contractIsInterface() {
        assertTrue(KnockoutProductSpecificationSnapshotRepository::class.java.isInterface)
    }

    @Test
    fun contractHasExactlyOnePublicFunction() {
        assertEquals(1, publicMethods().size)
    }

    @Test
    fun functionIsNamedFindByProductIsin() {
        assertEquals("findByProductIsin", publicMethods().single().name)
    }

    @Test
    fun functionIsSuspendAndAcceptsExactlyOneStringValue() {
        val parameterTypes = publicMethods().single().parameterTypes

        assertEquals(listOf(String::class.java), parameterTypes.dropLast(1))
        assertEquals(Continuation::class.java, parameterTypes.last())
    }

    @Test
    fun successTransportsExactSnapshotInstance() {
        val snapshot = snapshot()
        val result = find(FakeRepository(RepositoryResult.Success(snapshot)), PRODUCT_ISIN)

        assertSame(snapshot, (result as RepositoryResult.Success).value)
    }

    @Test
    fun notFoundRemainsAvailable() {
        assertSame(
            RepositoryResult.NotFound,
            find(FakeRepository(RepositoryResult.NotFound), PRODUCT_ISIN)
        )
    }

    @Test
    fun dataAccessFailureRemainsAvailable() {
        assertSame(
            RepositoryResult.DataAccessFailure,
            find(FakeRepository(RepositoryResult.DataAccessFailure), PRODUCT_ISIN)
        )
    }

    @Test
    fun invalidDataRemainsAvailable() {
        assertSame(
            RepositoryResult.InvalidData,
            find(FakeRepository(RepositoryResult.InvalidData), PRODUCT_ISIN)
        )
    }

    @Test
    fun productIsinReachesImplementationExactlyUnchanged() {
        val productIsin = " De000Test001 "
        val repository = FakeRepository(RepositoryResult.NotFound)

        find(repository, productIsin)

        assertEquals(productIsin, repository.receivedProductIsin)
    }

    @Test
    fun emptyProductIsinIsNotValidatedByContract() {
        val repository = FakeRepository(RepositoryResult.NotFound)

        val result = find(repository, "")

        assertSame(RepositoryResult.NotFound, result)
        assertEquals("", repository.receivedProductIsin)
    }

    @Test
    fun publicApiHasNoDataRemoteProviderDtoAndroidComposePresentationOrNetworkDependency() {
        val forbiddenFragments = listOf(
            ".data.",
            ".remote.",
            ".provider.",
            "dto",
            "android.",
            "androidx.",
            "compose",
            "presentation",
            "retrofit",
            "okhttp",
            "network"
        )

        assertTrue(
            apiTypeNames().none { typeName ->
                forbiddenFragments.any { typeName.contains(it, ignoreCase = true) }
            }
        )
    }

    @Test
    fun functionHasNoDefaultImplementation() {
        assertTrue(Modifier.isAbstract(publicMethods().single().modifiers))
    }

    private class FakeRepository(
        private val result: RepositoryResult<KnockoutProductSpecificationSnapshot>
    ) : KnockoutProductSpecificationSnapshotRepository {

        var receivedProductIsin: String? = null
            private set

        override suspend fun findByProductIsin(
            productIsin: String
        ): RepositoryResult<KnockoutProductSpecificationSnapshot> {
            receivedProductIsin = productIsin
            return result
        }
    }

    private fun find(
        repository: KnockoutProductSpecificationSnapshotRepository,
        productIsin: String
    ): RepositoryResult<KnockoutProductSpecificationSnapshot> = runSuspend {
        repository.findByProductIsin(productIsin)
    }

    private fun publicMethods() =
        KnockoutProductSpecificationSnapshotRepository::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }

    private fun apiTypeNames(): Set<String> = buildSet {
        val type = KnockoutProductSpecificationSnapshotRepository::class.java
        add(type.name)
        type.declaredFields.forEach { add(it.type.name) }
        publicMethods().forEach { method ->
            add(method.returnType.name)
            method.parameterTypes.forEach { add(it.name) }
            method.genericParameterTypes.forEach { add(it.typeName) }
            add(method.genericReturnType.typeName)
        }
    }

    private fun snapshot() = KnockoutProductSpecificationSnapshot(
        specification = KnockoutProductSpecification(
            productIsin = PRODUCT_ISIN,
            productWkn = "ABC123",
            issuerId = "issuer-a",
            underlyingId = "underlying-a",
            direction = TradeDirection.LONG,
            basePrice = 80.0,
            knockoutBarrier = 82.0,
            ratio = 0.1,
            underlyingCurrency = "EUR",
            productCurrency = "EUR"
        ),
        sourceId = "test-source",
        retrievedAtEpochMillis = 100L,
        sourceTimestampEpochMillis = null
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
