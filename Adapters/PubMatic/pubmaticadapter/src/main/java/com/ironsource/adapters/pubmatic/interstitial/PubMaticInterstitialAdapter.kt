package com.ironsource.adapters.pubmatic.interstitial

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ironsource.adapters.pubmatic.PubMaticAdapter
import com.ironsource.adapters.pubmatic.PubMaticConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial
import com.pubmatic.sdk.common.POBAdFormat
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial

class PubMaticInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<PubMaticAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var interstitialAd: POBInterstitial? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: InterstitialAdListener) {
        val adUnitId = adData.getString(PubMaticConstants.AD_UNIT_ID_KEY)
        IronLog.ADAPTER_API.verbose(PubMaticConstants.Logs.AD_UNIT_ID.format(adUnitId ?: ""))

        if (adUnitId.isNullOrEmpty()) {
            val errorMessage = PubMaticConstants.Logs.MISSING_PARAM.format(PubMaticConstants.AD_UNIT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = PubMaticConstants.Logs.SERVER_DATA_IS_NULL
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        interstitialAd = POBInterstitial(context.applicationContext).apply {
            setListener(PubMaticInterstitialListener(listener))
        }

        mainHandler.post {
            interstitialAd?.loadAd(serverData, PubMaticAdapter.BIDDING_HOST)
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: InterstitialAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, PubMaticConstants.Logs.AD_NOT_AVAILABLE)
            return
        }

        mainHandler.post {
            interstitialAd?.show() ?: run {
                listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, PubMaticConstants.Logs.AD_IS_NULL)
            }
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean = interstitialAd?.isReady == true

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            interstitialAd?.destroy()
            interstitialAd = null
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
            IronLog.INTERNAL.error(PubMaticConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(PubMaticConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback, POBAdFormat.INTERSTITIAL)
    }

    // endregion
}
