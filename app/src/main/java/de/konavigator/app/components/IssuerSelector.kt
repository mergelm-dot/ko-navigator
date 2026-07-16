package de.konavigator.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.konavigator.app.models.IssuerOption

@Composable
fun IssuerSelector(
    issuers: SnapshotStateList<IssuerOption>
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val allSelected = issuers.all { issuer ->
        issuer.isSelected
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Emittenten",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    expanded = !expanded
                }
            ) {
                Text(
                    text = if (expanded) {
                        "Weitere Emittenten ausblenden ▲"
                    } else {
                        "Weitere Emittenten anzeigen ▼"
                    }
                )
            }
        }
        if (!allSelected) {
            TextButton(
                onClick = {
                    issuers.indices.forEach { index ->
                        issuers[index] = issuers[index].copy(
                            isSelected = true
                        )
                    }
                }
            ) {
                Text(
                    text = "Alle auswählen",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (expanded) {
            issuers.forEachIndexed { index, issuer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = issuer.isSelected,
                        onCheckedChange = { checked ->

                            val selectedCount = issuers.count { it.isSelected }

                            val mayChange =
                                checked || selectedCount > 1

                            if (mayChange) {
                                issuers[index] = issuer.copy(
                                    isSelected = checked
                                )
                            }
                        }
                    )

                    Text(
                        text = issuer.displayName,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

