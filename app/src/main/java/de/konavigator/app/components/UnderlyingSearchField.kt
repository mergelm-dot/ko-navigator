package de.konavigator.app.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import de.konavigator.app.models.UnderlyingAsset
import de.konavigator.app.search.UnderlyingSearchEngine

@Composable
fun UnderlyingSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onAssetSelected: (UnderlyingAsset) -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    val searchResults =
        if (dropdownExpanded && value.isNotBlank()) {
            UnderlyingSearchEngine.search(value)
        } else {
            emptyList()
        }

    Column(
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                dropdownExpanded = newValue.isNotBlank()
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Basiswert")
            },
            placeholder = {
                Text("Name, Ticker, WKN oder ISIN")
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = Color(0xFF20C967),
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = Color(0xFF20C967),
                unfocusedBorderColor = Color(0xFF283740),
                cursorColor = Color(0xFF20C967),
                focusedContainerColor = Color(0xFF0C171D),
                unfocusedContainerColor = Color(0xFF0C171D)
            )
        )

        DropdownMenu(
            expanded = dropdownExpanded && searchResults.isNotEmpty(),
            onDismissRequest = {
                dropdownExpanded = false
            },
            modifier = Modifier.fillMaxWidth(),
            properties = PopupProperties(
                focusable = false
            )
        ) {
            searchResults.forEach { asset ->
                DropdownMenuItem(
                    text = {
                        UnderlyingSearchResultItem(
                            asset = asset
                        )
                    },
                    onClick = {
                        dropdownExpanded = false
                        onAssetSelected(asset)
                    }
                )
            }
        }
    }
}

@Composable
private fun UnderlyingSearchResultItem(
    asset: UnderlyingAsset
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        asset.logoResId?.let { logoResId ->
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = "${asset.displayName} Logo",
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )
        }

        Column {
            Text(
                text = asset.displayName,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = asset.name,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${asset.ticker} · ${asset.referenceExchange} · ${asset.currency}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}