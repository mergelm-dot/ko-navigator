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
import de.konavigator.app.domain.freshness.MarketDataFreshnessResult
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.source.MarketDataSourceError
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceResult
import de.konavigator.app.domain.source.MarketDataSourceRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateSourceEvaluationApplicationServiceTest {

    @Test fun configuredSourceAllowsPurchasePrice() = assertAllowed(MarketDataCalculationType.PURCHASE_PRICE)
    @Test fun configuredSourceAllowsSalePrice() = assertAllowed(MarketDataCalculationType.SALE_PRICE)
    @Test fun configuredSourceAllowsSpread() = assertAllowed(MarketDataCalculationType.SPREAD)
    @Test fun configuredSourceAllowsMid() = assertAllowed(MarketDataCalculationType.MID)

    @Test fun unconfiguredSourceIsBlocked() {
        assertBlocked(evaluate(configured(MarketDataCalculationType.PURCHASE_PRICE), MarketDataCalculationType.PURCHASE_PRICE, candidate(sourceId = "SYNTH_SOURCE_B")), MarketDataSourceError.SOURCE_NOT_CONFIGURED)
    }

    @Test fun configuredSourceWithoutRequestedTypeIsBlocked() {
        assertBlocked(evaluate(configured(MarketDataCalculationType.PURCHASE_PRICE), MarketDataCalculationType.SALE_PRICE, candidate()), MarketDataSourceError.CALCULATION_TYPE_NOT_SUPPORTED)
    }

    @Test fun configuredSourceWithEmptyCapabilitiesIsBlocked() {
        assertBlocked(evaluate(policy("SYNTH_SOURCE_A", emptySet()), MarketDataCalculationType.MID, candidate()), MarketDataSourceError.CALCULATION_TYPE_NOT_SUPPORTED)
    }

    @Test fun sourceIdComparisonIsCaseSensitive() {
        assertBlocked(evaluate(configured(MarketDataCalculationType.MID), MarketDataCalculationType.MID, candidate(sourceId = "synth_source_a")), MarketDataSourceError.SOURCE_NOT_CONFIGURED)
    }

    @Test fun sourceIdWhitespaceIsNotNormalized() {
        val result = evaluated(configured(MarketDataCalculationType.MID), MarketDataCalculationType.MID, listOf(candidate(sourceId = "SYNTH_SOURCE_A"), candidate(sourceId = " SYNTH_SOURCE_A "), candidate(sourceId = "SYNTH_SOURCE_A ")))
        assertSame(MarketDataSourceResult.Allowed, result.candidates[0].sourceResult)
        assertBlocked(result.candidates[1].sourceResult, MarketDataSourceError.SOURCE_NOT_CONFIGURED)
        assertBlocked(result.candidates[2].sourceResult, MarketDataSourceError.SOURCE_NOT_CONFIGURED)
    }

    @Test fun emptyInputReturnsNoInputCandidates() {
        val result = service(configured(MarketDataCalculationType.MID)).execute(request(emptyList(), MarketDataCalculationType.MID))
        assertSame(KnockoutProductCandidateSourceEvaluationResult.NoInputCandidates, result)
        assertFalse(result is KnockoutProductCandidateSourceEvaluationResult.CandidatesWithSourceEvaluation)
    }

    @Test fun inputOrderIsPreserved() {
        val first = candidate(sourceId = "SYNTH_SOURCE_A"); val second = candidate(sourceId = "SYNTH_SOURCE_B"); val third = candidate(sourceId = "SYNTH_SOURCE_A")
        val result = evaluated(configured(MarketDataCalculationType.MID), MarketDataCalculationType.MID, listOf(first, second, third))
        assertEquals(listOf(first, second, third), result.candidates.map { it.candidateWithFreshness })
    }

    @Test fun allowedAndBlockedCandidatesRemainTogether() {
        val allowed = candidate(sourceId = "SYNTH_SOURCE_A"); val blocked = candidate(sourceId = "SYNTH_SOURCE_B")
        val result = evaluated(configured(MarketDataCalculationType.MID), MarketDataCalculationType.MID, listOf(allowed, blocked))
        assertEquals(2, result.candidates.size); assertSame(MarketDataSourceResult.Allowed, result.candidates[0].sourceResult); assertBlocked(result.candidates[1].sourceResult, MarketDataSourceError.SOURCE_NOT_CONFIGURED)
    }

    @Test fun candidateDuplicatesRemainDuplicated() {
        val candidate = candidate(); val result = evaluated(configured(MarketDataCalculationType.MID), MarketDataCalculationType.MID, listOf(candidate, candidate))
        assertEquals(2, result.candidates.size); assertSame(candidate, result.candidates[0].candidateWithFreshness); assertSame(candidate, result.candidates[1].candidateWithFreshness)
    }

    @Test fun equalSourceIdsAreEvaluatedAsSeparateEntries() {
        val first = candidate(); val second = candidate(productIsin = "SYNTH02")
        val result = evaluated(configured(MarketDataCalculationType.MID), MarketDataCalculationType.MID, listOf(first, second))
        assertEquals(2, result.candidates.size); assertSame(first, result.candidates[0].candidateWithFreshness); assertSame(second, result.candidates[1].candidateWithFreshness)
    }

    @Test fun equalIsinsWithDifferentSourcesRemainSeparate() {
        val allowed = candidate(sourceId = "SYNTH_SOURCE_A"); val blocked = candidate(sourceId = "SYNTH_SOURCE_B")
        val result = evaluated(configured(MarketDataCalculationType.MID), MarketDataCalculationType.MID, listOf(allowed, blocked))
        assertEquals(2, result.candidates.size); assertSame(MarketDataSourceResult.Allowed, result.candidates[0].sourceResult); assertBlocked(result.candidates[1].sourceResult, MarketDataSourceError.SOURCE_NOT_CONFIGURED)
    }

    @Test fun differentProductsFromSameIssuerRemainSeparate() {
        val first = candidate(productIsin = "SYNTH01", issuerId = "issuer-synthetic"); val second = candidate(productIsin = "SYNTH02", issuerId = "issuer-synthetic")
        val result = evaluated(configured(MarketDataCalculationType.MID), MarketDataCalculationType.MID, listOf(first, second))
        assertEquals(2, result.candidates.size); assertEquals(listOf("SYNTH01", "SYNTH02"), result.candidates.map { it.candidateWithFreshness.candidateWithCalculationAvailability.candidateWithDataQuality.candidateWithMarketData.specificationSnapshot.specification.productIsin })
    }

    @Test fun resultIsNotLimitedToThreeCandidates() {
        val input = (1..4).map { candidate(productIsin = "SYNTH0$it") }
        assertEquals(4, evaluated(configured(MarketDataCalculationType.MID), MarketDataCalculationType.MID, input).candidates.size)
    }

    @Test fun originalInstancesAndPreviousAssessmentsRemainUnchanged() {
        val finding = DataQualityFinding(DataQualityCategory.MISSING_REQUIRED_DATA, DataQualitySeverity.WARNING, DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID, DataQualityComponent.PRODUCT_MARKET_DATA)
        val assessment = DataQualityAssessment.warning(listOf(finding))
        val availability = MarketDataCalculationAvailabilityResult.StructurallyUnavailable(listOf(MarketDataCalculationAvailabilityError.MISSING_BID))
        val freshness = MarketDataFreshnessResult.NotFresh(listOf(MarketDataFreshnessError.STALE_ASK))
        val input = candidate(assessment = assessment, availability = availability, freshness = freshness)
        val paired = evaluated(configured(MarketDataCalculationType.MID), MarketDataCalculationType.MID, listOf(input)).candidates.single()
        val availabilityCandidate = paired.candidateWithFreshness.candidateWithCalculationAvailability
        val dataQualityCandidate = availabilityCandidate.candidateWithDataQuality
        val marketDataCandidate = dataQualityCandidate.candidateWithMarketData
        assertSame(input, paired.candidateWithFreshness); assertSame(availabilityCandidate, input.candidateWithCalculationAvailability); assertSame(dataQualityCandidate, availabilityCandidate.candidateWithDataQuality); assertSame(marketDataCandidate, dataQualityCandidate.candidateWithMarketData)
        assertSame(marketDataCandidate.specificationSnapshot, marketDataCandidate.specificationSnapshot); assertSame(marketDataCandidate.specificationSnapshot.specification, marketDataCandidate.specificationSnapshot.specification); assertSame(marketDataCandidate.marketData, marketDataCandidate.marketData)
        assertSame(assessment, dataQualityCandidate.dataQualityAssessment); assertEquals(listOf(finding), assessment.findings); assertSame(availability, availabilityCandidate.availabilityResult); assertEquals(listOf(MarketDataCalculationAvailabilityError.MISSING_BID), availability.errors); assertSame(freshness, input.freshnessResult); assertEquals(listOf(MarketDataFreshnessError.STALE_ASK), freshness.errors)
    }

    @Test fun serviceDoesNotMutateRequestOrInputList() {
        val allowed = candidate(); val blocked = candidate(sourceId = "SYNTH_SOURCE_B"); val input = mutableListOf(allowed, blocked, allowed); val original = input.toList()
        val request = request(input, MarketDataCalculationType.MID); evaluated(configured(MarketDataCalculationType.MID), request.calculationType, request.candidates)
        assertSame(input, request.candidates); assertEquals(original, input); assertEquals(MarketDataCalculationType.MID, request.calculationType)
        assertEquals(listOf("SYNTH_SOURCE_A", "SYNTH_SOURCE_B", "SYNTH_SOURCE_A"), input.map { sourceId(it) })
    }

    @Test fun resultMatchesDirectPolicyAndContainsNoLaterStageOutput() {
        val policy = configured(MarketDataCalculationType.PURCHASE_PRICE); val sourceCandidate = candidate(sourceId = "SYNTH_SOURCE_B")
        val request = request(listOf(sourceCandidate), MarketDataCalculationType.PURCHASE_PRICE)
        val direct = policy.evaluate(request.calculationType, sourceId(sourceCandidate)); val service = service(policy)
        val first = service.execute(request) as KnockoutProductCandidateSourceEvaluationResult.CandidatesWithSourceEvaluation
        val second = service.execute(request) as KnockoutProductCandidateSourceEvaluationResult.CandidatesWithSourceEvaluation
        assertEquals(direct, first.candidates.single().sourceResult); assertEquals(first, second); assertBlocked(first.candidates.single().sourceResult, MarketDataSourceError.SOURCE_NOT_CONFIGURED)
        assertSame(sourceCandidate, first.candidates.single().candidateWithFreshness); assertEquals(request.candidates, listOf(sourceCandidate)); assertEquals(MarketDataCalculationType.PURCHASE_PRICE, request.calculationType)
    }

    private fun assertAllowed(type: MarketDataCalculationType) = assertSame(MarketDataSourceResult.Allowed, evaluate(configured(type), type, candidate()))
    private fun configured(type: MarketDataCalculationType) = policy("SYNTH_SOURCE_A", setOf(type))
    private fun policy(sourceId: String, types: Set<MarketDataCalculationType>) = MarketDataSourcePolicy(MarketDataSourcePolicyConfig(listOf(MarketDataSourceRule(sourceId, types))))
    private fun service(policy: MarketDataSourcePolicy) = KnockoutProductCandidateSourceEvaluationApplicationService(policy)
    private fun request(candidates: List<KnockoutProductCandidateWithFreshness>, type: MarketDataCalculationType) = KnockoutProductCandidateSourceEvaluationRequest(candidates, type)
    private fun evaluate(policy: MarketDataSourcePolicy, type: MarketDataCalculationType, candidate: KnockoutProductCandidateWithFreshness) = evaluated(policy, type, listOf(candidate)).candidates.single().sourceResult
    private fun evaluated(policy: MarketDataSourcePolicy, type: MarketDataCalculationType, candidates: List<KnockoutProductCandidateWithFreshness>): KnockoutProductCandidateSourceEvaluationResult.CandidatesWithSourceEvaluation {
        val result = service(policy).execute(request(candidates, type)); assertTrue(result is KnockoutProductCandidateSourceEvaluationResult.CandidatesWithSourceEvaluation)
        return result as KnockoutProductCandidateSourceEvaluationResult.CandidatesWithSourceEvaluation
    }
    private fun assertBlocked(result: MarketDataSourceResult, error: MarketDataSourceError) { assertTrue(result is MarketDataSourceResult.Blocked); assertEquals(error, (result as MarketDataSourceResult.Blocked).error) }
    private fun sourceId(candidate: KnockoutProductCandidateWithFreshness) = candidate.candidateWithCalculationAvailability.candidateWithDataQuality.candidateWithMarketData.marketData.sourceId
    private fun candidate(productIsin: String = "SYNTH01", issuerId: String = "issuer-synthetic", sourceId: String = "SYNTH_SOURCE_A", assessment: DataQualityAssessment = DataQualityAssessment.passed(), availability: MarketDataCalculationAvailabilityResult = MarketDataCalculationAvailabilityResult.StructurallyAvailable, freshness: MarketDataFreshnessResult = MarketDataFreshnessResult.Fresh): KnockoutProductCandidateWithFreshness {
        val specification = KnockoutProductSpecification(productIsin, "SYN001", issuerId, "underlying-synthetic", TradeDirection.LONG, 100.0, 95.0, 0.1, "EUR", "EUR")
        val snapshot = KnockoutProductSpecificationSnapshot(specification, "snapshot-synthetic", 1_000L, 900L)
        val marketData = KnockoutProductMarketData(productIsin, 1.0, 1.2, 950L, 950L, "EUR", sourceId)
        return KnockoutProductCandidateWithFreshness(KnockoutProductCandidateWithCalculationAvailability(KnockoutProductCandidateWithDataQuality(KnockoutProductCandidateWithMarketData(snapshot, marketData), assessment), availability), freshness)
    }
}
