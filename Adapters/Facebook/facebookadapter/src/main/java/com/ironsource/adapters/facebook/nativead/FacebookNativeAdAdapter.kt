package com.ironsource.adapters.facebook.nativead

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.facebook.ads.NativeAd
import com.ironsource.adapters.facebook.FacebookAdapter
import com.ironsource.adapters.facebook.FacebookConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.NativeAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseNativeAd

class FacebookNativeAdAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseNativeAd<FacebookAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var nativeAd: NativeAd? = null

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: NativeAdListener) {
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

        val adOptionsPosition = getNativeAdProperties(adData).adOptionsPosition
        val appContext = context.applicationContext
        val serverData = adData.serverData
        mainHandler.post {
            try {
                val nativeAd = NativeAd(appContext, placementId)
                val configBuilder = nativeAd.buildLoadAdConfig()
                    .withAdListener(FacebookNativeAdListener(appContext, adOptionsPosition, listener))
                if (!serverData.isNullOrEmpty()) {
                    configBuilder.withBid(serverData)
                }
                this.nativeAd = nativeAd
                nativeAd.loadAd(configBuilder.build())
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

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            nativeAd?.destroy()
            nativeAd = null
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
            IronLog.INTERNAL.error(FacebookConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(FacebookConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }
        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion
}
