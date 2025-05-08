package com.ironsource.adapters.inmobi.rewardedvideo

import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiInterstitial
import com.inmobi.ads.listeners.InterstitialAdEventListener
import com.ironsource.adapters.inmobi.InMobiAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants

class InMobiRewardedVideoAdListener(
    private val listener: RewardedVideoSmashListener,
    private val placementId: String
) : InterstitialAdEventListener() {

    override fun onAdLoadSucceeded(
        inMobiInterstitial: InMobiInterstitial,
        adMetaInfo: AdMetaInfo
    ) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        listener.onRewardedVideoAvailabilityChanged(true)
    }

    /**
     * Callback to signal that a request to fetch an ad (by calling
     * [InMobiInterstitial.load] failed. The status code indicating the reason for failure
     * is available as a parameter. You should call [InMobiInterstitial.load] again to
     * request a fresh ad.
     *
     * @param inMobiInterstitial    Represents the [InMobiInterstitial] ad which failed to load
     * @param inMobiAdRequestStatus Represents the [InMobiAdRequestStatus] status containing error reason
     */
    override fun onAdLoadFailed(
        inMobiInterstitial: InMobiInterstitial,
        inMobiAdRequestStatus: InMobiAdRequestStatus
    ) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        val adapterError =
            "${inMobiAdRequestStatus.message}( ${inMobiAdRequestStatus.statusCode} )"

        val error =
            if (inMobiAdRequestStatus.statusCode == InMobiAdRequestStatus.StatusCode.NO_FILL) {
                IronSourceError(IronSourceError.ERROR_RV_LOAD_NO_FILL, adapterError)
            } else {
                ErrorBuilder.buildLoadFailedError(adapterError)
            }

        IronLog.ADAPTER_CALLBACK.error("adapterError = $error")

        listener.onRewardedVideoAvailabilityChanged(false)
        listener.onRewardedVideoLoadFailed(error)
    }

    /**
     * Called to indicate that the ad will be launching a fullscreen overlay.
     *
     * @param inMobiInterstitial Represents the [InMobiInterstitial] ad which will display
     */
    override fun onAdWillDisplay(inMobiInterstitial: InMobiInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")
    }

    /**
     * Called to indicate that the fullscreen overlay is now the topmost screen.
     *
     * @param inMobiInterstitial Represents the [InMobiInterstitial] ad which is displayed
     * @param adMetaInfo         Represents the ad meta information
     */
    override fun onAdDisplayed(
        inMobiInterstitial: InMobiInterstitial,
        adMetaInfo: AdMetaInfo
    ) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")
    }

    /**
     * Called to indicate that the fullscreen overlay is now the topmost screen.
     *
     * @param inMobiInterstitial Represents the [InMobiInterstitial] ad which is displayed
     */
    override fun onAdImpression(inMobiInterstitial: InMobiInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        listener.onRewardedVideoAdOpened()
        listener.onRewardedVideoAdStarted()
    }

    /**
     * Called to indicate that a request to show an ad (by calling [InMobiInterstitial.show]
     * failed. You should call [InMobiInterstitial.load] to request for a fresh ad.
     *
     * @param inMobiInterstitial Represents the [InMobiInterstitial] ad which failed to show
     */
    override fun onAdDisplayFailed(inMobiInterstitial: InMobiInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        listener.onRewardedVideoAdShowFailed(
            ErrorBuilder.buildShowFailedError(
                placementId,
                IronSourceConstants.REWARDED_VIDEO_AD_UNIT
            )
        )
    }

    /**
     * Called to indicate that an ad interaction was observed.
     *
     * @param inMobiInterstitial Represents the [InMobiInterstitial] ad on which user clicked
     * @param params             Represents the click parameters
     */
    override fun onAdClicked(
        inMobiInterstitial: InMobiInterstitial,
        params: Map<Any?, Any?>?
    ) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        listener.onRewardedVideoAdClicked()
    }

    /**
     * Called to indicate that the user may leave the application on account of interacting with the ad.
     *
     * @param inMobiInterstitial Represents the [InMobiInterstitial] ad
     */
    override fun onUserLeftApplication(inMobiInterstitial: InMobiInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")
    }

    /**
     * Called to indicate that rewards have been unlocked.
     *
     * @param inMobiInterstitial Represents the [InMobiInterstitial] ad for which rewards was unlocked
     * @param rewards            Represents the rewards unlocked
     */
    override fun onRewardsUnlocked(
        inMobiInterstitial: InMobiInterstitial,
        rewards: Map<Any?, Any?>?
    ) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        listener.onRewardedVideoAdRewarded()
    }

    /**
     * Called to indicate that the fullscreen overlay opened by the ad was closed.
     *
     * @param inMobiInterstitial Represents the [InMobiInterstitial] ad which was dismissed
     */
    override fun onAdDismissed(inMobiInterstitial: InMobiInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose("${InMobiAdapter.PLACEMENT_ID} = $placementId")

        listener.onRewardedVideoAdClosed()
    }

}