package de.konavigator.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AppBackground = Color(0xFF040A0E)
private val CardBackground = Color(0xFF0C171D)
private val BorderColor = Color(0xFF283740)
private val PrimaryText = Color(0xFFF3F4F6)
private val SecondaryText = Color(0xFF9CA3AF)
private val AccentGreen = Color(0xFF20C967)

@Composable
fun TradePlannerScreen() {

    var underlying by remember { mutableStateOf("NVIDIA") }
    var currentPrice by remember { mutableStateOf("100,00") }
    var entryPrice by remember { mutableStateOf("95,00") }
    var leverage by remember { mutableStateOf("3") }
    var direction by remember { mutableStateOf("Long") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {

        Text(
            text = "KO Navigator",
            color = PrimaryText,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Plane deinen Trade",
            color = SecondaryText,
            fontSize = 17.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = BorderColor
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "Trade-Setup",
                    color = PrimaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                PlannerTextField(
                    label = "Basiswert",
                    value = underlying,
                    onValueChange = { underlying = it }
                )

                PlannerTextField(
                    label = "Aktueller Kurs",
                    value = currentPrice,
                    onValueChange = { currentPrice = it },
                    numeric = true,
                    suffix = "€"
                )

                PlannerTextField(
                    label = if (direction == "Long") "Kaufkurs" else "Verkaufskurs",
                    value = entryPrice,
                    onValueChange = { entryPrice = it },
                    numeric = true,
                    suffix = "€"
                )

                PlannerTextField(
                    label = "Hebel",
                    value = leverage,
                    onValueChange = { leverage = it },
                    numeric = true
                )

                Text(
                    text = "Richtung",
                    color = SecondaryText,
                    fontSize = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    DirectionOption(
                        text = "Long",
                        selected = direction == "Long",
                        onClick = { direction = "Long" }
                    )

                    DirectionOption(
                        text = "Short",
                        selected = direction == "Short",
                        onClick = { direction = "Short" }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        // Die Zertifikatssuche ergänzen wir später.
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "PASSENDE ZERTIFIKATE FINDEN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannerTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    numeric: Boolean = false,
    suffix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(label)
        },
        suffix = {
            if (suffix != null) {
                Text(suffix)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) {
                KeyboardType.Decimal
            } else {
                KeyboardType.Text
            }
        ),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PrimaryText,
            unfocusedTextColor = PrimaryText,
            focusedLabelColor = AccentGreen,
            unfocusedLabelColor = SecondaryText,
            focusedBorderColor = AccentGreen,
            unfocusedBorderColor = BorderColor,
            cursorColor = AccentGreen,
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground
        )
    )
}

@Composable
private fun DirectionOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = AccentGreen,
                unselectedColor = SecondaryText
            )
        )

        Text(
            text = text,
            color = PrimaryText,
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 16.sp
        )
    }
}

