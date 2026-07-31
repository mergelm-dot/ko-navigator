package de.konavigator.app.application.productdiscovery

import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository
import de.konavigator.app.application.repository.KnockoutProductSpecificationCatalogRepository
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

class KnockoutProductDiscoveryApplicationServiceTest {

    @Test
    fun successfulCatalogAndAvailabilityReturnBrokerTradableCandidates() = runTest {
        val synth03 = snapshot(productIsin = "DE000SYNTH03")
        val synth01 = snapshot(productIsin = "DE000SYNTH01")
        val synth02 = snapshot(productIsin = "DE000SYNTH02")
        val context = context(
            catalogResult = catalogSuccess(synth03, synth01, synth02),
            availabilityResult = availabilitySuccess("DE000SYNTH01", "DE000SYNTH03")
        )

        val result = context.service.execute(request())

        assertTrue(result is KnockoutProductDiscoveryApplicationResult.BrokerTradableCandidates)
        val candidates = brokerTradableCandidates(result)
        assertEquals(2, candidates.size)
        assertSame(synth03, candidates[0])
        assertSame(synth01, candidates[1])
        assertFalse(candidates.any { it === synth02 })
    }

    @Test
    fun requestValuesAreForwardedExactly() = runTest {
        val candidate = snapshot(productIsin = "DE000SYNTH01", direction = TradeDirection.SHORT)
        val context = context(
            catalogResult = catalogSuccess(candidate),
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )
        val request = request(
            underlyingId = " Synthetic-Underlying ",
            direction = TradeDirection.SHORT,
            brokerId = " Synthetic-Broker "
        )

        context.service.execute(request)

        val catalogQuery = context.catalogRepository.queries.single()
        val availabilityQuery = context.brokerRepository.queries.single()
        assertEquals(" Synthetic-Underlying ", catalogQuery.underlyingId)
        assertEquals(TradeDirection.SHORT, catalogQuery.direction)
        assertEquals(" Synthetic-Broker ", availabilityQuery.brokerId)
    }

    @Test
    fun availabilityQueryContainsCatalogIsinsInExactOrder() = runTest {
        val productIsins = listOf(
            "DE000SYNTH03",
            " DE000SYNTH01 ",
            "DE000SYNTH03",
            "de000synth02"
        )
        val candidates = productIsins.map { productIsin -> snapshot(productIsin = productIsin) }
        val context = context(
            catalogResult = KnockoutProductSpecificationCatalogResult.Success(candidates),
            availabilityResult = KnockoutProductBrokerAvailabilityResult.Success(
                tradableProductIsins = productIsins.toSet()
            )
        )

        context.service.execute(request())

        assertEquals(productIsins, context.brokerRepository.queries.single().productIsins)
    }

    @Test
    fun emptyCatalogSuccessReturnsNoCatalogCandidates() = runTest {
        val context = context(
            catalogResult = KnockoutProductSpecificationCatalogResult.Success(emptyList()),
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )

        val result = context.service.execute(request())

        assertSame(KnockoutProductDiscoveryApplicationResult.NoCatalogCandidates, result)
        assertEquals(1, context.catalogRepository.queries.size)
        assertTrue(context.brokerRepository.queries.isEmpty())
    }

    @Test
    fun catalogDataAccessFailureIsMappedAndStopsPipeline() = runTest {
        val context = context(
            catalogResult = KnockoutProductSpecificationCatalogResult.DataAccessFailure,
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )

        val result = context.service.execute(request())

        assertSame(KnockoutProductDiscoveryApplicationResult.CatalogDataAccessFailure, result)
        assertTrue(context.brokerRepository.queries.isEmpty())
    }

    @Test
    fun catalogInvalidDataIsMappedAndStopsPipeline() = runTest {
        val context = context(
            catalogResult = KnockoutProductSpecificationCatalogResult.InvalidData,
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )

        val result = context.service.execute(request())

        assertSame(KnockoutProductDiscoveryApplicationResult.CatalogInvalidData, result)
        assertTrue(context.brokerRepository.queries.isEmpty())
    }

    @Test
    fun emptyAvailabilitySuccessReturnsNoBrokerTradableCandidates() = runTest {
        val context = context(
            catalogResult = catalogSuccess(snapshot()),
            availabilityResult = availabilitySuccess()
        )

        val result = context.service.execute(request())

        assertSame(KnockoutProductDiscoveryApplicationResult.NoBrokerTradableCandidates, result)
        assertEquals(1, context.catalogRepository.queries.size)
        assertEquals(1, context.brokerRepository.queries.size)
    }

    @Test
    fun availabilityWithoutMatchingCatalogIsinReturnsNoBrokerTradableCandidates() = runTest {
        val context = context(
            catalogResult = catalogSuccess(snapshot(productIsin = "DE000SYNTH01")),
            availabilityResult = availabilitySuccess("DE000SYNTH99")
        )

        val result = context.service.execute(request())

        assertSame(KnockoutProductDiscoveryApplicationResult.NoBrokerTradableCandidates, result)
    }

    @Test
    fun brokerAvailabilityDataAccessFailureIsMappedWithoutPartialResult() = runTest {
        val context = context(
            catalogResult = catalogSuccess(
                snapshot(productIsin = "DE000SYNTH01"),
                snapshot(productIsin = "DE000SYNTH02")
            ),
            availabilityResult = KnockoutProductBrokerAvailabilityResult.DataAccessFailure
        )

        val result = context.service.execute(request())

        assertSame(
            KnockoutProductDiscoveryApplicationResult.BrokerAvailabilityDataAccessFailure,
            result
        )
        assertFalse(result is KnockoutProductDiscoveryApplicationResult.BrokerTradableCandidates)
    }

    @Test
    fun brokerAvailabilityInvalidDataIsMappedWithoutPartialResult() = runTest {
        val context = context(
            catalogResult = catalogSuccess(
                snapshot(productIsin = "DE000SYNTH01"),
                snapshot(productIsin = "DE000SYNTH02")
            ),
            availabilityResult = KnockoutProductBrokerAvailabilityResult.InvalidData
        )

        val result = context.service.execute(request())

        assertSame(
            KnockoutProductDiscoveryApplicationResult.BrokerAvailabilityInvalidData,
            result
        )
        assertFalse(result is KnockoutProductDiscoveryApplicationResult.BrokerTradableCandidates)
    }

    @Test
    fun nonTradableCandidatesAreExcluded() = runTest {
        val synth01 = snapshot(productIsin = "DE000SYNTH01")
        val synth02 = snapshot(productIsin = "DE000SYNTH02")
        val synth03 = snapshot(productIsin = "DE000SYNTH03")
        val context = context(
            catalogResult = catalogSuccess(synth01, synth02, synth03),
            availabilityResult = availabilitySuccess("DE000SYNTH02")
        )

        val candidates = brokerTradableCandidates(context.service.execute(request()))

        assertEquals(1, candidates.size)
        assertSame(synth02, candidates.single())
    }

    @Test
    fun catalogOrderIsPreservedAfterFiltering() = runTest {
        val first = snapshot(
            productIsin = "DE000SYNTH30",
            productWkn = "SYN030",
            issuerId = "synthetic-issuer-c",
            basePrice = 90.0,
            knockoutBarrier = 91.0
        )
        val excluded = snapshot(
            productIsin = "DE000SYNTH10",
            productWkn = "SYN010",
            issuerId = "synthetic-issuer-a",
            basePrice = 70.0,
            knockoutBarrier = 71.0
        )
        val third = snapshot(
            productIsin = "DE000SYNTH20",
            productWkn = "SYN020",
            issuerId = "synthetic-issuer-b",
            basePrice = 80.0,
            knockoutBarrier = 81.0
        )
        val context = context(
            catalogResult = catalogSuccess(first, excluded, third),
            availabilityResult = availabilitySuccess("DE000SYNTH20", "DE000SYNTH30")
        )

        val candidates = brokerTradableCandidates(context.service.execute(request()))

        assertEquals(2, candidates.size)
        assertSame(first, candidates[0])
        assertSame(third, candidates[1])
    }

    @Test
    fun duplicateCatalogCandidatesRemainDuplicated() = runTest {
        val duplicate = snapshot(productIsin = "DE000SYNTH01")
        val context = context(
            catalogResult = catalogSuccess(duplicate, duplicate),
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )

        val candidates = brokerTradableCandidates(context.service.execute(request()))

        assertEquals(2, candidates.size)
        assertSame(duplicate, candidates[0])
        assertSame(duplicate, candidates[1])
    }

    @Test
    fun productsFromSameIssuerRemainSeparate() = runTest {
        val first = snapshot(
            productIsin = "DE000SYNTH01",
            issuerId = "synthetic-shared-issuer"
        )
        val second = snapshot(
            productIsin = "DE000SYNTH02",
            issuerId = "synthetic-shared-issuer"
        )
        val context = context(
            catalogResult = catalogSuccess(first, second),
            availabilityResult = availabilitySuccess("DE000SYNTH01", "DE000SYNTH02")
        )

        val candidates = brokerTradableCandidates(context.service.execute(request()))

        assertEquals(2, candidates.size)
        assertSame(first, candidates[0])
        assertSame(second, candidates[1])
    }

    @Test
    fun resultIsNotLimitedToThreeCandidates() = runTest {
        val stored = (1..4).map { index -> snapshot(productIsin = "DE000SYNTH0$index") }
        val context = context(
            catalogResult = KnockoutProductSpecificationCatalogResult.Success(stored),
            availabilityResult = KnockoutProductBrokerAvailabilityResult.Success(
                tradableProductIsins = stored.map { it.specification.productIsin }.toSet()
            )
        )

        val candidates = brokerTradableCandidates(context.service.execute(request()))

        assertEquals(4, candidates.size)
        stored.indices.forEach { index -> assertSame(stored[index], candidates[index]) }
    }

    @Test
    fun snapshotAndSpecificationInstancesRemainUnchanged() = runTest {
        val specification = KnockoutProductSpecification(
            productIsin = "DE000SYNTH01",
            productWkn = "SYN001",
            issuerId = "synthetic-issuer",
            underlyingId = "synthetic-underlying",
            direction = TradeDirection.LONG,
            basePrice = 80.125,
            knockoutBarrier = 82.5,
            ratio = 0.1,
            underlyingCurrency = "USD",
            productCurrency = "EUR"
        )
        val snapshot = KnockoutProductSpecificationSnapshot(
            specification = specification,
            sourceId = "synthetic-source",
            retrievedAtEpochMillis = 1_700_000_000_500L,
            sourceTimestampEpochMillis = 1_700_000_000_250L
        )
        val context = context(
            catalogResult = catalogSuccess(snapshot),
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )

        val returned = brokerTradableCandidates(context.service.execute(request())).single()

        assertSame(snapshot, returned)
        assertSame(specification, returned.specification)
        assertEquals("synthetic-source", returned.sourceId)
        assertEquals(1_700_000_000_500L, returned.retrievedAtEpochMillis)
        assertEquals(1_700_000_000_250L, returned.sourceTimestampEpochMillis)
    }

    @Test
    fun nullSourceTimestampRemainsNull() = runTest {
        val snapshot = snapshot(
            productIsin = "DE000SYNTH01",
            retrievedAtEpochMillis = 1_700_000_000_500L,
            sourceTimestampEpochMillis = null
        )
        val context = context(
            catalogResult = catalogSuccess(snapshot),
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )

        val returned = brokerTradableCandidates(context.service.execute(request())).single()

        assertSame(snapshot, returned)
        assertEquals(1_700_000_000_500L, returned.retrievedAtEpochMillis)
        assertNull(returned.sourceTimestampEpochMillis)
    }

    @Test
    fun serviceDoesNotMutateCatalogCandidateList() = runTest {
        val candidates = mutableListOf(
            snapshot(productIsin = "DE000SYNTH03"),
            snapshot(productIsin = "DE000SYNTH01"),
            snapshot(productIsin = "DE000SYNTH02")
        )
        val original = candidates.toList()
        val context = context(
            catalogResult = KnockoutProductSpecificationCatalogResult.Success(candidates),
            availabilityResult = availabilitySuccess("DE000SYNTH01", "DE000SYNTH03")
        )

        context.service.execute(request())

        assertEquals(original, candidates)
        original.indices.forEach { index -> assertSame(original[index], candidates[index]) }
    }

    @Test
    fun serviceDoesNotMutateRequest() = runTest {
        val enabledIssuerIds = linkedSetOf(
            "synthetic-issuer",
            "synthetic-issuer-a"
        )
        val request = request(
            underlyingId = " Synthetic-Underlying ",
            direction = TradeDirection.SHORT,
            brokerId = " Synthetic-Broker ",
            enabledIssuerIds = enabledIssuerIds
        )
        val context = context(
            catalogResult = catalogSuccess(
                snapshot(productIsin = "DE000SYNTH01", direction = TradeDirection.SHORT)
            ),
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )

        context.service.execute(request)

        assertEquals(" Synthetic-Underlying ", request.underlyingId)
        assertEquals(TradeDirection.SHORT, request.direction)
        assertEquals(" Synthetic-Broker ", request.brokerId)
        assertSame(enabledIssuerIds, request.enabledIssuerIds)
        assertEquals(setOf("synthetic-issuer", "synthetic-issuer-a"), request.enabledIssuerIds)
    }

    @Test
    fun repositoriesAreCalledSequentiallyExactlyOnceOnSuccess() = runTest {
        val events = mutableListOf<String>()
        val context = context(
            catalogResult = catalogSuccess(snapshot()),
            availabilityResult = availabilitySuccess("DE000SYNTH01"),
            events = events
        )

        context.service.execute(request())

        assertEquals(listOf("catalog", "availability"), events)
        assertEquals(1, context.catalogRepository.queries.size)
        assertEquals(1, context.brokerRepository.queries.size)
    }

    @Test
    fun brokerRepositoryIsNeverCalledBeforeSuccessfulNonEmptyCatalog() = runTest {
        val catalogResults = listOf<KnockoutProductSpecificationCatalogResult>(
            KnockoutProductSpecificationCatalogResult.Success(emptyList()),
            KnockoutProductSpecificationCatalogResult.DataAccessFailure,
            KnockoutProductSpecificationCatalogResult.InvalidData
        )

        catalogResults.forEach { catalogResult ->
            val context = context(
                catalogResult = catalogResult,
                availabilityResult = availabilitySuccess("DE000SYNTH01")
            )

            context.service.execute(request())

            assertEquals(1, context.catalogRepository.queries.size)
            assertTrue(context.brokerRepository.queries.isEmpty())
        }
    }

    @Test
    fun emptyEnabledIssuerSetReturnsNoEnabledIssuerCandidates() = runTest {
        val candidate = snapshot(productIsin = "DE000SYNTH01", issuerId = "synthetic-issuer-a")
        val context = context(
            catalogResult = catalogSuccess(candidate),
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )

        val result = context.service.execute(request(enabledIssuerIds = emptySet()))

        assertSame(KnockoutProductDiscoveryApplicationResult.NoEnabledIssuerCandidates, result)
        assertFalse(result === KnockoutProductDiscoveryApplicationResult.NoCatalogCandidates)
        assertFalse(result === KnockoutProductDiscoveryApplicationResult.NoBrokerTradableCandidates)
    }

    @Test
    fun noMatchingEnabledIssuerReturnsNoEnabledIssuerCandidates() = runTest {
        val context = context(
            catalogResult = catalogSuccess(
                snapshot(productIsin = "DE000SYNTH01", issuerId = "synthetic-issuer-a")
            ),
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )

        val result = context.service.execute(
            request(enabledIssuerIds = setOf("synthetic-issuer-b"))
        )

        assertSame(KnockoutProductDiscoveryApplicationResult.NoEnabledIssuerCandidates, result)
    }

    @Test
    fun disabledBrokerTradableCandidatesAreExcluded() = runTest {
        val synth01 = snapshot(productIsin = "DE000SYNTH01", issuerId = "issuer-a")
        val synth02 = snapshot(productIsin = "DE000SYNTH02", issuerId = "issuer-b")
        val synth03 = snapshot(productIsin = "DE000SYNTH03", issuerId = "issuer-c")
        val context = context(
            catalogResult = catalogSuccess(synth01, synth02, synth03),
            availabilityResult = availabilitySuccess(
                "DE000SYNTH01",
                "DE000SYNTH02",
                "DE000SYNTH03"
            )
        )

        val result = context.service.execute(
            request(enabledIssuerIds = setOf("issuer-a", "issuer-c"))
        )

        assertTrue(result is KnockoutProductDiscoveryApplicationResult.BrokerTradableCandidates)
        val candidates = brokerTradableCandidates(result)
        assertEquals(2, candidates.size)
        assertSame(synth01, candidates[0])
        assertSame(synth03, candidates[1])
        assertFalse(candidates.any { it === synth02 })
    }

    @Test
    fun issuerSelectionRemainsCaseSensitiveAcrossPipeline() = runTest {
        val context = context(
            catalogResult = catalogSuccess(
                snapshot(productIsin = "DE000SYNTH01", issuerId = "Synthetic-Issuer")
            ),
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )

        val result = context.service.execute(
            request(enabledIssuerIds = setOf("synthetic-issuer"))
        )

        assertSame(KnockoutProductDiscoveryApplicationResult.NoEnabledIssuerCandidates, result)
    }

    @Test
    fun issuerSelectionRemainsWhitespaceSensitiveAcrossPipeline() = runTest {
        val candidate = snapshot(
            productIsin = "DE000SYNTH01",
            issuerId = " Synthetic-Issuer "
        )
        val withoutWhitespaceContext = context(
            catalogResult = catalogSuccess(candidate),
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )
        val exactContext = context(
            catalogResult = catalogSuccess(candidate),
            availabilityResult = availabilitySuccess("DE000SYNTH01")
        )

        val withoutWhitespace = withoutWhitespaceContext.service.execute(
            request(enabledIssuerIds = setOf("Synthetic-Issuer"))
        )
        val exact = exactContext.service.execute(
            request(enabledIssuerIds = setOf(" Synthetic-Issuer "))
        )

        assertSame(
            KnockoutProductDiscoveryApplicationResult.NoEnabledIssuerCandidates,
            withoutWhitespace
        )
        assertSame(candidate, brokerTradableCandidates(exact).single())
    }

    @Test
    fun issuerFilteringPreservesOrderDuplicatesAndInstances() = runTest {
        val first = snapshot(productIsin = "DE000SYNTH04", issuerId = "issuer-c")
        val duplicate = snapshot(productIsin = "DE000SYNTH01", issuerId = "issuer-a")
        val excluded = snapshot(productIsin = "DE000SYNTH02", issuerId = "issuer-b")
        val last = snapshot(productIsin = "DE000SYNTH03", issuerId = "issuer-c")
        val context = context(
            catalogResult = catalogSuccess(first, duplicate, excluded, duplicate, last),
            availabilityResult = availabilitySuccess(
                "DE000SYNTH01",
                "DE000SYNTH02",
                "DE000SYNTH03",
                "DE000SYNTH04"
            )
        )

        val candidates = brokerTradableCandidates(
            context.service.execute(
                request(enabledIssuerIds = setOf("issuer-a", "issuer-c"))
            )
        )

        assertEquals(4, candidates.size)
        assertSame(first, candidates[0])
        assertSame(duplicate, candidates[1])
        assertSame(duplicate, candidates[2])
        assertSame(last, candidates[3])
        assertFalse(candidates.any { it === excluded })
    }

    @Test
    fun upstreamEmptyAndFailureStatesRemainDistinctFromIssuerSelection() = runTest {
        val candidate = snapshot(productIsin = "DE000SYNTH01")
        val scenarios = listOf(
            context(
                catalogResult = KnockoutProductSpecificationCatalogResult.Success(emptyList()),
                availabilityResult = availabilitySuccess("DE000SYNTH01")
            ) to KnockoutProductDiscoveryApplicationResult.NoCatalogCandidates,
            context(
                catalogResult = catalogSuccess(candidate),
                availabilityResult = availabilitySuccess()
            ) to KnockoutProductDiscoveryApplicationResult.NoBrokerTradableCandidates,
            context(
                catalogResult = KnockoutProductSpecificationCatalogResult.DataAccessFailure,
                availabilityResult = availabilitySuccess("DE000SYNTH01")
            ) to KnockoutProductDiscoveryApplicationResult.CatalogDataAccessFailure,
            context(
                catalogResult = KnockoutProductSpecificationCatalogResult.InvalidData,
                availabilityResult = availabilitySuccess("DE000SYNTH01")
            ) to KnockoutProductDiscoveryApplicationResult.CatalogInvalidData,
            context(
                catalogResult = catalogSuccess(candidate),
                availabilityResult = KnockoutProductBrokerAvailabilityResult.DataAccessFailure
            ) to KnockoutProductDiscoveryApplicationResult.BrokerAvailabilityDataAccessFailure,
            context(
                catalogResult = catalogSuccess(candidate),
                availabilityResult = KnockoutProductBrokerAvailabilityResult.InvalidData
            ) to KnockoutProductDiscoveryApplicationResult.BrokerAvailabilityInvalidData
        )

        scenarios.forEach { (context, expectedResult) ->
            val result = context.service.execute(request(enabledIssuerIds = emptySet()))

            assertSame(expectedResult, result)
            assertFalse(result === KnockoutProductDiscoveryApplicationResult.NoEnabledIssuerCandidates)
        }
    }

    @Test
    fun serviceContainsNoMarketDataRankingOrCalculationOutput() {
        val enabledIssuerIds = setOf("synthetic-issuer")
        val request = KnockoutProductDiscoveryApplicationRequest(
            underlyingId = "synthetic-underlying",
            direction = TradeDirection.LONG,
            brokerId = "synthetic-broker",
            enabledIssuerIds = enabledIssuerIds
        )
        val candidates = listOf(snapshot())
        val result = KnockoutProductDiscoveryApplicationResult.BrokerTradableCandidates(
            candidates = candidates
        )

        assertEquals("synthetic-underlying", request.underlyingId)
        assertEquals(TradeDirection.LONG, request.direction)
        assertEquals("synthetic-broker", request.brokerId)
        assertSame(enabledIssuerIds, request.enabledIssuerIds)
        assertSame(candidates, result.candidates)
    }

    private fun context(
        catalogResult: KnockoutProductSpecificationCatalogResult,
        availabilityResult: KnockoutProductBrokerAvailabilityResult,
        events: MutableList<String>? = null
    ): TestContext {
        val catalogRepository = FakeCatalogRepository(catalogResult, events)
        val brokerRepository = FakeBrokerAvailabilityRepository(availabilityResult, events)
        return TestContext(
            service = KnockoutProductDiscoveryApplicationService(
                catalogRepository = catalogRepository,
                brokerAvailabilityRepository = brokerRepository,
                issuerSelectionFilter = KnockoutProductIssuerSelectionFilter()
            ),
            catalogRepository = catalogRepository,
            brokerRepository = brokerRepository
        )
    }

    private fun request(
        underlyingId: String = "synthetic-underlying",
        direction: TradeDirection = TradeDirection.LONG,
        brokerId: String = "synthetic-broker",
        enabledIssuerIds: Set<String> = setOf(
            "synthetic-issuer",
            "synthetic-issuer-a",
            "synthetic-issuer-b",
            "synthetic-issuer-c",
            "synthetic-shared-issuer"
        )
    ) = KnockoutProductDiscoveryApplicationRequest(
        underlyingId = underlyingId,
        direction = direction,
        brokerId = brokerId,
        enabledIssuerIds = enabledIssuerIds
    )

    private fun catalogSuccess(
        vararg candidates: KnockoutProductSpecificationSnapshot
    ) = KnockoutProductSpecificationCatalogResult.Success(candidates.toList())

    private fun availabilitySuccess(
        vararg productIsins: String
    ) = KnockoutProductBrokerAvailabilityResult.Success(productIsins.toSet())

    private fun brokerTradableCandidates(
        result: KnockoutProductDiscoveryApplicationResult
    ): List<KnockoutProductSpecificationSnapshot> =
        (result as KnockoutProductDiscoveryApplicationResult.BrokerTradableCandidates).candidates

    private fun snapshot(
        productIsin: String = "DE000SYNTH01",
        productWkn: String? = "SYN001",
        issuerId: String = "synthetic-issuer",
        underlyingId: String = "synthetic-underlying",
        direction: TradeDirection = TradeDirection.LONG,
        basePrice: Double = 80.0,
        knockoutBarrier: Double = 82.0,
        ratio: Double = 0.1,
        underlyingCurrency: String = "USD",
        productCurrency: String = "EUR",
        sourceId: String = "synthetic-source",
        retrievedAtEpochMillis: Long = 1_700_000_000_500L,
        sourceTimestampEpochMillis: Long? = 1_700_000_000_250L
    ) = KnockoutProductSpecificationSnapshot(
        specification = KnockoutProductSpecification(
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

    private data class TestContext(
        val service: KnockoutProductDiscoveryApplicationService,
        val catalogRepository: FakeCatalogRepository,
        val brokerRepository: FakeBrokerAvailabilityRepository
    )

    private class FakeCatalogRepository(
        private val result: KnockoutProductSpecificationCatalogResult,
        private val events: MutableList<String>? = null
    ) : KnockoutProductSpecificationCatalogRepository {

        val queries = mutableListOf<KnockoutProductSpecificationCatalogQuery>()

        override suspend fun findCandidates(
            query: KnockoutProductSpecificationCatalogQuery
        ): KnockoutProductSpecificationCatalogResult {
            events?.add("catalog")
            queries += query
            return result
        }
    }

    private class FakeBrokerAvailabilityRepository(
        private val result: KnockoutProductBrokerAvailabilityResult,
        private val events: MutableList<String>? = null
    ) : KnockoutProductBrokerAvailabilityRepository {

        val queries = mutableListOf<KnockoutProductBrokerAvailabilityQuery>()

        override suspend fun findTradableProductIsins(
            query: KnockoutProductBrokerAvailabilityQuery
        ): KnockoutProductBrokerAvailabilityResult {
            events?.add("availability")
            queries += query
            return result
        }
    }
}
