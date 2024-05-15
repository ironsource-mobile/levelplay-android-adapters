package com.ironsource.adapters.yandex.interstitial

import com.ironsource.adapters.yandex.YandexAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import java.lang.ref.WeakReference

class YandexInterstitialAdListener(
    private val mListener: InterstitialSmashListener,
    private val mAdapter: WeakReference<YandexInterstitialAdapter>,
) : InterstitialAdLoadListener, InterstitialAdEventListener {

    override fun onAdLoaded(interstitialAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mAdapter.get()?.setInterstitialAd(interstitialAd)
        mAdapter.get()?.setInterstitialAdAvailability(true)
        mListener.onInterstitialAdReady()
    }

    override fun onAdFailedToLoad(error: AdRequestError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${error.code}, errorMessage = ${error.description}")
        mAdapter.get()?.setInterstitialAdAvailability(false)
        mListener.onInterstitialAdLoadFailed(
            YandexAdapter.getLoadErrorAndCheckNoFill(
                error,
                IronSourceError.ERROR_IS_LOAD_NO_FILL
            )
        )
        mAdapter.get()?.destroyInterstitialAd()
    }

    override fun onAdShown() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    override fun onAdImpression(impressionData: ImpressionData?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdOpened()
        mListener.onInterstitialAdShowSucceeded()
    }

    override fun onAdFailedToShow(adError: AdError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to show, errorMessage = ${adError.description}")
        val interstitialError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.INTERSTITIAL_AD_UNIT,
            adError.description
        )
        mListener.onInterstitialAdShowFailed(interstitialError)
    }

    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClicked()
    }

    override fun onAdDismissed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClosed()
        mAdapter.get()?.destroyInterstitialAd()
    }
}