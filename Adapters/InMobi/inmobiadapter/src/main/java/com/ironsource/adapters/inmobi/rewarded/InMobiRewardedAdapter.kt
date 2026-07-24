package com.ironsource.adapters.inmobi.rewarded

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.inmobi.ads.InMobiInterstitial
import com.ironsource.adapters.inmobi.InMobiAdapter
import com.ironsource.adapters.inmobi.InMobiConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo

class InMobiRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<InMobiAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var rewardedAd: InMobiInterstitial? = null

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: RewardedVideoAdListener
    ) {
        // Fetch and validate placementId
        val placementId = adData.getString(InMobiConstants.PLACEMENT_ID_KEY)

        IronLog.ADAPTER_API.verbose(InMobiConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        if (adData.serverData.isNullOrEmpty()) {
            val errorMessage = InMobiConstants.Logs.MISSING_PARAM.format(InMobiConstants.SERVER_DATA_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val placement = placementId?.toLongOrNull()
        if (placement == null) {
            val errorMessage = InMobiConstants.Logs.MISSING_PARAM.format(InMobiConstants.PLACEMENT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        mainHandler.post {
            rewardedAd = InMobiInterstitial(
                context.applicationContext,
                placement,
                InMobiRewardedListener(listener)
            )

            val bytes = adData.serverData.toByteArray(Charsets.UTF_8)
            rewardedAd?.load(bytes)
        }
    }

    override fun showAd(
        adData: AdData,
        activity: Activity,
        listener: RewardedVideoAdListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            val errorMessage = InMobiConstants.Logs.AD_NOT_READY_REWARDED_VIDEO
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
            return
        }

        rewardedAd?.let {
            mainHandler.post {
                it.show()
            }
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        return rewardedAd?.isReady() == true
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
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            val errorMessage = InMobiConstants.Logs.NETWORK_ADAPTER_IS_NULL
            IronLog.INTERNAL.error(errorMessage)
            biddingDataCallback.onFailure(errorMessage)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion
}
