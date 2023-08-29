package com.ironsource.adapters.vungle

import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.vungle.ads.AdConfig
import com.vungle.ads.BaseAd
import com.vungle.ads.InterstitialAd
import com.vungle.ads.InterstitialAdListener
import com.vungle.ads.VungleError

internal class VungleInterstitialAdapter(
    placementId: String,
    adConfig: AdConfig,
    private val listener: InterstitialSmashListener?
) : InterstitialAdListener {
    private var mInterstitialAd: InterstitialAd?

    init {
        mInterstitialAd = InterstitialAd(
            ContextProvider.getInstance().applicationContext,
            placementId,
            adConfig
        )
        mInterstitialAd?.adListener = this
    }

    fun loadWithBid(serverData: String?) {
        mInterstitialAd?.load(serverData)
    }

    fun canPlayAd(): Boolean {
        return mInterstitialAd?.canPlayAd() == true
    }

    fun play() {
        mInterstitialAd?.play()
    }

    fun destroy() {
        mInterstitialAd = null
    }

    override fun onAdLoaded(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onInterstitialAdReady()
    }

    override fun onAdStart(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onInterstitialAdShowSucceeded()
    }

    override fun onAdImpression(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onInterstitialAdOpened()
    }

    override fun onAdClicked(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onInterstitialAdClicked()
    }

    override fun onAdEnd(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = ${baseAd.placementId}")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        listener.onInterstitialAdClosed()
    }

    override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToPlay placementId = ${baseAd.placementId}, error = $adError")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        val errorMessage = " reason = " + adError.errorMessage + " errorCode = " + adError.code
        listener.onInterstitialAdShowFailed(
            ErrorBuilder.buildShowFailedError(
                IronSourceConstants.INTERSTITIAL_AD_UNIT,
                errorMessage
            )
        )
    }

    override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToLoad placementId = ${baseAd.placementId}, error = $adError")
        if (listener == null) {
            IronLog.INTERNAL.verbose("listener is null")
            return
        }
        val error: IronSourceError = if (adError.code == VungleError.NO_SERVE) {
            IronSourceError(IronSourceError.ERROR_IS_LOAD_NO_FILL, adError.errorMessage)
        } else {
            ErrorBuilder.buildLoadFailedError(adError.errorMessage)
        }
        listener.onInterstitialAdLoadFailed(error)
    }

    override fun onAdLeftApplication(baseAd: BaseAd) {
        // no-op
    }
}
