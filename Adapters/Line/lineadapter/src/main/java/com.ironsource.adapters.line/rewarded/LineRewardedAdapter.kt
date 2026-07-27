package com.ironsource.adapters.line.rewarded

import android.app.Activity
import android.content.Context
import com.five_corp.ad.BidData
import com.five_corp.ad.FiveAdVideoReward
import com.ironsource.adapters.line.LineAdapter
import com.ironsource.adapters.line.LineConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import java.lang.ref.WeakReference

class LineRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<LineAdapter>(networkSettings) {

    private var rewardedAdListener: LineRewardedListener? = null
    private var rewardedAd: FiveAdVideoReward? = null
    private var isAdAvailableFlag = false

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = LineConstants.Logs.MISSING_PARAM.format(LineConstants.SERVER_DATA)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val adLoader = LineAdapter.getAdLoader(context)
        if (adLoader == null) {
            IronLog.INTERNAL.error(LineConstants.Logs.AD_LOADER_NULL)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                LineConstants.Logs.AD_LOADER_NULL
            )
            return
        }

        setRewardedAdAvailability(false)

        rewardedAdListener = LineRewardedListener(listener, WeakReference(this))
        adLoader.loadRewardAd(BidData(serverData, null), rewardedAdListener!!)
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                LineConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        rewardedAd?.apply {
            rewardedAdListener?.let { setEventListener(it) }
            showAd()
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        return rewardedAd != null && isAdAvailableFlag
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedAd = null
        rewardedAdListener = null
        isAdAvailableFlag = false
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            biddingDataCallback.onFailure(LineConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        val appId = adData?.getString(LineConstants.APP_ID_KEY)
        val slotId = adData?.getString(LineConstants.SLOT_ID_KEY).orEmpty()
        networkAdapter.collectBiddingData(context, appId, slotId, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    internal fun setRewardedAdAvailability(isAvailable: Boolean) {
        isAdAvailableFlag = isAvailable
    }

    internal fun setRewardedAd(ad: FiveAdVideoReward) {
        rewardedAd = ad
    }

    // endregion
}
