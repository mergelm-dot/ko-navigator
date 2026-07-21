package de.konavigator.app.debug.marketdata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import de.konavigator.app.presentation.marketdata.MarketDataCalculationViewModel
import de.konavigator.app.presentation.marketdata.MarketDataCalculationViewModelFactory
import de.konavigator.app.ui.theme.KONavigatorTheme

/** Debug-exklusiver zweiter Launcher-Einstieg für den lokalen Engine-Datenfluss. */
class MarketDataCalculationDemoActivity : ComponentActivity() {

    private val viewModelFactory: MarketDataCalculationViewModelFactory by lazy(
        LazyThreadSafetyMode.NONE
    ) {
        MarketDataCalculationDemoComposition.createFactory()
    }

    private val viewModel: MarketDataCalculationViewModel by lazy(
        LazyThreadSafetyMode.NONE
    ) {
        ViewModelProvider(this, viewModelFactory)[
            MarketDataCalculationViewModel::class.java
        ]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KONavigatorTheme {
                MarketDataCalculationDemoRoute(viewModel = viewModel)
            }
        }
    }
}
