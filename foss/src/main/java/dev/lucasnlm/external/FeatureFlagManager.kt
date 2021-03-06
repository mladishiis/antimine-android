package dev.lucasnlm.external

class FeatureFlagManager : IFeatureFlagManager() {
    override val isGameHistoryEnabled: Boolean = true
    override val isRateUsEnabled: Boolean = false
    override val isInAppAdsEnabled: Boolean = false
    override val isGameplayAnalyticsEnabled: Boolean = false
    override val isGameOverAdEnabled: Boolean = false
    override val isAdsOnNewGameEnabled: Boolean = false
    override val isAdsOnContinueEnabled: Boolean = false
    override val isContinueGameEnabled: Boolean = true
    override val isRecyclerScrollEnabled: Boolean = true
    override val isFoos: Boolean = true
    override val isThemeTastingEnabled: Boolean = true

    override suspend fun refresh() {
        // No Feature Flags on FOSS
    }
}
