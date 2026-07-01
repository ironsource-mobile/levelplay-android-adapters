package com.ironsource.adapters.unityads.interstitial

import com.ironsource.adapters.unityads.UnityAdsAdapter
import com.ironsource.adapters.unityads.UnityAdsConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.unity3d.ads.InterstitialAd
import com.unity3d.ads.LoadListener
import com.unity3d.ads.UnityAdsError
import com.unity3d.ads.UnityAdsExperimental
import java.lang.ref.WeakReference

@OptIn(UnityAdsExperimental::class)
class UnityAdsInterstitialLoadListener(
    private val listener: InterstitialAdListener,
    private val adapter: WeakReference<UnityAdsInterstitialAdapter>
) : LoadListener<InterstitialAd> {

    override fun onAdLoaded(unityAd: InterstitialAd?, error: UnityAdsError?) {
        if (unityAd != null) {
            IronLog.ADAPTER_CALLBACK.verbose()
            adapter.get()?.setInterstitialAd(unityAd)
            listener.onAdLoadSuccess()
        } else {
            val errorCode = error?.code ?: AdapterErrors.ADAPTER_ERROR_INTERNAL
            val errorMessage = error?.message.orEmpty()
            IronLog.ADAPTER_CALLBACK.error(UnityAdsConstants.Logs.FAILED_TO_LOAD.format(errorCode, errorMessage))
            listener.onAdLoadFailed(UnityAdsAdapter.getLoadErrorType(error), errorCode, errorMessage)
        }
    }
}
