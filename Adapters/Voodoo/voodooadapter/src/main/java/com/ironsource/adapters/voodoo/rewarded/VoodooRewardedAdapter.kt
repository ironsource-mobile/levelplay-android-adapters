package com.ironsource.adapters.voodoo.rewarded

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.voodoo.VoodooAdapter
import com.ironsource.adapters.voodoo.VoodooConstants
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo
import io.adn.sdk.publisher.AdnAdPlacement
import io.adn.sdk.publisher.AdnAdRequest
import io.adn.sdk.publisher.AdnFullscreenAd
import io.adn.sdk.publisher.AdnLoadTimeout
import io.adn.sdk.publisher.AdnSdk

class VoodooRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<VoodooAdapter>(networkSettings) {

    private var rewardedAd: AdnFullscreenAd? = null
    private var rewardedAdListener: VoodooRewardedListener? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val placementId = adData.getString(VoodooConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(VoodooConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = VoodooConstants.Logs.SERVER_DATA_EMPTY
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val activity: Activity = ContextProvider.getInstance().currentActiveActivity
        val voodooRewardedListener = VoodooRewardedListener(listener)
        rewardedAdListener = voodooRewardedListener
        rewardedAd = AdnSdk.getRewardedAdInstance(activity, voodooRewardedListener)
        rewardedAd?.load(
            AdnAdRequest.AdBidRequest(AdnAdPlacement.REWARDED, serverData, AdnLoadTimeout.MEDIATION)
        ) ?: listener.onAdLoadFailed(
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
            AdapterErrors.ADAPTER_ERROR_INTERNAL,
            VoodooConstants.Logs.AD_NULL
        )
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                VoodooConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        rewardedAd?.show() ?: listener.onAdShowFailed(
            AdapterErrors.ADAPTER_ERROR_INTERNAL,
            VoodooConstants.Logs.AD_NULL
        )
    }

    override fun isAdAvailable(adData: AdData): Boolean = rewardedAd?.isReady() == true

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        rewardedAd?.destroy()
        rewardedAd = null
        rewardedAdListener = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(VoodooConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(VoodooConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback, AdnAdPlacement.REWARDED)
    }

    // endregion
}
