package de.konavigator.app.domain.availability

enum class MarketDataCalculationAvailabilityError {
    MISSING_BID,
    MISSING_ASK,
    BID_NOT_POSITIVE_FOR_SALE
}
