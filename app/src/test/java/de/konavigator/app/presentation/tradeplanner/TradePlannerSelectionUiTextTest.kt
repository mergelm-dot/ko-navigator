package de.konavigator.app.presentation.tradeplanner

import org.junit.Assert.assertTrue
import org.junit.Test

class TradePlannerSelectionUiTextTest {

    @Test
    fun everyInputErrorHasAResourceMapping() {
        TradePlannerSelectionUiInputError.entries.forEach { error ->
            assertTrue(TradePlannerSelectionUiText.inputErrorResource(error) != 0)
        }
    }

    @Test
    fun everyNoSelectionReasonHasAResourceMapping() {
        TradePlannerSelectionUiNoSelectionReason.entries.forEach { reason ->
            assertTrue(TradePlannerSelectionUiText.noSelectionReasonResource(reason) != 0)
        }
    }

    @Test
    fun everyMappingErrorHasAResourceMapping() {
        TradePlannerSelectionUiMappingError.entries.forEach { error ->
            assertTrue(TradePlannerSelectionUiText.mappingErrorResource(error) != 0)
        }
    }
}
