package com.ironsource.adapters.mobilefuse.interstitial

import android.app.Activity
import android.content.Context
import com.ironsource.adapters.mobilefuse.MobileFuseAdapter
import com.ironsource.adapters.mobilefuse.MobileFuseConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.mobilefuse.sdk.MobileFuseInterstitialAd
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial

class MobileFuseInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<MobileFuseAdapter>(networkSettings) {

    private var interstitialAd: MobileFuseInterstitialAd? = null

    // region LevelPlay Interstitial API

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: InterstitialAdListener
    ) {
        val placementId = adData.getString(MobileFuseConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MobileFuseConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        if (placementId.isNullOrEmpty()) {
            val errorMessage = MobileFuseConstants.Logs.MISSING_PARAM.format(MobileFuseConstants.PLACEMENT_ID_KEY)
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
            val errorMessage = MobileFuseConstants.Logs.MISSING_PARAM.format(MobileFuseConstants.SERVER_DATA)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        interstitialAd = MobileFuseInterstitialAd(context.applicationContext, placementId)
        interstitialAd?.setListener(MobileFuseInterstitialListener(listener))
        interstitialAd?.loadAdFromBiddingToken(serverData)
    }

    override fun showAd(
        adData: AdData,
        activity: Activity,
        listener: InterstitialAdListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                MobileFuseConstants.AD_NOT_AVAILABLE
            )
            return
        }

        interstitialAd?.showAd()
    }

    override fun isAdAvailable(adData: AdData): Boolean = interstitialAd?.isLoaded == true

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        interstitialAd?.setListener(null)
        interstitialAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(MobileFuseConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(MobileFuseConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion
}
