package de.konavigator.app.application.productdiscovery

import de.konavigator.app.application.repository.KnockoutProductSpecificationCatalogRepository
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductSpecificationCatalogContractsTest {

    @Test
    fun queryPreservesUnderlyingIdExactly() {
        val query = KnockoutProductSpecificationCatalogQuery(
            underlyingId = " Synthetic-Underlying ",
            direction = TradeDirection.LONG
        )

        assertEquals(" Synthetic-Underlying ", query.underlyingId)
    }

    @Test
    fun queryPreservesLongDirection() {
        val query = KnockoutProductSpecificationCatalogQuery(
            underlyingId = "synthetic-underlying",
            direction = TradeDirection.LONG
        )

        assertEquals(TradeDirection.LONG, query.direction)
    }

    @Test
    fun queryPreservesShortDirection() {
        val query = KnockoutProductSpecificationCatalogQuery(
            underlyingId = "synthetic-underlying",
            direction = TradeDirection.SHORT
        )

        assertEquals(TradeDirection.SHORT, query.direction)
    }

    @Test
    fun successPreservesCandidateOrderAndSnapshotMetadata() {
        val candidate1 = syntheticSnapshot(
            productIsin = "DE000SYNTH01",
            productWkn = "SYN001",
            issuerId = "synthetic-issuer-a",
            basePrice = 80.0,
            knockoutBarrier = 82.0,
            sourceId = "synthetic-source-a",
            retrievedAtEpochMillis = 1_700_000_000_500L,
            sourceTimestampEpochMillis = 1_700_000_000_250L
        )
        val candidate2 = syntheticSnapshot(
            productIsin = "DE000SYNTH02",
            productWkn = "SYN002",
            issuerId = "synthetic-issuer-b",
            basePrice = 81.0,
            knockoutBarrier = 83.0,
            sourceId = "synthetic-source-b",
            retrievedAtEpochMillis = 1_700_000_000_600L,
            sourceTimestampEpochMillis = null
        )
        val candidates = listOf(candidate1, candidate2)

        val result = KnockoutProductSpecificationCatalogResult.Success(candidates)

        assertSame(candidates, result.candidates)
        assertEquals(listOf(candidate1, candidate2), result.candidates)
        assertSame(candidate1, result.candidates[0])
        assertSame(candidate2, result.candidates[1])
        assertSnapshotMetadata(
            snapshot = result.candidates[0],
            sourceId = "synthetic-source-a",
            retrievedAtEpochMillis = 1_700_000_000_500L,
            sourceTimestampEpochMillis = 1_700_000_000_250L
        )
        assertSnapshotMetadata(
            snapshot = result.candidates[1],
            sourceId = "synthetic-source-b",
            retrievedAtEpochMillis = 1_700_000_000_600L,
            sourceTimestampEpochMillis = null
        )
    }

    @Test
    fun successPreservesDuplicateEntries() {
        val candidate = syntheticSnapshot()

        val result = KnockoutProductSpecificationCatalogResult.Success(
            candidates = listOf(candidate, candidate)
        )

        assertEquals(2, result.candidates.size)
        assertSame(candidate, result.candidates[0])
        assertSame(candidate, result.candidates[1])
    }

    @Test
    fun emptySuccessRepresentsSuccessfulSearchWithoutCandidates() {
        val result: KnockoutProductSpecificationCatalogResult =
            KnockoutProductSpecificationCatalogResult.Success(candidates = emptyList())

        assertFalse(result is KnockoutProductSpecificationCatalogResult.DataAccessFailure)
        assertFalse(result is KnockoutProductSpecificationCatalogResult.InvalidData)
        assertTrue(result is KnockoutProductSpecificationCatalogResult.Success)
        assertTrue(
            (result as KnockoutProductSpecificationCatalogResult.Success).candidates.isEmpty()
        )
    }

    @Test
    fun dataAccessFailureIsDistinctFromEmptySuccess() {
        val emptySuccess: KnockoutProductSpecificationCatalogResult =
            KnockoutProductSpecificationCatalogResult.Success(candidates = emptyList())
        val failure: KnockoutProductSpecificationCatalogResult =
            KnockoutProductSpecificationCatalogResult.DataAccessFailure

        assertNotEquals(emptySuccess, failure)
        assertFalse(emptySuccess === failure)
    }

    @Test
    fun invalidDataIsDistinctFromEmptySuccessAndDataAccessFailure() {
        val emptySuccess: KnockoutProductSpecificationCatalogResult =
            KnockoutProductSpecificationCatalogResult.Success(candidates = emptyList())
        val dataAccessFailure: KnockoutProductSpecificationCatalogResult =
            KnockoutProductSpecificationCatalogResult.DataAccessFailure
        val invalidData: KnockoutProductSpecificationCatalogResult =
            KnockoutProductSpecificationCatalogResult.InvalidData

        assertNotEquals(emptySuccess, dataAccessFailure)
        assertNotEquals(emptySuccess, invalidData)
        assertNotEquals(dataAccessFailure, invalidData)
    }

    @Test
    fun repositoryReceivesExactQueryAndReturnsExactResult() = runTest {
        val query = KnockoutProductSpecificationCatalogQuery(
            underlyingId = " Synthetic-Underlying ",
            direction = TradeDirection.SHORT
        )
        val expectedResult: KnockoutProductSpecificationCatalogResult =
            KnockoutProductSpecificationCatalogResult.Success(
                candidates = listOf(syntheticSnapshot(direction = TradeDirection.SHORT))
            )
        val repository = FakeCatalogRepository(expectedResult)

        val actualResult = repository.findCandidates(query)

        assertSame(query, repository.receivedQuery)
        assertEquals(" Synthetic-Underlying ", repository.receivedQuery?.underlyingId)
        assertEquals(TradeDirection.SHORT, repository.receivedQuery?.direction)
        assertSame(expectedResult, actualResult)
    }

    @Test
    fun resultContainsNoBrokerOrRankingDecision() {
        val query = KnockoutProductSpecificationCatalogQuery(
            underlyingId = "synthetic-underlying",
            direction = TradeDirection.LONG
        )
        val candidates = listOf(syntheticSnapshot())
        val result = KnockoutProductSpecificationCatalogResult.Success(candidates)

        assertEquals("synthetic-underlying", query.underlyingId)
        assertEquals(TradeDirection.LONG, query.direction)
        assertSame(candidates, result.candidates)
    }

    private fun syntheticSnapshot(
        productIsin: String = "DE000SYNTH01",
        productWkn: String? = "SYN001",
        issuerId: String = "synthetic-issuer-a",
        direction: TradeDirection = TradeDirection.LONG,
        basePrice: Double = 80.0,
        knockoutBarrier: Double = 82.0,
        sourceId: String = "synthetic-source-a",
        retrievedAtEpochMillis: Long = 1_700_000_000_500L,
        sourceTimestampEpochMillis: Long? = 1_700_000_000_250L
    ) = KnockoutProductSpecificationSnapshot(
        specification = KnockoutProductSpecification(
            productIsin = productIsin,
            productWkn = productWkn,
            issuerId = issuerId,
            underlyingId = "synthetic-underlying",
            direction = direction,
            basePrice = basePrice,
            knockoutBarrier = knockoutBarrier,
            ratio = 0.1,
            underlyingCurrency = "USD",
            productCurrency = "EUR"
        ),
        sourceId = sourceId,
        retrievedAtEpochMillis = retrievedAtEpochMillis,
        sourceTimestampEpochMillis = sourceTimestampEpochMillis
    )

    private fun assertSnapshotMetadata(
        snapshot: KnockoutProductSpecificationSnapshot,
        sourceId: String,
        retrievedAtEpochMillis: Long,
        sourceTimestampEpochMillis: Long?
    ) {
        assertEquals(sourceId, snapshot.sourceId)
        assertEquals(retrievedAtEpochMillis, snapshot.retrievedAtEpochMillis)
        assertEquals(sourceTimestampEpochMillis, snapshot.sourceTimestampEpochMillis)
    }

    private class FakeCatalogRepository(
        private val result: KnockoutProductSpecificationCatalogResult
    ) : KnockoutProductSpecificationCatalogRepository {

        var receivedQuery: KnockoutProductSpecificationCatalogQuery? = null
            private set

        override suspend fun findCandidates(
            query: KnockoutProductSpecificationCatalogQuery
        ): KnockoutProductSpecificationCatalogResult {
            receivedQuery = query
            return result
        }
    }
}
