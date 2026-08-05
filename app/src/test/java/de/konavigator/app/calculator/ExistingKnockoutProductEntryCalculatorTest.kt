package de.konavigator.app.calculator

import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.currency.CurrencyConversionCreationResult
import de.konavigator.app.domain.model.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExistingKnockoutProductEntryCalculatorTest {
    @Test fun longIntrinsicValueUsesActualBasePrice() { val input=input(entry=120.0,base=100.0,barrier=90.0); val result=success(input); assertEquals(KoCalculator.calculateIntrinsicValue(120.0,100.0,0.1,true),result.intrinsicValueInUnderlyingCurrency,0.0) }
    @Test fun shortIntrinsicValueUsesActualBasePrice() { val input=input(entry=80.0,base=100.0,barrier=110.0,direction=TradeDirection.SHORT); val result=success(input); assertEquals(KoCalculator.calculateIntrinsicValue(80.0,100.0,0.1,false),result.intrinsicValueInUnderlyingCurrency,0.0) }
    @Test fun longKnockoutDistanceUsesActualKnockoutBarrier() { val input=input(entry=120.0,base=100.0,barrier=90.0); val result=success(input); assertEquals(KoCalculator.calculateKnockoutDistanceAbsolute(120.0,90.0,true),result.knockoutDistanceAbsolute,0.0); assertEquals(KoCalculator.calculateKnockoutDistancePercent(120.0,90.0,true),result.knockoutDistancePercent,0.0) }
    @Test fun shortKnockoutDistanceUsesActualKnockoutBarrier() { val input=input(entry=80.0,base=100.0,barrier=110.0,direction=TradeDirection.SHORT); val result=success(input); assertEquals(KoCalculator.calculateKnockoutDistanceAbsolute(80.0,110.0,false),result.knockoutDistanceAbsolute,0.0); assertEquals(KoCalculator.calculateKnockoutDistancePercent(80.0,110.0,false),result.knockoutDistancePercent,0.0) }
    @Test fun basePriceAndKnockoutBarrierRemainIndependent() { val input=input(entry=150.0,base=100.0,barrier=80.0); val result=success(input); assertEquals(5.0,result.intrinsicValueInUnderlyingCurrency,0.0); assertEquals(70.0,result.knockoutDistanceAbsolute,0.0) }
    @Test fun sameCurrencyProductValueEqualsIntrinsicValue() { val input=input(entry=120.0,base=100.0,barrier=90.0); val result=success(input); assertEquals(result.intrinsicValueInUnderlyingCurrency,result.theoreticalProductValue,0.0) }
    @Test fun crossCurrencyProductValueUsesDivision() { val input=input(entry=120.0,base=100.0,barrier=90.0,conversion=cross(2.0)); val result=success(input); assertEquals(result.intrinsicValueInUnderlyingCurrency/2.0,result.theoreticalProductValue,0.0) }
    @Test fun calculatedLeverageMatchesExistingLeverageCalculator() { val input=input(entry=120.0,base=100.0,barrier=90.0); val result=success(input); val direct=TheoreticalLeverageCalculator.calculate(120.0,0.1,input.currencyConversion,result.theoreticalProductValue) as TheoreticalLeverageCalculationResult.Success; assertEquals(direct.underlyingExposureInProductCurrency,result.underlyingExposureInProductCurrency,0.0); assertEquals(direct.calculatedTheoreticalLeverageAtEntry,result.calculatedLeverageAtEntry,0.0) }
    @Test fun validLongCalculationReturnsExpectedCurrencies() { val input=input(entry=120.0,base=100.0,barrier=90.0); val result=success(input); assertEquals(input.currencyConversion.underlyingCurrency,result.underlyingCurrency); assertEquals(input.currencyConversion.productCurrency,result.productCurrency) }
    @Test fun leverageAtOrBelowOnePreservesInvalidCalculatedLeverage() { assertFailure(input(entry=100.0,base=200.0,barrier=110.0,direction=TradeDirection.SHORT),ExistingKnockoutProductEntryCalculationError.INVALID_CALCULATED_LEVERAGE) }
    @Test fun invalidPlannedEntryPriceFails() { assertFailure(input(entry=0.0),ExistingKnockoutProductEntryCalculationError.INVALID_PLANNED_ENTRY_PRICE) }
    @Test fun invalidBasePriceFails() { assertFailure(input(base=0.0),ExistingKnockoutProductEntryCalculationError.INVALID_BASE_PRICE) }
    @Test fun invalidKnockoutBarrierFails() { assertFailure(input(barrier=0.0),ExistingKnockoutProductEntryCalculationError.INVALID_KNOCKOUT_BARRIER) }
    @Test fun invalidRatioFails() { assertFailure(input(ratio=0.0),ExistingKnockoutProductEntryCalculationError.INVALID_RATIO) }
    @Test fun longAtKnockoutFails() { assertFailure(input(entry=90.0,base=80.0,barrier=90.0),ExistingKnockoutProductEntryCalculationError.INVALID_KNOCKOUT_DISTANCE) }
    @Test fun shortAtKnockoutFails() { assertFailure(input(entry=110.0,base=120.0,barrier=110.0,direction=TradeDirection.SHORT),ExistingKnockoutProductEntryCalculationError.INVALID_KNOCKOUT_DISTANCE) }
    @Test fun longBeyondKnockoutFails() { assertFailure(input(entry=80.0,base=70.0,barrier=90.0),ExistingKnockoutProductEntryCalculationError.INVALID_KNOCKOUT_DISTANCE) }
    @Test fun shortBeyondKnockoutFails() { assertFailure(input(entry=120.0,base=130.0,barrier=110.0,direction=TradeDirection.SHORT),ExistingKnockoutProductEntryCalculationError.INVALID_KNOCKOUT_DISTANCE) }
    @Test fun zeroIntrinsicValueFailsWithoutFallback() { val result=ExistingKnockoutProductEntryCalculator.calculate(input(entry=100.0,base=120.0,barrier=90.0)); assertTrue(result is ExistingKnockoutProductEntryCalculationResult.Failure); assertEquals(ExistingKnockoutProductEntryCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE,(result as ExistingKnockoutProductEntryCalculationResult.Failure).error) }
    @Test fun inputRemainsUnchangedRepeatedCallsShareNoStateAndDoNotDeriveTargetBarrier() { val conversion=cross(1.5); val input=input(entry=150.125,base=100.0,barrier=80.0,ratio=0.1,conversion=conversion); val first=ExistingKnockoutProductEntryCalculator.calculate(input); val second=ExistingKnockoutProductEntryCalculator.calculate(input); assertEquals(first,second); assertEquals(150.125,input.plannedEntryPrice,0.0); assertEquals(100.0,input.basePrice,0.0); assertEquals(80.0,input.knockoutBarrier,0.0); assertSame(conversion,input.currencyConversion) }

    private fun success(input:ExistingKnockoutProductEntryCalculationInput):ExistingKnockoutProductEntryCalculationResult.Success { val result=ExistingKnockoutProductEntryCalculator.calculate(input); assertTrue(result is ExistingKnockoutProductEntryCalculationResult.Success); return result as ExistingKnockoutProductEntryCalculationResult.Success }
    private fun assertFailure(input:ExistingKnockoutProductEntryCalculationInput,error:ExistingKnockoutProductEntryCalculationError) { val result=ExistingKnockoutProductEntryCalculator.calculate(input); assertTrue(result is ExistingKnockoutProductEntryCalculationResult.Failure); assertEquals(error,(result as ExistingKnockoutProductEntryCalculationResult.Failure).error) }
    private fun input(entry:Double=120.0,base:Double=100.0,barrier:Double=90.0,direction:TradeDirection=TradeDirection.LONG,ratio:Double=0.1,conversion:CurrencyConversion=same())=ExistingKnockoutProductEntryCalculationInput(entry,base,barrier,direction,ratio,conversion)
    private fun same()=CurrencyConversion.SameCurrency(code("EUR"))
    private fun cross(rate:Double):CurrencyConversion.CrossCurrency=(CurrencyConversion.CrossCurrency.create(code("USD"),code("EUR"),rate) as CurrencyConversionCreationResult.Success).conversion
    private fun code(value:String)=(CurrencyCode.create(value) as CurrencyCodeCreationResult.Success).currencyCode
}
