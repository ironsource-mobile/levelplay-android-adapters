package com.ironsource.adapters.unityads.banner

import android.widget.FrameLayout
import com.ironsource.adapters.unityads.UnityAdsAdapter
import com.ironsource.adapters.unityads.UnityAdsConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.unity3d.ads.BannerAd
import com.unity3d.ads.LoadListener
import com.unity3d.ads.UnityAdsError
import com.unity3d.ads.UnityAdsExperimental
import java.lang.ref.WeakReference

@OptIn(UnityAdsExperimental::class)
class UnityAdsBannerLoadListener(
    private val listener: BannerAdListener,
    private val layoutParams: FrameLayout.LayoutParams,
    private val adapter: WeakReference<UnityAdsBannerAdapter>
) : LoadListener<BannerAd> {

    override fun onAdLoaded(unityAd: BannerAd?, error: UnityAdsError?) {
        if (unityAd != null) {
            IronLog.ADAPTER_CALLBACK.verbose()
            adapter.get()?.setBannerAdView(unityAd)
            listener.onAdLoadSuccess(unityAd.view, layoutParams)
        } else {
            val errorCode = error?.code ?: AdapterErrors.ADAPTER_ERROR_INTERNAL
            val errorMessage = error?.message.orEmpty()
            IronLog.ADAPTER_CALLBACK.error(UnityAdsConstants.Logs.FAILED_TO_LOAD.format(errorCode, errorMessage))
            listener.onAdLoadFailed(UnityAdsAdapter.getLoadErrorType(error), errorCode, errorMessage)
        }
    }
}
