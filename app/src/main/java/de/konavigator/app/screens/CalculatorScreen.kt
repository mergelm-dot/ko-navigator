package de.konavigator.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.konavigator.app.calculator.KoCalculator

@Composable
fun CalculatorScreen() {

    var underlying by remember { mutableStateOf("190") }
    var knockout by remember { mutableStateOf("160") }
    var ratio by remember { mutableStateOf("0.1") }

    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "KO-Rechner",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = underlying,
            onValueChange = { underlying = it },
            label = { Text("Basiswertkurs") }
        )

        OutlinedTextField(
            value = knockout,
            onValueChange = { knockout = it },
            label = { Text("Knock-out") }
        )

        OutlinedTextField(
            value = ratio,
            onValueChange = { ratio = it },
            label = { Text("Bezugsverhältnis") }
        )

        Button(
            onClick = {

                val price =
                    KoCalculator.calculateCertificatePrice(
                        underlyingPrice = underlying.toDouble(),
                        knockoutPrice = knockout.toDouble(),
                        ratio = ratio.toDouble(),
                        isLong = true
                    )

                result = "%.2f €".format(price)
            }
        ) {
            Text("Berechnen")
        }

        if (result.isNotEmpty()) {

            Text(
                text = "Zertifikatspreis",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = result,
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}