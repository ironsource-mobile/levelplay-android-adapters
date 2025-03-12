package com.ironsource.adapters.bigo.rewardedvideo

import com.ironsource.adapters.bigo.BigoAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.RewardAdInteractionListener
import sg.bigo.ads.api.RewardVideoAd
import java.lang.ref.WeakReference

class BigoRewardedVideoAdListener(
    private val mAdapter: WeakReference<BigoRewardedVideoAdapter>,
    private val mListener: RewardedVideoSmashListener) : RewardAdInteractionListener, AdLoadListener<RewardVideoAd> {

    /**
     * Called when ad request succeeds
     *
     * @param ad - RewardedVideo ad
     */
    override fun onAdLoaded(ad: RewardVideoAd) {
        IronLog.INTERNAL.verbose()
        mAdapter.get()?.setRewardedVideoAd(ad)
        mAdapter.get()?.setRewardedVideoAdAvailability(true)
        mListener.onRewardedVideoAvailabilityChanged(true)
    }

    /**
     * Called when something wrong during ad loading
     *
     * @param error - bigo ad error
     */
    override fun onError(error: AdError) {
        IronLog.INTERNAL.verbose("onError code: ${error.code}, ${error.message}")
        mListener.onRewardedVideoAvailabilityChanged(false)
        mListener.onRewardedVideoLoadFailed(BigoAdapter.getLoadError(error))
    }

    /**
     * When the fullscreen ad covers the screen.
     *
     */
    override fun onAdOpened() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdStarted()
    }

    /**
     * There's something wrong when using this ad
     *
     * @param error - bigo ad error
     */
    override fun onAdError(error: AdError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${error.code}, errorMessage = ${error.message}")
        val rewardedError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.REWARDED_VIDEO_AD_UNIT,
            error.message
        )
        mListener.onRewardedVideoAdShowFailed(rewardedError)
    }

    /**
     * Indicates that the ad has been displayed successfully on the device screen.
     *
     */
    override fun onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdOpened()
    }

    /**
     * It's time to offer some reward to the user.
     *
     */
    override fun onAdRewarded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdRewarded()

    }

    /**
     * Indicates that the ad is clicked by the user.
     *
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClicked()
    }

    /**
     * When the fullsceen ad closes.
     *
     */
    override fun onAdClosed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdEnded()
        mListener.onRewardedVideoAdClosed()
        mAdapter.get()?.destroyRewardedVideoAd()
    }

}
