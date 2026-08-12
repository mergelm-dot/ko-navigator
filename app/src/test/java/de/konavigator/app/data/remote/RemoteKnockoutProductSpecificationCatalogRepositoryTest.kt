package de.konavigator.app.data.remote

import de.konavigator.app.application.productdiscovery.KnockoutProductSpecificationCatalogQuery
import de.konavigator.app.application.productdiscovery.KnockoutProductSpecificationCatalogResult
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.data.remote.provider.KnockoutProductSpecificationCatalogProvider
import de.konavigator.app.data.remote.provider.KnockoutProductSpecificationCatalogProviderResult
import de.konavigator.app.domain.model.TradeDirection
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKnockoutProductSpecificationCatalogRepositoryTest {

    @Test
    fun queryUnderlyingIdAndDirectionReachProviderExactlyUnchanged() {
        val provider = RecordingProvider(
            KnockoutProductSpecificationCatalogProviderResult.Success(emptyList())
        )
        val query = KnockoutProductSpecificationCatalogQuery(
            underlyingId = " underlying-ID CaseSensitive ",
            direction = TradeDirection.SHORT
        )

        find(provider, query)

        assertEquals(" underlying-ID CaseSensitive ", provider.receivedUnderlyingId)
        assertEquals(TradeDirection.SHORT, provider.receivedDirection)
    }

    @Test
    fun successfulCandidatesMapInProviderOrderWithEvidenceAndDistinctBasePriceAndBarrier() {
        val first = snapshotDto(
            productIsin = "DE000FIRST01",
            productWkn = "FIRST1",
            basePrice = 88.0,
            knockoutBarrier = 91.25,
            sourceId = " First Source ",
            retrievedAtEpochMillis = 12_345L,
            sourceTimestampEpochMillis = null
        )
        val second = snapshotDto(
            productIsin = "DE000SECOND1",
            productWkn = "SECOND",
            basePrice = 100.0,
            knockoutBarrier = 107.0,
            sourceId = "Second Source",
            retrievedAtEpochMillis = 54_321L,
            sourceTimestampEpochMillis = 54_000L
        )

        val result = success(
            find(
                RecordingProvider(
                    KnockoutProductSpecificationCatalogProviderResult.Success(listOf(first, second))
                )
            )
        )

        assertEquals(listOf("DE000FIRST01", "DE000SECOND1"), result.candidates.map {
            it.specification.productIsin
        })
        assertEquals("FIRST1", result.candidates.first().specification.productWkn)
        assertEquals(88.0, result.candidates.first().specification.basePrice, 0.0)
        assertEquals(91.25, result.candidates.first().specification.knockoutBarrier, 0.0)
        assertEquals(" First Source ", result.candidates.first().sourceId)
        assertEquals(12_345L, result.candidates.first().retrievedAtEpochMillis)
        assertNull(result.candidates.first().sourceTimestampEpochMillis)
        assertEquals(54_000L, result.candidates.last().sourceTimestampEpochMillis)
    }

    @Test
    fun duplicateProviderCandidatesRemainDuplicatesInTheSameOrder() {
        val first = snapshotDto(productIsin = "DE000FIRST01")
        val second = snapshotDto(productIsin = "DE000SECOND1")

        val result = success(
            find(
                RecordingProvider(
                    KnockoutProductSpecificationCatalogProviderResult.Success(
                        listOf(first, second, first)
                    )
                )
            )
        )

        assertEquals(
            listOf("DE000FIRST01", "DE000SECOND1", "DE000FIRST01"),
            result.candidates.map { it.specification.productIsin }
        )
    }

    @Test
    fun emptyProviderSuccessRemainsEmptyCatalogSuccess() {
        val result = find(
            RecordingProvider(
                KnockoutProductSpecificationCatalogProviderResult.Success(emptyList())
            )
        )

        assertTrue(result is KnockoutProductSpecificationCatalogResult.Success)
        assertTrue((result as KnockoutProductSpecificationCatalogResult.Success).candidates.isEmpty())
    }

    @Test
    fun providerDataAccessFailureRemainsCatalogDataAccessFailure() {
        assertSame(
            KnockoutProductSpecificationCatalogResult.DataAccessFailure,
            find(
                RecordingProvider(
                    KnockoutProductSpecificationCatalogProviderResult.DataAccessFailure
                )
            )
        )
    }

    @Test
    fun providerInvalidDataRemainsCatalogInvalidData() {
        assertSame(
            KnockoutProductSpecificationCatalogResult.InvalidData,
            find(
                RecordingProvider(
                    KnockoutProductSpecificationCatalogProviderResult.InvalidData
                )
            )
        )
    }

    @Test
    fun oneMappingFailureMakesTheEntireCatalogInvalidData() {
        val valid = snapshotDto(productIsin = "DE000VALID01")
        val invalid = snapshotDto(productIsin = null)

        assertSame(
            KnockoutProductSpecificationCatalogResult.InvalidData,
            find(
                RecordingProvider(
                    KnockoutProductSpecificationCatalogProviderResult.Success(
                        listOf(valid, invalid, valid)
                    )
                )
            )
        )
    }

    private class RecordingProvider(
        private val result: KnockoutProductSpecificationCatalogProviderResult
    ) : KnockoutProductSpecificationCatalogProvider {

        var receivedUnderlyingId: String? = null
            private set

        var receivedDirection: TradeDirection? = null
            private set

        override suspend fun findCandidates(
            underlyingId: String,
            direction: TradeDirection
        ): KnockoutProductSpecificationCatalogProviderResult {
            receivedUnderlyingId = underlyingId
            receivedDirection = direction
            return result
        }
    }

    private fun find(
        provider: KnockoutProductSpecificationCatalogProvider,
        query: KnockoutProductSpecificationCatalogQuery =
            KnockoutProductSpecificationCatalogQuery(
                underlyingId = "underlying-a",
                direction = TradeDirection.LONG
            )
    ): KnockoutProductSpecificationCatalogResult = runSuspend {
        RemoteKnockoutProductSpecificationCatalogRepository(provider).findCandidates(query)
    }

    private fun success(
        result: KnockoutProductSpecificationCatalogResult
    ) = result as KnockoutProductSpecificationCatalogResult.Success

    private fun snapshotDto(
        productIsin: String? = "DE000TEST001",
        productWkn: String? = "ABC123",
        issuerId: String? = "issuer-a",
        underlyingId: String? = "underlying-a",
        direction: String? = "LONG",
        basePrice: Double? = 80.0,
        knockoutBarrier: Double? = 82.0,
        ratio: Double? = 0.1,
        underlyingCurrency: String? = "EUR",
        productCurrency: String? = "EUR",
        sourceId: String = "synthetic-catalog-source",
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
}
