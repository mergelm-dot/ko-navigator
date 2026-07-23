package de.konavigator.app.data.remote.provider

/**
 * Providerneutraler technischer Ergebnisvertrag für externe Datenzugriffe.
 *
 * Der Vertrag veröffentlicht keine Netzwerk-, HTTP- oder providerspezifischen
 * Fehlerdetails und enthält keine Mappingfehler.
 */
sealed interface ProviderResult<out T> {

    data class Success<T>(
        val value: T
    ) : ProviderResult<T>

    data object NotFound : ProviderResult<Nothing>

    data object DataAccessFailure : ProviderResult<Nothing>
}
