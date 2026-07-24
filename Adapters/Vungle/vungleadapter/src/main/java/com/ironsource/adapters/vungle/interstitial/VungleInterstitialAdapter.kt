package com.ironsource.adapters.vungle.interstitial

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ironsource.adapters.vungle.VungleAdapter
import com.ironsource.adapters.vungle.VungleConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial
import com.vungle.ads.AdConfig
import com.vungle.ads.InterstitialAd
import com.vungle.ads.VungleMediationLogger

class VungleInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<VungleAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var interstitialAd: InterstitialAd? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: InterstitialAdListener) {
        val placementId = adData.getString(VungleConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(VungleConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        if (placementId.isNullOrEmpty()) {
            val errorMessage = VungleConstants.Logs.MISSING_PARAM.format(VungleConstants.PLACEMENT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            VungleMediationLogger.logError(null, VungleConstants.Logs.NO_PLACEMENT_ID.format("Interstitial"))
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        interstitialAd = InterstitialAd(context.applicationContext, placementId, AdConfig()).apply {
            adListener = VungleInterstitialListener(listener)
            adapterAdFormat = VungleConstants.ADAPTER_FORMAT_INTERSTITIAL
        }
        interstitialAd?.load(adData.serverData)
    }

    override fun showAd(adData: AdData, activity: Activity, listener: InterstitialAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            IronLog.INTERNAL.error(VungleConstants.Logs.AD_NOT_AVAILABLE)
            VungleMediationLogger.logError(interstitialAd, VungleConstants.Logs.NO_ADS_TO_SHOW.format("Interstitial"))
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, VungleConstants.Logs.AD_NOT_AVAILABLE)
            return
        }

        mainHandler.post {
            interstitialAd?.play()
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        return interstitialAd?.canPlayAd() == true
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        interstitialAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(VungleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(VungleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion
}
