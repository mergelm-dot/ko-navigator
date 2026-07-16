package de.konavigator.app.repository

import de.konavigator.app.data.UnderlyingTestData
import de.konavigator.app.models.UnderlyingAsset

object UnderlyingRepository {

    fun getAllAssets(): List<UnderlyingAsset> {
        return UnderlyingTestData.assets
    }

}