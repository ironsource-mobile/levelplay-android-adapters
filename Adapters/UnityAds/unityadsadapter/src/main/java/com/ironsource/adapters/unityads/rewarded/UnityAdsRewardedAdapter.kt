package com.ironsource.adapters.unityads.rewarded

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.unityads.UnityAdsAdapter
import com.ironsource.adapters.unityads.UnityAdsConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.ads.AdFormat
import com.unity3d.ads.LoadConfiguration
import com.unity3d.ads.RewardedAd
import com.unity3d.ads.ShowConfiguration
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import java.lang.ref.WeakReference

class UnityAdsRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<UnityAdsAdapter>(networkSettings) {

    private var rewardedAd: RewardedAd? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val zoneId = adData.getString(UnityAdsConstants.ZONE_ID_KEY)
        IronLog.ADAPTER_API.verbose(UnityAdsConstants.Logs.ZONE_ID.format(zoneId ?: ""))

        if (zoneId.isNullOrEmpty()) {
            val errorMessage = UnityAdsConstants.Logs.MISSING_PARAM.format(UnityAdsConstants.ZONE_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val loadConfiguration = LoadConfiguration.Builder(zoneId)
            .withMediationInfo(UnityAdsAdapter.mediationInfo)
            .apply {
                adData.serverData?.let { serverData ->
                    if (serverData.isNotEmpty()) {
                        withAdMarkup(serverData)
                    }
                }
            }
            .build()

        RewardedAd.load(loadConfiguration, UnityAdsRewardedLoadListener(listener, WeakReference(this)))
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            IronLog.INTERNAL.error(UnityAdsConstants.Logs.AD_NOT_AVAILABLE)
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                UnityAdsConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        val showConfiguration = ShowConfiguration.Builder()
            .apply {
                getDynamicUserId()?.let { userId ->
                    if (userId.isNotEmpty()) {
                        withCustomRewardString(userId)
                    }
                }
            }
            .build()

        rewardedAd?.show(activity, showConfiguration, UnityAdsRewardedShowListener(listener))
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        return rewardedAd != null
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(UnityAdsConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(UnityAdsConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(adData, biddingDataCallback, AdFormat.REWARDED)
    }

    // endregion

    // region Helper Methods

    internal fun setRewardedAd(ad: RewardedAd) {
        rewardedAd = ad
    }

    // endregion
}
