package de.konavigator.app.domain.orchestration

import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityError
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.calculator.MarketDataCalculationError
import de.konavigator.app.domain.freshness.MarketDataFreshnessError
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.source.MarketDataSourceError
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import de.konavigator.app.domain.validation.KnockoutProductCompatibilityError
import de.konavigator.app.domain.validation.KnockoutProductMarketDataValidationError
import de.konavigator.app.domain.validation.KnockoutProductValidationError
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataCalculationOrchestratorTest {

    @Test
    fun requestContainsExactlyFourFields() {
        assertEquals(
            listOf(
                "calculationType",
                "specification",
                "marketData",
                "evaluationTimeEpochMillis"
            ),
            instanceFields(MarketDataCalculationRequest::class.java).map { it.name }
        )
    }

    @Test
    fun requestHasNoDefaultValues() {
        assertEquals(1, MarketDataCalculationRequest::class.java.constructors.size)
        assertEquals(4, MarketDataCalculationRequest::class.java.constructors.single().parameterCount)
    }

    @Test
    fun valueContainsExactlyFourSubtypes() {
        assertEquals(
            setOf("PurchasePrice", "SalePrice", "Spread", "MidPrice"),
            MarketDataCalculationValue::class.java.declaredClasses.map { it.simpleName }.toSet()
        )
    }

    @Test
    fun orchestrationResultContainsExactlyEightSubtypes() {
        assertEquals(
            setOf(
                "InvalidSpecification",
                "InvalidMarketData",
                "Incompatible",
                "StructurallyUnavailable",
                "NotFresh",
                "SourceBlocked",
                "CalculationFailure",
                "Success"
            ),
            MarketDataCalculationOrchestrationResult::class.java.declaredClasses
                .map { it.simpleName }
                .toSet()
        )
    }

    @Test
    fun orchestrationResultContainsNoStageProperty() {
        assertTrue(
            resultTypes().flatMap { instanceFields(it) }.none { it.name == "stage" }
        )
    }

    @Test
    fun orchestratorExposesExactlyOnePublicCalculateFunction() {
        val methods = MarketDataCalculationOrchestrator::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }

        assertEquals(listOf("calculate"), methods.map { it.name })
    }

    @Test
    fun orchestratorHasExactlyTwoConstructorDependencies() {
        val constructor = MarketDataCalculationOrchestrator::class.java.constructors.single()

        assertEquals(
            listOf(MarketDataFreshnessPolicy::class.java, MarketDataSourcePolicy::class.java),
            constructor.parameterTypes.toList()
        )
    }

    @Test
    fun publicApiContainsNoAndroidOrComposeTypes() {
        assertTrue(
            publicApiTypeNames().none {
                it.startsWith("android.") || it.contains("compose", ignoreCase = true)
            }
        )
    }

    @Test
    fun resultAndValueTypesContainNoMessagesOrUiTexts() {
        val fieldNames = (resultTypes() + valueTypes())
            .flatMap { instanceFields(it) }
            .map { it.name.lowercase() }

        assertTrue(fieldNames.none { it.contains("message") || it.contains("text") })
    }

    @Test
    fun resultContainsNoSuccessBoolean() {
        assertTrue(
            resultTypes().flatMap { instanceFields(it) }.none {
                it.name == "isSuccess" || it.type == Boolean::class.java
            }
        )
    }

    @Test
    fun purchaseReturnsAskExactly() {
        val result = successValue(request(PURCHASE_PRICE, marketData(ask = 2.123456789)))

        assertEquals(2.123456789, (result as MarketDataCalculationValue.PurchasePrice).value, 0.0)
    }

    @Test
    fun purchaseReturnsMarketDataCurrency() {
        val result = successValue(
            request(
                PURCHASE_PRICE,
                marketData = marketData(currency = "USD"),
                specification = specification(productCurrency = "USD")
            )
        )

        assertEquals("USD", (result as MarketDataCalculationValue.PurchasePrice).currency)
    }

    @Test
    fun purchaseDoesNotChangeAsk() {
        val quote = marketData(bid = 1.0, ask = 1.0000000000000002)

        successValue(request(PURCHASE_PRICE, quote))

        assertEquals(1.0000000000000002, quote.ask!!, 0.0)
    }

    @Test
    fun saleReturnsPositiveBidExactly() {
        val result = successValue(request(SALE_PRICE, marketData(bid = 1.987654321)))

        assertEquals(1.987654321, (result as MarketDataCalculationValue.SalePrice).value, 0.0)
    }

    @Test
    fun saleReturnsMarketDataCurrency() {
        val result = successValue(
            request(
                SALE_PRICE,
                marketData = marketData(currency = "USD"),
                specification = specification(productCurrency = "USD")
            )
        )

        assertEquals("USD", (result as MarketDataCalculationValue.SalePrice).currency)
    }

    @Test
    fun saleDoesNotChangeBid() {
        val quote = marketData(bid = 1.0000000000000002)

        successValue(request(SALE_PRICE, quote))

        assertEquals(1.0000000000000002, quote.bid!!, 0.0)
    }

    @Test
    fun spreadReturnsAbsoluteSpread() {
        val result = spreadValue(bid = 1.8, ask = 2.0)

        assertEquals(0.2, result.absoluteSpread, 1e-15)
    }

    @Test
    fun spreadReturnsRelativeSpreadToAskPercent() {
        val result = spreadValue(bid = 1.8, ask = 2.0)

        assertEquals((2.0 - 1.8) / 2.0 * 100.0, result.relativeSpreadToAskPercent, 0.0)
    }

    @Test
    fun spreadReturnsBothValuesInOneTypedResult() {
        val result = successValue(request(SPREAD, marketData()))

        assertTrue(result is MarketDataCalculationValue.Spread)
        assertEquals(
            listOf("absoluteSpread", "relativeSpreadToAskPercent", "currency"),
            instanceFields(result.javaClass).map { it.name }
        )
    }

    @Test
    fun spreadReturnsMarketDataCurrency() {
        val result = successValue(
            request(
                SPREAD,
                marketData = marketData(currency = "USD"),
                specification = specification(productCurrency = "USD")
            )
        )

        assertEquals("USD", (result as MarketDataCalculationValue.Spread).currency)
    }

    @Test
    fun spreadDoesNotRoundAbsoluteValue() {
        val result = spreadValue(bid = 1.111111111111, ask = 1.222222222222)

        assertEquals(1.222222222222 - 1.111111111111, result.absoluteSpread, 0.0)
    }

    @Test
    fun spreadDoesNotRoundRelativeValue() {
        val bid = 1.111111111111
        val ask = 1.222222222222
        val result = spreadValue(bid = bid, ask = ask)

        assertEquals((ask - bid) / ask * 100.0, result.relativeSpreadToAskPercent, 0.0)
    }

    @Test
    fun midReturnsCalculatorResult() {
        val result = successValue(request(MID, marketData(bid = 1.0, ask = 2.0)))

        assertEquals(1.5, (result as MarketDataCalculationValue.MidPrice).value, 0.0)
    }

    @Test
    fun midReturnsMarketDataCurrency() {
        val result = successValue(
            request(
                MID,
                marketData = marketData(currency = "USD"),
                specification = specification(productCurrency = "USD")
            )
        )

        assertEquals("USD", (result as MarketDataCalculationValue.MidPrice).currency)
    }

    @Test
    fun midDoesNotRoundCalculatorResult() {
        val bid = 1.111111111111
        val ask = 1.222222222222
        val result = successValue(request(MID, marketData(bid = bid, ask = ask)))

        assertEquals(
            bid + (ask - bid) / 2.0,
            (result as MarketDataCalculationValue.MidPrice).value,
            0.0
        )
    }

    @Test
    fun invalidSpecificationReturnsInvalidSpecification() {
        val result = calculate(request(PURCHASE_PRICE, specification = specification(productIsin = "")))

        assertTrue(result is MarketDataCalculationOrchestrationResult.InvalidSpecification)
    }

    @Test
    fun allSpecificationErrorsRemainUnchanged() {
        val invalid = specification(
            productIsin = "",
            productWkn = " ",
            issuerId = "",
            underlyingId = " ",
            basePrice = Double.NaN,
            knockoutBarrier = 0.0,
            ratio = -1.0,
            underlyingCurrency = "usd",
            productCurrency = "EU"
        )
        val result = calculate(request(PURCHASE_PRICE, specification = invalid))
            as MarketDataCalculationOrchestrationResult.InvalidSpecification

        assertEquals(KnockoutProductValidationError.entries, result.errors)
    }

    @Test
    fun marketDataFailureCannotOverrideSpecificationFailure() {
        val result = calculate(
            request(
                PURCHASE_PRICE,
                specification = specification(productIsin = ""),
                marketData = marketData(productIsin = "", sourceId = "")
            )
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.InvalidSpecification)
    }

    @Test
    fun laterStagesCannotOverrideSpecificationFailure() {
        val result = blockedOrchestrator().calculate(
            request(
                PURCHASE_PRICE,
                specification = specification(productIsin = ""),
                marketData = marketData(askTimestampEpochMillis = 0L),
                evaluationTimeEpochMillis = 100_000L
            )
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.InvalidSpecification)
    }

    @Test
    fun invalidMarketDataReturnsInvalidMarketData() {
        val result = calculate(request(PURCHASE_PRICE, marketData = marketData(currency = "eur")))

        assertTrue(result is MarketDataCalculationOrchestrationResult.InvalidMarketData)
    }

    @Test
    fun allMarketDataErrorsRemainUnchanged() {
        val invalid = marketData(
            productIsin = "",
            sourceId = "",
            currency = "eur",
            bid = -1.0,
            bidTimestampEpochMillis = null,
            ask = 0.0,
            askTimestampEpochMillis = null
        )
        val result = calculate(request(PURCHASE_PRICE, marketData = invalid))
            as MarketDataCalculationOrchestrationResult.InvalidMarketData

        assertEquals(
            listOf(
                KnockoutProductMarketDataValidationError.MISSING_PRODUCT_ISIN,
                KnockoutProductMarketDataValidationError.MISSING_SOURCE_ID,
                KnockoutProductMarketDataValidationError.INVALID_CURRENCY,
                KnockoutProductMarketDataValidationError.INVALID_BID,
                KnockoutProductMarketDataValidationError.MISSING_BID_TIMESTAMP,
                KnockoutProductMarketDataValidationError.INVALID_ASK,
                KnockoutProductMarketDataValidationError.MISSING_ASK_TIMESTAMP
            ),
            result.errors
        )
    }

    @Test
    fun compatibilityCannotOverrideMarketDataFailure() {
        val result = calculate(
            request(
                PURCHASE_PRICE,
                marketData = marketData(productIsin = "OTHER", currency = "eur")
            )
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.InvalidMarketData)
    }

    @Test
    fun sourcePolicyCannotOverrideMarketDataFailure() {
        val result = blockedOrchestrator().calculate(
            request(PURCHASE_PRICE, marketData = marketData(currency = "eur"))
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.InvalidMarketData)
    }

    @Test
    fun isinMismatchReturnsIncompatible() {
        val result = calculate(request(PURCHASE_PRICE, marketData = marketData(productIsin = "OTHER")))

        assertEquals(
            listOf(KnockoutProductCompatibilityError.PRODUCT_ISIN_MISMATCH),
            (result as MarketDataCalculationOrchestrationResult.Incompatible).errors
        )
    }

    @Test
    fun currencyMismatchReturnsIncompatible() {
        val result = calculate(request(PURCHASE_PRICE, marketData = marketData(currency = "USD")))

        assertEquals(
            listOf(KnockoutProductCompatibilityError.PRODUCT_CURRENCY_MISMATCH),
            (result as MarketDataCalculationOrchestrationResult.Incompatible).errors
        )
    }

    @Test
    fun compatibilityErrorsRemainCompleteAndOrdered() {
        val result = calculate(
            request(PURCHASE_PRICE, marketData = marketData(productIsin = "OTHER", currency = "USD"))
        ) as MarketDataCalculationOrchestrationResult.Incompatible

        assertEquals(KnockoutProductCompatibilityError.entries, result.errors)
    }

    @Test
    fun availabilityCannotOverrideCompatibilityFailure() {
        val result = calculate(
            request(
                PURCHASE_PRICE,
                marketData = marketData(productIsin = "OTHER", ask = null, askTimestampEpochMillis = null)
            )
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.Incompatible)
    }

    @Test
    fun purchaseWithoutAskIsStructurallyUnavailable() {
        assertUnavailable(
            request(PURCHASE_PRICE, marketData = marketData(ask = null, askTimestampEpochMillis = null)),
            MarketDataCalculationAvailabilityError.MISSING_ASK
        )
    }

    @Test
    fun saleWithoutBidIsStructurallyUnavailable() {
        assertUnavailable(
            request(SALE_PRICE, marketData = marketData(bid = null, bidTimestampEpochMillis = null)),
            MarketDataCalculationAvailabilityError.MISSING_BID
        )
    }

    @Test
    fun zeroBidForSaleIsStructurallyUnavailable() {
        assertUnavailable(
            request(SALE_PRICE, marketData = marketData(bid = 0.0)),
            MarketDataCalculationAvailabilityError.BID_NOT_POSITIVE_FOR_SALE
        )
    }

    @Test
    fun spreadWithoutBidIsStructurallyUnavailable() {
        assertUnavailable(
            request(SPREAD, marketData = marketData(bid = null, bidTimestampEpochMillis = null)),
            MarketDataCalculationAvailabilityError.MISSING_BID
        )
    }

    @Test
    fun spreadWithoutAskIsStructurallyUnavailable() {
        assertUnavailable(
            request(SPREAD, marketData = marketData(ask = null, askTimestampEpochMillis = null)),
            MarketDataCalculationAvailabilityError.MISSING_ASK
        )
    }

    @Test
    fun midWithoutBidIsStructurallyUnavailable() {
        assertUnavailable(
            request(MID, marketData = marketData(bid = null, bidTimestampEpochMillis = null)),
            MarketDataCalculationAvailabilityError.MISSING_BID
        )
    }

    @Test
    fun midWithoutAskIsStructurallyUnavailable() {
        assertUnavailable(
            request(MID, marketData = marketData(ask = null, askTimestampEpochMillis = null)),
            MarketDataCalculationAvailabilityError.MISSING_ASK
        )
    }

    @Test
    fun availabilityErrorsRemainCompleteAndOrdered() {
        val result = calculate(
            request(
                SPREAD,
                marketData = marketData(
                    bid = null,
                    ask = null,
                    bidTimestampEpochMillis = null,
                    askTimestampEpochMillis = null
                )
            )
        ) as MarketDataCalculationOrchestrationResult.StructurallyUnavailable

        assertEquals(
            listOf(
                MarketDataCalculationAvailabilityError.MISSING_BID,
                MarketDataCalculationAvailabilityError.MISSING_ASK
            ),
            result.errors
        )
    }

    @Test
    fun freshnessCannotOverrideAvailabilityFailure() {
        val result = staleOrchestrator().calculate(
            request(PURCHASE_PRICE, marketData = marketData(ask = null, askTimestampEpochMillis = null))
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.StructurallyUnavailable)
    }

    @Test
    fun staleAskForPurchaseReturnsNotFresh() {
        assertNotFresh(
            staleOrchestrator().calculate(
                request(PURCHASE_PRICE, marketData = marketData(askTimestampEpochMillis = 0L), 100L)
            ),
            MarketDataFreshnessError.STALE_ASK
        )
    }

    @Test
    fun staleBidForSaleReturnsNotFresh() {
        assertNotFresh(
            staleOrchestrator().calculate(
                request(SALE_PRICE, marketData = marketData(bidTimestampEpochMillis = 0L), 100L)
            ),
            MarketDataFreshnessError.STALE_BID
        )
    }

    @Test
    fun staleSpreadErrorsRemainCompleteAndOrdered() {
        val result = staleOrchestrator().calculate(
            request(
                SPREAD,
                marketData = marketData(
                    bidTimestampEpochMillis = 0L,
                    askTimestampEpochMillis = 1L
                ),
                evaluationTimeEpochMillis = 100L
            )
        ) as MarketDataCalculationOrchestrationResult.NotFresh

        assertEquals(
            listOf(MarketDataFreshnessError.STALE_BID, MarketDataFreshnessError.STALE_ASK),
            result.errors
        )
    }

    @Test
    fun futureErrorRemainsUnchanged() {
        val result = strictTimeOrchestrator().calculate(
            request(PURCHASE_PRICE, marketData = marketData(askTimestampEpochMillis = 101L), 100L)
        )

        assertNotFresh(result, MarketDataFreshnessError.ASK_TIMESTAMP_IN_FUTURE)
    }

    @Test
    fun explicitEvaluationTimeControlsFreshness() {
        val quote = marketData(askTimestampEpochMillis = 100L)

        val fresh = strictTimeOrchestrator().calculate(request(PURCHASE_PRICE, quote, 100L))
        val stale = strictTimeOrchestrator().calculate(request(PURCHASE_PRICE, quote, 101L))

        assertTrue(fresh is MarketDataCalculationOrchestrationResult.Success)
        assertNotFresh(stale, MarketDataFreshnessError.STALE_ASK)
    }

    @Test
    fun blockedSourceCannotOverrideNotFresh() {
        val result = orchestrator(
            thresholds = thresholds(maxBidAgeMillis = 0L, maxAskAgeMillis = 0L),
            sourceRules = emptyList()
        ).calculate(
            request(PURCHASE_PRICE, marketData = marketData(askTimestampEpochMillis = 0L), 100L)
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.NotFresh)
    }

    @Test
    fun sourcePolicyBecomesEffectiveAfterSuccessfulFreshness() {
        val result = blockedOrchestrator().calculate(request(PURCHASE_PRICE))

        assertTrue(result is MarketDataCalculationOrchestrationResult.SourceBlocked)
    }

    @Test
    fun unknownSourceReturnsSourceNotConfigured() {
        val result = blockedOrchestrator().calculate(request(PURCHASE_PRICE))

        assertEquals(
            MarketDataSourceError.SOURCE_NOT_CONFIGURED,
            (result as MarketDataCalculationOrchestrationResult.SourceBlocked).error
        )
    }

    @Test
    fun unsupportedCalculationTypeReturnsCalculationTypeNotSupported() {
        val result = orchestrator(
            sourceRules = listOf(MarketDataSourceRule(SOURCE_ID, setOf(PURCHASE_PRICE)))
        ).calculate(request(MID))

        assertEquals(
            MarketDataSourceError.CALCULATION_TYPE_NOT_SUPPORTED,
            (result as MarketDataCalculationOrchestrationResult.SourceBlocked).error
        )
    }

    @Test
    fun sourceErrorRemainsSingleAndUnchanged() {
        val result = blockedOrchestrator().calculate(request(PURCHASE_PRICE))
            as MarketDataCalculationOrchestrationResult.SourceBlocked

        assertEquals(listOf("error"), instanceFields(result.javaClass).map { it.name })
        assertEquals(MarketDataSourceError.SOURCE_NOT_CONFIGURED, result.error)
    }

    @Test
    fun sourceBlockedPreventsSuccessfulCalculationResult() {
        val result = blockedOrchestrator().calculate(request(SPREAD))

        assertFalse(result is MarketDataCalculationOrchestrationResult.Success)
        assertTrue(result is MarketDataCalculationOrchestrationResult.SourceBlocked)
    }

    @Test
    fun freshDataFromBlockedSourceRemainsBlocked() {
        val result = blockedOrchestrator().calculate(request(MID, evaluationTimeEpochMillis = NOW))

        assertTrue(result is MarketDataCalculationOrchestrationResult.SourceBlocked)
    }

    @Test
    fun allowedSourceContinuesToCalculation() {
        assertTrue(calculate(request(MID)) is MarketDataCalculationOrchestrationResult.Success)
    }

    @Test
    fun specificationFailureHasHighestPriority() {
        val result = blockedOrchestrator().calculate(
            request(
                PURCHASE_PRICE,
                specification = specification(productIsin = ""),
                marketData = marketData(currency = "usd", ask = null, askTimestampEpochMillis = null)
            )
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.InvalidSpecification)
    }

    @Test
    fun marketDataFailurePrecedesCompatibility() {
        val result = calculate(
            request(PURCHASE_PRICE, marketData = marketData(productIsin = "OTHER", currency = "usd"))
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.InvalidMarketData)
    }

    @Test
    fun compatibilityFailurePrecedesAvailability() {
        val result = calculate(
            request(
                PURCHASE_PRICE,
                marketData = marketData(
                    productIsin = "OTHER",
                    ask = null,
                    askTimestampEpochMillis = null
                )
            )
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.Incompatible)
    }

    @Test
    fun availabilityFailurePrecedesFreshness() {
        val result = staleOrchestrator().calculate(
            request(PURCHASE_PRICE, marketData = marketData(ask = null, askTimestampEpochMillis = null))
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.StructurallyUnavailable)
    }

    @Test
    fun freshnessFailurePrecedesSourcePolicy() {
        val result = orchestrator(
            thresholds = thresholds(maxBidAgeMillis = 0L, maxAskAgeMillis = 0L),
            sourceRules = emptyList()
        ).calculate(
            request(PURCHASE_PRICE, marketData = marketData(askTimestampEpochMillis = 0L), 1L)
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.NotFresh)
    }

    @Test
    fun sourceFailurePrecedesCalculation() {
        val result = blockedOrchestrator().calculate(request(SPREAD))

        assertTrue(result is MarketDataCalculationOrchestrationResult.SourceBlocked)
    }

    @Test
    fun errorsFromDifferentStagesAreNotAggregated() {
        val result = blockedOrchestrator().calculate(
            request(
                PURCHASE_PRICE,
                specification = specification(productIsin = ""),
                marketData = marketData(currency = "usd")
            )
        ) as MarketDataCalculationOrchestrationResult.InvalidSpecification

        assertEquals(listOf(KnockoutProductValidationError.MISSING_PRODUCT_ISIN), result.errors)
    }

    @Test
    fun multipleErrorsRemainWithinSpecificationStage() {
        val result = calculate(
            request(
                PURCHASE_PRICE,
                specification = specification(productIsin = "", issuerId = "")
            )
        ) as MarketDataCalculationOrchestrationResult.InvalidSpecification

        assertEquals(
            listOf(
                KnockoutProductValidationError.MISSING_PRODUCT_ISIN,
                KnockoutProductValidationError.MISSING_ISSUER_ID
            ),
            result.errors
        )
    }

    @Test
    fun calculationFailurePreservesInvalidBidCode() {
        val result = MarketDataCalculationOrchestrationResult.CalculationFailure(
            MarketDataCalculationError.INVALID_BID
        )

        assertEquals(MarketDataCalculationError.INVALID_BID, result.error)
    }

    @Test
    fun calculationFailurePreservesInvalidAskCode() {
        val result = MarketDataCalculationOrchestrationResult.CalculationFailure(
            MarketDataCalculationError.INVALID_ASK
        )

        assertEquals(MarketDataCalculationError.INVALID_ASK, result.error)
    }

    @Test
    fun calculationFailurePreservesBidAboveAskCode() {
        val result = MarketDataCalculationOrchestrationResult.CalculationFailure(
            MarketDataCalculationError.BID_ABOVE_ASK
        )

        assertEquals(MarketDataCalculationError.BID_ABOVE_ASK, result.error)
    }

    @Test
    fun calculationFailureContainsNoExceptionOrMessage() {
        assertEquals(
            listOf("error"),
            instanceFields(MarketDataCalculationOrchestrationResult.CalculationFailure::class.java)
                .map { it.name }
        )
    }

    @Test
    fun identicalRequestAndPoliciesProduceIdenticalResult() {
        val orchestrator = allowedOrchestrator()
        val request = request(SPREAD)

        assertEquals(orchestrator.calculate(request), orchestrator.calculate(request))
    }

    @Test
    fun negativeEvaluationTimeIsForwardedUnchanged() {
        val quote = marketData(
            bidTimestampEpochMillis = -100L,
            askTimestampEpochMillis = -100L
        )

        assertTrue(
            calculate(request(MID, quote, -100L)) is
                MarketDataCalculationOrchestrationResult.Success
        )
    }

    @Test
    fun minimumEvaluationTimeIsForwardedUnchanged() {
        val quote = marketData(
            bidTimestampEpochMillis = Long.MIN_VALUE,
            askTimestampEpochMillis = Long.MIN_VALUE
        )

        assertTrue(
            calculate(request(MID, quote, Long.MIN_VALUE)) is
                MarketDataCalculationOrchestrationResult.Success
        )
    }

    @Test
    fun maximumEvaluationTimeIsForwardedUnchanged() {
        val quote = marketData(
            bidTimestampEpochMillis = Long.MAX_VALUE,
            askTimestampEpochMillis = Long.MAX_VALUE
        )

        assertTrue(
            calculate(request(MID, quote, Long.MAX_VALUE)) is
                MarketDataCalculationOrchestrationResult.Success
        )
    }

    @Test
    fun calculationDoesNotMutateRequestOrModels() {
        val original = request(SPREAD)
        val copy = original.copy(
            specification = original.specification.copy(),
            marketData = original.marketData.copy()
        )

        calculate(original)

        assertEquals(copy, original)
    }

    @Test
    fun calculationDoesNotMutatePolicyConfiguration() {
        val capabilities = mutableSetOf(PURCHASE_PRICE, SALE_PRICE, SPREAD, MID)
        val rules = mutableListOf(MarketDataSourceRule(SOURCE_ID, capabilities))
        val sourcePolicy = MarketDataSourcePolicy(MarketDataSourcePolicyConfig(rules))
        val orchestrator = MarketDataCalculationOrchestrator(freshnessPolicy(), sourcePolicy)

        orchestrator.calculate(request(PURCHASE_PRICE))

        assertEquals(setOf(PURCHASE_PRICE, SALE_PRICE, SPREAD, MID), capabilities)
        assertEquals(1, rules.size)
    }

    @Test
    fun orchestrationDoesNotNormalizeSourceId() {
        val result = calculate(request(PURCHASE_PRICE, marketData = marketData(sourceId = "SOURCE-A")))

        assertEquals(
            MarketDataSourceError.SOURCE_NOT_CONFIGURED,
            (result as MarketDataCalculationOrchestrationResult.SourceBlocked).error
        )
    }

    @Test
    fun successValuesContainOnlyNumericValuesAndCurrency() {
        val fieldNames = valueTypes().associate { type ->
            type.simpleName to instanceFields(type).map { it.name }
        }

        assertEquals(listOf("value", "currency"), fieldNames["PurchasePrice"])
        assertEquals(listOf("value", "currency"), fieldNames["SalePrice"])
        assertEquals(
            listOf("absoluteSpread", "relativeSpreadToAskPercent", "currency"),
            fieldNames["Spread"]
        )
        assertEquals(listOf("value", "currency"), fieldNames["MidPrice"])
    }

    @Test
    fun orchestrationApiContainsNoNetworkRepositoryOrUiTypes() {
        assertTrue(
            publicApiTypeNames().none {
                it.contains("network", true) ||
                    it.contains("repository", true) ||
                    it.contains("presentation", true) ||
                    it.contains("ui.", true)
            }
        )
    }

    @Test
    fun orchestrationApiContainsNoTradeEngineOrKoCalculator() {
        assertTrue(
            publicApiTypeNames().none {
                it.contains("TradeCalculationEngine") || it.contains("KoCalculator")
            }
        )
    }

    @Test
    fun requestAndResultsAreImmutableByPublicApi() {
        val types = listOf(MarketDataCalculationRequest::class.java) + resultTypes() + valueTypes()

        assertTrue(
            types.flatMap { it.declaredMethods.toList() }.none {
                Modifier.isPublic(it.modifiers) && it.name.startsWith("set")
            }
        )
    }

    @Test
    fun noResultPayloadIsNullableByDeclaredJavaType() {
        assertTrue(
            (resultTypes() + valueTypes()).flatMap { instanceFields(it) }.none {
                it.type.name == "java.util.Optional"
            }
        )
    }

    private fun calculate(
        request: MarketDataCalculationRequest
    ): MarketDataCalculationOrchestrationResult = allowedOrchestrator().calculate(request)

    private fun successValue(
        request: MarketDataCalculationRequest
    ): MarketDataCalculationValue = (
        calculate(request) as MarketDataCalculationOrchestrationResult.Success
        ).value

    private fun spreadValue(
        bid: Double = 1.8,
        ask: Double = 2.0,
        currency: String = "EUR"
    ): MarketDataCalculationValue.Spread = successValue(
        request(SPREAD, marketData(bid = bid, ask = ask, currency = currency))
    ) as MarketDataCalculationValue.Spread

    private fun assertUnavailable(
        request: MarketDataCalculationRequest,
        expected: MarketDataCalculationAvailabilityError
    ) {
        val result = calculate(request)
            as MarketDataCalculationOrchestrationResult.StructurallyUnavailable

        assertEquals(listOf(expected), result.errors)
    }

    private fun assertNotFresh(
        result: MarketDataCalculationOrchestrationResult,
        expected: MarketDataFreshnessError
    ) {
        assertEquals(
            listOf(expected),
            (result as MarketDataCalculationOrchestrationResult.NotFresh).errors
        )
    }

    private fun request(
        calculationType: MarketDataCalculationType,
        marketData: KnockoutProductMarketData = marketData(),
        evaluationTimeEpochMillis: Long = NOW,
        specification: KnockoutProductSpecification = specification()
    ) = MarketDataCalculationRequest(
        calculationType = calculationType,
        specification = specification,
        marketData = marketData,
        evaluationTimeEpochMillis = evaluationTimeEpochMillis
    )

    private fun specification(
        productIsin: String = PRODUCT_ISIN,
        productWkn: String? = "ABC123",
        issuerId: String = "issuer-a",
        underlyingId: String = "underlying-a",
        basePrice: Double = 80.0,
        knockoutBarrier: Double = 82.0,
        ratio: Double = 0.1,
        underlyingCurrency: String = "EUR",
        productCurrency: String = "EUR"
    ) = KnockoutProductSpecification(
        productIsin = productIsin,
        productWkn = productWkn,
        issuerId = issuerId,
        underlyingId = underlyingId,
        direction = TradeDirection.LONG,
        basePrice = basePrice,
        knockoutBarrier = knockoutBarrier,
        ratio = ratio,
        underlyingCurrency = underlyingCurrency,
        productCurrency = productCurrency
    )

    private fun marketData(
        productIsin: String = PRODUCT_ISIN,
        bid: Double? = 1.8,
        ask: Double? = 2.0,
        bidTimestampEpochMillis: Long? = NOW,
        askTimestampEpochMillis: Long? = NOW,
        currency: String = "EUR",
        sourceId: String = SOURCE_ID
    ) = KnockoutProductMarketData(
        productIsin = productIsin,
        bid = bid,
        ask = ask,
        bidTimestampEpochMillis = bidTimestampEpochMillis,
        askTimestampEpochMillis = askTimestampEpochMillis,
        currency = currency,
        sourceId = sourceId
    )

    private fun allowedOrchestrator() = orchestrator()

    private fun blockedOrchestrator() = orchestrator(sourceRules = emptyList())

    private fun staleOrchestrator() = orchestrator(
        thresholds = thresholds(maxBidAgeMillis = 10L, maxAskAgeMillis = 10L)
    )

    private fun strictTimeOrchestrator() = orchestrator(
        thresholds = thresholds(
            maxBidAgeMillis = 0L,
            maxAskAgeMillis = 0L,
            maxBidAskDifferenceMillis = 0L,
            allowedFutureSkewMillis = 0L
        )
    )

    private fun orchestrator(
        thresholds: MarketDataFreshnessThresholds = thresholds(),
        sourceRules: List<MarketDataSourceRule> = listOf(
            MarketDataSourceRule(
                sourceId = SOURCE_ID,
                supportedCalculationTypes = MarketDataCalculationType.entries.toSet()
            )
        )
    ) = MarketDataCalculationOrchestrator(
        freshnessPolicy = MarketDataFreshnessPolicy(thresholds),
        sourcePolicy = MarketDataSourcePolicy(MarketDataSourcePolicyConfig(sourceRules))
    )

    private fun freshnessPolicy() = MarketDataFreshnessPolicy(thresholds())

    private fun thresholds(
        maxBidAgeMillis: Long = Long.MAX_VALUE,
        maxAskAgeMillis: Long = Long.MAX_VALUE,
        maxBidAskDifferenceMillis: Long = Long.MAX_VALUE,
        allowedFutureSkewMillis: Long = Long.MAX_VALUE
    ) = MarketDataFreshnessThresholds(
        maxBidAgeMillis = maxBidAgeMillis,
        maxAskAgeMillis = maxAskAgeMillis,
        maxBidAskDifferenceMillis = maxBidAskDifferenceMillis,
        allowedFutureSkewMillis = allowedFutureSkewMillis
    )

    private fun instanceFields(type: Class<*>) = type.declaredFields
        .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }

    private fun resultTypes() = MarketDataCalculationOrchestrationResult::class.java.declaredClasses
        .toList()

    private fun valueTypes() = MarketDataCalculationValue::class.java.declaredClasses.toList()

    private fun publicApiTypeNames(): Set<String> {
        val orchestrator = MarketDataCalculationOrchestrator::class.java
        val methods = orchestrator.declaredMethods.filter { Modifier.isPublic(it.modifiers) }
        val constructors = orchestrator.constructors.toList()

        return buildSet {
            constructors.forEach { constructor ->
                constructor.parameterTypes.forEach { add(it.name) }
            }
            methods.forEach { method ->
                add(method.returnType.name)
                method.parameterTypes.forEach { add(it.name) }
            }
            (resultTypes() + valueTypes() + MarketDataCalculationRequest::class.java).forEach { type ->
                add(type.name)
                instanceFields(type).forEach { add(it.type.name) }
            }
        }
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000TEST001"
        const val SOURCE_ID = "source-a"
        const val NOW = 1_000L

        val PURCHASE_PRICE = MarketDataCalculationType.PURCHASE_PRICE
        val SALE_PRICE = MarketDataCalculationType.SALE_PRICE
        val SPREAD = MarketDataCalculationType.SPREAD
        val MID = MarketDataCalculationType.MID
    }
}
