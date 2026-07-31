package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductIssuerSelectionFilterTest {

    private val filter = KnockoutProductIssuerSelectionFilter()

    @Test
    fun enabledIssuersReturnMatchingCandidates() {
        val synth03 = snapshot(productIsin = "DE000SYNTH03", issuerId = "issuer-c")
        val synth01 = snapshot(productIsin = "DE000SYNTH01", issuerId = "issuer-a")
        val synth02 = snapshot(productIsin = "DE000SYNTH02", issuerId = "issuer-b")

        val result = filter.filter(
            request(
                candidates = listOf(synth03, synth01, synth02),
                enabledIssuerIds = setOf("issuer-a", "issuer-c")
            )
        )

        assertTrue(result is KnockoutProductIssuerSelectionResult.EnabledIssuerCandidates)
        val candidates = enabledCandidates(result)
        assertEquals(2, candidates.size)
        assertSame(synth03, candidates[0])
        assertSame(synth01, candidates[1])
        assertFalse(candidates.any { it === synth02 })
    }

    @Test
    fun emptyInputReturnsNoInputCandidates() {
        val result = filter.filter(
            request(candidates = emptyList(), enabledIssuerIds = setOf("issuer-a"))
        )

        assertSame(KnockoutProductIssuerSelectionResult.NoInputCandidates, result)
        assertFalse(result === KnockoutProductIssuerSelectionResult.NoEnabledIssuerCandidates)
    }

    @Test
    fun emptyEnabledIssuerSetReturnsNoEnabledIssuerCandidates() {
        val result = filter.filter(
            request(candidates = listOf(snapshot()), enabledIssuerIds = emptySet())
        )

        assertSame(KnockoutProductIssuerSelectionResult.NoEnabledIssuerCandidates, result)
    }

    @Test
    fun noMatchingIssuerReturnsNoEnabledIssuerCandidates() {
        val result = filter.filter(
            request(
                candidates = listOf(
                    snapshot(productIsin = "DE000SYNTH01", issuerId = "issuer-a"),
                    snapshot(productIsin = "DE000SYNTH02", issuerId = "issuer-b")
                ),
                enabledIssuerIds = setOf("issuer-c")
            )
        )

        assertSame(KnockoutProductIssuerSelectionResult.NoEnabledIssuerCandidates, result)
    }

    @Test
    fun issuerMatchingIsCaseSensitive() {
        val result = filter.filter(
            request(
                candidates = listOf(snapshot(issuerId = "Synthetic-Issuer")),
                enabledIssuerIds = setOf("synthetic-issuer")
            )
        )

        assertSame(KnockoutProductIssuerSelectionResult.NoEnabledIssuerCandidates, result)
    }

    @Test
    fun issuerMatchingIsWhitespaceSensitive() {
        val candidate = snapshot(issuerId = " Synthetic-Issuer ")

        val withoutWhitespace = filter.filter(
            request(
                candidates = listOf(candidate),
                enabledIssuerIds = setOf("Synthetic-Issuer")
            )
        )
        val exact = filter.filter(
            request(
                candidates = listOf(candidate),
                enabledIssuerIds = setOf(" Synthetic-Issuer ")
            )
        )

        assertSame(
            KnockoutProductIssuerSelectionResult.NoEnabledIssuerCandidates,
            withoutWhitespace
        )
        assertSame(candidate, enabledCandidates(exact).single())
    }

    @Test
    fun exactUnnormalizedIssuerIdRemainsSupported() {
        val candidate = snapshot(issuerId = " synthetic-issuer ")

        val result = filter.filter(
            request(
                candidates = listOf(candidate),
                enabledIssuerIds = setOf(" synthetic-issuer ")
            )
        )

        assertSame(candidate, enabledCandidates(result).single())
        assertEquals(" synthetic-issuer ", candidate.specification.issuerId)
    }

    @Test
    fun disabledIssuerCandidatesAreExcluded() {
        val issuerA = snapshot(productIsin = "DE000SYNTH01", issuerId = "issuer-a")
        val issuerB = snapshot(productIsin = "DE000SYNTH02", issuerId = "issuer-b")
        val issuerC = snapshot(productIsin = "DE000SYNTH03", issuerId = "issuer-c")

        val candidates = enabledCandidates(
            filter.filter(
                request(
                    candidates = listOf(issuerA, issuerB, issuerC),
                    enabledIssuerIds = setOf("issuer-b")
                )
            )
        )

        assertEquals(1, candidates.size)
        assertSame(issuerB, candidates.single())
    }

    @Test
    fun allExplicitlyEnabledIssuersPreserveAllCandidates() {
        val candidates = listOf(
            snapshot(productIsin = "DE000SYNTH01", issuerId = "issuer-a"),
            snapshot(productIsin = "DE000SYNTH02", issuerId = "issuer-b"),
            snapshot(productIsin = "DE000SYNTH03", issuerId = "issuer-c")
        )
        val explicitlyEnabled = setOf("issuer-a", "issuer-b", "issuer-c")

        val returned = enabledCandidates(
            filter.filter(request(candidates, explicitlyEnabled))
        )

        assertEquals(3, returned.size)
        candidates.indices.forEach { index -> assertSame(candidates[index], returned[index]) }
        assertEquals(setOf("issuer-a", "issuer-b", "issuer-c"), explicitlyEnabled)
    }

    @Test
    fun duplicateCandidatesRemainDuplicated() {
        val duplicate = snapshot(issuerId = "issuer-a")

        val candidates = enabledCandidates(
            filter.filter(
                request(
                    candidates = listOf(duplicate, duplicate),
                    enabledIssuerIds = setOf("issuer-a")
                )
            )
        )

        assertEquals(2, candidates.size)
        assertSame(duplicate, candidates[0])
        assertSame(duplicate, candidates[1])
    }

    @Test
    fun differentProductsFromSameIssuerRemainSeparate() {
        val first = snapshot(productIsin = "DE000SYNTH01", issuerId = "issuer-a")
        val second = snapshot(productIsin = "DE000SYNTH02", issuerId = "issuer-a")

        val candidates = enabledCandidates(
            filter.filter(
                request(
                    candidates = listOf(first, second),
                    enabledIssuerIds = setOf("issuer-a")
                )
            )
        )

        assertEquals(2, candidates.size)
        assertSame(first, candidates[0])
        assertSame(second, candidates[1])
    }

    @Test
    fun originalCandidateOrderIsPreserved() {
        val first = snapshot(
            productIsin = "DE000SYNTH30",
            productWkn = "SYN030",
            issuerId = "issuer-c",
            basePrice = 90.0,
            knockoutBarrier = 91.0
        )
        val excluded = snapshot(
            productIsin = "DE000SYNTH10",
            productWkn = "SYN010",
            issuerId = "issuer-a",
            basePrice = 70.0,
            knockoutBarrier = 71.0
        )
        val third = snapshot(
            productIsin = "DE000SYNTH20",
            productWkn = "SYN020",
            issuerId = "issuer-b",
            basePrice = 80.0,
            knockoutBarrier = 81.0
        )

        val candidates = enabledCandidates(
            filter.filter(
                request(
                    candidates = listOf(first, excluded, third),
                    enabledIssuerIds = setOf("issuer-b", "issuer-c")
                )
            )
        )

        assertEquals(2, candidates.size)
        assertSame(first, candidates[0])
        assertSame(third, candidates[1])
    }

    @Test
    fun resultIsNotLimitedToThreeCandidates() {
        val candidates = (1..4).map { index ->
            snapshot(productIsin = "DE000SYNTH0$index", issuerId = "issuer-a")
        }

        val returned = enabledCandidates(
            filter.filter(request(candidates, setOf("issuer-a")))
        )

        assertEquals(4, returned.size)
        candidates.indices.forEach { index -> assertSame(candidates[index], returned[index]) }
    }

    @Test
    fun snapshotAndSpecificationInstancesRemainUnchanged() {
        val specification = KnockoutProductSpecification(
            productIsin = "DE000SYNTH01",
            productWkn = "SYN001",
            issuerId = "issuer-a",
            underlyingId = "synthetic-underlying",
            direction = TradeDirection.LONG,
            basePrice = 80.125,
            knockoutBarrier = 82.5,
            ratio = 0.1,
            underlyingCurrency = "USD",
            productCurrency = "EUR"
        )
        val candidate = KnockoutProductSpecificationSnapshot(
            specification = specification,
            sourceId = "synthetic-source",
            retrievedAtEpochMillis = 1_700_000_000_500L,
            sourceTimestampEpochMillis = 1_700_000_000_250L
        )

        val returned = enabledCandidates(
            filter.filter(request(listOf(candidate), setOf("issuer-a")))
        ).single()

        assertSame(candidate, returned)
        assertSame(specification, returned.specification)
        assertEquals("synthetic-source", returned.sourceId)
        assertEquals(1_700_000_000_500L, returned.retrievedAtEpochMillis)
        assertEquals(1_700_000_000_250L, returned.sourceTimestampEpochMillis)
    }

    @Test
    fun nullSourceTimestampRemainsNull() {
        val candidate = snapshot(
            issuerId = "issuer-a",
            retrievedAtEpochMillis = 1_700_000_000_500L,
            sourceTimestampEpochMillis = null
        )

        val returned = enabledCandidates(
            filter.filter(request(listOf(candidate), setOf("issuer-a")))
        ).single()

        assertSame(candidate, returned)
        assertEquals(1_700_000_000_500L, returned.retrievedAtEpochMillis)
        assertNull(returned.sourceTimestampEpochMillis)
    }

    @Test
    fun filterDoesNotMutateCandidateList() {
        val duplicate = snapshot(productIsin = "DE000SYNTH01", issuerId = "issuer-a")
        val candidates = mutableListOf(
            snapshot(productIsin = "DE000SYNTH03", issuerId = "issuer-c"),
            duplicate,
            snapshot(productIsin = "DE000SYNTH02", issuerId = "issuer-b"),
            duplicate
        )
        val original = candidates.toList()

        filter.filter(request(candidates, setOf("issuer-a", "issuer-c")))

        assertEquals(original, candidates)
        original.indices.forEach { index -> assertSame(original[index], candidates[index]) }
    }

    @Test
    fun filterDoesNotMutateEnabledIssuerSet() {
        val enabledIssuerIds = mutableSetOf("issuer-a", "issuer-b", "issuer-c")
        val original = enabledIssuerIds.toSet()

        filter.filter(
            request(
                candidates = listOf(snapshot(issuerId = "issuer-a")),
                enabledIssuerIds = enabledIssuerIds
            )
        )

        assertEquals(original, enabledIssuerIds)
    }

    @Test
    fun requestValuesRemainUnchangedAfterFiltering() {
        val candidates = mutableListOf(snapshot(issuerId = " issuer-a "))
        val enabledIssuerIds = mutableSetOf(" issuer-a ")
        val request = KnockoutProductIssuerSelectionRequest(candidates, enabledIssuerIds)

        filter.filter(request)

        assertSame(candidates, request.candidates)
        assertSame(enabledIssuerIds, request.enabledIssuerIds)
        assertEquals(listOf(candidates.single()), request.candidates)
        assertEquals(setOf(" issuer-a "), request.enabledIssuerIds)
    }

    @Test
    fun issuerSelectionDoesNotRecheckBrokerAvailability() {
        val candidates = listOf(snapshot(issuerId = "issuer-a"))
        val enabledIssuerIds = setOf("issuer-a")
        val request = KnockoutProductIssuerSelectionRequest(
            candidates = candidates,
            enabledIssuerIds = enabledIssuerIds
        )

        val result = filter.filter(request)

        assertSame(candidates, request.candidates)
        assertSame(enabledIssuerIds, request.enabledIssuerIds)
        assertSame(candidates.single(), enabledCandidates(result).single())
    }

    @Test
    fun filterContainsNoMarketDataRankingOrCalculationOutput() {
        val candidate = snapshot(issuerId = "issuer-a")
        val result = filter.filter(
            KnockoutProductIssuerSelectionRequest(
                candidates = listOf(candidate),
                enabledIssuerIds = setOf("issuer-a")
            )
        )

        val candidates = enabledCandidates(result)
        assertEquals(1, candidates.size)
        assertSame(candidate, candidates.single())
    }

    private fun request(
        candidates: List<KnockoutProductSpecificationSnapshot>,
        enabledIssuerIds: Set<String>
    ) = KnockoutProductIssuerSelectionRequest(
        candidates = candidates,
        enabledIssuerIds = enabledIssuerIds
    )

    private fun enabledCandidates(
        result: KnockoutProductIssuerSelectionResult
    ): List<KnockoutProductSpecificationSnapshot> =
        (result as KnockoutProductIssuerSelectionResult.EnabledIssuerCandidates).candidates

    private fun snapshot(
        productIsin: String = "DE000SYNTH01",
        productWkn: String? = "SYN001",
        issuerId: String = "issuer-a",
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
}
