package com.ironsource.adapters.chartboost.interstitial

import android.app.Activity
import android.content.Context
import com.chartboost.sdk.ads.Interstitial
import com.ironsource.adapters.chartboost.ChartboostAdapter
import com.ironsource.adapters.chartboost.ChartboostConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial

class ChartboostInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<ChartboostAdapter>(networkSettings) {

    private var interstitialAd: Interstitial? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        context: Context,
        listener: InterstitialAdListener
    ) {
        val locationId = adData.getString(ChartboostConstants.AD_LOCATION_KEY)
        IronLog.ADAPTER_API.verbose(ChartboostConstants.Logs.AD_LOCATION.format(locationId ?: ""))

        if (locationId.isNullOrEmpty()) {
            val errorMessage = ChartboostConstants.Logs.MISSING_PARAM.format(ChartboostConstants.AD_LOCATION_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val serverData = adData.serverData
        interstitialAd = Interstitial(
            locationId,
            ChartboostInterstitialListener(listener),
            ChartboostAdapter.mediation
        )

        if (serverData.isNullOrEmpty()) {
            interstitialAd?.cache()
        } else {
            interstitialAd?.cache(serverData)
        }
    }

    override fun showAd(
        adData: AdData,
        activity: Activity,
        listener: InterstitialAdListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            IronLog.INTERNAL.error(ChartboostConstants.Logs.AD_NOT_AVAILABLE)
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                ChartboostConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        interstitialAd?.show()
    }

    @Suppress("DEPRECATION")
    override fun isAdAvailable(adData: AdData): Boolean =
        interstitialAd?.isCached() == true

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        interstitialAd = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(ChartboostConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(ChartboostConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion
}
