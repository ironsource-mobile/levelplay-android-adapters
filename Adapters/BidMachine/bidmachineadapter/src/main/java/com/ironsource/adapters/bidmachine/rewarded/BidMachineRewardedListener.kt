package com.ironsource.adapters.bidmachine.rewarded

import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.adapters.bidmachine.BidMachineConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.logger.IronLog
import io.bidmachine.Ad
import io.bidmachine.AdLoadCallback
import io.bidmachine.BMError
import io.bidmachine.format.rewardedad.RewardedAd
import io.bidmachine.format.rewardedad.RewardedListener

class BidMachineRewardedListener(
    private val listener: RewardedVideoAdListener
) : AdLoadCallback<RewardedAd>, RewardedListener {

    /**
     * Called when Ad was loaded and ready to be displayed
     * @param ad - RewardedAd instance
     */
    override fun onAdLoaded(ad: RewardedAd) {
        val creativeId = (ad as Ad).getAuctionInfo().creativeId
        IronLog.ADAPTER_CALLBACK.verbose(BidMachineConstants.Logs.CREATIVE_ID.format(creativeId ?: ""))

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess()
        } else {
            val extraData: Map<String, Any> = mapOf(BidMachineConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(extraData)
        }
    }

    /**
     * Called when Ad failed to load
     * @param bmError - BMError with additional info about error
     */
    override fun onAdLoadFailed(bmError: BMError) {
        IronLog.ADAPTER_CALLBACK.error(BidMachineConstants.Logs.FAILED_TO_LOAD.format(bmError.code, bmError.message))
        val errorType = BidMachineAdapter.getLoadErrorType(bmError)
        listener.onAdLoadFailed(errorType, bmError.code, bmError.message)
    }

    override fun onAdPresented(ad: RewardedAd) {
        // Keep reporting onAdOpened when the impression is tracked.
    }

    /**
     * Called when Ad Impression has been tracked
     * @param ad - RewardedAd instance
     */
    override fun onAdImpression(ad: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when Ad show failed
     * @param ad - RewardedAd instance
     * @param bmError - BMError with additional info about error
     */
    override fun onAdShowFailed(ad: RewardedAd, bmError: BMError) {
        IronLog.ADAPTER_CALLBACK.error(BidMachineConstants.Logs.FAILED_TO_SHOW.format(bmError.code, bmError.message))
        listener.onAdShowFailed(bmError.code, bmError.message)
    }

    /**
     * Called when Ad has been clicked
     * @param ad - RewardedAd instance
     */
    override fun onAdClicked(ad: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when Rewarded Ad was completed (e.g.: the video has been played to the end).
     * You can use this event to reward user
     * @param ad - RewardedAd instance
     */
    override fun onAdRewarded(ad: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }

    /**
     * Called when Ad was closed (e.g - user click close button)
     * @param ad - RewardedAd instance
     */
    override fun onAdClosed(ad: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when Ad expired
     * @param ad - RewardedAd instance
     */
    override fun onAdExpired(ad: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadFailed(
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
            AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
            BidMachineConstants.AD_EXPIRED
        )
    }
}
