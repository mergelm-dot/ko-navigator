package de.konavigator.app.scenarios

import de.konavigator.app.calculator.TradeCalculationError
import de.konavigator.app.calculator.TradeCalculationInput
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.currency.CurrencyConversionCreationResult

object TradeCalculationScenarioFixtures {

    private val eur = currencyCode("EUR")
    private val usd = currencyCode("USD")
    private val sameCurrencyEur = CurrencyConversion.SameCurrency(eur)
    private val usdToEur = crossCurrency(usd, eur, 1.25)
    private val eurToUsd = crossCurrency(eur, usd, 0.8)

    val scenarios: List<TradeCalculationScenario> = listOf(
        TradeCalculationScenario(
            name = "standard_long_same_currency",
            group = "standard",
            input = input(110.0, 100.0, 5.0, true, 0.01, sameCurrencyEur),
            expectation = success(100.0, 80.0, 5.0, 0.2, 0.2, 1.0, 5.0, eur, eur, 20.0, 20.0)
        ),
        TradeCalculationScenario(
            name = "standard_short_same_currency",
            group = "standard",
            input = input(90.0, 100.0, 5.0, false, 0.01, sameCurrencyEur),
            expectation = success(100.0, 120.0, 5.0, 0.2, 0.2, 1.0, 5.0, eur, eur, 20.0, 20.0)
        ),
        TradeCalculationScenario(
            name = "usd_to_eur_long",
            group = "cross_currency",
            input = input(110.0, 100.0, 5.0, true, 0.1, usdToEur),
            expectation = success(100.0, 80.0, 5.0, 2.0, 1.6, 8.0, 5.0, usd, eur, 20.0, 20.0)
        ),
        TradeCalculationScenario(
            name = "usd_to_eur_short",
            group = "cross_currency",
            input = input(90.0, 100.0, 5.0, false, 0.1, usdToEur),
            expectation = success(100.0, 120.0, 5.0, 2.0, 1.6, 8.0, 5.0, usd, eur, 20.0, 20.0)
        ),
        TradeCalculationScenario(
            name = "eur_to_usd_long",
            group = "cross_currency",
            input = input(110.0, 100.0, 5.0, true, 0.1, eurToUsd),
            expectation = success(100.0, 80.0, 5.0, 2.0, 2.5, 12.5, 5.0, eur, usd, 20.0, 20.0)
        ),
        TradeCalculationScenario(
            name = "eur_to_usd_short",
            group = "cross_currency",
            input = input(90.0, 100.0, 5.0, false, 0.1, eurToUsd),
            expectation = success(100.0, 120.0, 5.0, 2.0, 2.5, 12.5, 5.0, eur, usd, 20.0, 20.0)
        ),
        TradeCalculationScenario(
            name = "same_currency_ratio_0_1",
            group = "ratio",
            input = input(100.0, 100.0, 5.0, true, 0.1, sameCurrencyEur),
            expectation = success(100.0, 80.0, 5.0, 2.0, 2.0, 10.0, 5.0, eur, eur, 20.0, 20.0)
        ),
        TradeCalculationScenario(
            name = "same_currency_ratio_0_01",
            group = "ratio",
            input = input(200.0, 200.0, 4.0, true, 0.01, sameCurrencyEur),
            expectation = success(200.0, 150.0, 4.0, 0.5, 0.5, 2.0, 4.0, eur, eur, 50.0, 25.0)
        ),
        TradeCalculationScenario(
            name = "same_currency_ratio_0_001",
            group = "ratio",
            input = input(100.0, 100.0, 5.0, false, 0.001, sameCurrencyEur),
            expectation = success(100.0, 120.0, 5.0, 0.02, 0.02, 0.1, 5.0, eur, eur, 20.0, 20.0)
        ),
        TradeCalculationScenario(
            name = "low_valid_target_leverage_1_1",
            group = "leverage_boundary",
            input = input(100.0, 100.0, 1.1, true, 0.01, sameCurrencyEur),
            expectation = success(
                100.0,
                9.090909090909092,
                1.1,
                0.9090909090909091,
                0.9090909090909091,
                1.0,
                1.1,
                eur,
                eur,
                90.9090909090909,
                90.9090909090909
            )
        ),
        TradeCalculationScenario(
            name = "high_valid_target_leverage_1000",
            group = "leverage_boundary",
            input = input(100.0, 100.0, 1000.0, true, 0.01, sameCurrencyEur),
            expectation = success(100.0, 99.9, 1000.0, 0.001, 0.001, 1.0, 1000.0, eur, eur, 0.1, 0.1)
        ),
        TradeCalculationScenario(
            name = "high_valid_entry_price",
            group = "price_scale",
            input = input(950_000.0, 1_000_000.0, 5.0, false, 0.001, sameCurrencyEur),
            expectation = success(
                1_000_000.0,
                1_200_000.0,
                5.0,
                200.0,
                200.0,
                1_000.0,
                5.0,
                eur,
                eur,
                200_000.0,
                20.0
            )
        ),
        TradeCalculationScenario(
            name = "normal_decimal_price",
            group = "precision",
            input = input(120.0, 123.45, 3.7, true, 0.01, sameCurrencyEur),
            expectation = success(
                123.45,
                90.08513513513514,
                3.7,
                0.33364864864864865,
                0.33364864864864865,
                1.2345,
                3.7,
                eur,
                eur,
                33.36486486486486,
                27.027027027027028
            )
        ),
        TradeCalculationScenario(
            name = "very_small_positive_product_value",
            group = "precision",
            input = input(10.0, 10.0, 5.0, false, 0.0001, sameCurrencyEur),
            expectation = success(10.0, 12.0, 5.0, 0.0002, 0.0002, 0.001, 5.0, eur, eur, 2.0, 20.0)
        ),
        TradeCalculationScenario(
            name = "characterization_underlying_price_is_not_engine_operand",
            group = "characterization",
            input = input(999.0, 90.0, 3.0, false, 0.01, sameCurrencyEur),
            expectation = success(
                90.0,
                120.0,
                3.0,
                0.3,
                0.3,
                0.9,
                3.0,
                eur,
                eur,
                30.0,
                33.333333333333336
            )
        ),
        TradeCalculationScenario(
            name = "zero_planned_entry_price",
            group = "validation",
            input = input(100.0, 0.0, 5.0, true, 0.01, sameCurrencyEur),
            expectation = failure(TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE)
        ),
        TradeCalculationScenario(
            name = "target_leverage_one",
            group = "validation",
            input = input(100.0, 100.0, 1.0, true, 0.01, sameCurrencyEur),
            expectation = failure(TradeCalculationError.INVALID_TARGET_LEVERAGE)
        ),
        TradeCalculationScenario(
            name = "zero_ratio",
            group = "validation",
            input = input(100.0, 100.0, 5.0, true, 0.0, sameCurrencyEur),
            expectation = failure(TradeCalculationError.INVALID_RATIO)
        ),
        TradeCalculationScenario(
            name = "nan_ratio",
            group = "validation",
            input = input(100.0, 100.0, 5.0, true, Double.NaN, sameCurrencyEur),
            expectation = failure(TradeCalculationError.INVALID_RATIO)
        ),
        TradeCalculationScenario(
            name = "infinite_ratio",
            group = "validation",
            input = input(100.0, 100.0, 5.0, true, Double.POSITIVE_INFINITY, sameCurrencyEur),
            expectation = failure(TradeCalculationError.INVALID_RATIO)
        ),
        TradeCalculationScenario(
            name = "derived_knockout_price_overflow",
            group = "numeric_boundary",
            input = input(100.0, Double.MAX_VALUE, 2.0, false, 0.01, sameCurrencyEur),
            expectation = failure(TradeCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE)
        ),
        TradeCalculationScenario(
            name = "theoretical_product_value_overflow",
            group = "numeric_boundary",
            input = input(100.0, Double.MAX_VALUE, 2.0, true, 4.0, sameCurrencyEur),
            expectation = failure(TradeCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE)
        ),
        TradeCalculationScenario(
            name = "underlying_exposure_overflow",
            group = "numeric_boundary",
            input = input(100.0, Double.MAX_VALUE, 2.0, true, 2.0, sameCurrencyEur),
            expectation = failure(TradeCalculationError.INVALID_CALCULATED_LEVERAGE)
        ),
        TradeCalculationScenario(
            name = "validation_priority_entry_before_leverage_and_ratio",
            group = "validation_priority",
            input = input(100.0, 0.0, 1.0, true, 0.0, sameCurrencyEur),
            expectation = failure(TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE)
        ),
        TradeCalculationScenario(
            name = "validation_priority_leverage_before_ratio",
            group = "validation_priority",
            input = input(100.0, 100.0, 1.0, true, 0.0, sameCurrencyEur),
            expectation = failure(TradeCalculationError.INVALID_TARGET_LEVERAGE)
        )
    )

    private fun input(
        underlyingPrice: Double,
        plannedEntryPrice: Double,
        targetLeverage: Double,
        isLong: Boolean,
        ratio: Double,
        currencyConversion: CurrencyConversion
    ) = TradeCalculationInput(
        underlyingPrice = underlyingPrice,
        plannedEntryPrice = plannedEntryPrice,
        targetLeverage = targetLeverage,
        isLong = isLong,
        ratio = ratio,
        currencyConversion = currencyConversion
    )

    private fun success(
        expectedUnderlyingPrice: Double,
        expectedKnockoutPrice: Double,
        expectedTargetLeverage: Double,
        expectedTheoreticalValueInUnderlyingCurrency: Double,
        expectedTheoreticalProductValue: Double,
        expectedUnderlyingExposureInProductCurrency: Double,
        expectedCalculatedTheoreticalLeverageAtEntry: Double,
        expectedUnderlyingCurrency: CurrencyCode,
        expectedProductCurrency: CurrencyCode,
        expectedDistanceToKnockoutAbsolute: Double,
        expectedDistanceToKnockoutPercent: Double
    ) = TradeCalculationExpectation.Success(
        expectedUnderlyingPrice = expectedUnderlyingPrice,
        expectedKnockoutPrice = expectedKnockoutPrice,
        expectedTargetLeverage = expectedTargetLeverage,
        expectedTheoreticalValueInUnderlyingCurrency =
            expectedTheoreticalValueInUnderlyingCurrency,
        expectedTheoreticalProductValue = expectedTheoreticalProductValue,
        expectedUnderlyingExposureInProductCurrency =
            expectedUnderlyingExposureInProductCurrency,
        expectedCalculatedTheoreticalLeverageAtEntry =
            expectedCalculatedTheoreticalLeverageAtEntry,
        expectedUnderlyingCurrency = expectedUnderlyingCurrency,
        expectedProductCurrency = expectedProductCurrency,
        expectedDistanceToKnockoutAbsolute = expectedDistanceToKnockoutAbsolute,
        expectedDistanceToKnockoutPercent = expectedDistanceToKnockoutPercent
    )

    private fun failure(expectedError: TradeCalculationError) =
        TradeCalculationExpectation.Failure(expectedError)

    private fun crossCurrency(
        underlyingCurrency: CurrencyCode,
        productCurrency: CurrencyCode,
        rate: Double
    ): CurrencyConversion.CrossCurrency =
        when (
            val result = CurrencyConversion.CrossCurrency.create(
                underlyingCurrency = underlyingCurrency,
                productCurrency = productCurrency,
                underlyingCurrencyPerProductCurrencyRate = rate
            )
        ) {
            is CurrencyConversionCreationResult.Success -> result.conversion
            is CurrencyConversionCreationResult.Failure ->
                error("Unexpected invalid fixture conversion: ${result.error}")
        }

    private fun currencyCode(value: String): CurrencyCode =
        when (val result = CurrencyCode.create(value)) {
            is CurrencyCodeCreationResult.Success -> result.currencyCode
            is CurrencyCodeCreationResult.Failure ->
                error("Unexpected invalid fixture currency: ${result.error}")
        }
}
