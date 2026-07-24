package com.ironsource.adapters.unityads.banner

import com.ironsource.adapters.unityads.UnityAdsConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.logger.IronLog
import com.unity3d.ads.BannerAd
import com.unity3d.ads.BannerShowListener
import com.unity3d.ads.UnityAdsError
import com.unity3d.ads.UnityAdsExperimental

@OptIn(UnityAdsExperimental::class)
class UnityAdsBannerShowListener(
    private val listener: BannerAdListener
) : BannerShowListener {

    override fun onImpression(banner: BannerAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    override fun onClicked(banner: BannerAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    override fun onFailedToShow(banner: BannerAd, error: UnityAdsError) {
        IronLog.ADAPTER_CALLBACK.error(UnityAdsConstants.Logs.FAILED_TO_SHOW.format(error.code, error.message.orEmpty()))
    }
}
