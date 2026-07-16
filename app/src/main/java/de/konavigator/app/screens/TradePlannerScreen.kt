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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.TextButton
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
private val DangerRed = Color(0xFFFF4D4D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradePlannerScreen() {

    var underlying by remember { mutableStateOf("NVIDIA") }
    var currentPrice by remember { mutableStateOf("100,00") }
    var entryPrice by remember { mutableStateOf("95,00") }
    var leverage by remember { mutableStateOf("3") }
    var direction by remember { mutableStateOf("Long") }
    var showOrderHint by remember { mutableStateOf(true) }
    var selectedBroker by remember {
        mutableStateOf("Scalable Capital")
    }

    var brokerMenuExpanded by remember {
        mutableStateOf(false)
    }

    val brokers = listOf(
        "Scalable Capital",
        "Trade Republic",
        "ING",
        "comdirect",
        "Consorsbank",
        "flatex",
        "Alle Broker"
    )
    val accentColor =
        if (direction == "Long") AccentGreen else DangerRed
    val current = currentPrice
        .replace(",", ".")
        .toDoubleOrNull()

    val entry = entryPrice
        .replace(",", ".")
        .toDoubleOrNull()

    val orderType = when {
        current == null || entry == null -> ""

        direction == "Long" && entry < current ->
            "Buy Limit"

        direction == "Long" && entry > current ->
            "Buy Stop"

        direction == "Short" && entry > current ->
            "Sell Limit"

        direction == "Short" && entry < current ->
            "Sell Stop"

        else ->
            "Market"
    }

            val orderExplanation = when (orderType) {
            "Buy Limit" ->
                "Du versuchst, unterhalb des aktuellen Kurses günstiger einzusteigen. Der Kauf erfolgt nur, wenn der Basiswert deinen Kaufkurs erreicht."

            "Buy Stop" ->
                "Du steigst erst ein, wenn der Basiswert über deinen Kaufkurs steigt. Das eignet sich beispielsweise für einen bestätigten Ausbruch."

            "Sell Limit" ->
                "Du planst den Short-Einstieg oberhalb des aktuellen Kurses. Die Order wird erst bei Erreichen des Verkaufskurses ausgelöst."

            "Sell Stop" ->
                "Du steigst erst ein, wenn der Basiswert unter deinen Verkaufskurs fällt. Das eignet sich beispielsweise für einen bestätigten Abwärtstrend."

            "Market" ->
                "Der geplante Einstieg entspricht ungefähr dem aktuellen Kurs und würde grundsätzlich sofort ausgeführt."

            else ->
                "Bitte gib einen gültigen aktuellen Kurs und Einstiegskurs ein."


    }

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
                    onValueChange = { underlying = it },
                            accentColor = accentColor
                )
                ExposedDropdownMenuBox(
                    expanded = brokerMenuExpanded,
                    onExpandedChange = {
                        brokerMenuExpanded = !brokerMenuExpanded
                    }
                ) {
                    OutlinedTextField(
                        value = selectedBroker,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = {
                            Text("Broker")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = brokerMenuExpanded
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = SecondaryText,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = brokerMenuExpanded,
                        onDismissRequest = {
                            brokerMenuExpanded = false
                        }
                    ) {
                        brokers.forEach { broker ->
                            DropdownMenuItem(
                                text = {
                                    Text(broker)
                                },
                                onClick = {
                                    selectedBroker = broker
                                    brokerMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                PlannerTextField(
                    label = "Aktueller Kurs",
                    value = currentPrice,
                    onValueChange = { currentPrice = it },
                    numeric = true,
                    suffix = "€",
                            accentColor = accentColor

                )

                PlannerTextField(
                    label = if (direction == "Long") "Kaufkurs" else "Verkaufskurs",
                    value = entryPrice,
                    onValueChange = { entryPrice = it },
                    numeric = true,
                    suffix = "€",
                            accentColor = accentColor
                )

                PlannerTextField(
                    label = "Hebel",
                    value = leverage,
                    onValueChange = { leverage = it },
                    numeric = true,
                            accentColor = accentColor
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
                        onClick = { direction = "Long" },
                        accentColor = accentColor
                    )

                    DirectionOption(
                        text = "Short",
                        selected = direction == "Short",
                        onClick = { direction = "Short" },
                        accentColor = accentColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = orderType,
                    color = accentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (showOrderHint) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = orderExplanation,
                    color = SecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
                TextButton(
                    onClick = {
                        showOrderHint = !showOrderHint
                    }
                ) {
                    Text(
                        text = if (showOrderHint) {
                            "Hinweis ausblenden"
                        } else {
                            "Hinweis anzeigen"
                        },
                        color = SecondaryText
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
                        containerColor =
                            if (direction == "Long") AccentGreen
                            else DangerRed,
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
    accentColor: Color,
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
            focusedLabelColor = accentColor,
            unfocusedLabelColor = SecondaryText,
            focusedBorderColor = accentColor,
            unfocusedBorderColor = BorderColor,
            cursorColor = accentColor,
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground
        )
    )
}

@Composable
private fun DirectionOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Row {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = accentColor,
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

