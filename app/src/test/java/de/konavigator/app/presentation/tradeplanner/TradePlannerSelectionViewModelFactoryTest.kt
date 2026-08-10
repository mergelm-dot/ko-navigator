package de.konavigator.app.presentation.tradeplanner

import androidx.lifecycle.ViewModel
import de.konavigator.app.domain.availability.MarketDataCalculationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TradePlannerSelectionViewModelFactoryTest {

    @Test
    fun createBuildsSelectionViewModelWithTheThreeInjectedDependencies() {
        val executor = TradePlannerSelectionExecutor {
            error("The factory test must not execute the pipeline.")
        }
        val settings = TradePlannerSelectionExecutionSettings(
            calculationType = MarketDataCalculationType.MID,
            maxFxAgeMillis = 5_000L,
            maxRelativeLeverageDeviationPercent = 5.0,
            maxBarrierDeviationPercentOfPlannedEntry = 10.0
        )
        val timeProvider = TradePlannerSelectionEvaluationTimeProvider { 123L }

        val result = TradePlannerSelectionViewModelFactory(
            selectionExecutor = executor,
            executionSettings = settings,
            evaluationTimeProvider = timeProvider
        ).create(TradePlannerSelectionViewModel::class.java)

        assertEquals(TradePlannerSelectionViewModel::class.java, result.javaClass)
        assertSame(
            executor,
            result.javaClass.getDeclaredField("selectionExecutor").apply {
                isAccessible = true
            }.get(result)
        )
        assertSame(
            settings,
            result.javaClass.getDeclaredField("executionSettings").apply {
                isAccessible = true
            }.get(result)
        )
        assertSame(
            timeProvider,
            result.javaClass.getDeclaredField("evaluationTimeProvider").apply {
                isAccessible = true
            }.get(result)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun createRejectsUnknownViewModelClass() {
        TradePlannerSelectionViewModelFactory(
            selectionExecutor = TradePlannerSelectionExecutor {
                error("The factory test must not execute the pipeline.")
            },
            executionSettings = TradePlannerSelectionExecutionSettings(
                calculationType = MarketDataCalculationType.MID,
                maxFxAgeMillis = 1L,
                maxRelativeLeverageDeviationPercent = 1.0,
                maxBarrierDeviationPercentOfPlannedEntry = 1.0
            ),
            evaluationTimeProvider = TradePlannerSelectionEvaluationTimeProvider { 1L }
        ).create(UnrelatedViewModel::class.java)
    }

    private class UnrelatedViewModel : ViewModel()
}
