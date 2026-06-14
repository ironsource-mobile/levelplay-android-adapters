package com.ironsource.adapters.vungle.rewarded

import com.ironsource.adapters.vungle.VungleAdapter
import com.ironsource.adapters.vungle.VungleConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.logger.IronLog
import com.vungle.ads.BaseAd
import com.vungle.ads.RewardedAdListener
import com.vungle.ads.VungleError

class VungleRewardedListener(
    private val listener: RewardedVideoAdListener
) : RewardedAdListener {

    /**
     * Called when the advertisement assets have been downloaded and are ready to play.
     * @param baseAd - identifier for which the advertisement assets have been downloaded.
     */
    override fun onAdLoaded(baseAd: BaseAd) {
        val creativeId = baseAd.creativeId
        IronLog.ADAPTER_CALLBACK.verbose(VungleConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess()
        } else {
            val extraData: Map<String, Any> = mapOf(VungleConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(extraData)
        }
    }

    /**
     * Called when an error has occurred while downloading assets.
     * @param baseAd - identifier for which the advertisement for which the error occurred.
     * @param adError - error details suggesting the cause of failure.
     */
    override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.error(VungleConstants.Logs.FAILED_TO_LOAD.format(adError.code, adError.errorMessage))
        listener.onAdLoadFailed(VungleAdapter.getLoadErrorType(adError), adError.code, adError.errorMessage)
    }

    /**
     * Called when the Vungle SDK has launched the advertisement and it will begin playing momentarily.
     * @param baseAd - identifier for which the advertisement being played.
     */
    override fun onAdStart(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the Vungle SDK has shown an ad for the play/render request.
     * @param baseAd - identifier for which the advertisement being played.
     */
    override fun onAdImpression(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when an error has occurred while playing the advertisement.
     * @param baseAd - identifier for which the advertisement for which the error occurred.
     * @param adError - error details suggesting the cause of failure.
     */
    override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.error(VungleConstants.Logs.FAILED_TO_PLAY.format(adError.code, adError.errorMessage))
        listener.onAdShowFailed(adError.code, adError.errorMessage)
    }

    /**
     * Called when the user has tapped on an ad.
     * @param baseAd - identifier for which the advertisement that was clicked.
     */
    override fun onAdClicked(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the user has left the app.
     * @param baseAd - identifier for which the advertisement that resulted in leaving the app.
     */
    override fun onAdLeftApplication(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the user has watched the advertisement to completion and is eligible for a reward.
     * @param baseAd - identifier for which the advertisement got rewarded.
     */
    override fun onAdRewarded(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }

    /**
     * Called when the advertisement has finished playing and the user has closed it.
     * @param baseAd - identifier for which the advertisement that ended.
     */
    override fun onAdEnd(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}
