package de.konavigator.app.domain.tradeplanning

/**
 * Vergleicht einen geplanten Einstiegskurs mit dem aktuellen Basiswertkurs.
 *
 * Version 1 verwendet für zwei positive, endliche Werte die exakte numerische
 * Gleichheit. Es gibt keine Rundung, Toleranz- oder Tick-Size-Annahme. Der
 * Evaluator ist brokerneutral und kennt weder Handelsrichtung, Kauf oder
 * Verkauf, Ordertypen, Handelsstrategie, UI, Android noch Compose.
 */
object EntryPriceRelationEvaluator {

    fun evaluate(
        currentPrice: Double,
        plannedEntryPrice: Double
    ): EntryPriceRelationEvaluationResult {
        if (!currentPrice.isFinite() || currentPrice <= 0.0) {
            return EntryPriceRelationEvaluationResult.Failure(
                EntryPriceRelationEvaluationError.INVALID_CURRENT_PRICE
            )
        }

        if (!plannedEntryPrice.isFinite() || plannedEntryPrice <= 0.0) {
            return EntryPriceRelationEvaluationResult.Failure(
                EntryPriceRelationEvaluationError.INVALID_PLANNED_ENTRY_PRICE
            )
        }

        val relation = when {
            plannedEntryPrice < currentPrice -> EntryPriceRelation.BELOW_CURRENT
            plannedEntryPrice > currentPrice -> EntryPriceRelation.ABOVE_CURRENT
            else -> EntryPriceRelation.AT_CURRENT
        }

        return EntryPriceRelationEvaluationResult.Success(relation)
    }
}
