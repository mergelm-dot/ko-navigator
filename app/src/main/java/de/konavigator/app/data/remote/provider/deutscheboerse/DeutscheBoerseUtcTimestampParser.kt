package de.konavigator.app.data.remote.provider.deutscheboerse

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal data class DeutscheBoerseUtcTimestamp(
    val epochSecond: Long,
    val nanoOfSecond: Int
) : Comparable<DeutscheBoerseUtcTimestamp> {

    val epochMillis: Long
        get() = epochSecond * MILLIS_PER_SECOND + nanoOfSecond / NANOS_PER_MILLISECOND

    override fun compareTo(other: DeutscheBoerseUtcTimestamp): Int {
        val secondsComparison = epochSecond.compareTo(other.epochSecond)
        return if (secondsComparison != 0) {
            secondsComparison
        } else {
            nanoOfSecond.compareTo(other.nanoOfSecond)
        }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val NANOS_PER_MILLISECOND = 1_000_000
    }
}

internal object DeutscheBoerseUtcTimestampParser {

    fun parse(value: String): DeutscheBoerseUtcTimestamp? {
        val match = UTC_TIMESTAMP_PATTERN.matchEntire(value) ?: return null
        val seconds = match.groupValues[1]
        val fraction = match.groupValues[2]
        val formatter = SimpleDateFormat(UTC_SECONDS_PATTERN, Locale.ROOT).apply {
            isLenient = false
            timeZone = UTC_TIME_ZONE
        }
        val position = ParsePosition(0)
        val date = formatter.parse(seconds, position) ?: return null
        if (position.index != seconds.length || position.errorIndex >= 0) {
            return null
        }

        return DeutscheBoerseUtcTimestamp(
            epochSecond = date.time / MILLIS_PER_SECOND,
            nanoOfSecond = fraction
                .padEnd(NANOSECOND_DIGITS, '0')
                .toInt()
        )
    }

    private val UTC_TIMESTAMP_PATTERN =
        Regex("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.(\\d{1,9}))?Z$")
    private const val UTC_SECONDS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
    private const val MILLIS_PER_SECOND = 1_000L
    private const val NANOSECOND_DIGITS = 9
    private val UTC_TIME_ZONE: TimeZone = TimeZone.getTimeZone("UTC")
}
