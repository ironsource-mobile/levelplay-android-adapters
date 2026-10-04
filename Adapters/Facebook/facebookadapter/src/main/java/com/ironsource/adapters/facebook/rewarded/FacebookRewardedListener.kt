package com.ironsource.adapters.facebook.rewarded

import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.RewardedVideoAdExtendedListener
import com.ironsource.adapters.facebook.FacebookAdapter
import com.ironsource.adapters.facebook.FacebookConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class FacebookRewardedListener(
    private val listener: RewardedVideoAdListener,
    private val adapter: WeakReference<FacebookRewardedAdapter>
) : RewardedVideoAdExtendedListener {

    internal var showAttempted = false
    private var didCallClosed = false

    /**
     * Called when the rewarded video ad was loaded successfully
     */
    override fun onAdLoaded(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setRewardedAdAvailability(true)
        listener.onAdLoadSuccess()
    }

    /**
     * Called when the rewarded video ad failed to load or show
     * @param ad the ad instance
     * @param adError the error describing the failure
     */
    override fun onError(ad: Ad, adError: AdError) {
        IronLog.ADAPTER_CALLBACK.error(FacebookConstants.Logs.FAILED_TO_LOAD.format(adError.errorCode, adError.errorMessage))
        if (showAttempted) {
            listener.onAdShowFailed(adError.errorCode, adError.errorMessage)
        } else {
            adapter.get()?.setRewardedAdAvailability(false)
            listener.onAdLoadFailed(FacebookAdapter.getLoadErrorType(adError), adError.errorCode, adError.errorMessage)
        }
    }

    /**
     * Called when an impression is recorded and the rewarded video started playing
     */
    override fun onLoggingImpression(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        didCallClosed = false
        listener.onAdOpened()
    }

    /**
     * Called when the rewarded video ad was clicked
     */
    override fun onAdClicked(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the rewarded video finished playing and the reward should be granted
     */
    override fun onRewardedVideoCompleted() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }

    /**
     * Called when the rewarded video ad was closed
     */
    override fun onRewardedVideoClosed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        didCallClosed = true
        listener.onAdClosed()
    }

    /**
     * Called when the rewarded video activity was destroyed without being properly closed.
     * This can happen for singleTask launch mode apps relaunched from background.
     */
    override fun onRewardedVideoActivityDestroyed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        if (!didCallClosed) {
            didCallClosed = true
            listener.onAdClosed()
        }
    }
}
