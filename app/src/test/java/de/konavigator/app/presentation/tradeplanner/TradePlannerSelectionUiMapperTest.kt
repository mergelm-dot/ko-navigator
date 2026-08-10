package de.konavigator.app.presentation.tradeplanner

import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCalculationOutcome
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCalculationPipelineApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCurrencyConversionApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCurrencyConversionEvidence
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCurrencyConversionFailure
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateMarketDataResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidatePlannedEntrySelectionApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateSelectionPipelineApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetLeverageInput
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetSelectionApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithCalculation
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithCalculationAvailability
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithCurrencyConversion
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithDataQuality
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithExistingEntryCalculation
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithFreshness
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithMarketData
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithSourceEvaluation
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithTargetDeviation
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithTargetFit
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithTargetLeveragePlan
import de.konavigator.app.application.productdiscovery.KnockoutProductDiscoveryApplicationResult
import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationError
import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationError
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitError
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitResult
import de.konavigator.app.calculator.TradeCalculationResult
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationError
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.currency.CurrencyConversionCreationResult
import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.freshness.MarketDataFreshnessResult
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue
import de.konavigator.app.domain.source.MarketDataSourceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TradePlannerSelectionUiMapperTest {

    @Test
    fun mapsPrimaryAlternativesAndRawSuccessfulValuesWithoutReordering() {
        val primary = fixture(
            productIsin = "DE000AB12CD3",
            productWkn = "AB12CD",
            issuerId = "hsbc",
            knockoutBarrier = 91.25,
            targetKnockoutBarrier = 88.0,
            theoreticalProductValue = 1.84,
            calculatedLeverageAtEntry = 4.72,
            knockoutDistanceAbsolute = 8.75,
            knockoutDistancePercent = 8.75,
            relativeLeverageDeviationPercent = 5.6,
            barrierDeviationPercentOfPlannedEntry = 3.25,
            leverageWithinTolerance = true,
            barrierWithinTolerance = false,
            withinAllTargetTolerances = false
        )
        val alternativeOne = fixture(
            productIsin = "DE000ALT0001",
            productWkn = null,
            currencyConversion = crossCurrency("USD", "EUR", 1.1)
        )
        val alternativeTwo = fixture(productIsin = "DE000ALT0002", productWkn = "ALT002")
        val alternatives = listOf(alternativeOne.targetFit, alternativeTwo.targetFit)
        val originalAlternatives = alternatives.toList()
        val currencyCandidates = listOf(
            currencyCandidate(
                primary,
                KnockoutProductCandidateCurrencyConversionEvidence.SameCurrency
            ),
            currencyCandidate(
                alternativeOne,
                KnockoutProductCandidateCurrencyConversionEvidence.CrossCurrency(
                    sourceId = "test-fx-source",
                    observedAtEpochMillis = 123_456_789L
                )
            ),
            currencyCandidate(
                alternativeTwo,
                KnockoutProductCandidateCurrencyConversionEvidence.SameCurrency
            )
        )
        val targetResult = KnockoutProductCandidateTargetSelectionApplicationResult
            .SelectedCandidates(
                primaryCandidate = primary.targetFit,
                alternativeCandidates = alternatives,
                targetDeviationFailedCandidates = emptyList(),
                nonMatchingCandidates = emptyList(),
                targetFitFailedCandidates = emptyList()
            )
        val applicationResult = plannedPipelineResult(
            currencyCandidates = currencyCandidates,
            plannedResult = targetSelectionEvaluated(targetResult)
        )

        val result = selected(TradePlannerSelectionUiMapper.map(applicationResult))

        assertEquals("DE000AB12CD3", result.primaryCandidate.productIsin)
        assertEquals("AB12CD", result.primaryCandidate.productWkn)
        assertEquals("hsbc", result.primaryCandidate.issuerId)
        assertEquals("EUR", result.primaryCandidate.productCurrency)
        assertEquals(1.84, result.primaryCandidate.calculatedProductPriceAtPlannedEntry, 0.0)
        assertEquals(4.72, result.primaryCandidate.calculatedLeverageAtPlannedEntry, 0.0)
        assertEquals(91.25, result.primaryCandidate.knockoutBarrier, 0.0)
        assertEquals(88.0, primary.targetPlan.tradeCalculationResult.knockoutPrice!!, 0.0)
        assertTrue(result.primaryCandidate.knockoutBarrier != 88.0)
        assertEquals(8.75, result.primaryCandidate.knockoutDistanceAbsolute, 0.0)
        assertEquals(8.75, result.primaryCandidate.knockoutDistancePercent, 0.0)
        assertEquals(5.6, result.primaryCandidate.relativeLeverageDeviationPercent, 0.0)
        assertEquals(3.25, result.primaryCandidate.barrierDeviationPercentOfPlannedEntry, 0.0)
        assertTrue(result.primaryCandidate.leverageWithinTolerance)
        assertTrue(!result.primaryCandidate.barrierWithinTolerance)
        assertTrue(!result.primaryCandidate.withinAllTargetTolerances)
        assertSame(
            TradePlannerSelectionCurrencyEvidence.SameCurrency,
            result.primaryCandidate.currencyEvidence
        )
        assertEquals(
            listOf("DE000ALT0001", "DE000ALT0002"),
            result.alternativeCandidates.map { it.productIsin }
        )
        assertNull(result.alternativeCandidates[0].productWkn)
        assertEquals(
            TradePlannerSelectionCurrencyEvidence.CrossCurrency(
                sourceId = "test-fx-source",
                observedAtEpochMillis = 123_456_789L
            ),
            result.alternativeCandidates[0].currencyEvidence
        )
        assertEquals(TradePlannerSelectionUiDiagnostics(), result.diagnostics)
        assertSame(primary.targetFit, targetResult.primaryCandidate)
        assertSame(alternativeOne.targetFit, targetResult.alternativeCandidates[0])
        assertSame(alternativeTwo.targetFit, targetResult.alternativeCandidates[1])
        assertEquals(originalAlternatives, alternatives)
        assertSame(
            applicationResult.currencyConversionResult.successfulCandidates,
            currencyCandidates
        )
    }

    @Test
    fun resolvesEqualDuplicateInputsExclusivelyByObjectIdentity() {
        val first = fixture(productIsin = "DUPLICATE")
        val second = fixture(productIsin = "DUPLICATE")
        assertEquals(first.targetLeverageInput, second.targetLeverageInput)
        assertNotSame(first.targetLeverageInput, second.targetLeverageInput)
        val currencyCandidates = listOf(
            currencyCandidate(
                first,
                KnockoutProductCandidateCurrencyConversionEvidence.SameCurrency
            ),
            currencyCandidate(
                second,
                KnockoutProductCandidateCurrencyConversionEvidence.CrossCurrency(
                    sourceId = "second-instance-source",
                    observedAtEpochMillis = 222L
                )
            )
        )
        val applicationResult = selectedPipelineResult(
            primary = second,
            currencyCandidates = currencyCandidates
        )

        val result = selected(TradePlannerSelectionUiMapper.map(applicationResult))

        assertEquals(
            TradePlannerSelectionCurrencyEvidence.CrossCurrency(
                sourceId = "second-instance-source",
                observedAtEpochMillis = 222L
            ),
            result.primaryCandidate.currencyEvidence
        )
        assertSame(first.targetLeverageInput, currencyCandidates[0].targetLeverageInput)
        assertSame(second.targetLeverageInput, currencyCandidates[1].targetLeverageInput)
    }

    @Test
    fun reportsMissingAndAmbiguousCurrencyEvidenceWithoutGuessing() {
        val candidate = fixture(productIsin = "EVIDENCE")
        val missing = selectedPipelineResult(
            primary = candidate,
            currencyCandidates = emptyList()
        )
        val ambiguous = selectedPipelineResult(
            primary = candidate,
            currencyCandidates = listOf(
                currencyCandidate(
                    candidate,
                    KnockoutProductCandidateCurrencyConversionEvidence.SameCurrency
                ),
                currencyCandidate(
                    candidate,
                    KnockoutProductCandidateCurrencyConversionEvidence.CrossCurrency(
                        sourceId = "duplicate-source",
                        observedAtEpochMillis = 333L
                    )
                )
            )
        )

        assertEquals(
            TradePlannerSelectionUiMappingError
                .SELECTED_CANDIDATE_CURRENCY_EVIDENCE_NOT_FOUND,
            inconsistent(TradePlannerSelectionUiMapper.map(missing)).error
        )
        assertEquals(
            TradePlannerSelectionUiMappingError
                .SELECTED_CANDIDATE_CURRENCY_EVIDENCE_AMBIGUOUS,
            inconsistent(TradePlannerSelectionUiMapper.map(ambiguous)).error
        )
    }

    @Test
    fun reportsEachUnexpectedSelectedFailureChainWithoutThrowing() {
        val existingEntryFailure = fixture(
            productIsin = "ENTRY-FAILURE",
            existingEntryResult = ExistingKnockoutProductEntryCalculationResult.Failure(
                ExistingKnockoutProductEntryCalculationError.INVALID_BASE_PRICE
            )
        )
        val deviationFailure = fixture(
            productIsin = "DEVIATION-FAILURE",
            targetDeviationResult = ExistingKnockoutProductTargetDeviationResult.Failure(
                ExistingKnockoutProductTargetDeviationError.INVALID_ACTUAL_LEVERAGE
            )
        )
        val targetFitFailure = fixture(
            productIsin = "FIT-FAILURE",
            targetFitResult = ExistingKnockoutProductTargetFitResult.Failure(
                ExistingKnockoutProductTargetFitError
                    .INVALID_RELATIVE_LEVERAGE_DEVIATION_PERCENT
            )
        )
        val cases = listOf(
            existingEntryFailure to TradePlannerSelectionUiMappingError
                .SELECTED_CANDIDATE_EXISTING_ENTRY_NOT_SUCCESSFUL,
            deviationFailure to TradePlannerSelectionUiMappingError
                .SELECTED_CANDIDATE_TARGET_DEVIATION_NOT_SUCCESSFUL,
            targetFitFailure to TradePlannerSelectionUiMappingError
                .SELECTED_CANDIDATE_TARGET_FIT_NOT_SUCCESSFUL
        )

        cases.forEach { (candidate, expectedError) ->
            val result = selectedPipelineResult(
                primary = candidate,
                currencyCandidates = listOf(
                    currencyCandidate(
                        candidate,
                        KnockoutProductCandidateCurrencyConversionEvidence.SameCurrency
                    )
                )
            )
            assertEquals(
                expectedError,
                inconsistent(TradePlannerSelectionUiMapper.map(result)).error
            )
        }
    }

    @Test
    fun mapsAllBackendNoSelectionGroupsToDistinctMachineReadableReasons() {
        val fixture = fixture(productIsin = "NO-SELECTION")
        val calculationSuccess = calculationSuccess()
        val currencySuccess = currencySuccess()
        val cases: List<
            Pair<
                KnockoutProductCandidateSelectionPipelineApplicationResult,
                TradePlannerSelectionUiNoSelectionReason
                >
            > = listOf(
            discoveryStopped(KnockoutProductDiscoveryApplicationResult.NoCatalogCandidates) to
                TradePlannerSelectionUiNoSelectionReason.NO_CATALOG_CANDIDATES,
            discoveryStopped(
                KnockoutProductDiscoveryApplicationResult.NoBrokerTradableCandidates
            ) to TradePlannerSelectionUiNoSelectionReason.NO_BROKER_TRADABLE_CANDIDATES,
            discoveryStopped(
                KnockoutProductDiscoveryApplicationResult.NoEnabledIssuerCandidates
            ) to TradePlannerSelectionUiNoSelectionReason.NO_ENABLED_ISSUER_CANDIDATES,
            discoveryStopped(
                KnockoutProductDiscoveryApplicationResult.CatalogDataAccessFailure
            ) to TradePlannerSelectionUiNoSelectionReason.CATALOG_DATA_ACCESS_FAILURE,
            discoveryStopped(KnockoutProductDiscoveryApplicationResult.CatalogInvalidData) to
                TradePlannerSelectionUiNoSelectionReason.CATALOG_INVALID_DATA,
            discoveryStopped(
                KnockoutProductDiscoveryApplicationResult.BrokerAvailabilityDataAccessFailure
            ) to TradePlannerSelectionUiNoSelectionReason
                .BROKER_AVAILABILITY_DATA_ACCESS_FAILURE,
            discoveryStopped(
                KnockoutProductDiscoveryApplicationResult.BrokerAvailabilityInvalidData
            ) to TradePlannerSelectionUiNoSelectionReason.BROKER_AVAILABILITY_INVALID_DATA,
            calculationStopped(
                KnockoutProductCandidateCalculationPipelineApplicationResult.MarketDataStopped(
                    KnockoutProductCandidateMarketDataResult.MarketDataNotFound("ISIN")
                )
            ) to TradePlannerSelectionUiNoSelectionReason.MARKET_DATA_NOT_FOUND,
            calculationStopped(
                KnockoutProductCandidateCalculationPipelineApplicationResult.MarketDataStopped(
                    KnockoutProductCandidateMarketDataResult.MarketDataDataAccessFailure("ISIN")
                )
            ) to TradePlannerSelectionUiNoSelectionReason.MARKET_DATA_DATA_ACCESS_FAILURE,
            calculationStopped(
                KnockoutProductCandidateCalculationPipelineApplicationResult.MarketDataStopped(
                    KnockoutProductCandidateMarketDataResult.MarketDataInvalidData("ISIN")
                )
            ) to TradePlannerSelectionUiNoSelectionReason.MARKET_DATA_INVALID_DATA,
            calculationStopped(
                KnockoutProductCandidateCalculationPipelineApplicationResult
                    .NoStructurallyEligibleCandidates(listOf(fixture.dataQuality))
            ) to TradePlannerSelectionUiNoSelectionReason.NO_STRUCTURALLY_ELIGIBLE_CANDIDATES,
            calculationStopped(
                KnockoutProductCandidateCalculationPipelineApplicationResult
                    .NoCalculationAvailableCandidates(
                        blockedDataQualityCandidates = listOf(fixture.dataQuality),
                        calculationUnavailableCandidates = listOf(fixture.availability)
                    )
            ) to TradePlannerSelectionUiNoSelectionReason.NO_CALCULATION_AVAILABLE_CANDIDATES,
            calculationStopped(
                KnockoutProductCandidateCalculationPipelineApplicationResult.NoFreshCandidates(
                    blockedDataQualityCandidates = listOf(fixture.dataQuality),
                    calculationUnavailableCandidates = listOf(fixture.availability),
                    notFreshCandidates = listOf(fixture.freshness)
                )
            ) to TradePlannerSelectionUiNoSelectionReason.NO_FRESH_CANDIDATES,
            calculationStopped(
                KnockoutProductCandidateCalculationPipelineApplicationResult
                    .NoSourceAllowedCandidates(
                        blockedDataQualityCandidates = listOf(fixture.dataQuality),
                        calculationUnavailableCandidates = listOf(fixture.availability),
                        notFreshCandidates = listOf(fixture.freshness),
                        sourceBlockedCandidates = listOf(fixture.source)
                    )
            ) to TradePlannerSelectionUiNoSelectionReason.NO_SOURCE_ALLOWED_CANDIDATES,
            calculationStopped(
                KnockoutProductCandidateCalculationPipelineApplicationResult
                    .NoSuccessfulCalculationCandidates(
                        blockedDataQualityCandidates = listOf(fixture.dataQuality),
                        calculationUnavailableCandidates = listOf(fixture.availability),
                        notFreshCandidates = listOf(fixture.freshness),
                        sourceBlockedCandidates = listOf(fixture.source),
                        failedCalculationCandidates = listOf(fixture.calculation)
                    )
            ) to TradePlannerSelectionUiNoSelectionReason.NO_SUCCESSFUL_CALCULATION_CANDIDATES,
            KnockoutProductCandidateSelectionPipelineApplicationResult.CurrencyConversionStopped(
                calculationPipelineResult = calculationSuccess,
                currencyConversionResult = KnockoutProductCandidateCurrencyConversionApplicationResult
                    .NoCurrencyConvertibleCandidates(listOf(currencyFailure(fixture)))
            ) to TradePlannerSelectionUiNoSelectionReason.NO_CURRENCY_CONVERTIBLE_CANDIDATES,
            plannedPipelineResult(
                currencyCandidates = emptyList(),
                plannedResult = KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                    .NoValidTargetLeveragePlanCandidates(listOf(fixture.targetPlan)),
                calculationResult = calculationSuccess,
                currencyResult = currencySuccess
            ) to TradePlannerSelectionUiNoSelectionReason
                .NO_VALID_TARGET_LEVERAGE_PLAN_CANDIDATES,
            plannedPipelineResult(
                currencyCandidates = emptyList(),
                plannedResult = KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                    .NoSuccessfulExistingEntryCalculationCandidates(
                        invalidTargetLeveragePlanCandidates = listOf(fixture.targetPlan),
                        failedCandidates = listOf(fixture.existingEntry)
                    ),
                calculationResult = calculationSuccess,
                currencyResult = currencySuccess
            ) to TradePlannerSelectionUiNoSelectionReason
                .NO_SUCCESSFUL_EXISTING_ENTRY_CALCULATION_CANDIDATES,
            plannedPipelineResult(
                currencyCandidates = emptyList(),
                plannedResult = targetSelectionEvaluated(
                    KnockoutProductCandidateTargetSelectionApplicationResult
                        .NoSuccessfulTargetDeviationCandidates(listOf(fixture.targetDeviation))
                ),
                calculationResult = calculationSuccess,
                currencyResult = currencySuccess
            ) to TradePlannerSelectionUiNoSelectionReason
                .NO_SUCCESSFUL_TARGET_DEVIATION_CANDIDATES,
            plannedPipelineResult(
                currencyCandidates = emptyList(),
                plannedResult = targetSelectionEvaluated(
                    KnockoutProductCandidateTargetSelectionApplicationResult
                        .NoCandidatesWithinTargetTolerances(
                            targetDeviationFailedCandidates = listOf(fixture.targetDeviation),
                            nonMatchingCandidates = listOf(fixture.targetFit),
                            targetFitFailedCandidates = listOf(fixture.targetFit)
                        )
                ),
                calculationResult = calculationSuccess,
                currencyResult = currencySuccess
            ) to TradePlannerSelectionUiNoSelectionReason.NO_CANDIDATES_WITHIN_TARGET_TOLERANCES
        )

        cases.forEach { (applicationResult, expectedReason) ->
            assertEquals(
                expectedReason,
                noSelection(TradePlannerSelectionUiMapper.map(applicationResult)).reason
            )
        }
    }

    @Test
    fun mapsDefensiveNoInputStatesAsInternalPipelineReasons() {
        val calculationSuccess = calculationSuccess()
        val currencySuccess = currencySuccess()
        val cases: List<
            Pair<
                KnockoutProductCandidateSelectionPipelineApplicationResult,
                TradePlannerSelectionUiNoSelectionReason
                >
            > = listOf(
            calculationStopped(
                KnockoutProductCandidateCalculationPipelineApplicationResult.MarketDataStopped(
                    KnockoutProductCandidateMarketDataResult.NoInputCandidates
                )
            ) to TradePlannerSelectionUiNoSelectionReason.EMPTY_MARKET_DATA_PIPELINE_INPUT,
            KnockoutProductCandidateSelectionPipelineApplicationResult.CurrencyConversionStopped(
                calculationPipelineResult = calculationSuccess,
                currencyConversionResult =
                    KnockoutProductCandidateCurrencyConversionApplicationResult.NoInputCandidates
            ) to TradePlannerSelectionUiNoSelectionReason
                .EMPTY_CURRENCY_CONVERSION_PIPELINE_INPUT,
            plannedPipelineResult(
                currencyCandidates = emptyList(),
                plannedResult =
                    KnockoutProductCandidatePlannedEntrySelectionApplicationResult.NoInputCandidates,
                calculationResult = calculationSuccess,
                currencyResult = currencySuccess
            ) to TradePlannerSelectionUiNoSelectionReason
                .EMPTY_PLANNED_ENTRY_SELECTION_PIPELINE_INPUT,
            plannedPipelineResult(
                currencyCandidates = emptyList(),
                plannedResult = targetSelectionEvaluated(
                    KnockoutProductCandidateTargetSelectionApplicationResult.NoInputCandidates
                ),
                calculationResult = calculationSuccess,
                currencyResult = currencySuccess
            ) to TradePlannerSelectionUiNoSelectionReason.EMPTY_TARGET_SELECTION_PIPELINE_INPUT
        )

        cases.forEach { (applicationResult, expectedReason) ->
            assertEquals(
                expectedReason,
                noSelection(TradePlannerSelectionUiMapper.map(applicationResult)).reason
            )
        }
    }

    @Test
    fun preservesEveryExistingDiagnosticGroupSizeWithoutReclassification() {
        val fixture = fixture(productIsin = "DIAGNOSTICS")
        val calculationResult = calculationSuccess(
            blockedDataQualityCandidates = List(2) { fixture.dataQuality },
            calculationUnavailableCandidates = listOf(fixture.availability),
            notFreshCandidates = List(3) { fixture.freshness },
            sourceBlockedCandidates = listOf(fixture.source),
            failedCalculationCandidates = List(2) { fixture.calculation }
        )
        val currencyResult = KnockoutProductCandidateCurrencyConversionApplicationResult
            .CandidatesWithCurrencyConversion(
                successfulCandidates = listOf(
                    currencyCandidate(
                        fixture,
                        KnockoutProductCandidateCurrencyConversionEvidence.SameCurrency
                    )
                ),
                failedCandidates = List(2) { currencyFailure(fixture) }
            )
        val targetResult = KnockoutProductCandidateTargetSelectionApplicationResult
            .NoCandidatesWithinTargetTolerances(
                targetDeviationFailedCandidates = List(2) { fixture.targetDeviation },
                nonMatchingCandidates = List(4) { fixture.targetFit },
                targetFitFailedCandidates = listOf(fixture.targetFit)
            )
        val applicationResult = plannedPipelineResult(
            currencyCandidates = currencyResult.successfulCandidates,
            plannedResult = KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                .TargetSelectionEvaluated(
                    targetSelectionResult = targetResult,
                    invalidTargetLeveragePlanCandidates = listOf(fixture.targetPlan),
                    existingEntryFailedCandidates = listOf(fixture.existingEntry)
                ),
            calculationResult = calculationResult,
            currencyResult = currencyResult
        )

        val result = noSelection(TradePlannerSelectionUiMapper.map(applicationResult))

        assertEquals(
            TradePlannerSelectionUiDiagnostics(
                dataQualityBlockedCount = 2,
                calculationUnavailableCount = 1,
                notFreshCount = 3,
                sourceBlockedCount = 1,
                calculationFailedCount = 2,
                currencyConversionFailedCount = 2,
                invalidTargetLeveragePlanCount = 1,
                existingEntryFailedCount = 1,
                targetDeviationFailedCount = 2,
                nonMatchingTargetFitCount = 4,
                targetFitFailedCount = 1
            ),
            result.diagnostics
        )
    }

    @Test
    fun reportsStructurallyImpossibleStoppedSuccessResults() {
        val fixture = fixture(productIsin = "INCONSISTENT")
        val calculationStops = listOf(
            calculationStopped(calculationSuccess()),
            discoveryStopped(
                KnockoutProductDiscoveryApplicationResult.BrokerTradableCandidates(
                    listOf(fixture.snapshot)
                )
            ),
            calculationStopped(
                KnockoutProductCandidateCalculationPipelineApplicationResult.MarketDataStopped(
                    KnockoutProductCandidateMarketDataResult.CandidatesWithMarketData(
                        listOf(fixture.marketData)
                    )
                )
            )
        )
        calculationStops.forEach { result ->
            assertEquals(
                TradePlannerSelectionUiMappingError
                    .CALCULATION_PIPELINE_STOPPED_WITH_SUCCESS_RESULT,
                inconsistent(TradePlannerSelectionUiMapper.map(result)).error
            )
        }

        val currencyStop = KnockoutProductCandidateSelectionPipelineApplicationResult
            .CurrencyConversionStopped(
                calculationPipelineResult = calculationSuccess(),
                currencyConversionResult = currencySuccess()
            )
        assertEquals(
            TradePlannerSelectionUiMappingError
                .CURRENCY_CONVERSION_STOPPED_WITH_SUCCESS_RESULT,
            inconsistent(TradePlannerSelectionUiMapper.map(currencyStop)).error
        )
    }

    private fun selectedPipelineResult(
        primary: CandidateFixture,
        currencyCandidates: List<KnockoutProductCandidateWithCurrencyConversion>
    ) = plannedPipelineResult(
        currencyCandidates = currencyCandidates,
        plannedResult = targetSelectionEvaluated(
            KnockoutProductCandidateTargetSelectionApplicationResult.SelectedCandidates(
                primaryCandidate = primary.targetFit,
                alternativeCandidates = emptyList(),
                targetDeviationFailedCandidates = emptyList(),
                nonMatchingCandidates = emptyList(),
                targetFitFailedCandidates = emptyList()
            )
        )
    )

    private fun plannedPipelineResult(
        currencyCandidates: List<KnockoutProductCandidateWithCurrencyConversion>,
        plannedResult: KnockoutProductCandidatePlannedEntrySelectionApplicationResult,
        calculationResult:
            KnockoutProductCandidateCalculationPipelineApplicationResult
                .SuccessfulCalculationCandidates = calculationSuccess(),
        currencyResult:
            KnockoutProductCandidateCurrencyConversionApplicationResult
                .CandidatesWithCurrencyConversion = currencySuccess(currencyCandidates)
    ) = KnockoutProductCandidateSelectionPipelineApplicationResult
        .PlannedEntrySelectionEvaluated(
            calculationPipelineResult = calculationResult,
            currencyConversionResult = currencyResult,
            plannedEntrySelectionResult = plannedResult
        )

    private fun targetSelectionEvaluated(
        result: KnockoutProductCandidateTargetSelectionApplicationResult
    ) = KnockoutProductCandidatePlannedEntrySelectionApplicationResult.TargetSelectionEvaluated(
        targetSelectionResult = result,
        invalidTargetLeveragePlanCandidates = emptyList(),
        existingEntryFailedCandidates = emptyList()
    )

    private fun discoveryStopped(
        result: KnockoutProductDiscoveryApplicationResult
    ) = calculationStopped(
        KnockoutProductCandidateCalculationPipelineApplicationResult.DiscoveryStopped(result)
    )

    private fun calculationStopped(
        result: KnockoutProductCandidateCalculationPipelineApplicationResult
    ) = KnockoutProductCandidateSelectionPipelineApplicationResult.CalculationPipelineStopped(
        result
    )

    private fun calculationSuccess(
        blockedDataQualityCandidates: List<KnockoutProductCandidateWithDataQuality> = emptyList(),
        calculationUnavailableCandidates:
            List<KnockoutProductCandidateWithCalculationAvailability> = emptyList(),
        notFreshCandidates: List<KnockoutProductCandidateWithFreshness> = emptyList(),
        sourceBlockedCandidates: List<KnockoutProductCandidateWithSourceEvaluation> = emptyList(),
        failedCalculationCandidates: List<KnockoutProductCandidateWithCalculation> = emptyList()
    ) = KnockoutProductCandidateCalculationPipelineApplicationResult
        .SuccessfulCalculationCandidates(
            successfulCandidates = emptyList(),
            blockedDataQualityCandidates = blockedDataQualityCandidates,
            calculationUnavailableCandidates = calculationUnavailableCandidates,
            notFreshCandidates = notFreshCandidates,
            sourceBlockedCandidates = sourceBlockedCandidates,
            failedCalculationCandidates = failedCalculationCandidates
        )

    private fun currencySuccess(
        candidates: List<KnockoutProductCandidateWithCurrencyConversion> = emptyList()
    ) = KnockoutProductCandidateCurrencyConversionApplicationResult
        .CandidatesWithCurrencyConversion(
            successfulCandidates = candidates,
            failedCandidates = emptyList()
        )

    private fun currencyCandidate(
        fixture: CandidateFixture,
        evidence: KnockoutProductCandidateCurrencyConversionEvidence
    ) = KnockoutProductCandidateWithCurrencyConversion(
        targetLeverageInput = fixture.targetLeverageInput,
        evidence = evidence
    )

    private fun currencyFailure(
        fixture: CandidateFixture
    ) = KnockoutProductCandidateCurrencyConversionFailure.InvalidUnderlyingCurrency(
        candidateWithCalculation = fixture.calculation,
        error = CurrencyCodeCreationError.INVALID_FORMAT
    )

    private fun fixture(
        productIsin: String,
        productWkn: String? = "SYN001",
        issuerId: String = "synthetic-issuer",
        knockoutBarrier: Double = 91.25,
        targetKnockoutBarrier: Double = 88.0,
        theoreticalProductValue: Double = 1.84,
        calculatedLeverageAtEntry: Double = 4.72,
        knockoutDistanceAbsolute: Double = 8.75,
        knockoutDistancePercent: Double = 8.75,
        relativeLeverageDeviationPercent: Double = 5.6,
        barrierDeviationPercentOfPlannedEntry: Double = 3.25,
        leverageWithinTolerance: Boolean = true,
        barrierWithinTolerance: Boolean = true,
        withinAllTargetTolerances: Boolean = true,
        currencyConversion: CurrencyConversion? = null,
        existingEntryResult: ExistingKnockoutProductEntryCalculationResult? = null,
        targetDeviationResult: ExistingKnockoutProductTargetDeviationResult? = null,
        targetFitResult: ExistingKnockoutProductTargetFitResult? = null
    ): CandidateFixture {
        val eur = currency("EUR")
        val effectiveConversion = currencyConversion ?: CurrencyConversion.SameCurrency(eur)
        val snapshot = KnockoutProductSpecificationSnapshot(
            specification = KnockoutProductSpecification(
                productIsin = productIsin,
                productWkn = productWkn,
                issuerId = issuerId,
                underlyingId = "SYNTHETIC-UNDERLYING",
                direction = TradeDirection.LONG,
                basePrice = 90.0,
                knockoutBarrier = knockoutBarrier,
                ratio = 0.1,
                underlyingCurrency = effectiveConversion.underlyingCurrency.value,
                productCurrency = effectiveConversion.productCurrency.value
            ),
            sourceId = "SYNTHETIC-SPECIFICATION-SOURCE",
            retrievedAtEpochMillis = 1_000L,
            sourceTimestampEpochMillis = 900L
        )
        val marketData = KnockoutProductCandidateWithMarketData(
            specificationSnapshot = snapshot,
            marketData = KnockoutProductMarketData(
                productIsin = productIsin,
                bid = 1.8,
                ask = 1.9,
                bidTimestampEpochMillis = 995L,
                askTimestampEpochMillis = 995L,
                currency = effectiveConversion.productCurrency.value,
                sourceId = "SYNTHETIC-MARKET-SOURCE"
            )
        )
        val dataQuality = KnockoutProductCandidateWithDataQuality(
            candidateWithMarketData = marketData,
            dataQualityAssessment = DataQualityAssessment.passed()
        )
        val availability = KnockoutProductCandidateWithCalculationAvailability(
            candidateWithDataQuality = dataQuality,
            availabilityResult = MarketDataCalculationAvailabilityResult.StructurallyAvailable
        )
        val freshness = KnockoutProductCandidateWithFreshness(
            candidateWithCalculationAvailability = availability,
            freshnessResult = MarketDataFreshnessResult.Fresh
        )
        val source = KnockoutProductCandidateWithSourceEvaluation(
            candidateWithFreshness = freshness,
            sourceResult = MarketDataSourceResult.Allowed
        )
        val calculation = KnockoutProductCandidateWithCalculation(
            candidateWithSourceEvaluation = source,
            calculationOutcome = KnockoutProductCandidateCalculationOutcome.Success(
                MarketDataCalculationValue.MidPrice(
                    value = 1.85,
                    currency = effectiveConversion.productCurrency.value
                )
            )
        )
        val targetLeverageInput = KnockoutProductCandidateTargetLeverageInput(
            candidateWithCalculation = calculation,
            currencyConversion = effectiveConversion
        )
        val targetPlan = KnockoutProductCandidateWithTargetLeveragePlan(
            input = targetLeverageInput,
            tradeCalculationResult = TradeCalculationResult(
                isValid = true,
                underlyingPrice = 100.0,
                targetLeverage = 5.0,
                knockoutPrice = targetKnockoutBarrier,
                theoreticalValueInUnderlyingCurrency = 20.0,
                theoreticalProductValue = 2.0,
                underlyingExposureInProductCurrency = 10.0,
                calculatedTheoreticalLeverageAtEntry = 5.0,
                underlyingCurrency = effectiveConversion.underlyingCurrency,
                productCurrency = effectiveConversion.productCurrency,
                distanceToKnockoutAbsolute = 12.0,
                distanceToKnockoutPercent = 12.0
            )
        )
        val effectiveExistingEntryResult = existingEntryResult ?:
            ExistingKnockoutProductEntryCalculationResult.Success(
                intrinsicValueInUnderlyingCurrency = 18.0,
                theoreticalProductValue = theoreticalProductValue,
                knockoutDistanceAbsolute = knockoutDistanceAbsolute,
                knockoutDistancePercent = knockoutDistancePercent,
                underlyingExposureInProductCurrency = 10.0,
                calculatedLeverageAtEntry = calculatedLeverageAtEntry,
                underlyingCurrency = effectiveConversion.underlyingCurrency,
                productCurrency = effectiveConversion.productCurrency
            )
        val existingEntry = KnockoutProductCandidateWithExistingEntryCalculation(
            candidateWithTargetLeveragePlan = targetPlan,
            existingEntryCalculationResult = effectiveExistingEntryResult
        )
        val effectiveTargetDeviationResult = targetDeviationResult ?:
            ExistingKnockoutProductTargetDeviationResult.Success(
                leverageDifference = -0.28,
                absoluteLeverageDeviation = 0.28,
                relativeLeverageDeviationPercent = relativeLeverageDeviationPercent,
                barrierDifference = 3.25,
                absoluteBarrierDeviation = 3.25,
                barrierDeviationPercentOfPlannedEntry =
                    barrierDeviationPercentOfPlannedEntry
            )
        val targetDeviation = KnockoutProductCandidateWithTargetDeviation(
            candidateWithExistingEntryCalculation = existingEntry,
            targetDeviationResult = effectiveTargetDeviationResult
        )
        val effectiveTargetFitResult = targetFitResult ?:
            ExistingKnockoutProductTargetFitResult.Success(
                leverageWithinTolerance = leverageWithinTolerance,
                barrierWithinTolerance = barrierWithinTolerance,
                withinAllTargetTolerances = withinAllTargetTolerances
            )
        val targetFit = KnockoutProductCandidateWithTargetFit(
            candidateWithTargetDeviation = targetDeviation,
            targetFitResult = effectiveTargetFitResult
        )
        return CandidateFixture(
            snapshot = snapshot,
            marketData = marketData,
            dataQuality = dataQuality,
            availability = availability,
            freshness = freshness,
            source = source,
            calculation = calculation,
            targetLeverageInput = targetLeverageInput,
            targetPlan = targetPlan,
            existingEntry = existingEntry,
            targetDeviation = targetDeviation,
            targetFit = targetFit
        )
    }

    private fun crossCurrency(
        underlyingCurrency: String,
        productCurrency: String,
        rate: Double
    ): CurrencyConversion.CrossCurrency = when (
        val result = CurrencyConversion.CrossCurrency.create(
            underlyingCurrency = currency(underlyingCurrency),
            productCurrency = currency(productCurrency),
            underlyingCurrencyPerProductCurrencyRate = rate
        )
    ) {
        is CurrencyConversionCreationResult.Success -> result.conversion
        is CurrencyConversionCreationResult.Failure ->
            error("Unexpected invalid synthetic conversion: ${result.error}")
    }

    private fun currency(value: String): CurrencyCode = when (val result = CurrencyCode.create(value)) {
        is CurrencyCodeCreationResult.Success -> result.currencyCode
        is CurrencyCodeCreationResult.Failure ->
            error("Unexpected invalid synthetic currency: ${result.error}")
    }

    private fun selected(result: TradePlannerSelectionUiResult): TradePlannerSelectionUiResult.Selected {
        assertTrue(result is TradePlannerSelectionUiResult.Selected)
        return result as TradePlannerSelectionUiResult.Selected
    }

    private fun noSelection(
        result: TradePlannerSelectionUiResult
    ): TradePlannerSelectionUiResult.NoSelection {
        assertTrue(result is TradePlannerSelectionUiResult.NoSelection)
        return result as TradePlannerSelectionUiResult.NoSelection
    }

    private fun inconsistent(
        result: TradePlannerSelectionUiResult
    ): TradePlannerSelectionUiResult.InconsistentData {
        assertTrue(result is TradePlannerSelectionUiResult.InconsistentData)
        return result as TradePlannerSelectionUiResult.InconsistentData
    }

    private data class CandidateFixture(
        val snapshot: KnockoutProductSpecificationSnapshot,
        val marketData: KnockoutProductCandidateWithMarketData,
        val dataQuality: KnockoutProductCandidateWithDataQuality,
        val availability: KnockoutProductCandidateWithCalculationAvailability,
        val freshness: KnockoutProductCandidateWithFreshness,
        val source: KnockoutProductCandidateWithSourceEvaluation,
        val calculation: KnockoutProductCandidateWithCalculation,
        val targetLeverageInput: KnockoutProductCandidateTargetLeverageInput,
        val targetPlan: KnockoutProductCandidateWithTargetLeveragePlan,
        val existingEntry: KnockoutProductCandidateWithExistingEntryCalculation,
        val targetDeviation: KnockoutProductCandidateWithTargetDeviation,
        val targetFit: KnockoutProductCandidateWithTargetFit
    )
}
