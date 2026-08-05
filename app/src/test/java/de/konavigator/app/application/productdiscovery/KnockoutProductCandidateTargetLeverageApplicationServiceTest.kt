package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.calculator.TradeCalculationError
import de.konavigator.app.calculator.TradeCalculationInput
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityError
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult
import de.konavigator.app.domain.calculator.MarketDataCalculationError
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.currency.CurrencyConversionCreationResult
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
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue
import de.konavigator.app.domain.source.MarketDataSourceError
import de.konavigator.app.domain.source.MarketDataSourceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateTargetLeverageApplicationServiceTest {
    private val service = KnockoutProductCandidateTargetLeverageApplicationService(TradeCalculationEngine)
    private val underlyingPrice = 110.0
    private val plannedEntryPrice = 100.0
    private val targetLeverage = 5.0

    @Test fun longCandidateIsMappedToLongTradeCalculation() { val input=input(direction=TradeDirection.LONG); assertEquals(direct(input,true),single(input).tradeCalculationResult) }
    @Test fun shortCandidateIsMappedToShortTradeCalculation() { val input=input(direction=TradeDirection.SHORT); assertEquals(direct(input,false),single(input).tradeCalculationResult) }
    @Test fun underlyingPriceIsForwardedExactly() { val input=input(); val request=request(listOf(input),underlying=123.45); assertEquals(direct(input,true,request),result(request).candidates.single().tradeCalculationResult) }
    @Test fun plannedEntryPriceIsForwardedExactly() { val input=input(); val request=request(listOf(input),entry=87.5); assertEquals(direct(input,true,request),result(request).candidates.single().tradeCalculationResult) }
    @Test fun targetLeverageIsForwardedExactly() { val input=input(); val request=request(listOf(input),leverage=7.25); assertEquals(direct(input,true,request),result(request).candidates.single().tradeCalculationResult) }
    @Test fun specificationRatioIsForwardedExactly() { val first=input(ratio=0.1); val second=input(isin="B",ratio=0.2); val plans=result(request(listOf(first,second))).candidates; assertEquals(direct(first,true),plans[0].tradeCalculationResult); assertEquals(direct(second,true),plans[1].tradeCalculationResult) }
    @Test fun sameCurrencyConversionIsForwardedUnchanged() { val conversion=same(); val input=input(conversion=conversion); val plan=single(input); assertSame(conversion,plan.input.currencyConversion); assertEquals(direct(input,true),plan.tradeCalculationResult) }
    @Test fun crossCurrencyConversionIsForwardedUnchanged() { val conversion=cross(); val input=input(conversion=conversion); val plan=single(input); assertSame(conversion,plan.input.currencyConversion); assertEquals(direct(input,true),plan.tradeCalculationResult) }
    @Test fun validLongResultMatchesDirectEngineResult() { val input=input(direction=TradeDirection.LONG); assertEquals(direct(input,true),single(input).tradeCalculationResult) }
    @Test fun validShortResultMatchesDirectEngineResult() { val input=input(direction=TradeDirection.SHORT); assertEquals(direct(input,false),single(input).tradeCalculationResult) }
    @Test fun invalidPlannedEntryPricePreservesEngineError() { assertError(request(listOf(input()),entry=0.0),TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE) }
    @Test fun invalidTargetLeveragePreservesEngineError() { assertError(request(listOf(input()),leverage=1.0),TradeCalculationError.INVALID_TARGET_LEVERAGE) }
    @Test fun invalidSpecificationRatioPreservesEngineError() { assertError(request(listOf(input(ratio=0.0))),TradeCalculationError.INVALID_RATIO) }
    @Test fun emptyInputReturnsNoInputCandidates() { assertSame(KnockoutProductCandidateTargetLeverageResult.NoInputCandidates,service.execute(request(emptyList()))) }
    @Test fun inputOrderIsPreserved() { val long=input(isin="A",direction=TradeDirection.LONG,ratio=0.1); val short=input(isin="B",direction=TradeDirection.SHORT,ratio=0.2); val third=input(isin="C",direction=TradeDirection.LONG,ratio=0.3); assertEquals(listOf(long,short,third),result(request(listOf(long,short,third))).candidates.map { it.input }) }
    @Test fun validAndInvalidEngineResultsRemainTogether() { val valid=input(isin="A"); val invalid=input(isin="B",ratio=0.0); val plans=result(request(listOf(valid,invalid))).candidates; assertEquals(2,plans.size); assertTrue(plans[0].tradeCalculationResult.isValid); assertEquals(TradeCalculationError.INVALID_RATIO,plans[1].tradeCalculationResult.error) }
    @Test fun duplicateCandidateInputRemainsDuplicated() { val input=input(); val plans=result(request(listOf(input,input))).candidates; assertEquals(2,plans.size); assertSame(input,plans[0].input); assertSame(input,plans[1].input) }
    @Test fun equalIsinsRemainSeparateCandidateEntries() { val first=input(isin="DE000SYNTH01"); val second=input(isin="DE000SYNTH01"); val plans=result(request(listOf(first,second))).candidates; assertEquals(2,plans.size); assertSame(first,plans[0].input); assertSame(second,plans[1].input) }
    @Test fun originalInstancesRequestAndInputListRemainUnchanged() { val finding=DataQualityFinding(DataQualityCategory.MISSING_REQUIRED_DATA,DataQualitySeverity.WARNING,DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID,DataQualityComponent.PRODUCT_MARKET_DATA); val assessment=DataQualityAssessment.warning(listOf(finding)); val availability=MarketDataCalculationAvailabilityResult.StructurallyUnavailable(listOf(MarketDataCalculationAvailabilityError.MISSING_BID)); val freshness=MarketDataFreshnessResult.NotFresh(listOf(MarketDataFreshnessError.STALE_ASK)); val source=MarketDataSourceResult.Blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED); val outcome=KnockoutProductCandidateCalculationOutcome.Success(MarketDataCalculationValue.MidPrice(1.0,"EUR")); val conversion=same(); val input=input(assessment=assessment,availability=availability,freshness=freshness,source=source,outcome=outcome,conversion=conversion); val inputs=mutableListOf(input,input); val original=inputs.toList(); val request=request(inputs); val plan=result(request).candidates.first(); val calculation=plan.input.candidateWithCalculation; val sourceCandidate=calculation.candidateWithSourceEvaluation; val freshnessCandidate=sourceCandidate.candidateWithFreshness; val availabilityCandidate=freshnessCandidate.candidateWithCalculationAvailability; val qualityCandidate=availabilityCandidate.candidateWithDataQuality; assertSame(inputs,request.candidates); assertEquals(original,inputs); assertEquals(underlyingPrice,request.underlyingPrice,0.0); assertEquals(plannedEntryPrice,request.plannedEntryPrice,0.0); assertEquals(targetLeverage,request.targetLeverage,0.0); assertSame(input,plan.input); assertSame(conversion,plan.input.currencyConversion); assertSame(calculation,plan.input.candidateWithCalculation); assertSame(sourceCandidate,calculation.candidateWithSourceEvaluation); assertSame(freshnessCandidate,sourceCandidate.candidateWithFreshness); assertSame(availabilityCandidate,freshnessCandidate.candidateWithCalculationAvailability); assertSame(qualityCandidate,availabilityCandidate.candidateWithDataQuality); assertSame(qualityCandidate.candidateWithMarketData,qualityCandidate.candidateWithMarketData); assertSame(qualityCandidate.candidateWithMarketData.specificationSnapshot,qualityCandidate.candidateWithMarketData.specificationSnapshot); assertSame(qualityCandidate.candidateWithMarketData.marketData,qualityCandidate.candidateWithMarketData.marketData); assertSame(assessment,qualityCandidate.dataQualityAssessment); assertEquals(listOf(finding),assessment.findings); assertSame(availability,availabilityCandidate.availabilityResult); assertSame(freshness,freshnessCandidate.freshnessResult); assertSame(source,sourceCandidate.sourceResult); assertSame(outcome,calculation.calculationOutcome) }
    @Test fun repeatedExecutionMatchesDirectEngineAndContainsNoLaterStageOutput() { val long=input(isin="A",direction=TradeDirection.LONG); val short=input(isin="B",direction=TradeDirection.SHORT,conversion=cross()); val request=request(listOf(long,short)); val first=result(request); val second=result(request); assertEquals(first,second); assertEquals(direct(long,true,request),first.candidates[0].tradeCalculationResult); assertEquals(direct(short,false,request),first.candidates[1].tradeCalculationResult); assertSame(long,first.candidates[0].input); assertSame(short,second.candidates[1].input) }

    private fun single(input:KnockoutProductCandidateTargetLeverageInput)=result(request(listOf(input))).candidates.single()
    private fun result(request:KnockoutProductCandidateTargetLeverageRequest):KnockoutProductCandidateTargetLeverageResult.CandidatesWithTargetLeveragePlan { val result=service.execute(request); assertTrue(result is KnockoutProductCandidateTargetLeverageResult.CandidatesWithTargetLeveragePlan); return result as KnockoutProductCandidateTargetLeverageResult.CandidatesWithTargetLeveragePlan }
    private fun assertError(request:KnockoutProductCandidateTargetLeverageRequest,error:TradeCalculationError) { assertEquals(error,result(request).candidates.single().tradeCalculationResult.error) }
    private fun direct(input:KnockoutProductCandidateTargetLeverageInput,isLong:Boolean,request:KnockoutProductCandidateTargetLeverageRequest=request(listOf(input)))=TradeCalculationEngine.calculateTrade(TradeCalculationInput(request.underlyingPrice,request.plannedEntryPrice,request.targetLeverage,isLong,ratio(input),input.currencyConversion))
    private fun ratio(input:KnockoutProductCandidateTargetLeverageInput)=input.candidateWithCalculation.candidateWithSourceEvaluation.candidateWithFreshness.candidateWithCalculationAvailability.candidateWithDataQuality.candidateWithMarketData.specificationSnapshot.specification.ratio
    private fun request(candidates:List<KnockoutProductCandidateTargetLeverageInput>,underlying:Double=underlyingPrice,entry:Double=plannedEntryPrice,leverage:Double=targetLeverage)=KnockoutProductCandidateTargetLeverageRequest(candidates,underlying,entry,leverage)
    private fun same()=CurrencyConversion.SameCurrency(code("EUR"))
    private fun cross():CurrencyConversion.CrossCurrency=(CurrencyConversion.CrossCurrency.create(code("USD"),code("EUR"),1.1) as CurrencyConversionCreationResult.Success).conversion
    private fun code(value:String)=(CurrencyCode.create(value) as CurrencyCodeCreationResult.Success).currencyCode
    private fun input(isin:String="DE000SYNTH01",direction:TradeDirection=TradeDirection.LONG,ratio:Double=0.1,conversion:CurrencyConversion=same(),assessment:DataQualityAssessment=DataQualityAssessment.passed(),availability:MarketDataCalculationAvailabilityResult=MarketDataCalculationAvailabilityResult.StructurallyAvailable,freshness:MarketDataFreshnessResult=MarketDataFreshnessResult.Fresh,source:MarketDataSourceResult=MarketDataSourceResult.Allowed,outcome:KnockoutProductCandidateCalculationOutcome=KnockoutProductCandidateCalculationOutcome.Success(MarketDataCalculationValue.MidPrice(1.0,"EUR"))):KnockoutProductCandidateTargetLeverageInput { val specification=KnockoutProductSpecification(isin,"SYN001","issuer","underlying",direction,100.0,95.0,ratio,"EUR","EUR"); val snapshot=KnockoutProductSpecificationSnapshot(specification,"snapshot",1000L,900L); val marketData=KnockoutProductMarketData(isin,1.0,1.2,950L,950L,"EUR","SYNTH_SOURCE"); val marketCandidate=KnockoutProductCandidateWithMarketData(snapshot,marketData); val qualityCandidate=KnockoutProductCandidateWithDataQuality(marketCandidate,assessment); val availabilityCandidate=KnockoutProductCandidateWithCalculationAvailability(qualityCandidate,availability); val sourceCandidate=KnockoutProductCandidateWithSourceEvaluation(KnockoutProductCandidateWithFreshness(availabilityCandidate,freshness),source); return KnockoutProductCandidateTargetLeverageInput(KnockoutProductCandidateWithCalculation(sourceCandidate,outcome),conversion) }
}
