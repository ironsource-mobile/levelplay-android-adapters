package com.ironsource.adapters.verve.rewarded

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.verve.VerveAdapter
import com.ironsource.adapters.verve.VerveConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import net.pubnative.lite.sdk.rewarded.HyBidRewardedAd

class VerveRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<VerveAdapter>(networkSettings) {

    private var rewardedAdListener: VerveRewardedListener? = null
    private var ad: HyBidRewardedAd? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: RewardedVideoAdListener
    ) {
        val zoneId = adData.getString(VerveConstants.ZONE_ID_KEY)
        IronLog.ADAPTER_API.verbose(VerveConstants.Logs.ZONE_ID.format(zoneId ?: ""))

        if (zoneId.isNullOrEmpty()) {
            val errorMessage = VerveConstants.Logs.MISSING_PARAM.format(VerveConstants.ZONE_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = VerveConstants.Logs.MISSING_PARAM.format(VerveConstants.SERVER_DATA)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val rewardedListener = VerveRewardedListener(listener)
        rewardedAdListener = rewardedListener

        val rewardedAd = HyBidRewardedAd(
            context.applicationContext,
            zoneId,
            rewardedAdListener
        )

        ad = rewardedAd
        rewardedAd.prepareAd(serverData)
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
                VerveConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        ad?.show() ?: run {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                VerveConstants.Logs.AD_NOT_AVAILABLE
            )
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean = ad?.isReady == true

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        ad?.destroy()
        ad = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            biddingDataCallback.onFailure(VerveConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion

}
