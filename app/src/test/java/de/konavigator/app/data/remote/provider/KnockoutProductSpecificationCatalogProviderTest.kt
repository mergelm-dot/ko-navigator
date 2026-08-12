package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.domain.model.TradeDirection
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductSpecificationCatalogProviderTest {

    @Test
    fun searchTransportsUnderlyingIdAndDirectionExactly() {
        val provider = RecordingProvider(
            KnockoutProductSpecificationCatalogProviderResult.Success(emptyList())
        )

        find(provider, " underlying-ID CaseSensitive ", TradeDirection.SHORT)

        assertEquals(" underlying-ID CaseSensitive ", provider.receivedUnderlyingId)
        assertEquals(TradeDirection.SHORT, provider.receivedDirection)
    }

    @Test
    fun successTransportsExactSnapshotDtosIncludingEmptyLists() {
        val dto = snapshotDto()
        val candidates = listOf(dto)

        val success = KnockoutProductSpecificationCatalogProviderResult.Success(candidates)
        val emptySuccess = KnockoutProductSpecificationCatalogProviderResult.Success(emptyList())

        assertSame(candidates, success.candidates)
        assertSame(dto, success.candidates.single())
        assertTrue(emptySuccess.candidates.isEmpty())
    }

    @Test
    fun dataAccessFailureAndInvalidDataRemainDistinctStates() {
        assertNotEquals(
            KnockoutProductSpecificationCatalogProviderResult.DataAccessFailure::class,
            KnockoutProductSpecificationCatalogProviderResult.InvalidData::class
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
        underlyingId: String,
        direction: TradeDirection
    ): KnockoutProductSpecificationCatalogProviderResult = runSuspend {
        provider.findCandidates(underlyingId, direction)
    }

    private fun snapshotDto() = KnockoutProductSpecificationSnapshotDto(
        specification = KnockoutProductSpecificationDto(
            productIsin = "DE000TEST001",
            productWkn = "ABC123",
            issuerId = "issuer-a",
            underlyingId = "underlying-a",
            direction = "LONG",
            basePrice = 88.0,
            knockoutBarrier = 91.25,
            ratio = 0.1,
            underlyingCurrency = "EUR",
            productCurrency = "EUR"
        ),
        sourceId = "synthetic-catalog-source",
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
        return (completed ?: error("Suspend provider call did not complete synchronously"))
            .getOrThrow()
    }
}
