package com.ironsource.adapters.vungle.rewardedvideo

import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.vungle.ads.BaseAd
import com.vungle.ads.RewardedAdListener
import com.vungle.ads.VungleError
import java.lang.ref.WeakReference

class VungleRewardedVideoAdListener (
    private val mAdapter: WeakReference<VungleRewardedVideoAdapter>,
    private val mListener: RewardedVideoSmashListener,
    private val mPlacementId: String
): RewardedAdListener{

    /**
     * Called to indicate that an ad was loaded and it can now be shown.
     *
     * @param baseAd - RewardedAd instance
     */
    override fun onAdLoaded(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mAdapter.get()?.setRewardedVideoAdAvailability(mPlacementId,true)
        mListener.onRewardedVideoAvailabilityChanged(true)
    }

    /**
     * Called when Ad failed to load
     *
     * @param baseAd - RewardedAd instance
     * @param adError - adError with additional info about error
     */
    override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, placementId = $mPlacementId, errorCode = ${adError.code}, errorMessage = ${adError.message}")
        mAdapter.get()?.setRewardedVideoAdAvailability(mPlacementId,false)
        val adapterError = "${adError.errorMessage}( ${adError.code} )"
        val error = if (adError.code == VungleError.NO_SERVE) {
            IronSourceError(IronSourceError.ERROR_RV_LOAD_NO_FILL, adapterError)
        } else {
            ErrorBuilder.buildLoadFailedError(adapterError)
        }
        mListener.onRewardedVideoAvailabilityChanged(false)
        mListener.onRewardedVideoLoadFailed(error)
    }

    /**
     * Called to indicate that an ad started.
     *
     * @param baseAd - RewardedAd instance
     */
    override fun onAdStart(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onRewardedVideoAdStarted()
    }

    /**
     * Called to indicate that the fullscreen overlay is now the topmost screen.
     *
     * @param baseAd - RewardedAd instance
     */
    override fun onAdImpression(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onRewardedVideoAdOpened()
    }

    /**
     * Called to indicate that a request to show an ad (by calling [VungleRewarded.show]
     * failed. You should call [VungleRewarded.load] to request for a fresh ad.
     *
     * @param baseAd - RewardedAd instance
     * @param adError - adError with additional info about error
     */
    override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToPlay placementId = $mPlacementId, errorCode = ${adError.code}, errorMessage = ${adError.message}")
        val errorMessage = " reason = " + adError.errorMessage + " errorCode = " + adError.code
        val error: IronSourceError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.REWARDED_VIDEO_AD_UNIT,
            errorMessage
        )
        mListener.onRewardedVideoAdShowFailed(error)
    }

    /**
     * Called to indicate that an ad interaction was observed.
     *
     * @param baseAd - RewardedAd instance
     */
    override fun onAdClicked(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onRewardedVideoAdClicked()
    }

    /**
     * Called when Rewarded Ad was completed (e.g.: the video has been played to the end).
     * You can use this event to reward user
     *
     * @param baseAd - RewardedAd instance
     */
    override fun onAdRewarded(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onRewardedVideoAdRewarded()
    }

    /**
     * Called to indicate that the ad was ended and closed.
     *
     * @param baseAd - RewardedAd instance
     */
    override fun onAdEnd(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onRewardedVideoAdEnded()
        mListener.onRewardedVideoAdClosed()
    }

    /**
     * Called to indicate that the user may leave the application on account of interacting with the ad.
     *
     * @param baseAd Represents the [VungleRewardedVideoAd] ad
     */
    override fun onAdLeftApplication(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
    }
}
