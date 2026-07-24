package com.ironsource.adapters.fyber.rewarded

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.InneractiveAdRequest
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController
import com.ironsource.adapters.fyber.FyberAdapter
import com.ironsource.adapters.fyber.FyberConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseRewardedVideo

class FyberRewardedAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseRewardedVideo<FyberAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var rewardedAd: InneractiveAdSpot? = null
    private var rewardedAdListener: FyberRewardedListener? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: RewardedVideoAdListener) {
        val spotId = adData.getString(FyberConstants.SPOT_ID_KEY)
        IronLog.ADAPTER_API.verbose(FyberConstants.Logs.SPOT_ID.format(spotId ?: ""))

        if (spotId.isNullOrEmpty()) {
            val errorMessage = FyberConstants.Logs.MISSING_PARAM.format(FyberConstants.SPOT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val serverData = adData.serverData
        mainHandler.post {
            val fyberRewardedListener = FyberRewardedListener(listener)
            rewardedAdListener = fyberRewardedListener

            rewardedAd = InneractiveAdSpotManager.get().createSpot().apply {
                setMediationName(FyberConstants.MEDIATION_NAME)
                setMediationVersion(FyberConstants.ADAPTER_VERSION)
                addUnitController(InneractiveFullscreenUnitController())
                setRequestListener(fyberRewardedListener)
            }

            if (serverData.isNullOrEmpty()) {
                rewardedAd?.requestAd(InneractiveAdRequest(spotId))
            } else {
                rewardedAd?.loadAd(serverData)
            }
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: RewardedVideoAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            IronLog.INTERNAL.error(FyberConstants.Logs.AD_NOT_AVAILABLE)
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, FyberConstants.Logs.AD_NOT_AVAILABLE)
            return
        }

        getDynamicUserId()?.let { userId ->
            if (userId.isNotEmpty()) {
                IronLog.INTERNAL.verbose(FyberConstants.Logs.SET_USER_ID.format(userId))
                InneractiveAdManager.setUserId(userId)
            }
        }

        mainHandler.post {
            val unitController = rewardedAd?.selectedUnitController as? InneractiveFullscreenUnitController
            unitController?.setRewardedListener(rewardedAdListener)
            unitController?.setEventsListener(rewardedAdListener)

            val videoContentController = InneractiveFullscreenVideoContentController()
            videoContentController.setEventsListener(rewardedAdListener)
            unitController?.addContentController(videoContentController)

            unitController?.show(activity)
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean = rewardedAd?.isReady == true

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            rewardedAd?.destroy()
            rewardedAd = null
            rewardedAdListener = null
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
            IronLog.INTERNAL.error(FyberConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(FyberConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion
}
