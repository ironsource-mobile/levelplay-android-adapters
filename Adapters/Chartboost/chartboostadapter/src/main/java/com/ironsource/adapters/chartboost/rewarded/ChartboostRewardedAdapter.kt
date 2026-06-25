package com.ironsource.adapters.chartboost.rewarded

import android.app.Activity
import android.content.Context
import com.chartboost.sdk.ads.Rewarded
import com.ironsource.adapters.chartboost.ChartboostAdapter
import com.ironsource.adapters.chartboost.ChartboostConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo

class ChartboostRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<ChartboostAdapter>(networkSettings) {

    private var rewardedAd: Rewarded? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: RewardedVideoAdListener
    ) {
        val locationId = adData.getString(ChartboostConstants.AD_LOCATION_KEY)
        IronLog.ADAPTER_API.verbose(ChartboostConstants.Logs.AD_LOCATION.format(locationId ?: ""))

        if (locationId.isNullOrEmpty()) {
            val errorMessage = ChartboostConstants.Logs.MISSING_PARAM.format(ChartboostConstants.AD_LOCATION_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val serverData = adData.serverData
        rewardedAd = Rewarded(
            locationId,
            ChartboostRewardedListener(listener),
            ChartboostAdapter.mediation
        )

        if (serverData.isNullOrEmpty()) {
            rewardedAd?.cache()
        } else {
            rewardedAd?.cache(serverData)
        }
    }

    override fun showAd(
        adData: AdData,
        activity: Activity,
        listener: RewardedVideoAdListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            IronLog.INTERNAL.error(ChartboostConstants.Logs.AD_NOT_AVAILABLE)
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                ChartboostConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        rewardedAd?.show()
    }

    @Suppress("DEPRECATION")
    override fun isAdAvailable(adData: AdData): Boolean =
        rewardedAd?.isCached() == true

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(ChartboostConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(ChartboostConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion
}
