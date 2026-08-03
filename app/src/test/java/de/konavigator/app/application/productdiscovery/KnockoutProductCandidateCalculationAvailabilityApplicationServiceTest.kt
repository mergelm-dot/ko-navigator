package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityError
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.dataquality.DataQualityCategory
import de.konavigator.app.domain.dataquality.DataQualityComponent
import de.konavigator.app.domain.dataquality.DataQualityFinding
import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.dataquality.DataQualitySeverity
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateCalculationAvailabilityApplicationServiceTest {

    private val service = KnockoutProductCandidateCalculationAvailabilityApplicationService()

    @Test
    fun purchasePriceWithAskIsStructurallyAvailable() {
        val result = singleAvailability(MarketDataCalculationType.PURCHASE_PRICE, candidate(ask = 1.2))

        assertSame(MarketDataCalculationAvailabilityResult.StructurallyAvailable, result)
    }

    @Test
    fun purchasePriceWithoutAskIsStructurallyUnavailable() {
        val result = singleAvailability(MarketDataCalculationType.PURCHASE_PRICE, candidate(ask = null))

        assertEquals(
            listOf(MarketDataCalculationAvailabilityError.MISSING_ASK),
            unavailable(result).errors
        )
    }

    @Test
    fun purchasePriceDoesNotRequireBid() {
        val result = singleAvailability(
            MarketDataCalculationType.PURCHASE_PRICE,
            candidate(bid = null, ask = 1.2)
        )

        assertSame(MarketDataCalculationAvailabilityResult.StructurallyAvailable, result)
    }

    @Test
    fun salePriceWithPositiveBidIsStructurallyAvailable() {
        val result = singleAvailability(
            MarketDataCalculationType.SALE_PRICE,
            candidate(bid = 1.0, ask = null)
        )

        assertSame(MarketDataCalculationAvailabilityResult.StructurallyAvailable, result)
    }

    @Test
    fun salePriceWithoutBidIsStructurallyUnavailable() {
        val result = singleAvailability(MarketDataCalculationType.SALE_PRICE, candidate(bid = null))

        assertEquals(
            listOf(MarketDataCalculationAvailabilityError.MISSING_BID),
            unavailable(result).errors
        )
    }

    @Test
    fun salePriceWithZeroBidIsStructurallyUnavailable() {
        val result = singleAvailability(MarketDataCalculationType.SALE_PRICE, candidate(bid = 0.0))

        assertEquals(
            listOf(MarketDataCalculationAvailabilityError.BID_NOT_POSITIVE_FOR_SALE),
            unavailable(result).errors
        )
    }

    @Test
    fun spreadWithBidAndAskIsStructurallyAvailable() {
        val result = singleAvailability(MarketDataCalculationType.SPREAD, candidate(bid = 1.0, ask = 1.2))

        assertSame(MarketDataCalculationAvailabilityResult.StructurallyAvailable, result)
    }

    @Test
    fun spreadWithoutBidIsStructurallyUnavailable() {
        val result = singleAvailability(MarketDataCalculationType.SPREAD, candidate(bid = null, ask = 1.2))

        assertEquals(
            listOf(MarketDataCalculationAvailabilityError.MISSING_BID),
            unavailable(result).errors
        )
    }

    @Test
    fun spreadWithoutAskIsStructurallyUnavailable() {
        val result = singleAvailability(MarketDataCalculationType.SPREAD, candidate(bid = 1.0, ask = null))

        assertEquals(
            listOf(MarketDataCalculationAvailabilityError.MISSING_ASK),
            unavailable(result).errors
        )
    }

    @Test
    fun spreadWithoutBothQuotesPreservesErrorOrder() {
        val result = singleAvailability(MarketDataCalculationType.SPREAD, candidate(bid = null, ask = null))

        assertEquals(
            listOf(
                MarketDataCalculationAvailabilityError.MISSING_BID,
                MarketDataCalculationAvailabilityError.MISSING_ASK
            ),
            unavailable(result).errors
        )
    }

    @Test
    fun midWithBidAndAskIsStructurallyAvailable() {
        val result = singleAvailability(MarketDataCalculationType.MID, candidate(bid = 0.0, ask = 1.2))

        assertSame(MarketDataCalculationAvailabilityResult.StructurallyAvailable, result)
    }

    @Test
    fun midWithoutRequiredQuotesIsStructurallyUnavailable() {
        val result = singleAvailability(MarketDataCalculationType.MID, candidate(bid = null, ask = null))

        assertEquals(
            listOf(
                MarketDataCalculationAvailabilityError.MISSING_BID,
                MarketDataCalculationAvailabilityError.MISSING_ASK
            ),
            unavailable(result).errors
        )
    }

    @Test
    fun emptyInputReturnsNoInputCandidates() {
        val result = service.execute(
            request(candidates = emptyList(), calculationType = MarketDataCalculationType.MID)
        )

        assertSame(KnockoutProductCandidateCalculationAvailabilityResult.NoInputCandidates, result)
        assertFalse(
            result is KnockoutProductCandidateCalculationAvailabilityResult
                .CandidatesWithCalculationAvailability
        )
    }

    @Test
    fun inputOrderIsPreserved() {
        val first = candidate(productIsin = "SYNTH03", bid = 1.0, ask = 1.2)
        val second = candidate(productIsin = "SYNTH01", bid = null, ask = 1.2)
        val third = candidate(productIsin = "SYNTH02", bid = 1.1, ask = 1.3)

        val result = candidatesResult(MarketDataCalculationType.SPREAD, listOf(first, second, third))

        assertEquals(
            listOf(first, second, third),
            result.candidates.map { it.candidateWithDataQuality }
        )
    }

    @Test
    fun availableAndUnavailableCandidatesRemainTogether() {
        val available = candidate(productIsin = "SYNTH01", bid = 1.0, ask = 1.2)
        val unavailable = candidate(productIsin = "SYNTH02", bid = null, ask = 1.2)

        val result = candidatesResult(MarketDataCalculationType.SPREAD, listOf(available, unavailable))

        assertEquals(2, result.candidates.size)
        assertSame(available, result.candidates[0].candidateWithDataQuality)
        assertSame(unavailable, result.candidates[1].candidateWithDataQuality)
        assertSame(MarketDataCalculationAvailabilityResult.StructurallyAvailable, result.candidates[0].availabilityResult)
        assertTrue(result.candidates[1].availabilityResult is MarketDataCalculationAvailabilityResult.StructurallyUnavailable)
    }

    @Test
    fun candidateDuplicatesRemainDuplicated() {
        val candidate = candidate(bid = 1.0, ask = 1.2)

        val result = candidatesResult(MarketDataCalculationType.MID, listOf(candidate, candidate))

        assertEquals(2, result.candidates.size)
        assertSame(candidate, result.candidates[0].candidateWithDataQuality)
        assertSame(candidate, result.candidates[1].candidateWithDataQuality)
    }

    @Test
    fun equalIsinsAreEvaluatedAsSeparateEntries() {
        val available = candidate(productIsin = "SYNTH01", bid = 1.0, ask = 1.2)
        val unavailable = candidate(productIsin = "SYNTH01", bid = null, ask = 1.2)

        val result = candidatesResult(MarketDataCalculationType.SPREAD, listOf(available, unavailable))

        assertEquals(2, result.candidates.size)
        assertSame(MarketDataCalculationAvailabilityResult.StructurallyAvailable, result.candidates[0].availabilityResult)
        assertEquals(
            listOf(MarketDataCalculationAvailabilityError.MISSING_BID),
            unavailable(result.candidates[1].availabilityResult).errors
        )
    }

    @Test
    fun originalDomainAssessmentAndFindingInstancesRemainUnchanged() {
        val finding = DataQualityFinding(
            category = DataQualityCategory.MISSING_REQUIRED_DATA,
            severity = DataQualitySeverity.WARNING,
            code = DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID,
            component = DataQualityComponent.PRODUCT_MARKET_DATA
        )
        val assessment = DataQualityAssessment.warning(findings = listOf(finding))
        val specification = specification("SYNTH01")
        val snapshot = snapshot(specification)
        val marketData = marketData("SYNTH01", bid = 1.0, ask = 1.2)
        val candidateWithMarketData = KnockoutProductCandidateWithMarketData(snapshot, marketData)
        val candidate = KnockoutProductCandidateWithDataQuality(candidateWithMarketData, assessment)

        val evaluated = candidatesResult(MarketDataCalculationType.MID, listOf(candidate))
            .candidates.single().candidateWithDataQuality

        assertSame(candidate, evaluated)
        assertSame(candidateWithMarketData, evaluated.candidateWithMarketData)
        assertSame(snapshot, evaluated.candidateWithMarketData.specificationSnapshot)
        assertSame(specification, evaluated.candidateWithMarketData.specificationSnapshot.specification)
        assertSame(marketData, evaluated.candidateWithMarketData.marketData)
        assertSame(assessment, evaluated.dataQualityAssessment)
        assertEquals(listOf(finding), evaluated.dataQualityAssessment.findings)
    }

    @Test
    fun serviceDoesNotMutateRequestOrInputList() {
        val first = candidate(productIsin = "SYNTH01", bid = 1.0, ask = 1.2)
        val second = candidate(productIsin = "SYNTH02", bid = null, ask = 1.2)
        val input = mutableListOf(first, second, first)
        val original = input.toList()
        val request = request(input, MarketDataCalculationType.SPREAD)

        service.execute(request)

        assertSame(input, request.candidates)
        assertEquals(original, input)
        assertEquals(MarketDataCalculationType.SPREAD, request.calculationType)
    }

    @Test
    fun resultContainsNoFreshnessRankingOrCalculationOutput() {
        val candidate = candidate(bid = 1.0, ask = 1.2)
        val request = request(listOf(candidate), MarketDataCalculationType.MID)

        val result = candidatesResult(request.calculationType, request.candidates).candidates.single()

        assertSame(candidate, result.candidateWithDataQuality)
        assertSame(MarketDataCalculationAvailabilityResult.StructurallyAvailable, result.availabilityResult)
    }

    private fun singleAvailability(
        calculationType: MarketDataCalculationType,
        candidate: KnockoutProductCandidateWithDataQuality
    ): MarketDataCalculationAvailabilityResult {
        return candidatesResult(calculationType, listOf(candidate)).candidates.single().availabilityResult
    }

    private fun candidatesResult(
        calculationType: MarketDataCalculationType,
        candidates: List<KnockoutProductCandidateWithDataQuality>
    ): KnockoutProductCandidateCalculationAvailabilityResult.CandidatesWithCalculationAvailability {
        val result = service.execute(request(candidates, calculationType))
        assertTrue(
            result is KnockoutProductCandidateCalculationAvailabilityResult
                .CandidatesWithCalculationAvailability
        )
        return result as KnockoutProductCandidateCalculationAvailabilityResult
            .CandidatesWithCalculationAvailability
    }

    private fun request(
        candidates: List<KnockoutProductCandidateWithDataQuality>,
        calculationType: MarketDataCalculationType
    ): KnockoutProductCandidateCalculationAvailabilityRequest {
        return KnockoutProductCandidateCalculationAvailabilityRequest(candidates, calculationType)
    }

    private fun unavailable(
        result: MarketDataCalculationAvailabilityResult
    ): MarketDataCalculationAvailabilityResult.StructurallyUnavailable {
        assertTrue(result is MarketDataCalculationAvailabilityResult.StructurallyUnavailable)
        return result as MarketDataCalculationAvailabilityResult.StructurallyUnavailable
    }

    private fun candidate(
        productIsin: String = "SYNTH01",
        bid: Double? = 1.0,
        ask: Double? = 1.2
    ): KnockoutProductCandidateWithDataQuality {
        return KnockoutProductCandidateWithDataQuality(
            candidateWithMarketData = KnockoutProductCandidateWithMarketData(
                specificationSnapshot = snapshot(specification(productIsin)),
                marketData = marketData(productIsin, bid, ask)
            ),
            dataQualityAssessment = DataQualityAssessment.passed()
        )
    }

    private fun specification(productIsin: String): KnockoutProductSpecification {
        return KnockoutProductSpecification(
            productIsin = productIsin,
            productWkn = "SYN001",
            issuerId = "issuer-synthetic",
            underlyingId = "underlying-synthetic",
            direction = TradeDirection.LONG,
            basePrice = 100.0,
            knockoutBarrier = 95.0,
            ratio = 0.1,
            underlyingCurrency = "EUR",
            productCurrency = "EUR"
        )
    }

    private fun snapshot(
        specification: KnockoutProductSpecification
    ): KnockoutProductSpecificationSnapshot {
        return KnockoutProductSpecificationSnapshot(
            specification = specification,
            sourceId = "snapshot-synthetic",
            retrievedAtEpochMillis = 1_000L,
            sourceTimestampEpochMillis = 900L
        )
    }

    private fun marketData(
        productIsin: String,
        bid: Double?,
        ask: Double?
    ): KnockoutProductMarketData {
        return KnockoutProductMarketData(
            productIsin = productIsin,
            bid = bid,
            ask = ask,
            bidTimestampEpochMillis = 1_100L,
            askTimestampEpochMillis = 1_200L,
            currency = "EUR",
            sourceId = "market-synthetic"
        )
    }
}
