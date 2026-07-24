package com.ironsource.adapters.moloco.interstitial

import com.ironsource.adapters.moloco.MolocoConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.logger.IronLog
import com.moloco.sdk.publisher.AdLoad
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError

class MolocoInterstitialLoadListener(
    private val listener: InterstitialAdListener
) : AdLoad.Listener {

    /**
     * Called when Ad was loaded and ready to be displayed
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdLoadSuccess(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess()
    }

    /**
     * Called when Ad failed to load
     *
     * @param molocoAdError - MolocoAdError with additional info about error
     */
    override fun onAdLoadFailed(molocoAdError: MolocoAdError) {
        val errorCode = molocoAdError.errorType.errorCode
        IronLog.ADAPTER_CALLBACK.error(MolocoConstants.Logs.FAILED_TO_LOAD.format(errorCode, molocoAdError.description))

        val errorType = if (molocoAdError.errorType == MolocoAdError.ErrorType.AD_LOAD_FAILED) {
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
        } else {
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
        }

        listener.onAdLoadFailed(errorType, errorCode, molocoAdError.description)
    }
}
