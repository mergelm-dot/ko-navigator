package de.konavigator.app.debug.tradeplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import de.konavigator.app.R
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionRoute
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionViewModel
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionViewModelFactory
import de.konavigator.app.ui.theme.KONavigatorTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Separate debug-only launcher for controlled synthetic selection data. */
class TradePlannerSelectionDemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KONavigatorTheme {
                TradePlannerSelectionDeviceDemoContent(
                    workingDirectory = File(filesDir, "trade-planner-selection-demo"),
                    createViewModel = { factory ->
                        ViewModelProvider(this, factory)[
                            TradePlannerSelectionViewModel::class.java
                        ]
                    }
                )
            }
        }
    }
}

@Composable
private fun TradePlannerSelectionDeviceDemoContent(
    workingDirectory: File,
    createViewModel: (TradePlannerSelectionViewModelFactory) -> TradePlannerSelectionViewModel
) {
    val demoResult by produceState<HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult?>(
        initialValue = null,
        workingDirectory
    ) {
        value = withContext(Dispatchers.IO) {
            HsbcDeutscheBoerseTradePlannerSelectionDeviceDemo.create(workingDirectory)
        }
    }

    when (val result = demoResult) {
        null -> DemoLoadingContent()

        is HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult.Failure ->
            DemoFailureContent(result.error)

        is HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult.Success -> {
            val viewModel = createViewModel(result.viewModelFactory)

            TradePlannerSelectionDeviceDemoReadyContent(
                viewModel = viewModel,
                brokerOptions = result.brokerOptions,
                issuerOptions = result.issuerOptions
            )
        }
    }
}

@Composable
private fun DemoLoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040A0E)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.trade_planner_selection_demo_loading),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun DemoFailureContent(
    error: HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoError
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040A0E))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.trade_planner_selection_demo_preparation_failed),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = error.javaClass.simpleName,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun TradePlannerSelectionDeviceDemoReadyContent(
    viewModel: TradePlannerSelectionViewModel,
    brokerOptions: List<de.konavigator.app.presentation.tradeplanner.TradePlannerBrokerUiOption>,
    issuerOptions: List<de.konavigator.app.presentation.tradeplanner.TradePlannerIssuerUiOption>
) {
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.onBrokerSelected("synthetic-broker")
        viewModel.onEnabledIssuerIdsChanged(setOf("synthetic-issuer"))
        viewModel.onTargetLeverageChanged("5")
        viewModel.onDirectionChanged(de.konavigator.app.domain.model.TradeDirection.LONG)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.trade_planner_selection_demo_notice),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF3A2F00))
                .statusBarsPadding()
                .padding(12.dp),
            color = Color(0xFFFFE8A3),
            style = MaterialTheme.typography.bodyMedium
        )
        TradePlannerSelectionRoute(
            viewModel = viewModel,
            brokerOptions = brokerOptions,
            issuerOptions = issuerOptions,
            modifier = Modifier.weight(1f)
        )
    }
}
