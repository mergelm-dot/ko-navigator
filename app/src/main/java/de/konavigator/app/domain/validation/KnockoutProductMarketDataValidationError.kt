package de.konavigator.app.domain.validation

enum class KnockoutProductMarketDataValidationError {
    MISSING_PRODUCT_ISIN,
    MISSING_SOURCE_ID,
    INVALID_CURRENCY,
    INVALID_BID,
    MISSING_BID_TIMESTAMP,
    ORPHAN_BID_TIMESTAMP,
    INVALID_ASK,
    MISSING_ASK_TIMESTAMP,
    ORPHAN_ASK_TIMESTAMP,
    BID_ABOVE_ASK
}
