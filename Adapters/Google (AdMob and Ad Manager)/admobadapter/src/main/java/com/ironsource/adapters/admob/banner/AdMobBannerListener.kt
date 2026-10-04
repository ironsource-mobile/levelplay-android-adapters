package com.ironsource.adapters.admob.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.ironsource.adapters.admob.AdMobAdapter
import com.ironsource.adapters.admob.AdMobConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.logger.IronLog

class AdMobBannerListener(
    private val listener: BannerAdListener,
    private val adView: AdView
) : AdLoadCallback<BannerAd>, BannerAdEventCallback {

    /**
     * Called when the banner ad was loaded successfully
     * @param ad - the loaded banner ad instance
     */
    override fun onAdLoaded(ad: BannerAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        ad.adEventCallback = this

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER }

        val creativeId = ad.getResponseInfo()?.responseId
        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess(adView, layoutParams)
        } else {
            IronLog.ADAPTER_CALLBACK.verbose(AdMobConstants.Logs.CREATIVE_ID.format(creativeId))
            listener.onAdLoadSuccess(adView, layoutParams, mapOf(AdMobConstants.CREATIVE_ID_KEY to creativeId))
        }
    }

    /**
     * Called when the banner ad failed to load
     * @param adError - the load error
     */
    override fun onAdFailedToLoad(adError: LoadAdError) {
        IronLog.ADAPTER_CALLBACK.error(AdMobConstants.Logs.FAILED_TO_LOAD.format(adError.code, adError.message))
        listener.onAdLoadFailed(AdMobAdapter.getLoadErrorType(adError.code), adError.code.ordinal, "${adError.message} (${adError.code})")
    }

    /**
     * Called when an impression is recorded for the banner ad
     */
    override fun onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the banner ad was clicked
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the banner ad opened a fullscreen overlay after a click
     */
    override fun onAdShowedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenPresented()
    }

    /**
     * Called when the fullscreen overlay was dismissed and the user returns to the app
     */
    override fun onAdDismissedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenDismissed()
    }
}
