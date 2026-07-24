package com.ironsource.adapters.unityads.rewarded

import com.ironsource.adapters.unityads.UnityAdsAdapter
import com.ironsource.adapters.unityads.UnityAdsConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.unity3d.ads.LoadListener
import com.unity3d.ads.RewardedAd
import com.unity3d.ads.UnityAdsError
import com.unity3d.ads.UnityAdsExperimental
import java.lang.ref.WeakReference

@OptIn(UnityAdsExperimental::class)
class UnityAdsRewardedLoadListener(
    private val listener: RewardedVideoAdListener,
    private val adapter: WeakReference<UnityAdsRewardedAdapter>
) : LoadListener<RewardedAd> {

    override fun onAdLoaded(unityAd: RewardedAd?, error: UnityAdsError?) {
        if (unityAd != null) {
            IronLog.ADAPTER_CALLBACK.verbose()
            adapter.get()?.setRewardedAd(unityAd)
            listener.onAdLoadSuccess()
        } else {
            val errorCode = error?.code ?: AdapterErrors.ADAPTER_ERROR_INTERNAL
            val errorMessage = error?.message.orEmpty()
            IronLog.ADAPTER_CALLBACK.error(UnityAdsConstants.Logs.FAILED_TO_LOAD.format(errorCode, errorMessage))
            listener.onAdLoadFailed(UnityAdsAdapter.getLoadErrorType(error), errorCode, errorMessage)
        }
    }
}
