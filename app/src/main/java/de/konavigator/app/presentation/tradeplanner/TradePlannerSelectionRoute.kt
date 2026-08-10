package de.konavigator.app.presentation.tradeplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.konavigator.app.screens.TradePlannerSelectionScreen

/** Lifecycle-sichere Route des parallelen Selection-UI-Pfads. */
@Composable
fun TradePlannerSelectionRoute(
    viewModel: TradePlannerSelectionViewModel,
    brokerOptions: List<TradePlannerBrokerUiOption>,
    issuerOptions: List<TradePlannerIssuerUiOption>,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TradePlannerSelectionScreen(
        state = state,
        brokerOptions = brokerOptions,
        issuerOptions = issuerOptions,
        onUnderlyingSelected = viewModel::onUnderlyingSelected,
        onBrokerSelected = viewModel::onBrokerSelected,
        onEnabledIssuerIdsChanged = viewModel::onEnabledIssuerIdsChanged,
        onCurrentPriceChanged = viewModel::onCurrentPriceChanged,
        onPlannedEntryPriceChanged = viewModel::onPlannedEntryPriceChanged,
        onTargetLeverageChanged = viewModel::onTargetLeverageChanged,
        onDirectionChanged = viewModel::onDirectionChanged,
        onCalculateClicked = viewModel::onCalculateClicked,
        modifier = modifier
    )
}
