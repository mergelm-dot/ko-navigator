package de.konavigator.app.debug.marketdata

import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationService
import de.konavigator.app.data.inmemory.InMemoryKnockoutProductMarketDataRepository
import de.konavigator.app.data.inmemory.InMemoryKnockoutProductSpecificationRepository
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiError
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiResult
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiSubmission
import de.konavigator.app.presentation.marketdata.MarketDataCalculationViewModel
import de.konavigator.app.presentation.marketdata.MarketDataCalculationViewModelFactory
import java.lang.reflect.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarketDataCalculationDemoCompositionTest {

    private lateinit var mainDispatcher: TestDispatcher

    @Before
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun graphCalculatesPurchasePriceForFixedDemoInput() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        fillInputs(viewModel, MarketDataCalculationType.PURCHASE_PRICE)

        viewModel.onCalculateClicked()
        advanceUntilIdle()

        assertEquals(
            MarketDataCalculationUiResult.PurchasePrice(2.0, "EUR"),
            completedResult(viewModel)
        )
    }

    @Test
    fun allFourCalculationTypesProduceTypedSuccessResults() = runTest(mainDispatcher) {
        val expectedTypes = mapOf(
            MarketDataCalculationType.PURCHASE_PRICE to
                MarketDataCalculationUiResult.PurchasePrice::class.java,
            MarketDataCalculationType.SALE_PRICE to
                MarketDataCalculationUiResult.SalePrice::class.java,
            MarketDataCalculationType.SPREAD to
                MarketDataCalculationUiResult.Spread::class.java,
            MarketDataCalculationType.MID to
                MarketDataCalculationUiResult.MidPrice::class.java
        )

        expectedTypes.forEach { (calculationType, expectedType) ->
            val viewModel = createViewModel()
            fillInputs(viewModel, calculationType)

            viewModel.onCalculateClicked()
            advanceUntilIdle()

            assertTrue(expectedType.isInstance(completedResult(viewModel)))
        }
    }

    @Test
    fun graphUsesOnlyNeutralIdentifiersAndFixedEvaluationTime() = runTest(mainDispatcher) {
        val factory = MarketDataCalculationDemoComposition.createFactory()
        val service = readField<MarketDataCalculationApplicationService>(
            factory,
            "applicationService"
        )
        val specificationRepository =
            readField<InMemoryKnockoutProductSpecificationRepository>(
                service,
                "specificationRepository"
            )
        val marketDataRepository = readField<InMemoryKnockoutProductMarketDataRepository>(
            service,
            "marketDataRepository"
        )
        val specification = readField<Map<String, KnockoutProductSpecification>>(
            specificationRepository,
            "specificationsByProductIsin"
        ).values.single()
        val marketData = readField<Map<String, KnockoutProductMarketData>>(
            marketDataRepository,
            "marketDataByProductIsin"
        ).values.single()

        assertEquals("DE000DEMO001", specification.productIsin)
        assertEquals("demo-issuer", specification.issuerId)
        assertEquals("demo-underlying", specification.underlyingId)
        assertEquals("demo-source", marketData.sourceId)
        assertEquals(1_700_000_000_000L, marketData.bidTimestampEpochMillis)
        assertEquals(1_700_000_000_000L, marketData.askTimestampEpochMillis)
        assertEquals(
            listOf("createFactory"),
            MarketDataCalculationDemoComposition::class.java.declaredMethods
                .filter { Modifier.isPublic(it.modifiers) }
                .map { it.name }
        )

        val viewModel = factory.create(MarketDataCalculationViewModel::class.java)
        fillInputs(
            viewModel = viewModel,
            calculationType = MarketDataCalculationType.PURCHASE_PRICE,
            evaluationTime = 1_700_000_000_001L
        )
        viewModel.onCalculateClicked()
        advanceUntilIdle()

        assertEquals(
            MarketDataCalculationUiResult.Failure(
                MarketDataCalculationUiError.MARKET_DATA_NOT_FRESH
            ),
            completedResult(viewModel)
        )
    }

    @Test
    fun separateFactoriesProduceIndependentDeterministicGraphs() = runTest(mainDispatcher) {
        val firstFactory = MarketDataCalculationDemoComposition.createFactory()
        val secondFactory = MarketDataCalculationDemoComposition.createFactory()
        val firstViewModel = firstFactory.create(MarketDataCalculationViewModel::class.java)
        val secondViewModel = secondFactory.create(MarketDataCalculationViewModel::class.java)

        assertNotSame(firstFactory, secondFactory)
        assertNotSame(firstViewModel, secondViewModel)
        firstViewModel.onProductIsinChanged("CHANGED")
        assertEquals("", secondViewModel.uiState.value.productIsinInput)

        fillInputs(firstViewModel, MarketDataCalculationType.MID)
        fillInputs(secondViewModel, MarketDataCalculationType.MID)
        firstViewModel.onCalculateClicked()
        secondViewModel.onCalculateClicked()
        advanceUntilIdle()

        assertEquals(completedResult(firstViewModel), completedResult(secondViewModel))
    }

    private fun createViewModel(): MarketDataCalculationViewModel =
        MarketDataCalculationDemoComposition.createFactory()
            .create(MarketDataCalculationViewModel::class.java)

    private fun fillInputs(
        viewModel: MarketDataCalculationViewModel,
        calculationType: MarketDataCalculationType,
        evaluationTime: Long = 1_700_000_000_000L
    ) {
        viewModel.onProductIsinChanged("DE000DEMO001")
        viewModel.onCalculationTypeChanged(calculationType)
        viewModel.onEvaluationTimeChanged(evaluationTime.toString())
    }

    private fun completedResult(
        viewModel: MarketDataCalculationViewModel
    ): MarketDataCalculationUiResult =
        (viewModel.uiState.value.submission as
            MarketDataCalculationUiSubmission.Completed).result

    @Suppress("UNCHECKED_CAST")
    private fun <T> readField(instance: Any, fieldName: String): T =
        instance.javaClass.getDeclaredField(fieldName)
            .apply { isAccessible = true }
            .get(instance) as T
}
