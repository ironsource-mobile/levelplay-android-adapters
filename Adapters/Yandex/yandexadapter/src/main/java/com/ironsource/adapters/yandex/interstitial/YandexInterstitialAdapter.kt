package com.ironsource.adapters.yandex.interstitial

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ironsource.adapters.yandex.YandexAdapter
import com.ironsource.adapters.yandex.YandexConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdType
import com.yandex.mobile.ads.common.BidderTokenRequest
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import java.lang.ref.WeakReference

class YandexInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<YandexAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var interstitialAdListener: YandexInterstitialListener? = null
    private var interstitialAd: InterstitialAd? = null
    private var isAdAvailableFlag = false

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: InterstitialAdListener) {
        val adUnitId = adData.getString(YandexConstants.AD_UNIT_ID_KEY)
        IronLog.ADAPTER_API.verbose(YandexConstants.Logs.AD_UNIT_ID.format(adUnitId ?: ""))

        if (adUnitId.isNullOrEmpty()) {
            val errorMessage = YandexConstants.Logs.MISSING_PARAM.format(YandexConstants.AD_UNIT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(YandexConstants.Logs.ADAPTER_UNAVAILABLE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                YandexConstants.Logs.ADAPTER_UNAVAILABLE
            )
            return
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = YandexConstants.Logs.MISSING_PARAM.format(YandexConstants.SERVER_DATA)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        setInterstitialAdAvailability(false)

        interstitialAdListener = YandexInterstitialListener(listener, WeakReference(this))

        val interstitialLoader = InterstitialAdLoader(context.applicationContext)

        val adRequest: AdRequest = AdRequest.Builder(adUnitId)
            .setBiddingData(serverData)
            .setParameters(networkAdapter.getConfigParams())
            .build()

        mainHandler.post {
            interstitialLoader.loadAd(adRequest, interstitialAdListener!!)
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: InterstitialAdListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isAdAvailable(adData)) {
            listener.onAdShowFailed(
                AdapterErrors.ADAPTER_ERROR_AD_EXPIRED,
                YandexConstants.Logs.AD_NOT_AVAILABLE
            )
        } else {
            mainHandler.post {
                interstitialAd?.apply {
                    setAdEventListener(interstitialAdListener)
                    show(activity)
                }
            }
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean {
        return interstitialAd != null && isAdAvailableFlag
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        interstitialAd?.setAdEventListener(null)
        interstitialAd = null
        interstitialAdListener = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            biddingDataCallback.onFailure(YandexConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        val bidderTokenRequest = BidderTokenRequest.interstitial(null, networkAdapter.getConfigParams())

        networkAdapter.collectBiddingData(context, biddingDataCallback, bidderTokenRequest)
    }

    // endregion

    // region Helper Methods

    internal fun setInterstitialAdAvailability(isAvailable: Boolean) {
        isAdAvailableFlag = isAvailable
    }

    internal fun setInterstitialAd(ad: InterstitialAd) {
        interstitialAd = ad
    }

    // endregion
}
