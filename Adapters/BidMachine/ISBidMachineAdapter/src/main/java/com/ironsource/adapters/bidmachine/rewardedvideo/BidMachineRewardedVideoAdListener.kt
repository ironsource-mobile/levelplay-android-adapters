package com.ironsource.adapters.bidmachine.rewardedvideo

import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import io.bidmachine.rewarded.RewardedAd
import io.bidmachine.rewarded.RewardedListener
import io.bidmachine.utils.BMError
import java.lang.ref.WeakReference

class BidMachineRewardedVideoAdListener(
    private val mListener: RewardedVideoSmashListener,
    private val mAdapter: WeakReference<BidMachineRewardedVideoAdapter>) : RewardedListener {

    /**
     * Called when Ad was loaded and ready to be displayed
     *
     * @param ad - RewardedAd instance
     */
    override fun onAdLoaded(ad: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mAdapter.get()?.setRewardedVideoAd(ad)
        mAdapter.get()?.setRewardedVideoAdAvailability(true)
        mListener.onRewardedVideoAvailabilityChanged(true)
    }

    /**
     * Called when Ad failed to load
     *
     * @param ad    - RewardedAd instance
     * @param error - BMError with additional info about error
     */
    override fun onAdLoadFailed(ad: RewardedAd, error: BMError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${error.code}, errorMessage = ${error.message}")
        mAdapter.get()?.setRewardedVideoAdAvailability(false)
        mListener.onRewardedVideoLoadFailed(
            BidMachineAdapter.getLoadErrorAndCheckNoFill(
                error,
                IronSourceError.ERROR_RV_LOAD_NO_FILL
            )
        )
        mAdapter.get()?.destroyRewardedVideoAd()
    }

    /**
     * Called when Ad Impression has been tracked
     *
     * @param ad - RewardedAd instance
     */
    override fun onAdImpression(ad: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdOpened()
        mListener.onRewardedVideoAdStarted()
    }

    /**
     * Called when Ad show failed
     *
     * @param ad    - RewardedAd instance
     * @param error - BMError with additional info about error
     */
    override fun onAdShowFailed(ad: RewardedAd, error: BMError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to show, errorCode = ${error.code} , errorMessage = ${error.message}")
        val rewardedVideoError: IronSourceError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.REWARDED_VIDEO_AD_UNIT,
            error.message
        )
        mListener.onRewardedVideoAdShowFailed(rewardedVideoError)
    }

    /**
     * Called when Ad has been clicked
     *
     * @param ad - RewardedAd instance
     */
    override fun onAdClicked(ad: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdClicked()
    }

    /**
     * Called when Rewarded Ad was completed (e.g.: the video has been played to the end).
     * You can use this event to reward user
     *
     * @param ad - RewardedAd instance
     */
    override fun onAdRewarded(ad: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdRewarded()
    }

    /**
     * Called when Ad was closed (e.g - user click close button)
     *
     * @param ad       - RewardedAd instance
     * @param finished - Value for indicated, if ads was finished (e.g - video playing finished)
     */
    override fun onAdClosed(ad: RewardedAd, finished: Boolean) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAdEnded()
        mListener.onRewardedVideoAdClosed()
        mAdapter.get()?.destroyRewardedVideoAd()
    }

    /**
     * Called when Ad expired
     *
     * @param ad - RewardedAd instance
     */
    override fun onAdExpired(ad: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}