package com.ironsource.adapters.moloco.interstitial

import com.ironsource.adapters.moloco.MolocoAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.moloco.sdk.publisher.AdLoad
import com.moloco.sdk.publisher.InterstitialAdShowListener
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError
import java.lang.ref.WeakReference

class MolocoInterstitialAdListener(
    private val mListener: InterstitialSmashListener,
    private val mAdapter: WeakReference<MolocoInterstitialAdapter>) : AdLoad.Listener, InterstitialAdShowListener {

    /**
     * Called when Ad was loaded and ready to be displayed
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdLoadSuccess(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdReady()
    }

    /**
     * Called when Ad failed to load
     *
     * @param molocoAdError - MolocoAdError with additional info about error
     */
    override fun onAdLoadFailed(molocoAdError: MolocoAdError) {
        val errorCode = MolocoAdError.ErrorType.AD_LOAD_FAILED.errorCode
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${errorCode}, errorMessage = ${molocoAdError.description}")
        mListener.onInterstitialAdLoadFailed(
            MolocoAdapter.getLoadErrorAndCheckNoFill(
                molocoAdError,
                IronSourceError.ERROR_IS_LOAD_NO_FILL
            )
        )
        mAdapter.get()?.destroyInterstitialAd()
    }

    /**
     * Called when an ad starts displaying. Impression can be recorded.
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdShowSuccess(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdOpened()
        mListener.onInterstitialAdShowSucceeded()
    }

    /**
     * Called when Ad show failed
     *
     * @param molocoAdError - MolocoAdError with additional info about error
     */
    override fun onAdShowFailed(molocoAdError: MolocoAdError) {
        val errorCode = MolocoAdError.ErrorType.AD_SHOW_ERROR
        IronLog.ADAPTER_CALLBACK.verbose("Failed to show, errorCode = ${errorCode}, errorMessage = ${molocoAdError.description}")
        val interstitialError = ErrorBuilder.buildShowFailedError(
            IronSourceConstants.INTERSTITIAL_AD_UNIT,
            molocoAdError.description
        )
        mListener.onInterstitialAdShowFailed(interstitialError)
    }

    /**
     * Called when Ad has been clicked
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdClicked(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClicked()
    }

    /**
     * Called when an ad is hidden.
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdHidden(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onInterstitialAdClosed()
        mAdapter.get()?.destroyInterstitialAd()
    }
}