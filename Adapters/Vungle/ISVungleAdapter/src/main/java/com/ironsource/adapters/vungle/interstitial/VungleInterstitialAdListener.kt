package com.ironsource.adapters.vungle.interstitial

import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.vungle.ads.BaseAd
import com.vungle.ads.InterstitialAdListener
import com.vungle.ads.VungleError
import java.lang.ref.WeakReference

class VungleInterstitialAdListener(
    private val mAdapter: WeakReference<VungleInterstitialAdapter>,
    private val mListener: InterstitialSmashListener,
    private val mPlacementId: String
) : InterstitialAdListener {

    /**
     * Called to indicate that an ad was loaded and it can now be shown.
     *
     * @param baseAd - Interstitial instance
     */
    override fun onAdLoaded(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mAdapter.get()?.setInterstitialAdAvailability(mPlacementId,true)
        mListener.onInterstitialAdReady()
    }

    /**
     * Called when Ad failed to load
     *
     * @param baseAd - Interstitial instance
     * @param adError - adError with additional info about error
     */
    override fun onAdFailedToLoad(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToLoad placementId = $mPlacementId, errorCode= ${adError.code} error = ${adError.errorMessage}")
        mAdapter.get()?.setInterstitialAdAvailability(mPlacementId,false)
        val adapterError = "${adError.errorMessage}( ${adError.code} )"
        val error = if (adError.code == VungleError.NO_SERVE) {
            IronSourceError(IronSourceError.ERROR_IS_LOAD_NO_FILL, adapterError)
        } else {
            ErrorBuilder.buildLoadFailedError(adapterError)
        }
        mListener.onInterstitialAdLoadFailed(error)
    }

    /**
     * Called to indicate that an ad started.
     *
     * @param baseAd - Interstitial instance
     */
    override fun onAdStart(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onInterstitialAdShowSucceeded()
    }

    /**
     * Called to indicate that the fullscreen overlay is now the topmost screen.
     *
     * @param baseAd - Interstitial instance
     */
    override fun onAdImpression(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onInterstitialAdOpened()
    }

    /**
     * Called to indicate that a request to show an ad (by calling [VungleInterstitial.show]
     * failed. You should call [VungleInterstitial.load] to request for a fresh ad.
     *
     * @param baseAd - Interstitial instance
     * @param adError - adError with additional info about error
     */
    override fun onAdFailedToPlay(baseAd: BaseAd, adError: VungleError) {
        IronLog.ADAPTER_CALLBACK.verbose("onAdFailedToPlay placementId = $mPlacementId, errorCode = ${adError.code}, errorMessage = ${adError.message}")
        val errorMessage = " reason = " + adError.errorMessage + " errorCode = " + adError.code
        val error: IronSourceError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.INTERSTITIAL_AD_UNIT,
            errorMessage
        )
        mListener.onInterstitialAdShowFailed(error)
    }

    /**
     * Called to indicate that an ad interaction was observed.
     *
     * @param baseAd - Interstitial instance
     */
    override fun onAdClicked(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onInterstitialAdClicked()
    }

    /**
     * Called to indicate that the ad was ended and closed.
     *
     * @param baseAd - Interstitial instance
     */
    override fun onAdEnd(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
        mListener.onInterstitialAdClosed()
    }

    /**
     * Called to indicate that the user may leave the application on account of interacting with the ad.
     *
     * @param baseAd Represents the [VungleInterstitialAd] ad
     */
    override fun onAdLeftApplication(baseAd: BaseAd) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = $mPlacementId")
    }
}
