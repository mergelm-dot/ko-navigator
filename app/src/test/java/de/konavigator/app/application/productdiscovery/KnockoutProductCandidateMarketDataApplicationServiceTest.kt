package de.konavigator.app.application.productdiscovery

import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateMarketDataApplicationServiceTest {

    @Test
    fun successfulMarketDataLoadingPreservesCandidateOrder() = runTest {
        val synth03 = snapshot(productIsin = "DE000SYNTH03")
        val synth01 = snapshot(productIsin = "DE000SYNTH01")
        val synth02 = snapshot(productIsin = "DE000SYNTH02")
        val market03 = marketData(productIsin = "DE000SYNTH03", bid = 3.0, ask = 3.1)
        val market01 = marketData(productIsin = "DE000SYNTH01", bid = 1.0, ask = 1.1)
        val market02 = marketData(productIsin = "DE000SYNTH02", bid = 2.0, ask = 2.1)
        val repository = repository(
            "DE000SYNTH03" to RepositoryResult.Success(market03),
            "DE000SYNTH01" to RepositoryResult.Success(market01),
            "DE000SYNTH02" to RepositoryResult.Success(market02)
        )

        val result = service(repository).execute(request(synth03, synth01, synth02))

        val candidates = candidatesWithMarketData(result)
        assertEquals(3, candidates.size)
        assertSame(synth03, candidates[0].specificationSnapshot)
        assertSame(market03, candidates[0].marketData)
        assertSame(synth01, candidates[1].specificationSnapshot)
        assertSame(market01, candidates[1].marketData)
        assertSame(synth02, candidates[2].specificationSnapshot)
        assertSame(market02, candidates[2].marketData)
        assertEquals(
            listOf("DE000SYNTH03", "DE000SYNTH01", "DE000SYNTH02"),
            repository.productIsinCalls
        )
    }

    @Test
    fun productIsinsAreForwardedExactly() = runTest {
        val productIsins = listOf(" DE000SYNTH01 ", "de000synth02", "DE000SYNTH03")
        val repository = FakeMarketDataRepository(
            responses = productIsins.associateWith { productIsin ->
                RepositoryResult.Success(marketData(productIsin = productIsin))
            }
        )

        service(repository).execute(
            KnockoutProductCandidateMarketDataRequest(
                candidates = productIsins.map { productIsin -> snapshot(productIsin = productIsin) }
            )
        )

        assertEquals(productIsins, repository.productIsinCalls)
    }

    @Test
    fun emptyInputReturnsNoInputCandidatesWithoutRepositoryCall() = runTest {
        val repository = repository()

        val result = service(repository).execute(
            KnockoutProductCandidateMarketDataRequest(candidates = emptyList())
        )

        assertSame(KnockoutProductCandidateMarketDataResult.NoInputCandidates, result)
        assertTrue(repository.productIsinCalls.isEmpty())
    }

    @Test
    fun marketDataNotFoundIsMappedWithExactProductIsin() = runTest {
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH01")
            ),
            " DE000SYNTH02 " to RepositoryResult.NotFound,
            "DE000SYNTH03" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH03")
            )
        )

        val result = service(repository).execute(
            request(
                snapshot(productIsin = "DE000SYNTH01"),
                snapshot(productIsin = " DE000SYNTH02 "),
                snapshot(productIsin = "DE000SYNTH03")
            )
        )

        assertEquals(
            KnockoutProductCandidateMarketDataResult.MarketDataNotFound(" DE000SYNTH02 "),
            result
        )
        assertEquals(listOf("DE000SYNTH01", " DE000SYNTH02 "), repository.productIsinCalls)
        assertFalse(result is KnockoutProductCandidateMarketDataResult.CandidatesWithMarketData)
    }

    @Test
    fun dataAccessFailureIsMappedWithExactProductIsin() = runTest {
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH01")
            ),
            "de000synth02" to RepositoryResult.DataAccessFailure,
            "DE000SYNTH03" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH03")
            )
        )

        val result = service(repository).execute(
            request(
                snapshot(productIsin = "DE000SYNTH01"),
                snapshot(productIsin = "de000synth02"),
                snapshot(productIsin = "DE000SYNTH03")
            )
        )

        assertEquals(
            KnockoutProductCandidateMarketDataResult
                .MarketDataDataAccessFailure("de000synth02"),
            result
        )
        assertEquals(listOf("DE000SYNTH01", "de000synth02"), repository.productIsinCalls)
        assertFalse(result is KnockoutProductCandidateMarketDataResult.CandidatesWithMarketData)
    }

    @Test
    fun invalidDataIsMappedWithExactProductIsin() = runTest {
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH01")
            ),
            "DE000SYNTH02" to RepositoryResult.InvalidData,
            "DE000SYNTH03" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH03")
            )
        )

        val result = service(repository).execute(
            request(
                snapshot(productIsin = "DE000SYNTH01"),
                snapshot(productIsin = "DE000SYNTH02"),
                snapshot(productIsin = "DE000SYNTH03")
            )
        )

        assertEquals(
            KnockoutProductCandidateMarketDataResult.MarketDataInvalidData("DE000SYNTH02"),
            result
        )
        assertEquals(listOf("DE000SYNTH01", "DE000SYNTH02"), repository.productIsinCalls)
        assertFalse(result is KnockoutProductCandidateMarketDataResult.CandidatesWithMarketData)
    }

    @Test
    fun failureStatesRemainDistinct() {
        val notFound: KnockoutProductCandidateMarketDataResult =
            KnockoutProductCandidateMarketDataResult.MarketDataNotFound("DE000SYNTH01")
        val dataAccessFailure: KnockoutProductCandidateMarketDataResult =
            KnockoutProductCandidateMarketDataResult
                .MarketDataDataAccessFailure("DE000SYNTH01")
        val invalidData: KnockoutProductCandidateMarketDataResult =
            KnockoutProductCandidateMarketDataResult.MarketDataInvalidData("DE000SYNTH01")

        assertFalse(notFound == dataAccessFailure)
        assertFalse(notFound == invalidData)
        assertFalse(dataAccessFailure == invalidData)
    }

    @Test
    fun duplicateProductIsinIsLoadedOnlyOnce() = runTest {
        val first = snapshot(productIsin = "DE000SYNTH01", issuerId = "issuer-a")
        val second = snapshot(productIsin = "DE000SYNTH01", issuerId = "issuer-b")
        val loaded = marketData(productIsin = "DE000SYNTH01")
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(loaded)
        )

        val candidates = candidatesWithMarketData(
            service(repository).execute(request(first, second))
        )

        assertEquals(listOf("DE000SYNTH01"), repository.productIsinCalls)
        assertEquals(2, candidates.size)
        assertSame(first, candidates[0].specificationSnapshot)
        assertSame(second, candidates[1].specificationSnapshot)
        assertSame(loaded, candidates[0].marketData)
        assertSame(loaded, candidates[1].marketData)
    }

    @Test
    fun nonAdjacentDuplicateProductIsinIsLoadedOnlyOnce() = runTest {
        val first = snapshot(productIsin = "DE000SYNTH01", issuerId = "issuer-a")
        val middle = snapshot(productIsin = "DE000SYNTH02", issuerId = "issuer-b")
        val last = snapshot(productIsin = "DE000SYNTH01", issuerId = "issuer-c")
        val market01 = marketData(productIsin = "DE000SYNTH01")
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(market01),
            "DE000SYNTH02" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH02")
            )
        )

        val candidates = candidatesWithMarketData(
            service(repository).execute(request(first, middle, last))
        )

        assertEquals(listOf("DE000SYNTH01", "DE000SYNTH02"), repository.productIsinCalls)
        assertEquals(3, candidates.size)
        assertSame(first, candidates[0].specificationSnapshot)
        assertSame(middle, candidates[1].specificationSnapshot)
        assertSame(last, candidates[2].specificationSnapshot)
        assertSame(market01, candidates[0].marketData)
        assertSame(market01, candidates[2].marketData)
    }

    @Test
    fun differentExactIsinValuesAreNotMerged() = runTest {
        val productIsins = listOf("DE000SYNTH01", "de000synth01", " DE000SYNTH01 ")
        val loaded = productIsins.mapIndexed { index, productIsin ->
            marketData(productIsin = productIsin, bid = index.toDouble())
        }
        val repository = FakeMarketDataRepository(
            responses = productIsins.indices.associate { index ->
                productIsins[index] to RepositoryResult.Success(loaded[index])
            }
        )

        val candidates = candidatesWithMarketData(
            service(repository).execute(
                KnockoutProductCandidateMarketDataRequest(
                    productIsins.map { productIsin -> snapshot(productIsin = productIsin) }
                )
            )
        )

        assertEquals(productIsins, repository.productIsinCalls)
        assertEquals(3, candidates.size)
        assertSame(loaded[0], candidates[0].marketData)
        assertSame(loaded[1], candidates[1].marketData)
        assertSame(loaded[2], candidates[2].marketData)
    }

    @Test
    fun candidateDuplicatesRemainDuplicated() = runTest {
        val duplicate = snapshot(productIsin = "DE000SYNTH01")
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH01")
            )
        )

        val candidates = candidatesWithMarketData(
            service(repository).execute(request(duplicate, duplicate))
        )

        assertEquals(2, candidates.size)
        assertSame(duplicate, candidates[0].specificationSnapshot)
        assertSame(duplicate, candidates[1].specificationSnapshot)
        assertEquals(listOf("DE000SYNTH01"), repository.productIsinCalls)
    }

    @Test
    fun differentProductsFromSameIssuerRemainSeparate() = runTest {
        val first = snapshot(productIsin = "DE000SYNTH01", issuerId = "shared-issuer")
        val second = snapshot(productIsin = "DE000SYNTH02", issuerId = "shared-issuer")
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH01")
            ),
            "DE000SYNTH02" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH02")
            )
        )

        val candidates = candidatesWithMarketData(
            service(repository).execute(request(first, second))
        )

        assertEquals(2, candidates.size)
        assertSame(first, candidates[0].specificationSnapshot)
        assertSame(second, candidates[1].specificationSnapshot)
        assertEquals(listOf("DE000SYNTH01", "DE000SYNTH02"), repository.productIsinCalls)
    }

    @Test
    fun resultIsNotLimitedToThreeCandidates() = runTest {
        val candidates = (1..4).map { index -> snapshot(productIsin = "DE000SYNTH0$index") }
        val repository = FakeMarketDataRepository(
            responses = candidates.associate { candidate ->
                val productIsin = candidate.specification.productIsin
                productIsin to RepositoryResult.Success(marketData(productIsin = productIsin))
            }
        )

        val result = candidatesWithMarketData(
            service(repository).execute(KnockoutProductCandidateMarketDataRequest(candidates))
        )

        assertEquals(4, result.size)
        assertEquals(4, repository.productIsinCalls.size)
        candidates.indices.forEach { index ->
            assertSame(candidates[index], result[index].specificationSnapshot)
        }
    }

    @Test
    fun snapshotAndSpecificationInstancesRemainUnchanged() = runTest {
        val specification = specification(productIsin = "DE000SYNTH01")
        val candidate = KnockoutProductSpecificationSnapshot(
            specification = specification,
            sourceId = "synthetic-source",
            retrievedAtEpochMillis = 1_700_000_000_500L,
            sourceTimestampEpochMillis = 1_700_000_000_250L
        )
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH01")
            )
        )

        val returned = candidatesWithMarketData(
            service(repository).execute(request(candidate))
        ).single().specificationSnapshot

        assertSame(candidate, returned)
        assertSame(specification, returned.specification)
        assertEquals("synthetic-source", returned.sourceId)
        assertEquals(1_700_000_000_500L, returned.retrievedAtEpochMillis)
        assertEquals(1_700_000_000_250L, returned.sourceTimestampEpochMillis)
    }

    @Test
    fun nullSourceTimestampRemainsNull() = runTest {
        val candidate = snapshot(
            productIsin = "DE000SYNTH01",
            retrievedAtEpochMillis = 1_700_000_000_500L,
            sourceTimestampEpochMillis = null
        )
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH01")
            )
        )

        val returned = candidatesWithMarketData(
            service(repository).execute(request(candidate))
        ).single().specificationSnapshot

        assertSame(candidate, returned)
        assertEquals(1_700_000_000_500L, returned.retrievedAtEpochMillis)
        assertNull(returned.sourceTimestampEpochMillis)
    }

    @Test
    fun marketDataInstanceAndValuesRemainUnchanged() = runTest {
        val loaded = KnockoutProductMarketData(
            productIsin = "DE000SYNTH01",
            bid = 1.25,
            ask = 1.35,
            bidTimestampEpochMillis = 1_700_000_000_100L,
            askTimestampEpochMillis = 1_700_000_000_200L,
            currency = "EUR",
            sourceId = "synthetic-market-source"
        )
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(loaded)
        )

        val returned = candidatesWithMarketData(
            service(repository).execute(request(snapshot(productIsin = "DE000SYNTH01")))
        ).single().marketData

        assertSame(loaded, returned)
        assertEquals("DE000SYNTH01", returned.productIsin)
        assertEquals(1.25, returned.bid!!, 0.0)
        assertEquals(1.35, returned.ask!!, 0.0)
        assertEquals(1_700_000_000_100L, returned.bidTimestampEpochMillis)
        assertEquals(1_700_000_000_200L, returned.askTimestampEpochMillis)
        assertEquals("EUR", returned.currency)
        assertEquals("synthetic-market-source", returned.sourceId)
    }

    @Test
    fun nullableQuoteValuesRemainUnchanged() = runTest {
        val loaded = marketData(
            productIsin = "DE000SYNTH01",
            bid = null,
            ask = null,
            bidTimestampEpochMillis = null,
            askTimestampEpochMillis = null
        )
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(loaded)
        )

        val returned = candidatesWithMarketData(
            service(repository).execute(request(snapshot(productIsin = "DE000SYNTH01")))
        ).single().marketData

        assertSame(loaded, returned)
        assertNull(returned.bid)
        assertNull(returned.ask)
        assertNull(returned.bidTimestampEpochMillis)
        assertNull(returned.askTimestampEpochMillis)
    }

    @Test
    fun successfulRepositoryResultIsNotCompatibilityValidated() = runTest {
        val incompatible = marketData(productIsin = "DE000SYNTH99")
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(incompatible)
        )

        val result = service(repository).execute(
            request(snapshot(productIsin = "DE000SYNTH01"))
        )

        assertTrue(result is KnockoutProductCandidateMarketDataResult.CandidatesWithMarketData)
        val returned = candidatesWithMarketData(result).single().marketData
        assertSame(incompatible, returned)
        assertEquals("DE000SYNTH99", returned.productIsin)
    }

    @Test
    fun serviceDoesNotMutateRequestOrCandidateList() = runTest {
        val duplicate = snapshot(productIsin = "DE000SYNTH01")
        val candidates = mutableListOf(
            duplicate,
            snapshot(productIsin = "DE000SYNTH02"),
            duplicate
        )
        val original = candidates.toList()
        val request = KnockoutProductCandidateMarketDataRequest(candidates)
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH01")
            ),
            "DE000SYNTH02" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH02")
            )
        )

        service(repository).execute(request)

        assertSame(candidates, request.candidates)
        assertEquals(original, candidates)
        original.indices.forEach { index -> assertSame(original[index], candidates[index]) }
    }

    @Test
    fun cacheIsLimitedToSingleExecuteCall() = runTest {
        val candidate = snapshot(productIsin = "DE000SYNTH01")
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH01")
            )
        )
        val service = service(repository)

        service.execute(request(candidate))
        service.execute(request(candidate))

        assertEquals(listOf("DE000SYNTH01", "DE000SYNTH01"), repository.productIsinCalls)
    }

    @Test
    fun firstFailureStopsLaterUniqueAndDuplicateLoads() = runTest {
        val first = snapshot(productIsin = "DE000SYNTH01")
        val failing = snapshot(productIsin = "DE000SYNTH02")
        val duplicate = snapshot(productIsin = "DE000SYNTH01")
        val later = snapshot(productIsin = "DE000SYNTH03")
        val repository = repository(
            "DE000SYNTH01" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH01")
            ),
            "DE000SYNTH02" to RepositoryResult.DataAccessFailure,
            "DE000SYNTH03" to RepositoryResult.Success(
                marketData(productIsin = "DE000SYNTH03")
            )
        )

        val result = service(repository).execute(request(first, failing, duplicate, later))

        assertEquals(
            KnockoutProductCandidateMarketDataResult
                .MarketDataDataAccessFailure("DE000SYNTH02"),
            result
        )
        assertEquals(listOf("DE000SYNTH01", "DE000SYNTH02"), repository.productIsinCalls)
        assertFalse(result is KnockoutProductCandidateMarketDataResult.CandidatesWithMarketData)
    }

    @Test
    fun resultContainsNoDataQualityRankingOrCalculationOutput() {
        val specificationSnapshot = snapshot(productIsin = "DE000SYNTH01")
        val marketData = marketData(productIsin = "DE000SYNTH01")
        val candidates = listOf(specificationSnapshot)
        val request = KnockoutProductCandidateMarketDataRequest(candidates)
        val pair = KnockoutProductCandidateWithMarketData(
            specificationSnapshot = specificationSnapshot,
            marketData = marketData
        )
        val result = KnockoutProductCandidateMarketDataResult.CandidatesWithMarketData(
            candidates = listOf(pair)
        )

        assertSame(candidates, request.candidates)
        assertSame(specificationSnapshot, pair.specificationSnapshot)
        assertSame(marketData, pair.marketData)
        assertEquals(listOf(pair), result.candidates)
    }

    private fun service(
        repository: KnockoutProductMarketDataRepository
    ) = KnockoutProductCandidateMarketDataApplicationService(repository)

    private fun request(
        vararg candidates: KnockoutProductSpecificationSnapshot
    ) = KnockoutProductCandidateMarketDataRequest(candidates.toList())

    private fun repository(
        vararg responses: Pair<String, RepositoryResult<KnockoutProductMarketData>>
    ) = FakeMarketDataRepository(mapOf(*responses))

    private fun candidatesWithMarketData(
        result: KnockoutProductCandidateMarketDataResult
    ): List<KnockoutProductCandidateWithMarketData> =
        (result as KnockoutProductCandidateMarketDataResult.CandidatesWithMarketData).candidates

    private fun snapshot(
        productIsin: String = "DE000SYNTH01",
        issuerId: String = "synthetic-issuer",
        retrievedAtEpochMillis: Long = 1_700_000_000_500L,
        sourceTimestampEpochMillis: Long? = 1_700_000_000_250L
    ) = KnockoutProductSpecificationSnapshot(
        specification = specification(productIsin = productIsin, issuerId = issuerId),
        sourceId = "synthetic-specification-source",
        retrievedAtEpochMillis = retrievedAtEpochMillis,
        sourceTimestampEpochMillis = sourceTimestampEpochMillis
    )

    private fun specification(
        productIsin: String,
        issuerId: String = "synthetic-issuer"
    ) = KnockoutProductSpecification(
        productIsin = productIsin,
        productWkn = "SYN001",
        issuerId = issuerId,
        underlyingId = "synthetic-underlying",
        direction = TradeDirection.LONG,
        basePrice = 80.0,
        knockoutBarrier = 82.0,
        ratio = 0.1,
        underlyingCurrency = "USD",
        productCurrency = "EUR"
    )

    private fun marketData(
        productIsin: String,
        bid: Double? = 1.0,
        ask: Double? = 1.1,
        bidTimestampEpochMillis: Long? = 1_700_000_000_100L,
        askTimestampEpochMillis: Long? = 1_700_000_000_200L,
        currency: String = "EUR",
        sourceId: String = "synthetic-market-source"
    ) = KnockoutProductMarketData(
        productIsin = productIsin,
        bid = bid,
        ask = ask,
        bidTimestampEpochMillis = bidTimestampEpochMillis,
        askTimestampEpochMillis = askTimestampEpochMillis,
        currency = currency,
        sourceId = sourceId
    )

    private class FakeMarketDataRepository(
        private val responses: Map<String, RepositoryResult<KnockoutProductMarketData>>
    ) : KnockoutProductMarketDataRepository {

        val productIsinCalls = mutableListOf<String>()

        override suspend fun findByProductIsin(
            productIsin: String
        ): RepositoryResult<KnockoutProductMarketData> {
            productIsinCalls += productIsin
            return responses[productIsin] ?: RepositoryResult.NotFound
        }
    }
}
