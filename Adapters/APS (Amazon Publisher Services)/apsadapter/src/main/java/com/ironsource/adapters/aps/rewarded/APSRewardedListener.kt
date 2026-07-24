package com.ironsource.adapters.aps.rewarded

import com.amazon.aps.ads.ApsAd
import com.amazon.aps.ads.listeners.ApsAdListener
import com.ironsource.adapters.aps.APSConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class APSRewardedListener(
    private val listener: RewardedVideoAdListener,
    private val adapter: WeakReference<APSRewardedAdapter>
) : ApsAdListener {

    /** Called when the rewarded video ad was loaded successfully */
    override fun onAdLoaded(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setAdAvailability(true)
        listener.onAdLoadSuccess()
    }

    /** Called when the rewarded video ad failed to load */
    override fun onAdFailedToLoad(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setAdAvailability(false)
        listener.onAdLoadFailed(
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
            AdapterErrors.ADAPTER_ERROR_INTERNAL,
            APSConstants.Logs.REWARDED_LOAD_FAILED
        )
    }

    /** Called when the rewarded video ad presents a fullscreen overlay */
    override fun onAdOpen(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /** Called when the rewarded video ad impression was fired */
    override fun onImpressionFired(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /** Called when the rewarded video ad encountered an error */
    override fun onAdError(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        // Do not call onAdShowFailed() because onAdError() is sometimes fired when ad display is successful.
    }

    /** Called when the rewarded video ad was clicked */
    override fun onAdClicked(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /** Called when the rewarded video completed and the reward is granted */
    override fun onVideoCompleted(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }

    /** Called when the rewarded video ad was closed */
    override fun onAdClosed(apsAd: ApsAd?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}
