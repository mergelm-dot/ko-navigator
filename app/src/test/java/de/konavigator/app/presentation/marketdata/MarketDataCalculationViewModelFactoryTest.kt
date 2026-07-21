package de.konavigator.app.presentation.marketdata

import android.content.Context
import androidx.lifecycle.ViewModel
import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationService
import de.konavigator.app.data.inmemory.InMemoryKnockoutProductMarketDataRepository
import de.konavigator.app.data.inmemory.InMemoryKnockoutProductSpecificationRepository
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrator
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataCalculationViewModelFactoryTest {

    @Test
    fun factoryCreatesMarketDataCalculationViewModel() {
        val viewModel = factory().create(MarketDataCalculationViewModel::class.java)

        assertEquals(MarketDataCalculationViewModel::class.java, viewModel.javaClass)
    }

    @Test
    fun createdViewModelUsesExactApplicationService() {
        val applicationService = applicationService()
        val viewModel = MarketDataCalculationViewModelFactory(applicationService)
            .create(MarketDataCalculationViewModel::class.java)
        val field = MarketDataCalculationViewModel::class.java
            .getDeclaredField("applicationService")
            .apply { isAccessible = true }

        assertSame(applicationService, field.get(viewModel))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownViewModelClassIsRejected() {
        factory().create(UnknownViewModel::class.java)
    }

    @Test
    fun factoryHasNoRepositoryPolicyOrOrchestratorDependency() {
        val parameterTypes = MarketDataCalculationViewModelFactory::class.java
            .constructors
            .single()
            .parameterTypes
            .toList()

        assertEquals(listOf(MarketDataCalculationApplicationService::class.java), parameterTypes)
        assertTrue(
            parameterTypes.none { type ->
                listOf("repository", "policy", "orchestrator").any {
                    type.name.contains(it, ignoreCase = true)
                }
            }
        )
    }

    @Test
    fun factoryHasNoAndroidContextDependency() {
        val apiTypes = buildSet {
            MarketDataCalculationViewModelFactory::class.java.constructors.forEach { constructor ->
                constructor.parameterTypes.forEach { add(it) }
            }
            MarketDataCalculationViewModelFactory::class.java.declaredMethods
                .filter { Modifier.isPublic(it.modifiers) }
                .forEach { method ->
                    add(method.returnType)
                    method.parameterTypes.forEach { add(it) }
                }
        }

        assertTrue(apiTypes.none { Context::class.java.isAssignableFrom(it) })
    }

    private fun factory() = MarketDataCalculationViewModelFactory(applicationService())

    private fun applicationService(): MarketDataCalculationApplicationService {
        val orchestrator = MarketDataCalculationOrchestrator(
            freshnessPolicy = MarketDataFreshnessPolicy(
                MarketDataFreshnessThresholds(
                    maxBidAgeMillis = 0L,
                    maxAskAgeMillis = 0L,
                    maxBidAskDifferenceMillis = 0L,
                    allowedFutureSkewMillis = 0L
                )
            ),
            sourcePolicy = MarketDataSourcePolicy(MarketDataSourcePolicyConfig(emptyList()))
        )
        return MarketDataCalculationApplicationService(
            specificationRepository = InMemoryKnockoutProductSpecificationRepository(emptyList()),
            marketDataRepository = InMemoryKnockoutProductMarketDataRepository(emptyList()),
            orchestrator = orchestrator
        )
    }

    private class UnknownViewModel : ViewModel()
}
