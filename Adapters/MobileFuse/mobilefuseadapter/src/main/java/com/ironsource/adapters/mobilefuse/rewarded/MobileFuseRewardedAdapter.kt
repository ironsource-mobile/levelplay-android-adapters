package com.ironsource.adapters.mobilefuse.rewarded

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.mobilefuse.MobileFuseAdapter
import com.ironsource.adapters.mobilefuse.MobileFuseConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.mobilefuse.sdk.MobileFuseRewardedAd
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo

class MobileFuseRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<MobileFuseAdapter>(networkSettings) {

    private var rewardedAd: MobileFuseRewardedAd? = null

    // region LevelPlay Rewarded Video API

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: RewardedVideoAdListener
    ) {
        val placementId = adData.getString(MobileFuseConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MobileFuseConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(MobileFuseConstants.Logs.PLACEMENT_ID_EMPTY)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                MobileFuseConstants.Logs.PLACEMENT_ID_EMPTY
            )
            return
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            IronLog.INTERNAL.error(MobileFuseConstants.SERVER_DATA_EMPTY)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                MobileFuseConstants.SERVER_DATA_EMPTY
            )
            return
        }

        val ad = MobileFuseRewardedAd(context.applicationContext, placementId)
        rewardedAd = ad

        val rewardedVideoAdListener = MobileFuseRewardedListener(listener)
        ad.setListener(rewardedVideoAdListener)
        ad.loadAdFromBiddingToken(serverData)
    }

    override fun showAd(
        adData: AdData,
        activity: Activity,
        listener: RewardedVideoAdListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                MobileFuseConstants.AD_NOT_AVAILABLE
            )
            return
        }

        rewardedAd?.showAd()
    }

    override fun isAdAvailable(adData: AdData): Boolean = rewardedAd?.isLoaded == true

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedAd?.setListener(null)
        rewardedAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(MobileFuseConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(MobileFuseConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion
}
