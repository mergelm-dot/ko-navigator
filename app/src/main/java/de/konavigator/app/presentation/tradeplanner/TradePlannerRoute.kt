package de.konavigator.app.presentation.tradeplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.konavigator.app.screens.TradePlannerScreen

/**
 * Lifecycle-sicherer Compose-Adapter zwischen [TradePlannerViewModel] und dem
 * state-gesteuerten [TradePlannerScreen].
 *
 * Die Route besitzt keinen eigenen State und kennt weder Factory, Composition,
 * Application-Service noch Berechnungsengine.
 */
@Composable
fun TradePlannerRoute(
    viewModel: TradePlannerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TradePlannerScreen(
        state = state,
        onCurrentPriceChanged = viewModel::onCurrentPriceChanged,
        onPlannedEntryPriceChanged = viewModel::onPlannedEntryPriceChanged,
        onTargetLeverageChanged = viewModel::onTargetLeverageChanged,
        onDirectionChanged = viewModel::onDirectionChanged,
        onCalculateClicked = viewModel::onCalculateClicked,
        modifier = modifier
    )
}
