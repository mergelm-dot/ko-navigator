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
import de.konavigator.app.domain.freshness.MarketDataFreshnessError
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessResult
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateFreshnessApplicationServiceTest {

    private val evaluationTime = 1_000L
    private val freshnessPolicy = MarketDataFreshnessPolicy(
        MarketDataFreshnessThresholds(100L, 100L, 20L, 10L)
    )
    private val service = KnockoutProductCandidateFreshnessApplicationService(freshnessPolicy)

    @Test fun purchasePriceFreshAskProducesFresh() {
        assertFresh(single(MarketDataCalculationType.PURCHASE_PRICE, candidate(askTimestamp = 950L)))
    }

    @Test fun purchasePriceStaleAskProducesNotFresh() {
        assertErrors(single(MarketDataCalculationType.PURCHASE_PRICE, candidate(askTimestamp = 899L)), MarketDataFreshnessError.STALE_ASK)
    }

    @Test fun purchasePriceFutureAskProducesNotFresh() {
        assertErrors(single(MarketDataCalculationType.PURCHASE_PRICE, candidate(askTimestamp = 1_011L)), MarketDataFreshnessError.ASK_TIMESTAMP_IN_FUTURE)
    }

    @Test fun purchasePriceIgnoresBidTimestamp() {
        assertFresh(single(MarketDataCalculationType.PURCHASE_PRICE, candidate(bidTimestamp = 0L, askTimestamp = 950L)))
    }

    @Test fun salePriceFreshBidProducesFresh() {
        assertFresh(single(MarketDataCalculationType.SALE_PRICE, candidate(bidTimestamp = 950L)))
    }

    @Test fun salePriceStaleBidProducesNotFresh() {
        assertErrors(single(MarketDataCalculationType.SALE_PRICE, candidate(bidTimestamp = 899L)), MarketDataFreshnessError.STALE_BID)
    }

    @Test fun salePriceFutureBidProducesNotFresh() {
        assertErrors(single(MarketDataCalculationType.SALE_PRICE, candidate(bidTimestamp = 1_011L)), MarketDataFreshnessError.BID_TIMESTAMP_IN_FUTURE)
    }

    @Test fun salePriceIgnoresAskTimestamp() {
        assertFresh(single(MarketDataCalculationType.SALE_PRICE, candidate(bidTimestamp = 950L, askTimestamp = 0L)))
    }

    @Test fun spreadFreshCloseTimestampsProducesFresh() {
        assertFresh(single(MarketDataCalculationType.SPREAD, candidate(bidTimestamp = 950L, askTimestamp = 960L)))
    }

    @Test fun spreadNotFreshErrorsRemainCompleteAndOrdered() {
        assertErrors(single(MarketDataCalculationType.SPREAD, candidate(bidTimestamp = 899L, askTimestamp = 895L)), MarketDataFreshnessError.STALE_BID, MarketDataFreshnessError.STALE_ASK)
    }

    @Test fun spreadTimestampDifferenceErrorIsPreserved() {
        assertErrors(single(MarketDataCalculationType.SPREAD, candidate(bidTimestamp = 950L, askTimestamp = 980L)), MarketDataFreshnessError.BID_ASK_TIMESTAMPS_TOO_FAR_APART)
    }

    @Test fun midUsesBothTimestampSides() {
        assertErrors(single(MarketDataCalculationType.MID, candidate(bidTimestamp = 899L, askTimestamp = 910L)), MarketDataFreshnessError.STALE_BID)
    }

    @Test fun inclusiveFreshnessBoundaryRemainsFresh() {
        assertFresh(single(MarketDataCalculationType.PURCHASE_PRICE, candidate(askTimestamp = 900L)))
    }

    @Test fun emptyInputReturnsNoInputCandidates() {
        val result = service.execute(request(emptyList(), MarketDataCalculationType.MID))
        assertSame(KnockoutProductCandidateFreshnessResult.NoInputCandidates, result)
        assertFalse(result is KnockoutProductCandidateFreshnessResult.CandidatesWithFreshness)
    }

    @Test fun inputOrderIsPreserved() {
        val first = candidate("SYNTH03", askTimestamp = 950L)
        val second = candidate("SYNTH01", askTimestamp = 899L)
        val third = candidate("SYNTH02", askTimestamp = 960L)
        val result = candidates(MarketDataCalculationType.PURCHASE_PRICE, listOf(first, second, third))
        assertEquals(listOf(first, second, third), result.candidates.map { it.candidateWithCalculationAvailability })
    }

    @Test fun freshAndNotFreshCandidatesRemainTogether() {
        val fresh = candidate("SYNTH01", askTimestamp = 950L)
        val stale = candidate("SYNTH02", askTimestamp = 899L)
        val result = candidates(MarketDataCalculationType.PURCHASE_PRICE, listOf(fresh, stale))
        assertEquals(2, result.candidates.size)
        assertFresh(result.candidates[0].freshnessResult)
        assertErrors(result.candidates[1].freshnessResult, MarketDataFreshnessError.STALE_ASK)
    }

    @Test fun candidateDuplicatesRemainDuplicated() {
        val candidate = candidate(askTimestamp = 950L)
        val result = candidates(MarketDataCalculationType.PURCHASE_PRICE, listOf(candidate, candidate))
        assertEquals(2, result.candidates.size)
        assertSame(candidate, result.candidates[0].candidateWithCalculationAvailability)
        assertSame(candidate, result.candidates[1].candidateWithCalculationAvailability)
    }

    @Test fun equalIsinsAreEvaluatedAsSeparateEntries() {
        val fresh = candidate("SYNTH01", askTimestamp = 950L)
        val stale = candidate("SYNTH01", askTimestamp = 899L)
        val result = candidates(MarketDataCalculationType.PURCHASE_PRICE, listOf(fresh, stale))
        assertEquals(2, result.candidates.size)
        assertFresh(result.candidates[0].freshnessResult)
        assertErrors(result.candidates[1].freshnessResult, MarketDataFreshnessError.STALE_ASK)
    }

    @Test fun originalInstancesAndInputListRemainUnchanged() {
        val finding = DataQualityFinding(DataQualityCategory.MISSING_REQUIRED_DATA, DataQualitySeverity.WARNING, DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID, DataQualityComponent.PRODUCT_MARKET_DATA)
        val assessment = DataQualityAssessment.warning(findings = listOf(finding))
        val availability = MarketDataCalculationAvailabilityResult.StructurallyUnavailable(listOf(MarketDataCalculationAvailabilityError.MISSING_BID))
        val first = candidate("SYNTH01", bidTimestamp = 950L, askTimestamp = 950L, assessment = assessment, availability = availability)
        val second = candidate("SYNTH02", askTimestamp = 899L)
        val input = mutableListOf(first, second, first)
        val original = input.toList()
        val request = request(input, MarketDataCalculationType.PURCHASE_PRICE)
        val result = candidates(request.calculationType, request.candidates)
        val preserved = result.candidates.first().candidateWithCalculationAvailability
        assertSame(input, request.candidates); assertEquals(original, input)
        assertEquals(MarketDataCalculationType.PURCHASE_PRICE, request.calculationType); assertEquals(evaluationTime, request.evaluationTimeEpochMillis)
        assertSame(first, preserved); assertSame(assessment, preserved.candidateWithDataQuality.dataQualityAssessment)
        assertEquals(listOf(finding), preserved.candidateWithDataQuality.dataQualityAssessment.findings)
        assertSame(availability, preserved.availabilityResult)
        assertEquals(listOf(MarketDataCalculationAvailabilityError.MISSING_BID), (preserved.availabilityResult as MarketDataCalculationAvailabilityResult.StructurallyUnavailable).errors)
        assertSame(first.candidateWithDataQuality.candidateWithMarketData, preserved.candidateWithDataQuality.candidateWithMarketData)
        assertSame(first.candidateWithDataQuality.candidateWithMarketData.specificationSnapshot, preserved.candidateWithDataQuality.candidateWithMarketData.specificationSnapshot)
        assertSame(first.candidateWithDataQuality.candidateWithMarketData.marketData, preserved.candidateWithDataQuality.candidateWithMarketData.marketData)
    }

    @Test fun resultMatchesDirectPolicyAndContainsNoLaterStageOutput() {
        val candidate = candidate(askTimestamp = 899L)
        val direct = freshnessPolicy.evaluate(MarketDataCalculationType.PURCHASE_PRICE, candidate.candidateWithDataQuality.candidateWithMarketData.marketData, evaluationTime)
        val serviceResult = single(MarketDataCalculationType.PURCHASE_PRICE, candidate)
        assertEquals(direct, serviceResult)
        assertErrors(serviceResult, MarketDataFreshnessError.STALE_ASK)
    }

    private fun single(type: MarketDataCalculationType, candidate: KnockoutProductCandidateWithCalculationAvailability): MarketDataFreshnessResult = candidates(type, listOf(candidate)).candidates.single().freshnessResult
    private fun candidates(type: MarketDataCalculationType, candidates: List<KnockoutProductCandidateWithCalculationAvailability>): KnockoutProductCandidateFreshnessResult.CandidatesWithFreshness {
        val result = service.execute(request(candidates, type)); assertTrue(result is KnockoutProductCandidateFreshnessResult.CandidatesWithFreshness)
        return result as KnockoutProductCandidateFreshnessResult.CandidatesWithFreshness
    }
    private fun request(candidates: List<KnockoutProductCandidateWithCalculationAvailability>, type: MarketDataCalculationType) = KnockoutProductCandidateFreshnessRequest(candidates, type, evaluationTime)
    private fun assertFresh(result: MarketDataFreshnessResult) = assertSame(MarketDataFreshnessResult.Fresh, result)
    private fun assertErrors(result: MarketDataFreshnessResult, vararg errors: MarketDataFreshnessError) {
        assertTrue(result is MarketDataFreshnessResult.NotFresh); assertEquals(errors.toList(), (result as MarketDataFreshnessResult.NotFresh).errors)
    }
    private fun candidate(productIsin: String = "SYNTH01", bidTimestamp: Long = 950L, askTimestamp: Long = 950L, assessment: DataQualityAssessment = DataQualityAssessment.passed(), availability: MarketDataCalculationAvailabilityResult = MarketDataCalculationAvailabilityResult.StructurallyAvailable): KnockoutProductCandidateWithCalculationAvailability {
        val specification = KnockoutProductSpecification(productIsin, "SYN001", "issuer-synthetic", "underlying-synthetic", TradeDirection.LONG, 100.0, 95.0, 0.1, "EUR", "EUR")
        val snapshot = KnockoutProductSpecificationSnapshot(specification, "snapshot-synthetic", 1_000L, 900L)
        val marketData = KnockoutProductMarketData(productIsin, 1.0, 1.2, bidTimestamp, askTimestamp, "EUR", "market-synthetic")
        return KnockoutProductCandidateWithCalculationAvailability(KnockoutProductCandidateWithDataQuality(KnockoutProductCandidateWithMarketData(snapshot, marketData), assessment), availability)
    }
}
