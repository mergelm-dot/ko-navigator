package de.konavigator.app.application.repository.adapter

import de.konavigator.app.application.repository.KnockoutProductSpecificationSnapshotRepository
import de.konavigator.app.application.repository.RepositoryResult
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

class SnapshotBackedKnockoutProductSpecificationRepositoryTest {

    @Test
    fun successfulSnapshotBecomesSpecificationSuccess() {
        val result = find(FakeSnapshotRepository(RepositoryResult.Success(snapshot())))

        assertTrue(result is RepositoryResult.Success)
    }

    @Test
    fun successReturnsExactContainedSpecificationInstance() {
        val snapshot = snapshot()

        val specification = successValue(
            find(FakeSnapshotRepository(RepositoryResult.Success(snapshot)))
        )

        assertSame(snapshot.specification, specification)
    }

    @Test
    fun adapterDoesNotChangeSnapshotSourceOrTimes() {
        val snapshot = snapshot(
            sourceId = " Source-Id ",
            retrievedAtEpochMillis = -1L,
            sourceTimestampEpochMillis = null
        )
        val before = snapshot.copy()

        find(FakeSnapshotRepository(RepositoryResult.Success(snapshot)))

        assertEquals(before, snapshot)
        assertEquals(" Source-Id ", snapshot.sourceId)
        assertEquals(-1L, snapshot.retrievedAtEpochMillis)
        assertEquals(null, snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun notFoundRemainsExactState() {
        assertSame(
            RepositoryResult.NotFound,
            find(FakeSnapshotRepository(RepositoryResult.NotFound))
        )
    }

    @Test
    fun dataAccessFailureRemainsExactState() {
        assertSame(
            RepositoryResult.DataAccessFailure,
            find(FakeSnapshotRepository(RepositoryResult.DataAccessFailure))
        )
    }

    @Test
    fun invalidDataRemainsExactState() {
        assertSame(
            RepositoryResult.InvalidData,
            find(FakeSnapshotRepository(RepositoryResult.InvalidData))
        )
    }

    @Test
    fun productIsinReachesSnapshotRepositoryExactlyUnchanged() {
        val productIsin = " De000Test001 "
        val repository = FakeSnapshotRepository(RepositoryResult.NotFound)

        find(repository, productIsin)

        assertEquals(productIsin, repository.receivedProductIsin)
    }

    @Test
    fun emptyProductIsinIsForwardedUnchanged() {
        val repository = FakeSnapshotRepository(RepositoryResult.NotFound)

        find(repository, "")

        assertEquals("", repository.receivedProductIsin)
    }

    @Test
    fun snapshotRepositoryIsCalledExactlyOncePerAdapterCall() {
        val repository = FakeSnapshotRepository(RepositoryResult.Success(snapshot()))

        find(repository)

        assertEquals(1, repository.invocationCount)
    }

    @Test
    fun repeatedCallsReturnSameContainedSpecificationInstance() {
        val snapshot = snapshot()
        val repository = FakeSnapshotRepository(RepositoryResult.Success(snapshot))

        val first = successValue(find(repository))
        val second = successValue(find(repository))

        assertSame(snapshot.specification, first)
        assertSame(first, second)
    }

    @Test
    fun publicAdapterApiHasNoDataRemoteProviderDtoAndroidComposeOrNetworkDependency() {
        val forbiddenFragments = listOf(
            ".data.",
            ".remote.",
            ".provider.",
            "dto",
            "android.",
            "androidx.",
            "compose",
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

    private class FakeSnapshotRepository(
        private val result: RepositoryResult<KnockoutProductSpecificationSnapshot>
    ) : KnockoutProductSpecificationSnapshotRepository {

        var receivedProductIsin: String? = null
            private set

        var invocationCount: Int = 0
            private set

        override suspend fun findByProductIsin(
            productIsin: String
        ): RepositoryResult<KnockoutProductSpecificationSnapshot> {
            invocationCount += 1
            receivedProductIsin = productIsin
            return result
        }
    }

    private fun find(
        repository: KnockoutProductSpecificationSnapshotRepository,
        productIsin: String = PRODUCT_ISIN
    ): RepositoryResult<KnockoutProductSpecification> = runSuspend {
        SnapshotBackedKnockoutProductSpecificationRepository(repository)
            .findByProductIsin(productIsin)
    }

    private fun successValue(
        result: RepositoryResult<KnockoutProductSpecification>
    ) = (result as RepositoryResult.Success).value

    private fun snapshot(
        sourceId: String = "test-source",
        retrievedAtEpochMillis: Long = 100L,
        sourceTimestampEpochMillis: Long? = 90L
    ) = KnockoutProductSpecificationSnapshot(
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
        sourceId = sourceId,
        retrievedAtEpochMillis = retrievedAtEpochMillis,
        sourceTimestampEpochMillis = sourceTimestampEpochMillis
    )

    private fun apiTypeNames(): Set<String> = buildSet {
        val type = SnapshotBackedKnockoutProductSpecificationRepository::class.java
        add(type.name)
        type.declaredConstructors
            .filterNot { it.isSynthetic }
            .forEach { constructor ->
                constructor.parameterTypes.forEach { add(it.name) }
            }
        type.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }
            .forEach { method ->
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
        return (completed ?: error("Suspend repository call did not complete synchronously"))
            .getOrThrow()
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000TEST001"
    }
}
