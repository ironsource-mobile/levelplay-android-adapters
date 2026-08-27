package com.ironsource.adapters.mintegral.interstitial

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.mintegral.MintegralAdapter
import com.ironsource.adapters.mintegral.MintegralConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.mbridge.msdk.mbbid.out.BidConstants
import com.mbridge.msdk.newinterstitial.out.MBBidNewInterstitialHandler
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class MintegralInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<MintegralAdapter>(networkSettings) {

    private var interstitialAd: MBBidNewInterstitialHandler? = null
    private var interstitialAdListener: MintegralInterstitialListener? = null
    private var reservedPlacementId: String? = null

    companion object {
        private val interstitialPlacementIds: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    }

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: InterstitialAdListener
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

        if (placementId in interstitialPlacementIds) {
            IronLog.INTERNAL.error(MintegralConstants.Logs.DUPLICATE_PLACEMENT_IS)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                MintegralConstants.Logs.DUPLICATE_PLACEMENT_IS
            )
            return
        }

        placementId?.let {
            interstitialPlacementIds.add(it)
            reservedPlacementId = it
        }

        interstitialAd = MBBidNewInterstitialHandler(
            context.applicationContext,
            placementId,
            unitId
        ).apply {
            interstitialAdListener = MintegralInterstitialListener(listener, this)
            setInterstitialVideoListener(interstitialAdListener)
        }

        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.LOAD_INTERSTITIAL.format(placementId, unitId, adData.serverData))
        interstitialAd?.loadFromBid(adData.serverData)
    }

    override fun showAd(
        adData: AdData,
        activity: Activity,
        listener: InterstitialAdListener
    ) {
        val placementId = adData.getString(MintegralConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.PLACEMENT_ID.format(placementId))
        reservedPlacementId?.let { interstitialPlacementIds.remove(it) }
        reservedPlacementId = null

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                MintegralConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        interstitialAd?.showFromBid(activity)
    }

    override fun isAdAvailable(adData: AdData): Boolean =
        interstitialAd?.isBidReady == true

    override fun destroyAd(adData: AdData) {
        val placementId = adData.getString(MintegralConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MintegralConstants.Logs.PLACEMENT_ID.format(placementId))
        reservedPlacementId?.let { interstitialPlacementIds.remove(it) }
        reservedPlacementId = null
        interstitialAdListener = null
        interstitialAd = null
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
        networkAdapter.collectBiddingData(context, BidConstants.BID_FILTER_VALUE_AD_TYPE_INTERSTITIAL_VIDEO, placementId, unitId, biddingDataCallback)
    }

    // endregion
}
