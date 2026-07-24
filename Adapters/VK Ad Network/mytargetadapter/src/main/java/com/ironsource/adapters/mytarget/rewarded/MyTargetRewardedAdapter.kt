package com.ironsource.adapters.mytarget.rewarded

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.mytarget.MyTargetAdapter
import com.ironsource.adapters.mytarget.MyTargetConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.my.target.ads.RewardedAd
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import java.lang.ref.WeakReference

class MyTargetRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<MyTargetAdapter>(networkSettings) {

    private var rewardedAd: RewardedAd? = null
    private var isAdAvailableFlag = false

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val slotId = adData.getString(MyTargetConstants.SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MyTargetConstants.Logs.SLOT_ID.format(slotId ?: ""))

        val slotIdInt = slotId?.toIntOrNull()
        if (slotIdInt == null) {
            val errorMessage = MyTargetConstants.Logs.ERROR_PARSING_PLACEMENT
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        setRewardedAdAvailability(false)

        val serverData = adData.serverData

        rewardedAd = RewardedAd(slotIdInt, context.applicationContext).apply {
            setListener(MyTargetRewardedListener(listener, WeakReference(this@MyTargetRewardedAdapter)))
            customParams.setCustomParam(MyTargetConstants.CUSTOM_PARAM_MEDIATION_KEY, MyTargetConstants.IRONSOURCE_MEDIATION)
            if (serverData != null) {
                loadFromBid(serverData)
            } else {
                load()
            }
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            IronLog.INTERNAL.error(MyTargetConstants.Logs.AD_NOT_AVAILABLE)
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, MyTargetConstants.Logs.AD_NOT_AVAILABLE)
            return
        }

        rewardedAd?.show()
    }

    override fun isAdAvailable(adData: AdData): Boolean = rewardedAd != null && isAdAvailableFlag

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(MyTargetConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(MyTargetConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    internal fun setRewardedAdAvailability(isAvailable: Boolean) {
        isAdAvailableFlag = isAvailable
    }

    // endregion
}
