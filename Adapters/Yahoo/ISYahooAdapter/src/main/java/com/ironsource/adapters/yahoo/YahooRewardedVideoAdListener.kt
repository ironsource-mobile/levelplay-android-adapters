package com.ironsource.adapters.yahoo

import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.yahoo.ads.ErrorInfo
import com.yahoo.ads.interstitialplacement.InterstitialAd
import java.lang.ref.WeakReference

class YahooRewardedVideoAdListener(private val mIListener: RewardedVideoSmashListener?,
                                   private val mAdapter: WeakReference<YahooAdapter>?,
                                   private val mPlacementId: String) : InterstitialAd.InterstitialAdListener {

    companion object{
        private const val RV_AD_REWARDED_EVENT   : String = "onVideoComplete"
    }

    // Called when the view for the InterstitialAd has been loaded.
    override fun onLoaded(interstitialAd: InterstitialAd?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mAdapter?.get()?.setRewardedVideoAd(mPlacementId, interstitialAd)
        mAdapter?.get()?.setRewardedVideoAdAvailability(mPlacementId, true)
        mIListener?.onRewardedVideoAvailabilityChanged(true)
    }

    // Called when there is an error requesting a InterstitialAd or loading a InterstitialAd from the cache.
    override fun onLoadFailed(interstitialAd: InterstitialAd?, errorInfo: ErrorInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load placementId = $mPlacementId with error: $errorInfo")

        mAdapter?.get()?.setRewardedVideoAdAvailability(mPlacementId, false)
        mIListener?.onRewardedVideoAvailabilityChanged(false)

        // For Rewarded Videos, when an adapter receives a failure reason from the network, it will pass it to the Mediation.
        // This is done in addition to the load failure report of the adapter for further analysis
        errorInfo?.let {
            val rewardedVideoError = mAdapter?.get()?.getLoadErrorAndCheckNoFill(errorInfo, IronSourceError.ERROR_RV_LOAD_NO_FILL)
            mIListener?.onRewardedVideoLoadFailed(rewardedVideoError)
        }
    }

    // Called when the InterstitialAd has been shown.
    override fun onShown(interstitialAd: InterstitialAd?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mIListener?.onRewardedVideoAdOpened()
        mIListener?.onRewardedVideoAdStarted()
    }

    // Called when an error occurs during the InterstitialAd lifecycle.
    override fun onError(interstitialAd: InterstitialAd?, errorInfo: ErrorInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to show placementId = $mPlacementId with error: $errorInfo")
        val errorMsg = mAdapter?.get()?.generateShowFailErrorMessage(errorInfo, "rewarded video show failed")
        val rewardedVideoError: IronSourceError = ErrorBuilder.buildShowFailedError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT, errorMsg)
        mIListener?.onRewardedVideoAdShowFailed(rewardedVideoError)
    }

    // Called when the InterstitialAd has been clicked.
    override fun onClicked(interstitialAd: InterstitialAd?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mIListener?.onRewardedVideoAdClicked()
    }

    // Called when the InterstitialAd causes the user to leave the application.
    override fun onAdLeftApplication(interstitialAd: InterstitialAd?) {
    }

    // This callback is used to surface additional events to the publisher from the SDK.
    override fun onEvent(interstitialAd: InterstitialAd?, source: String?, eventId: String?, arguments: MutableMap<String, Any>?) {
        if (eventId == RV_AD_REWARDED_EVENT) {
            IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
            mIListener?.onRewardedVideoAdRewarded()
        }
    }

    // Called when the InterstitialAd has been closed.
    override fun onClosed(interstitialAd: InterstitialAd?) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mIListener?.onRewardedVideoAdEnded()
        mIListener?.onRewardedVideoAdClosed()
    }
}