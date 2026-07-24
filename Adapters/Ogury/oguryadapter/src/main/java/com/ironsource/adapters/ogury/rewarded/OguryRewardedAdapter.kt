package com.ironsource.adapters.ogury.rewarded

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.adapters.ogury.OguryConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.ogury.ad.OguryRewardedAd
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo

class OguryRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<OguryAdapter>(networkSettings) {

    private var rewardedAd: OguryRewardedAd? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val adUnitId = adData.getString(OguryConstants.AD_UNIT_ID_KEY)
        IronLog.ADAPTER_API.verbose(OguryConstants.Logs.AD_UNIT_ID.format(adUnitId ?: ""))

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            IronLog.INTERNAL.error(OguryConstants.Logs.SERVER_DATA_EMPTY)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                OguryConstants.Logs.SERVER_DATA_EMPTY
            )
            return
        }

        rewardedAd = OguryRewardedAd(context.applicationContext, adUnitId, OguryAdapter.mediation).apply {
            setListener(OguryRewardedListener(listener))
            load(serverData)
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                OguryConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        rewardedAd?.show()
    }

    override fun isAdAvailable(adData: AdData): Boolean = rewardedAd?.isLoaded() == true

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
            IronLog.INTERNAL.error(OguryConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(OguryConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion
}
