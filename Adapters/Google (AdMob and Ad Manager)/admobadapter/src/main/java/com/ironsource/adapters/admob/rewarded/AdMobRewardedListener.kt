package com.ironsource.adapters.admob.rewarded

import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.ironsource.adapters.admob.AdMobAdapter
import com.ironsource.adapters.admob.AdMobConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import java.lang.ref.WeakReference

class AdMobRewardedListener(
    private val listener: RewardedVideoAdListener,
    private val adapter: WeakReference<AdMobRewardedAdapter>
) : AdLoadCallback<RewardedAd>, RewardedAdEventCallback, OnUserEarnedRewardListener {

    /**
     * Called when the rewarded video ad was loaded successfully
     * @param rewardedAd - the loaded rewarded ad instance
     */
    override fun onAdLoaded(rewardedAd: RewardedAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        adapter.get()?.setRewardedAd(rewardedAd)
        adapter.get()?.setRewardedAdAvailability(true)

        val creativeId = rewardedAd.getResponseInfo()?.responseId
        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess()
        } else {
            IronLog.ADAPTER_CALLBACK.verbose(AdMobConstants.Logs.CREATIVE_ID.format(creativeId))
            listener.onAdLoadSuccess(mapOf(AdMobConstants.CREATIVE_ID_KEY to creativeId))
        }
    }

    /**
     * Called when the rewarded video ad failed to load
     * @param loadAdError - the load error
     */
    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
        IronLog.ADAPTER_CALLBACK.error(AdMobConstants.Logs.FAILED_TO_LOAD.format(loadAdError.code, loadAdError.message))
        adapter.get()?.setRewardedAdAvailability(false)

        listener.onAdLoadFailed(AdMobAdapter.getLoadErrorType(loadAdError.code), loadAdError.code.ordinal, "${loadAdError.message} (${loadAdError.code})")
    }

    /**
     * Called when an impression is recorded for the rewarded video ad
     */
    override fun onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the rewarded video ad was clicked
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the user earned a reward
     * @param rewardItem - the reward item
     */
    override fun onUserEarnedReward(rewardItem: RewardItem) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdRewarded()
    }

    /**
     * Called when the rewarded video ad was dismissed
     */
    override fun onAdDismissedFullScreenContent() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    /**
     * Called when the rewarded video ad failed to show
     * @param error - the show error
     */
    override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
        IronLog.ADAPTER_CALLBACK.error(AdMobConstants.Logs.FAILED_TO_SHOW.format("${error.message} (${error.code})"))
        listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, "${error.message} (${error.code})")
    }
}
