package com.ironsource.adapters.fyber.interstitial

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.fyber.inneractive.sdk.external.InneractiveAdRequest
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController
import com.ironsource.adapters.fyber.FyberAdapter
import com.ironsource.adapters.fyber.FyberConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial

class FyberInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<FyberAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var interstitialAd: InneractiveAdSpot? = null
    private var interstitialAdListener: FyberInterstitialListener? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: InterstitialAdListener) {
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
            val fyberInterstitialListener = FyberInterstitialListener(listener)
            interstitialAdListener = fyberInterstitialListener

            interstitialAd = InneractiveAdSpotManager.get().createSpot().apply {
                setMediationName(FyberConstants.MEDIATION_NAME)
                setMediationVersion(FyberConstants.ADAPTER_VERSION)
                addUnitController(InneractiveFullscreenUnitController())
                setRequestListener(fyberInterstitialListener)
            }

            if (serverData.isNullOrEmpty()) {
                interstitialAd?.requestAd(InneractiveAdRequest(spotId))
            } else {
                interstitialAd?.loadAd(serverData)
            }
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: InterstitialAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            IronLog.INTERNAL.error(FyberConstants.Logs.AD_NOT_AVAILABLE)
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, FyberConstants.Logs.AD_NOT_AVAILABLE)
            return
        }

        mainHandler.post {
            val unitController = interstitialAd?.selectedUnitController as? InneractiveFullscreenUnitController
            unitController?.setEventsListener(interstitialAdListener)
            unitController?.addContentController(InneractiveFullscreenVideoContentController())
            unitController?.show(activity)
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean = interstitialAd?.isReady == true

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            interstitialAd?.destroy()
            interstitialAd = null
            interstitialAdListener = null
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
