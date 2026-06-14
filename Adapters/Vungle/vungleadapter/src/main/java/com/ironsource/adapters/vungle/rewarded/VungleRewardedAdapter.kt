package com.ironsource.adapters.vungle.rewarded

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ironsource.adapters.vungle.VungleAdapter
import com.ironsource.adapters.vungle.VungleConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import com.vungle.ads.AdConfig
import com.vungle.ads.RewardedAd

class VungleRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<VungleAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var rewardedVideoAd: RewardedAd? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val placementId = adData.getString(VungleConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(VungleConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        if (placementId.isNullOrEmpty()) {
            val errorMessage = VungleConstants.Logs.MISSING_PARAM.format(VungleConstants.PLACEMENT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        rewardedVideoAd = RewardedAd(context.applicationContext, placementId, AdConfig()).apply {
            adListener = VungleRewardedListener(listener)
            adapterAdFormat = VungleConstants.ADAPTER_FORMAT_REWARDED
        }
        rewardedVideoAd?.load(adData.serverData)
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            IronLog.INTERNAL.error(VungleConstants.Logs.AD_NOT_AVAILABLE)
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, VungleConstants.Logs.AD_NOT_AVAILABLE)
            return
        }

        getDynamicUserId()?.let { userId ->
            if (userId.isNotEmpty()) {
                rewardedVideoAd?.setUserId(userId)
            }
        }

        mainHandler.post {
            rewardedVideoAd?.play()
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        return rewardedVideoAd?.canPlayAd() == true
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedVideoAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(VungleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(VungleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion
}
