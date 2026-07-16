package de.konavigator.app.search

import de.konavigator.app.models.UnderlyingAsset
import de.konavigator.app.repository.UnderlyingRepository

object UnderlyingSearchEngine {

    fun search(
        query: String
    ): List<UnderlyingAsset> {

        val normalizedQuery = query
            .trim()
            .lowercase()

        if (normalizedQuery.isEmpty()) {
            return emptyList()
        }

        val assets = UnderlyingRepository.getAllAssets()

        return assets.filter { asset ->
            asset.name.lowercase().contains(normalizedQuery) ||
                    asset.ticker.lowercase().contains(normalizedQuery) ||
                    asset.wkn?.lowercase()?.contains(normalizedQuery) == true ||
                    asset.isin?.lowercase()?.contains(normalizedQuery) == true
        }
    }
}

