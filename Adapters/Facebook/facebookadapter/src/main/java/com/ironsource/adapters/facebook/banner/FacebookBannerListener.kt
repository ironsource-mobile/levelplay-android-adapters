package com.ironsource.adapters.facebook.banner

import android.widget.FrameLayout
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdListener
import com.facebook.ads.AdView
import com.ironsource.adapters.facebook.FacebookAdapter
import com.ironsource.adapters.facebook.FacebookConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.logger.IronLog

class FacebookBannerListener(
    private val listener: BannerAdListener,
    private val adView: AdView,
    private val layoutParams: FrameLayout.LayoutParams
) : AdListener {

    /**
     * Called when the banner ad was loaded successfully
     */
    override fun onAdLoaded(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess(adView, layoutParams)
    }

    /**
     * Called when the banner ad failed to load
     * @param ad the ad instance
     * @param adError the error describing the failure
     */
    override fun onError(ad: Ad, adError: AdError) {
        IronLog.ADAPTER_CALLBACK.error(FacebookConstants.Logs.FAILED_TO_LOAD.format(adError.errorCode, adError.errorMessage))
        listener.onAdLoadFailed(FacebookAdapter.getLoadErrorType(adError), adError.errorCode, adError.errorMessage)
    }

    /**
     * Called when an impression is recorded for the banner ad
     */
    override fun onLoggingImpression(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the banner ad was clicked
     */
    override fun onAdClicked(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }
}
