package com.ironsource.adapters.pubmatic.rewarded

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ironsource.adapters.pubmatic.PubMaticAdapter
import com.ironsource.adapters.pubmatic.PubMaticConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import com.pubmatic.sdk.common.POBAdFormat
import com.pubmatic.sdk.rewardedad.POBRewardedAd

class PubMaticRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<PubMaticAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var rewardedAd: POBRewardedAd? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val adUnitId = adData.getString(PubMaticConstants.AD_UNIT_ID_KEY)
        IronLog.ADAPTER_API.verbose(PubMaticConstants.Logs.AD_UNIT_ID.format(adUnitId ?: ""))

        if (adUnitId.isNullOrEmpty()) {
            val errorMessage = PubMaticConstants.Logs.MISSING_PARAM.format(PubMaticConstants.AD_UNIT_ID_KEY)
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
            val errorMessage = PubMaticConstants.Logs.SERVER_DATA_IS_NULL
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        rewardedAd = POBRewardedAd.getRewardedAd(context.applicationContext).apply {
            setListener(PubMaticRewardedListener(listener))
        }

        mainHandler.post {
            rewardedAd?.loadAd(serverData, PubMaticAdapter.BIDDING_HOST)
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, PubMaticConstants.Logs.AD_NOT_AVAILABLE)
            return
        }

        mainHandler.post {
            rewardedAd?.show() ?: run {
                listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, PubMaticConstants.Logs.AD_IS_NULL)
            }
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean = rewardedAd?.isReady == true

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            rewardedAd?.destroy()
            rewardedAd = null
        }
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(PubMaticConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(PubMaticConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback, POBAdFormat.REWARDEDAD)
    }

    // endregion
}
