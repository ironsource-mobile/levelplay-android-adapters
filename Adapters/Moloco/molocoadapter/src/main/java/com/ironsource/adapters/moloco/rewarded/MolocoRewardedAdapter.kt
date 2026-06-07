package com.ironsource.adapters.moloco.rewarded

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ironsource.adapters.moloco.MolocoAdapter
import com.ironsource.adapters.moloco.MolocoConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.RewardedInterstitialAd
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo

class MolocoRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<MolocoAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var rewardedAd: RewardedInterstitialAd? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: RewardedVideoAdListener
    ) {
        val adUnitId = adData.getString(MolocoConstants.AD_UNIT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MolocoConstants.Logs.AD_UNIT_ID_LOG.format(adUnitId ?: ""))

        if (adUnitId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(MolocoConstants.Logs.MISSING_PARAM.format(MolocoConstants.AD_UNIT_ID_KEY))
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                MolocoConstants.Logs.MISSING_PARAM.format(MolocoConstants.AD_UNIT_ID_KEY)
            )
            return
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            IronLog.INTERNAL.error(MolocoConstants.Logs.SERVER_DATA_EMPTY)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                MolocoConstants.Logs.SERVER_DATA_EMPTY
            )
            return
        }

        Moloco.createRewardedInterstitial(MolocoAdapter.mediationInfo, adUnitId) { ad, error ->
            if (error != null) {
                IronLog.ADAPTER_CALLBACK.error(MolocoConstants.Logs.CREATE_AD_ERROR.format(error.errorCode, error.description))
                listener.onAdLoadFailed(
                    AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                    error.errorCode,
                    error.description
                )
            } else {
                ad?.let {
                    rewardedAd = it
                    rewardedAd?.load(serverData, MolocoRewardedLoadListener(listener))
                } ?: run {
                    listener.onAdLoadFailed(
                        AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                        AdapterErrors.ADAPTER_ERROR_INTERNAL,
                        MolocoConstants.INVALID_CONFIGURATION
                    )
                }
            }
        }
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
                MolocoConstants.AD_NOT_AVAILABLE
            )
            return
        }

        rewardedAd?.show(MolocoRewardedShowListener(listener))
    }

    override fun isAdAvailable(adData: AdData): Boolean =
        rewardedAd != null && rewardedAd?.isLoaded == true

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
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(MolocoConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(MolocoConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion
}
