package com.ironsource.adapters.line.interstitial

import com.five_corp.ad.AdLoader
import com.five_corp.ad.FiveAdErrorCode
import com.five_corp.ad.FiveAdInterstitial
import com.five_corp.ad.FiveAdInterstitialEventListener
import com.ironsource.adapters.line.LineAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import java.lang.ref.WeakReference

class LineInterstitialAdListener (
    private val mListener: InterstitialSmashListener,
    private val mAdapter: WeakReference<LineInterstitialAdapter>
) : FiveAdInterstitialEventListener, AdLoader.LoadInterstitialAdCallback {

    override fun onLoad(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mAdapter.get()?.setInterstitialAd(interstitialAd)
        mListener.onInterstitialAdReady()
    }

    override fun onError(errorCode: FiveAdErrorCode) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode: ${errorCode.name}")
        mListener.onInterstitialAdLoadFailed(
            LineAdapter.getLoadError(errorCode)
        )
        mAdapter.get()?.destroyInterstitialAd()
    }

    override fun onViewError(interstitialAd: FiveAdInterstitial, errorCode: FiveAdErrorCode) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to show, error = ${errorCode.name}")
        val interstitialError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.INTERSTITIAL_AD_UNIT,
            errorCode.name
        )
        mListener.onInterstitialAdShowFailed(interstitialError)
    }

    override fun onImpression(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdOpened()
        mListener.onInterstitialAdShowSucceeded()
    }

    override fun onClick(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClicked()
    }

    override fun onFullScreenOpen(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    override fun onFullScreenClose(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClosed()
        mAdapter.get()?.destroyInterstitialAd()
    }

    override fun onPlay(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    override fun onPause(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    override fun onViewThrough(interstitialAd: FiveAdInterstitial) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

}