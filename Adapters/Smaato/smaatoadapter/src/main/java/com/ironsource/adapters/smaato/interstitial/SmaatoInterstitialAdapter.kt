package com.ironsource.adapters.smaato.interstitial

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ironsource.adapters.smaato.SmaatoAdapter
import com.ironsource.adapters.smaato.SmaatoConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.smaato.sdk.interstitial.Interstitial
import com.smaato.sdk.interstitial.InterstitialAd
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial
import java.lang.ref.WeakReference

class SmaatoInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<SmaatoAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var interstitialAd: InterstitialAd? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: InterstitialAdListener) {
        val adSpaceId = adData.getString(SmaatoConstants.AD_SPACE_ID_KEY)
        IronLog.ADAPTER_API.verbose(SmaatoConstants.Logs.AD_SPACE_ID.format(adSpaceId ?: ""))

        if (adSpaceId.isNullOrEmpty()) {
            val errorMessage = SmaatoConstants.Logs.MISSING_PARAM.format(SmaatoConstants.AD_SPACE_ID_KEY)
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
            val errorMessage = SmaatoConstants.Logs.MISSING_PARAM.format(SmaatoConstants.SERVER_DATA)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val adRequestParams = SmaatoAdapter.getAdRequest(serverData)
        if (adRequestParams == null) {
            IronLog.INTERNAL.error(SmaatoConstants.Logs.INVALID_AD_REQUEST)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                SmaatoConstants.Logs.INVALID_AD_REQUEST
            )
            return
        }

        val interstitialListener = SmaatoInterstitialListener(listener, WeakReference(this))

        mainHandler.post {
            Interstitial.loadAd(adSpaceId, interstitialListener, adRequestParams)
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: InterstitialAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                SmaatoConstants.Logs.AD_NOT_AVAILABLE
            )
            return
        }

        mainHandler.post {
            interstitialAd?.showAd(activity)
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean =
        interstitialAd?.isAvailableForPresentation == true

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
            IronLog.INTERNAL.error(SmaatoConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(SmaatoConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    internal fun setInterstitialAd(ad: InterstitialAd) {
        interstitialAd = ad
    }

    // endregion
}
