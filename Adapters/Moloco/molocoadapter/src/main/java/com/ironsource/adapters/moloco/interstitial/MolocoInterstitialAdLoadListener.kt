package com.ironsource.adapters.moloco.interstitial

import com.ironsource.adapters.moloco.MolocoAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.moloco.sdk.publisher.AdLoad
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError
import java.lang.ref.WeakReference

class MolocoInterstitialAdLoadListener(
    private val mListener: InterstitialSmashListener,
    private val mAdapter: WeakReference<MolocoInterstitialAdapter>) : AdLoad.Listener {

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
}