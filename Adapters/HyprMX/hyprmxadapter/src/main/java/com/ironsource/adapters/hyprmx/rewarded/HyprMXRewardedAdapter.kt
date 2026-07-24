package com.ironsource.adapters.hyprmx.rewarded

import android.app.Activity
import android.content.Context
import com.hyprmx.android.sdk.core.HyprMX
import com.hyprmx.android.sdk.placement.Placement
import com.ironsource.adapters.hyprmx.HyprMXAdapter
import com.ironsource.adapters.hyprmx.HyprMXConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class HyprMXRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<HyprMXAdapter>(networkSettings) {

    private var rewardedAd: Placement? = null
    private var reservedPropertyId: String? = null

    companion object {
        private val rewardedPropertyIds: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    }

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: RewardedVideoAdListener
    ) {
        val propertyId = adData.getString(HyprMXConstants.PROPERTY_ID_KEY)
        IronLog.ADAPTER_API.verbose(HyprMXConstants.Logs.PROPERTY_ID.format(propertyId ?: ""))

        if (propertyId.isNullOrEmpty()) {
            val errorMessage = HyprMXConstants.Logs.MISSING_PARAM.format(HyprMXConstants.PROPERTY_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        if (propertyId in rewardedPropertyIds) {
            IronLog.INTERNAL.error(HyprMXConstants.Logs.DUPLICATE_PLACEMENT_RV)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                HyprMXConstants.Logs.DUPLICATE_PLACEMENT_RV
            )
            return
        }

        rewardedPropertyIds.add(propertyId)
        reservedPropertyId = propertyId

        val rewardedAd = HyprMX.getPlacement(propertyId).also { this.rewardedAd = it }

        val onResult: (Boolean) -> Unit = { isAdAvailable ->
            if (isAdAvailable) {
                listener.onAdLoadSuccess()
            } else {
                listener.onAdLoadFailed(
                    AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL,
                    AdapterErrors.ADAPTER_ERROR_INTERNAL,
                    HyprMXConstants.Logs.AD_NOT_AVAILABLE
                )
            }
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            rewardedAd.loadAd(onResult)
        } else {
            rewardedAd.loadAd(serverData, onResult)
        }
    }

    override fun showAd(
        adData: AdData,
        activity: Activity,
        listener: RewardedVideoAdListener
    ) {
        val propertyId = adData.getString(HyprMXConstants.PROPERTY_ID_KEY)
        IronLog.ADAPTER_API.verbose(HyprMXConstants.Logs.PROPERTY_ID.format(propertyId))
        reservedPropertyId?.let { rewardedPropertyIds.remove(it) }
        reservedPropertyId = null

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                HyprMXConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        rewardedAd?.showAd(HyprMXRewardedListener(listener))
    }

    override fun isAdAvailable(adData: AdData): Boolean =
        rewardedAd?.isAdAvailable() == true

    override fun destroyAd(adData: AdData) {
        val propertyId = adData.getString(HyprMXConstants.PROPERTY_ID_KEY)
        IronLog.ADAPTER_API.verbose(HyprMXConstants.Logs.PROPERTY_ID.format(propertyId))
        reservedPropertyId?.let { rewardedPropertyIds.remove(it) }
        reservedPropertyId = null
        rewardedAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(HyprMXConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(HyprMXConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion
}
