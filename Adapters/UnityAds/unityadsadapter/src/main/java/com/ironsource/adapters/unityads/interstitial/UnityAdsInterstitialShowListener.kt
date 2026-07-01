package com.ironsource.adapters.unityads.interstitial

import com.ironsource.adapters.unityads.UnityAdsConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.logger.IronLog
import com.unity3d.ads.InterstitialAd
import com.unity3d.ads.InterstitialShowListener
import com.unity3d.ads.ShowFinishState
import com.unity3d.ads.UnityAdsError
import com.unity3d.ads.UnityAdsExperimental

@OptIn(UnityAdsExperimental::class)
class UnityAdsInterstitialShowListener(
    private val listener: InterstitialAdListener
) : InterstitialShowListener {

    override fun onStarted(unityAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    override fun onFailed(unityAd: InterstitialAd, error: UnityAdsError) {
        IronLog.ADAPTER_CALLBACK.error(UnityAdsConstants.Logs.FAILED_TO_SHOW.format(error.code, error.message.orEmpty()))
        listener.onAdShowFailed(error.code, error.message.orEmpty())
    }

    override fun onClicked(unityAd: InterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    override fun onCompleted(unityAd: InterstitialAd, state: ShowFinishState) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }
}
