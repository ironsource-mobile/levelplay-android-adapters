package com.ironsource.adapters.mintegral.rewarded

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.mintegral.MintegralAdapter
import com.ironsource.adapters.mintegral.MintegralConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.mbridge.msdk.mbbid.out.BidConstants
import com.mbridge.msdk.out.MBBidRewardVideoHandler
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class MintegralRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<MintegralAdapter>(networkSettings) {

    private var rewardedVideoAd: MBBidRewardVideoHandler? = null
    private var reservedPlacementId: String? = null

    companion object {
        private val rewardedPlacementIds: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    }

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: RewardedVideoAdListener
    ) {
        val placementId = adData.getString(MintegralConstants.PLACEMENT_ID_KEY)
        val unitId = adData.getString(MintegralConstants.UNIT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.PLACEMENT_ID_AND_UNIT_ID.format(placementId ?: "", unitId ?: ""))

        if (unitId.isNullOrEmpty()) {
            val errorMessage = MintegralConstants.Logs.MISSING_PARAM.format(MintegralConstants.UNIT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        if (placementId in rewardedPlacementIds) {
            IronLog.INTERNAL.error(MintegralConstants.Logs.DUPLICATE_PLACEMENT_RV)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                MintegralConstants.Logs.DUPLICATE_PLACEMENT_RV
            )
            return
        }

        placementId?.let {
            rewardedPlacementIds.add(it)
            reservedPlacementId = it
        }

        rewardedVideoAd = MBBidRewardVideoHandler(
            context.applicationContext,
            placementId,
            unitId
        ).apply {
            setRewardVideoListener(MintegralRewardedListener(listener, this))
        }

        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.LOAD_REWARDED.format(placementId, unitId, adData.serverData))
        rewardedVideoAd?.loadFromBid(adData.serverData)
    }

    override fun showAd(
        adData: AdData,
        activity: Activity,
        listener: RewardedVideoAdListener
    ) {
        val placementId = adData.getString(MintegralConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.PLACEMENT_ID.format(placementId))
        reservedPlacementId?.let { rewardedPlacementIds.remove(it) }
        reservedPlacementId = null

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                MintegralConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        val userId = getDynamicUserId()
        if (!userId.isNullOrEmpty()) {
            rewardedVideoAd?.showFromBid(activity, userId)
        } else {
            rewardedVideoAd?.showFromBid(activity)
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean =
        rewardedVideoAd?.isBidReady == true

    override fun destroyAd(adData: AdData) {
        val placementId = adData.getString(MintegralConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.PLACEMENT_ID.format(placementId))
        reservedPlacementId?.let { rewardedPlacementIds.remove(it) }
        reservedPlacementId = null
        rewardedVideoAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(MintegralConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(MintegralConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        val placementId = adData?.getString(MintegralConstants.PLACEMENT_ID_KEY)
        val unitId = adData?.getString(MintegralConstants.UNIT_ID_KEY)
        networkAdapter.collectBiddingData(context, BidConstants.BID_FILTER_VALUE_AD_TYPE_REWARD_VIDEO, placementId, unitId, biddingDataCallback)
    }

    // endregion
}
