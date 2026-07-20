package de.konavigator.app.domain.freshness

enum class MarketDataFreshnessError {
    BID_TIMESTAMP_IN_FUTURE,
    STALE_BID,
    ASK_TIMESTAMP_IN_FUTURE,
    STALE_ASK,
    BID_ASK_TIMESTAMPS_TOO_FAR_APART
}
