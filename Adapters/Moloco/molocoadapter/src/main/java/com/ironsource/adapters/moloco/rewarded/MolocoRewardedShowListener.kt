package com.ironsource.adapters.moloco.rewarded

import com.ironsource.adapters.moloco.MolocoConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.logger.IronLog
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.RewardedInterstitialAdShowListener

class MolocoRewardedShowListener(
    private val listener: RewardedVideoAdListener
) : RewardedInterstitialAdShowListener {

    /**
     * Called when an ad starts displaying. Impression can be recorded.
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdShowSuccess(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when rewarded video started
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onRewardedVideoStarted(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdStarted()
    }

    /**
     * Called when Ad show failed
     *
     * @param molocoAdError - MolocoAdError with additional info about error
     */
    override fun onAdShowFailed(molocoAdError: MolocoAdError) {
        val errorCode = MolocoAdError.ErrorType.AD_SHOW_ERROR.errorCode
        IronLog.ADAPTER_CALLBACK.error(MolocoConstants.Logs.FAILED_TO_SHOW.format(errorCode, molocoAdError.description))
        listener.onAdShowFailed(errorCode, molocoAdError.description)
    }

    /**
     * Called when Ad has been clicked
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdClicked(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when user has earned a reward
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onUserRewarded(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }

    /**
     * Called when rewarded video completed
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onRewardedVideoCompleted(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdEnded()
    }

    /**
     * Called when an ad is hidden.
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdHidden(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}
