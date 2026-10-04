package com.ironsource.adapters.facebook.interstitial

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.facebook.ads.InterstitialAd
import com.ironsource.adapters.facebook.FacebookAdapter
import com.ironsource.adapters.facebook.FacebookConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseInterstitial
import java.lang.ref.WeakReference

class FacebookInterstitialAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseInterstitial<FacebookAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var interstitialAd: InterstitialAd? = null
    private var interstitialAdListener: FacebookInterstitialListener? = null
    private var isAdAvailableFlag = false

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: InterstitialAdListener) {
        val placementId = adData.getString(FacebookConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(FacebookConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        if (placementId.isNullOrEmpty()) {
            val errorMessage = FacebookConstants.Logs.MISSING_PARAM.format(FacebookConstants.PLACEMENT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val serverData = adData.serverData
        val appContext = context.applicationContext

        setInterstitialAdAvailability(false)

        mainHandler.post {
            try {
                this.interstitialAd?.destroy()
                val interstitialAd = InterstitialAd(appContext, placementId)
                val cacheFlags = FacebookAdapter.interstitialFacebookCacheFlags
                val adListener = FacebookInterstitialListener(listener, WeakReference(this))
                interstitialAdListener = adListener
                val configBuilder = interstitialAd.buildLoadAdConfig()
                    .withCacheFlags(cacheFlags)
                    .withAdListener(adListener)
                if (!serverData.isNullOrEmpty()) {
                    configBuilder.withBid(serverData)
                }

                IronLog.ADAPTER_API.verbose(FacebookConstants.Logs.LOADING_INTERSTITIAL.format(placementId, cacheFlags))
                this.interstitialAd = interstitialAd
                interstitialAd.loadAd(configBuilder.build())
            } catch (e: Exception) {
                val errorMessage = FacebookConstants.Logs.LOAD_EXCEPTION.format(e.message)
                IronLog.INTERNAL.error(errorMessage)
                listener.onAdLoadFailed(
                    AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                    AdapterErrors.ADAPTER_ERROR_INTERNAL,
                    errorMessage
                )
            }
        }
    }

    override fun showAd(adData: AdData, activity: Activity, listener: InterstitialAdListener) {
        IronLog.ADAPTER_API.verbose()
        if (!isAdAvailable(adData)) {
            IronLog.INTERNAL.error(FacebookConstants.Logs.AD_NOT_AVAILABLE)
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, FacebookConstants.Logs.AD_NOT_AVAILABLE)
            return
        }

        mainHandler.post {
            try {
                val ad = interstitialAd
                if (ad == null || !ad.isAdLoaded || ad.isAdInvalidated) {
                    IronLog.INTERNAL.error(FacebookConstants.Logs.AD_NOT_AVAILABLE)
                    listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_AD_EXPIRED, FacebookConstants.Logs.AD_NOT_AVAILABLE)
                    return@post
                }
                setInterstitialAdAvailability(false)
                interstitialAdListener?.showAttempted = true
                ad.show()
            } catch (e: Exception) {
                val errorMessage = FacebookConstants.Logs.SHOW_EXCEPTION.format(e.message)
                IronLog.INTERNAL.error(errorMessage)
                listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
            }
        }
    }

    override fun isAdAvailable(adData: AdData): Boolean = isAdAvailableFlag

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        setInterstitialAdAvailability(false)
        mainHandler.post {
            interstitialAd?.destroy()
            interstitialAd = null
            interstitialAdListener = null
        }
    }

    override fun collectBiddingData(adData: AdData?, context: Context, biddingDataCallback: BiddingDataCallback) {
        IronLog.ADAPTER_API.verbose()
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(FacebookConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(FacebookConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }
        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    internal fun setInterstitialAdAvailability(isAvailable: Boolean) {
        isAdAvailableFlag = isAvailable
    }

    // endregion
}
