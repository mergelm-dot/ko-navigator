package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityError
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult
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
import de.konavigator.app.domain.source.MarketDataSourceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateSourceGateTest {
    private val gate = KnockoutProductCandidateSourceGate()

    @Test fun allowedCandidateIsAvailableForCalculation() { val c = candidate(); val r = allowedResult(listOf(c)); assertEquals(listOf(c), r.allowedCandidates); assertTrue(r.blockedCandidates.isEmpty()) }
    @Test fun blockedCandidateReturnsNoSourceAllowedCandidates() { val source = blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED); val c = candidate(source = source); val r = gate.filter(request(listOf(c))) as KnockoutProductCandidateSourceGateResult.NoSourceAllowedCandidates; assertSame(c, r.blockedCandidates.single()); assertSame(source, r.blockedCandidates.single().sourceResult) }
    @Test fun emptyInputReturnsNoInputCandidates() { assertSame(KnockoutProductCandidateSourceGateResult.NoInputCandidates, gate.filter(request(emptyList()))) }
    @Test fun allowedAndBlockedCandidatesArePartitioned() { val a = candidate(); val b = candidate(source = blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED)); val c = candidate("C"); val r = allowedResult(listOf(a,b,c)); assertEquals(listOf(a,c), r.allowedCandidates); assertEquals(listOf(b), r.blockedCandidates); assertEquals(3, r.allowedCandidates.size + r.blockedCandidates.size); assertFalse(r.allowedCandidates.any { it === b }) }
    @Test fun allAllowedCandidatesProduceEmptyBlockedList() { val a=candidate("A"); val b=candidate("B"); val r=allowedResult(listOf(a,b)); assertEquals(listOf(a,b),r.allowedCandidates); assertTrue(r.blockedCandidates.isEmpty()) }
    @Test fun allBlockedCandidatesRemainInNoAllowedResult() { val a=candidate("A",source=blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED)); val b=candidate("B",source=blocked(MarketDataSourceError.CALCULATION_TYPE_NOT_SUPPORTED)); val r=gate.filter(request(listOf(a,b))) as KnockoutProductCandidateSourceGateResult.NoSourceAllowedCandidates; assertEquals(listOf(a,b),r.blockedCandidates) }
    @Test fun allowedCandidateOrderIsPreserved() { val a=candidate("A"); val b=candidate("B",source=blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED)); val c=candidate("C"); assertEquals(listOf(a,c),allowedResult(listOf(a,b,c)).allowedCandidates) }
    @Test fun blockedCandidateOrderIsPreserved() { val a=candidate("A",source=blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED)); val b=candidate("B"); val c=candidate("C",source=blocked(MarketDataSourceError.CALCULATION_TYPE_NOT_SUPPORTED)); assertEquals(listOf(a,c),allowedResult(listOf(a,b,c)).blockedCandidates) }
    @Test fun duplicateAllowedCandidateRemainsDuplicated() { val c=candidate(); val r=allowedResult(listOf(c,c)); assertEquals(2,r.allowedCandidates.size); assertSame(c,r.allowedCandidates[0]); assertSame(c,r.allowedCandidates[1]) }
    @Test fun duplicateBlockedCandidateRemainsDuplicated() { val c=candidate(source=blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED)); val r=gate.filter(request(listOf(c,c))) as KnockoutProductCandidateSourceGateResult.NoSourceAllowedCandidates; assertEquals(2,r.blockedCandidates.size); assertSame(c,r.blockedCandidates[0]); assertSame(c,r.blockedCandidates[1]) }
    @Test fun equalIsinsRemainSeparateCandidateEntries() { val a=candidate("DE000SYNTH01"); val b=candidate("DE000SYNTH01",source=blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED)); val r=allowedResult(listOf(a,b)); assertSame(a,r.allowedCandidates.single()); assertSame(b,r.blockedCandidates.single()) }
    @Test fun differentExactIsinSpellingsRemainSeparate() { val a=candidate("DE000SYNTH01"); val b=candidate("de000synth01"); val c=candidate(" DE000SYNTH01 "); val r=allowedResult(listOf(a,b,c)); assertEquals(listOf("DE000SYNTH01","de000synth01"," DE000SYNTH01 "),r.allowedCandidates.map { isin(it) }) }
    @Test fun equalSourceIdsRemainSeparateCandidateEntries() { val a=candidate("A",sourceId="SYNTH_SOURCE"); val b=candidate("B",sourceId="SYNTH_SOURCE"); val r=allowedResult(listOf(a,b)); assertEquals(2,r.allowedCandidates.size); assertSame(a,r.allowedCandidates[0]); assertSame(b,r.allowedCandidates[1]) }
    @Test fun differentSourceIdsRemainSeparateCandidateEntries() { val a=candidate("A",sourceId="SOURCE_A"); val b=candidate("B",sourceId="SOURCE_B",source=blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED)); val r=allowedResult(listOf(a,b)); assertEquals(listOf("SOURCE_A"),r.allowedCandidates.map { sourceId(it) }); assertEquals(listOf("SOURCE_B"),r.blockedCandidates.map { sourceId(it) }) }
    @Test fun differentProductsFromSameIssuerRemainSeparate() { val a=candidate("A",issuer="issuer"); val b=candidate("B",issuer="issuer"); assertEquals(listOf(a,b),allowedResult(listOf(a,b)).allowedCandidates) }
    @Test fun resultIsNotLimitedToThreeCandidates() { val candidates=(1..4).map { candidate("S$it") }; assertEquals(candidates,allowedResult(candidates).allowedCandidates) }
    @Test fun candidateAndPreviousResultInstancesRemainUnchanged() { val source=blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED); val candidate=candidate(source=source); val preserved=(gate.filter(request(listOf(candidate))) as KnockoutProductCandidateSourceGateResult.NoSourceAllowedCandidates).blockedCandidates.single(); val freshness=preserved.candidateWithFreshness; val availability=freshness.candidateWithCalculationAvailability; val quality=availability.candidateWithDataQuality; val market=quality.candidateWithMarketData; assertSame(candidate,preserved); assertSame(freshness,preserved.candidateWithFreshness); assertSame(availability,freshness.candidateWithCalculationAvailability); assertSame(quality,availability.candidateWithDataQuality); assertSame(market,quality.candidateWithMarketData); assertSame(market.specificationSnapshot,quality.candidateWithMarketData.specificationSnapshot); assertSame(market.specificationSnapshot.specification,market.specificationSnapshot.specification); assertSame(market.marketData,market.marketData); assertSame(quality.dataQualityAssessment,quality.dataQualityAssessment); assertSame(availability.availabilityResult,availability.availabilityResult); assertSame(freshness.freshnessResult,freshness.freshnessResult); assertSame(source,preserved.sourceResult) }
    @Test fun previousErrorsAndFindingsRemainCompleteOrderedAndUnchanged() { val first=finding(DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID); val second=finding(DataQualityFindingCode.MARKET_DATA_MISSING_BID_TIMESTAMP); val assessment=DataQualityAssessment.warning(listOf(first,second)); val availability=MarketDataCalculationAvailabilityResult.StructurallyUnavailable(listOf(MarketDataCalculationAvailabilityError.MISSING_BID,MarketDataCalculationAvailabilityError.MISSING_ASK)); val freshness=MarketDataFreshnessResult.NotFresh(listOf(MarketDataFreshnessError.STALE_BID,MarketDataFreshnessError.STALE_ASK)); val candidate=candidate(assessment=assessment,availability=availability,freshness=freshness,source=blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED)); val preserved=(gate.filter(request(listOf(candidate))) as KnockoutProductCandidateSourceGateResult.NoSourceAllowedCandidates).blockedCandidates.single(); val resultAvailability=preserved.candidateWithFreshness.candidateWithCalculationAvailability; assertSame(assessment,resultAvailability.candidateWithDataQuality.dataQualityAssessment); assertEquals(listOf(first,second),assessment.findings); assertSame(availability,resultAvailability.availabilityResult); assertEquals(availability.errors,(resultAvailability.availabilityResult as MarketDataCalculationAvailabilityResult.StructurallyUnavailable).errors); assertSame(freshness,preserved.candidateWithFreshness.freshnessResult); assertEquals(freshness.errors,(preserved.candidateWithFreshness.freshnessResult as MarketDataFreshnessResult.NotFresh).errors) }
    @Test fun sourceErrorsRemainUnchangedAndInputIsNotMutated() { val missing=blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED); val unsupported=blocked(MarketDataSourceError.CALCULATION_TYPE_NOT_SUPPORTED); val a=candidate("A",sourceId="A",source=missing); val b=candidate("B",sourceId="B",source=unsupported); val input=mutableListOf(a,b,a); val original=input.toList(); val request=request(input); val result=gate.filter(request) as KnockoutProductCandidateSourceGateResult.NoSourceAllowedCandidates; assertEquals(listOf(missing,unsupported,missing),result.blockedCandidates.map { it.sourceResult }); assertSame(input,request.candidates); assertEquals(original,input); assertEquals(listOf("A","B","A"),input.map { sourceId(it) }) }
    @Test fun repeatedFilterCallsDoNotShareStateAndContainNoLaterStageOutput() { val a=candidate("A"); val b=candidate("B",source=blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED)); val request=request(listOf(a,b)); val first=gate.filter(request); val second=gate.filter(request); assertEquals(first,second); assertSame(a,allowedResult(first).allowedCandidates.single()); assertSame(b,allowedResult(second).blockedCandidates.single()) }

    private fun allowedResult(candidates: List<KnockoutProductCandidateWithSourceEvaluation>) = allowedResult(gate.filter(request(candidates)))
    private fun allowedResult(result: KnockoutProductCandidateSourceGateResult): KnockoutProductCandidateSourceGateResult.SourceAllowedCandidates { assertTrue(result is KnockoutProductCandidateSourceGateResult.SourceAllowedCandidates); return result as KnockoutProductCandidateSourceGateResult.SourceAllowedCandidates }
    private fun request(candidates: List<KnockoutProductCandidateWithSourceEvaluation>) = KnockoutProductCandidateSourceGateRequest(candidates)
    private fun blocked(error: MarketDataSourceError) = MarketDataSourceResult.Blocked(error)
    private fun isin(candidate: KnockoutProductCandidateWithSourceEvaluation) = candidate.candidateWithFreshness.candidateWithCalculationAvailability.candidateWithDataQuality.candidateWithMarketData.specificationSnapshot.specification.productIsin
    private fun sourceId(candidate: KnockoutProductCandidateWithSourceEvaluation) = candidate.candidateWithFreshness.candidateWithCalculationAvailability.candidateWithDataQuality.candidateWithMarketData.marketData.sourceId
    private fun finding(code: DataQualityFindingCode) = DataQualityFinding(DataQualityCategory.MISSING_REQUIRED_DATA,DataQualitySeverity.WARNING,code,DataQualityComponent.PRODUCT_MARKET_DATA)
    private fun candidate(isin:String="DE000SYNTH01", issuer:String="issuer", sourceId:String="SYNTH_SOURCE", assessment:DataQualityAssessment=DataQualityAssessment.passed(), availability:MarketDataCalculationAvailabilityResult=MarketDataCalculationAvailabilityResult.StructurallyAvailable, freshness:MarketDataFreshnessResult=MarketDataFreshnessResult.Fresh, source:MarketDataSourceResult=MarketDataSourceResult.Allowed): KnockoutProductCandidateWithSourceEvaluation {
        val specification=KnockoutProductSpecification(isin,"SYN001",issuer,"underlying",TradeDirection.LONG,100.0,95.0,0.1,"EUR","EUR")
        val snapshot=KnockoutProductSpecificationSnapshot(specification,"snapshot",1000L,900L)
        val marketData=KnockoutProductMarketData(isin,1.0,1.2,950L,950L,"EUR",sourceId)
        val marketCandidate=KnockoutProductCandidateWithMarketData(snapshot,marketData)
        val qualityCandidate=KnockoutProductCandidateWithDataQuality(marketCandidate,assessment)
        val availabilityCandidate=KnockoutProductCandidateWithCalculationAvailability(qualityCandidate,availability)
        return KnockoutProductCandidateWithSourceEvaluation(KnockoutProductCandidateWithFreshness(availabilityCandidate,freshness),source)
    }
}
