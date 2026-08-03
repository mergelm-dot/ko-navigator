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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateFreshnessGateTest {
    private val gate = KnockoutProductCandidateFreshnessGate()

    @Test fun freshCandidateIsAvailableForSourceEvaluation() { val c = candidate("DE000SYNTH01", fresh()); val r = freshResult(listOf(c)); assertEquals(listOf(c), r.freshCandidates); assertTrue(r.notFreshCandidates.isEmpty()) }
    @Test fun notFreshCandidateReturnsNoFreshCandidates() { val f = notFresh(MarketDataFreshnessError.STALE_ASK); val c = candidate("DE000SYNTH01", f); val r = gate.filter(request(listOf(c))); assertTrue(r is KnockoutProductCandidateFreshnessGateResult.NoFreshCandidates); assertSame(c, (r as KnockoutProductCandidateFreshnessGateResult.NoFreshCandidates).notFreshCandidates.single()); assertSame(f, r.notFreshCandidates.single().freshnessResult) }
    @Test fun emptyInputReturnsNoInputCandidates() { assertSame(KnockoutProductCandidateFreshnessGateResult.NoInputCandidates, gate.filter(request(emptyList()))) }
    @Test fun freshAndNotFreshCandidatesArePartitioned() { val a = candidate("A", fresh()); val b = candidate("B", notFresh(MarketDataFreshnessError.STALE_BID)); val c = candidate("C", fresh()); val r = freshResult(listOf(a,b,c)); assertEquals(listOf(a,c),r.freshCandidates); assertEquals(listOf(b),r.notFreshCandidates); assertEquals(3,r.freshCandidates.size+r.notFreshCandidates.size); assertFalse(r.freshCandidates.any { it===b }) }
    @Test fun allFreshCandidatesProduceEmptyNotFreshList() { val a=candidate("A",fresh()); val b=candidate("B",fresh()); val r=freshResult(listOf(a,b)); assertEquals(listOf(a,b),r.freshCandidates); assertTrue(r.notFreshCandidates.isEmpty()) }
    @Test fun allNotFreshCandidatesRemainInNoFreshResult() { val a=candidate("A",notFresh(MarketDataFreshnessError.STALE_BID)); val b=candidate("B",notFresh(MarketDataFreshnessError.STALE_ASK)); val r=gate.filter(request(listOf(a,b))) as KnockoutProductCandidateFreshnessGateResult.NoFreshCandidates; assertEquals(listOf(a,b),r.notFreshCandidates) }
    @Test fun freshCandidateOrderIsPreserved() { val a=candidate("A",fresh()); val n=candidate("N",notFresh(MarketDataFreshnessError.STALE_BID)); val b=candidate("B",fresh()); assertEquals(listOf(a,b),freshResult(listOf(a,n,b)).freshCandidates) }
    @Test fun notFreshCandidateOrderIsPreserved() { val a=candidate("A",notFresh(MarketDataFreshnessError.STALE_BID)); val f=candidate("F",fresh()); val b=candidate("B",notFresh(MarketDataFreshnessError.STALE_ASK)); assertEquals(listOf(a,b),freshResult(listOf(a,f,b)).notFreshCandidates) }
    @Test fun duplicateFreshCandidateRemainsDuplicated() { val c=candidate("A",fresh()); val r=freshResult(listOf(c,c)); assertEquals(2,r.freshCandidates.size); assertSame(c,r.freshCandidates[0]); assertSame(c,r.freshCandidates[1]) }
    @Test fun duplicateNotFreshCandidateRemainsDuplicated() { val c=candidate("A",notFresh(MarketDataFreshnessError.STALE_BID)); val r=gate.filter(request(listOf(c,c))) as KnockoutProductCandidateFreshnessGateResult.NoFreshCandidates; assertEquals(2,r.notFreshCandidates.size); assertSame(c,r.notFreshCandidates[0]); assertSame(c,r.notFreshCandidates[1]) }
    @Test fun equalIsinsRemainSeparateCandidateEntries() { val a=candidate("DE000SYNTH01",fresh()); val b=candidate("DE000SYNTH01",notFresh(MarketDataFreshnessError.STALE_ASK)); val r=freshResult(listOf(a,b)); assertSame(a,r.freshCandidates.single()); assertSame(b,r.notFreshCandidates.single()) }
    @Test fun differentExactIsinSpellingsRemainSeparate() { val a=candidate("DE000SYNTH01",fresh()); val b=candidate("de000synth01",fresh()); val c=candidate(" DE000SYNTH01 ",fresh()); val r=freshResult(listOf(a,b,c)); assertEquals(listOf("DE000SYNTH01","de000synth01"," DE000SYNTH01 "),r.freshCandidates.map{it.candidateWithCalculationAvailability.candidateWithDataQuality.candidateWithMarketData.specificationSnapshot.specification.productIsin}) }
    @Test fun differentProductsFromSameIssuerRemainSeparate() { val a=candidate("A",fresh(),"issuer"); val b=candidate("B",fresh(),"issuer"); assertEquals(listOf(a,b),freshResult(listOf(a,b)).freshCandidates) }
    @Test fun resultIsNotLimitedToThreeCandidates() { val cs=(1..4).map{candidate("S$it",fresh())}; assertEquals(cs,freshResult(cs).freshCandidates) }
    @Test fun candidateDomainAssessmentAvailabilityAndFreshnessInstancesRemainUnchanged() { val a=candidate("A",fresh()); val g=freshResult(listOf(a)).freshCandidates.single(); assertSame(a,g); assertSame(a.candidateWithCalculationAvailability,g.candidateWithCalculationAvailability); assertSame(a.candidateWithCalculationAvailability.candidateWithDataQuality,g.candidateWithCalculationAvailability.candidateWithDataQuality); assertSame(a.candidateWithCalculationAvailability.candidateWithDataQuality.candidateWithMarketData,g.candidateWithCalculationAvailability.candidateWithDataQuality.candidateWithMarketData); assertSame(a.freshnessResult,g.freshnessResult) }
    @Test fun dataQualityFindingsRemainCompleteOrderedAndUnchanged() { val one=finding(DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID); val two=finding(DataQualityFindingCode.MARKET_DATA_MISSING_BID_TIMESTAMP); val assessment=DataQualityAssessment.warning(findings=listOf(one,two)); val a=candidate("A",fresh(),assessment=assessment); val g=freshResult(listOf(a)).freshCandidates.single(); assertSame(assessment,g.candidateWithCalculationAvailability.candidateWithDataQuality.dataQualityAssessment); assertEquals(listOf(one,two),g.candidateWithCalculationAvailability.candidateWithDataQuality.dataQualityAssessment.findings) }
    @Test fun availabilityErrorsRemainCompleteOrderedAndUnchanged() { val avail=MarketDataCalculationAvailabilityResult.StructurallyUnavailable(listOf(MarketDataCalculationAvailabilityError.MISSING_BID,MarketDataCalculationAvailabilityError.MISSING_ASK)); val a=candidate("A",fresh(),availability=avail); val g=freshResult(listOf(a)).freshCandidates.single(); assertSame(avail,g.candidateWithCalculationAvailability.availabilityResult); assertEquals(listOf(MarketDataCalculationAvailabilityError.MISSING_BID,MarketDataCalculationAvailabilityError.MISSING_ASK),(g.candidateWithCalculationAvailability.availabilityResult as MarketDataCalculationAvailabilityResult.StructurallyUnavailable).errors) }
    @Test fun freshnessErrorsRemainCompleteOrderedAndUnchanged() { val f=notFresh(MarketDataFreshnessError.STALE_BID,MarketDataFreshnessError.STALE_ASK,MarketDataFreshnessError.BID_ASK_TIMESTAMPS_TOO_FAR_APART); val a=candidate("A",f); val r=gate.filter(request(listOf(a))) as KnockoutProductCandidateFreshnessGateResult.NoFreshCandidates; assertSame(f,r.notFreshCandidates.single().freshnessResult); assertEquals(f.errors,(r.notFreshCandidates.single().freshnessResult as MarketDataFreshnessResult.NotFresh).errors) }
    @Test fun gateDoesNotMutateRequestOrInputList() { val a=candidate("A",fresh()); val b=candidate("B",notFresh(MarketDataFreshnessError.STALE_BID)); val input=mutableListOf(a,b,a); val copy=input.toList(); val q=request(input); gate.filter(q); assertSame(input,q.candidates); assertEquals(copy,input) }
    @Test fun repeatedFilterCallsDoNotShareStateAndContainNoLaterStageOutput() { val a=candidate("A",fresh()); val b=candidate("B",notFresh(MarketDataFreshnessError.STALE_ASK)); val q=request(listOf(a,b)); val first=gate.filter(q); val second=gate.filter(q); assertEquals(first,second); assertSame(a,freshResult(first).freshCandidates.single()); assertSame(b,freshResult(second).notFreshCandidates.single()) }

    private fun freshResult(candidates: List<KnockoutProductCandidateWithFreshness>) = freshResult(gate.filter(request(candidates)))
    private fun freshResult(result: KnockoutProductCandidateFreshnessGateResult): KnockoutProductCandidateFreshnessGateResult.FreshCandidates { assertTrue(result is KnockoutProductCandidateFreshnessGateResult.FreshCandidates); return result as KnockoutProductCandidateFreshnessGateResult.FreshCandidates }
    private fun request(candidates: List<KnockoutProductCandidateWithFreshness>) = KnockoutProductCandidateFreshnessGateRequest(candidates)
    private fun fresh() = MarketDataFreshnessResult.Fresh
    private fun notFresh(vararg errors: MarketDataFreshnessError) = MarketDataFreshnessResult.NotFresh(errors.toList())
    private fun finding(code: DataQualityFindingCode) = DataQualityFinding(DataQualityCategory.MISSING_REQUIRED_DATA,DataQualitySeverity.WARNING,code,DataQualityComponent.PRODUCT_MARKET_DATA)
    private fun candidate(isin:String, freshness:MarketDataFreshnessResult, issuer:String="issuer", assessment:DataQualityAssessment=DataQualityAssessment.passed(), availability:MarketDataCalculationAvailabilityResult=MarketDataCalculationAvailabilityResult.StructurallyAvailable): KnockoutProductCandidateWithFreshness {
        val spec=KnockoutProductSpecification(isin,"SYN001",issuer,"underlying",TradeDirection.LONG,100.0,95.0,0.1,"EUR","EUR")
        val snapshot=KnockoutProductSpecificationSnapshot(spec,"source",1000L,900L)
        val market=KnockoutProductMarketData(isin,1.0,1.2,950L,950L,"EUR","market")
        val data=KnockoutProductCandidateWithDataQuality(KnockoutProductCandidateWithMarketData(snapshot,market),assessment)
        return KnockoutProductCandidateWithFreshness(KnockoutProductCandidateWithCalculationAvailability(data,availability),freshness)
    }
}
