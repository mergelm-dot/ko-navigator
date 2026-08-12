package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
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

class HsbcResearchKnockoutProductSpecificationCatalogProviderTest {

    @Test
    fun filtersOnlyByExactUnderlyingIdAndMappedDirectionInInputOrder() {
        val longA1 = snapshot("DE000A00001", "Underlying-A", "LONG")
        val shortA = snapshot("DE000A00002", "Underlying-A", "SHORT")
        val longA2 = snapshot("DE000A00003", "Underlying-A", "LONG")
        val longB = snapshot("DE000B00001", "Underlying-B", "LONG")
        val provider = provider(listOf(longA1, shortA, longA2, longB))

        assertEquals(
            listOf("DE000A00001", "DE000A00003"),
            candidates(provider, "Underlying-A", TradeDirection.LONG).map(::productIsin)
        )
        assertEquals(
            listOf("DE000A00002"),
            candidates(provider, "Underlying-A", TradeDirection.SHORT).map(::productIsin)
        )
        assertEquals(
            listOf("DE000B00001"),
            candidates(provider, "Underlying-B", TradeDirection.LONG).map(::productIsin)
        )
    }

    @Test
    fun underlyingIdMatchingIsCaseAndWhitespaceSensitive() {
        val provider = provider(listOf(snapshot("DE000A00001", "Underlying-A", "LONG")))

        assertTrue(candidates(provider, "underlying-a", TradeDirection.LONG).isEmpty())
        assertTrue(candidates(provider, " Underlying-A ", TradeDirection.LONG).isEmpty())
        assertEquals(1, candidates(provider, "Underlying-A", TradeDirection.LONG).size)
    }

    @Test
    fun noMatchIsSuccessfulEmptyList() {
        val result = find(
            provider(listOf(snapshot("DE000A00001", "Underlying-A", "LONG"))),
            "Missing-Underlying",
            TradeDirection.SHORT
        )

        assertTrue(result is KnockoutProductSpecificationCatalogProviderResult.Success)
        assertTrue(
            (result as KnockoutProductSpecificationCatalogProviderResult.Success)
                .candidates.isEmpty()
        )
    }

    @Test
    fun duplicatesAndAllSnapshotFieldsRemainUnchanged() {
        val duplicate = snapshot(
            productIsin = "DE000A00001",
            underlyingId = "Underlying-A",
            direction = "LONG",
            basePrice = 88.0,
            knockoutBarrier = 91.25,
            sourceTimestampEpochMillis = null
        )
        val provider = provider(listOf(duplicate, duplicate))

        val candidates = candidates(provider, "Underlying-A", TradeDirection.LONG)

        assertEquals(2, candidates.size)
        assertSame(duplicate, candidates[0])
        assertSame(duplicate, candidates[1])
        val snapshot = candidates.first()
        assertEquals("DE000A00001", snapshot.specification.productIsin)
        assertEquals("SYN001", snapshot.specification.productWkn)
        assertEquals("synthetic-hsbc", snapshot.specification.issuerId)
        assertEquals("Underlying-A", snapshot.specification.underlyingId)
        assertEquals("LONG", snapshot.specification.direction)
        assertEquals(88.0, snapshot.specification.basePrice)
        assertEquals(91.25, snapshot.specification.knockoutBarrier)
        assertEquals(0.1, snapshot.specification.ratio)
        assertEquals("USD", snapshot.specification.underlyingCurrency)
        assertEquals("EUR", snapshot.specification.productCurrency)
        assertEquals("HSBC_RESEARCH_LOCAL", snapshot.sourceId)
        assertEquals(RETRIEVED_AT, snapshot.retrievedAtEpochMillis)
        assertNull(snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun constructorDefensivelyCopiesInputList() {
        val original = snapshot("DE000A00001", "Underlying-A", "LONG")
        val input = mutableListOf(original)
        val provider = provider(input)

        input.clear()
        input += snapshot("DE000B00001", "Underlying-B", "LONG")

        assertEquals(
            listOf("DE000A00001"),
            candidates(provider, "Underlying-A", TradeDirection.LONG).map(::productIsin)
        )
        assertTrue(candidates(provider, "Underlying-B", TradeDirection.LONG).isEmpty())
    }

    private fun provider(snapshots: List<KnockoutProductSpecificationSnapshotDto>) =
        HsbcResearchKnockoutProductSpecificationCatalogProvider(snapshots)

    private fun candidates(
        provider: HsbcResearchKnockoutProductSpecificationCatalogProvider,
        underlyingId: String,
        direction: TradeDirection
    ) = (find(provider, underlyingId, direction) as
        KnockoutProductSpecificationCatalogProviderResult.Success).candidates

    private fun find(
        provider: HsbcResearchKnockoutProductSpecificationCatalogProvider,
        underlyingId: String,
        direction: TradeDirection
    ): KnockoutProductSpecificationCatalogProviderResult = runSuspend {
        provider.findCandidates(underlyingId, direction)
    }

    private fun productIsin(snapshot: KnockoutProductSpecificationSnapshotDto) =
        snapshot.specification.productIsin

    private fun snapshot(
        productIsin: String,
        underlyingId: String,
        direction: String,
        basePrice: Double = 80.0,
        knockoutBarrier: Double = 82.0,
        sourceTimestampEpochMillis: Long? = SOURCE_TIMESTAMP
    ) = KnockoutProductSpecificationSnapshotDto(
        specification = KnockoutProductSpecificationDto(
            productIsin = productIsin,
            productWkn = "SYN001",
            issuerId = "synthetic-hsbc",
            underlyingId = underlyingId,
            direction = direction,
            basePrice = basePrice,
            knockoutBarrier = knockoutBarrier,
            ratio = 0.1,
            underlyingCurrency = "USD",
            productCurrency = "EUR"
        ),
        sourceId = "HSBC_RESEARCH_LOCAL",
        retrievedAtEpochMillis = RETRIEVED_AT,
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
        return (completed ?: error("Suspend provider call did not complete synchronously"))
            .getOrThrow()
    }

    private companion object {
        const val RETRIEVED_AT = 1_700_000_000_500L
        const val SOURCE_TIMESTAMP = 1_700_000_000_250L
    }
}
