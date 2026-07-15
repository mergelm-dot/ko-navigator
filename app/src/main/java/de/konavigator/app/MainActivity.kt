package de.konavigator.app

import de.konavigator.app.screens.TradePlannerScreen
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.os.Bundle
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.konavigator.app.ui.theme.KONavigatorTheme

private val AppBackground = Color(0xFF060B10)
private val CardBackground = Color(0xFF0D151D)
private val BorderColor = Color(0xFF26313C)
private val AccentGreen = Color(0xFF22C55E)
private val DangerRed = Color(0xFFEF4444)
private val PrimaryText = Color(0xFFF3F4F6)
private val SecondaryText = Color(0xFF9CA3AF)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = android.graphics.Color.rgb(4, 10, 14)
            ),
            navigationBarStyle = SystemBarStyle.dark(
                scrim = android.graphics.Color.rgb(4, 10, 14)
            )
        )

        setContent {
            KONavigatorTheme {
                TradePlannerScreen()
            }
        }
    }
}

@Composable
fun KoNavigatorScreen() {
    var basiswert by remember { mutableStateOf("NVIDIA Corp.") }
    var aktuellerKurs by remember { mutableStateOf("950.24") }
    var knockoutSchwelle by remember { mutableStateOf("720.00") }
    var hebel by remember { mutableStateOf("3.00") }
    var einsatz by remember { mutableStateOf("1000") }
    var zertifikatPreis by remember { mutableStateOf("9.52") }

    val kurs = aktuellerKurs.toDoubleOrNull() ?: 0.0
    val knockout = knockoutSchwelle.toDoubleOrNull() ?: 0.0
    val abstand = if (kurs > 0) {
        ((kurs - knockout) / kurs) * 100
    } else {
        0.0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

        Text(
            text = "KO Navigator",
            color = PrimaryText,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Dein KO-Rechner & Risiko-Kompass",
            color = SecondaryText,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        AppCard {
            Text(
                text = "KO BERECHNEN",
                color = PrimaryText,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Berechne dein Risiko. Plane deinen Trade.",
                color = SecondaryText,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            AppInputField(
                label = "Basiswert",
                value = basiswert,
                onValueChange = { basiswert = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AppInputField(
                        label = "Aktueller Kurs",
                        value = aktuellerKurs,
                        onValueChange = { aktuellerKurs = it }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    AppInputField(
                        label = "Knock-out-Schwelle",
                        value = knockoutSchwelle,
                        onValueChange = { knockoutSchwelle = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AppInputField(
                        label = "Hebel",
                        value = hebel,
                        onValueChange = { hebel = it }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    AppInputField(
                        label = "Einsatz",
                        value = einsatz,
                        onValueChange = { einsatz = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AppInputField(
                label = "Kaufkurs Zertifikat",
                value = zertifikatPreis,
                onValueChange = { zertifikatPreis = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "BERECHNUNG AKTUALISIEREN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        AppCard {
            Text(
                text = "ERGEBNISSE",
                color = PrimaryText,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            ResultRow(
                title = "Abstand zum Knock-out",
                value = String.format("%.2f %%", abstand),
                valueColor = if (abstand >= 15) AccentGreen else DangerRed
            )

            Spacer(modifier = Modifier.height(14.dp))

            ResultRow(
                title = "Risiko bei K.O.",
                value = "-${einsatz.ifBlank { "0" }} €",
                valueColor = DangerRed
            )

            Spacer(modifier = Modifier.height(14.dp))

            ResultRow(
                title = "Risikoeinschätzung",
                value = when {
                    abstand >= 25 -> "MODERAT"
                    abstand >= 15 -> "ERHÖHT"
                    else -> "HOCH"
                },
                valueColor = when {
                    abstand >= 25 -> AccentGreen
                    abstand >= 15 -> Color(0xFFF59E0B)
                    else -> DangerRed
                }
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
    }
}

@Composable
fun AppCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = CardBackground,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = BorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp),
        content = content
    )
}

@Composable
fun AppInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                color = SecondaryText
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PrimaryText,
            unfocusedTextColor = PrimaryText,
            focusedBorderColor = AccentGreen,
            unfocusedBorderColor = BorderColor,
            focusedLabelColor = AccentGreen,
            unfocusedLabelColor = SecondaryText,
            cursorColor = AccentGreen,
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground
        ),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun ResultRow(
    title: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = SecondaryText,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = value,
            color = valueColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}