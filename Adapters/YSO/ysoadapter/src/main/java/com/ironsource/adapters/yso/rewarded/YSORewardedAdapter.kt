package com.ironsource.adapters.yso.rewarded

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.yso.YSOAdapter
import com.ironsource.adapters.yso.YSOConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import com.ysocorp.ysonetwork.YsoNetwork
import java.lang.ref.WeakReference

class YSORewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<YSOAdapter>(networkSettings) {

    private var rewardedAdListener: YSORewardedListener? = null
    private var isAdAvailableFlag = false

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val placementKey = adData.getString(YSOConstants.PLACEMENT_KEY)
        IronLog.ADAPTER_API.verbose(YSOConstants.Logs.PLACEMENT_KEY_LOG.format(placementKey ?: ""))

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = YSOConstants.Logs.SERVER_DATA_EMPTY
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        setRewardedAdAvailability(false)

        rewardedAdListener = YSORewardedListener(listener, WeakReference(this))
        YsoNetwork.rewardedLoad(placementKey, serverData, rewardedAdListener)
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        val placementKey = adData.getString(YSOConstants.PLACEMENT_KEY)
        IronLog.ADAPTER_API.verbose(YSOConstants.Logs.PLACEMENT_KEY_LOG.format(placementKey ?: ""))

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                YSOConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        setRewardedAdAvailability(false)
        YsoNetwork.rewardedShow(placementKey, rewardedAdListener, activity)
    }

    override fun isAdAvailable(adData: AdData): Boolean = isAdAvailableFlag

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        isAdAvailableFlag = false
        rewardedAdListener = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(YSOConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(YSOConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    internal fun setRewardedAdAvailability(isAvailable: Boolean) {
        isAdAvailableFlag = isAvailable
    }

    // endregion
}
