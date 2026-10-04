package com.ironsource.adapters.admob.nativead

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.AdFormat
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.ironsource.adapters.admob.AdMobAdapter
import com.ironsource.adapters.admob.AdMobConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.NativeAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdOptionsPosition
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseNativeAd
import java.lang.ref.WeakReference

class AdMobNativeAdAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseNativeAd<AdMobAdapter>(networkSettings) {

    private var nativeAd: NativeAd? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // region Adapter Methods

    override fun loadAd(adData: AdData, context: Context, listener: NativeAdListener) {
        val adUnitId = adData.getString(AdMobConstants.AD_UNIT_ID_KEY)
        IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.AD_UNIT_ID.format(adUnitId))

        if (adUnitId.isNullOrEmpty()) {
            val errorMessage = AdMobConstants.Logs.MISSING_PARAM.format(AdMobConstants.AD_UNIT_ID_KEY)
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
            IronLog.INTERNAL.error(AdMobConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                AdMobConstants.Logs.NETWORK_ADAPTER_IS_NULL
            )
            return
        }

        val adOptionsPosition = getNativeAdProperties(adData).adOptionsPosition
        val serverData = adData.serverData
        val nativeAdListener = AdMobNativeAdListener(WeakReference(this), listener)

        mainHandler.post {
            try {
                if (serverData.isNullOrEmpty()) {
                    val adRequest = networkAdapter.createNativeAdRequest(adUnitId, getAdChoicesPlacement(adOptionsPosition), adData)
                    NativeAdLoader.load(adRequest, nativeAdListener)
                } else {
                    IronLog.ADAPTER_API.verbose(AdMobConstants.Logs.LOAD_WITH_SERVER_DATA)
                    NativeAdLoader.loadFromAdResponse(serverData, nativeAdListener)
                }
            } catch (e: Exception) {
                IronLog.ADAPTER_API.error(AdMobConstants.Logs.LOAD_EXCEPTION.format(e.message))
                listener.onAdLoadFailed(
                    AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                    AdapterErrors.ADAPTER_ERROR_INTERNAL,
                    AdMobConstants.Logs.LOAD_EXCEPTION.format(e.message)
                )
            }
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            try {
                nativeAd?.destroy()
                nativeAd = null
            } catch (e: Exception) {
                IronLog.INTERNAL.error(AdMobConstants.Logs.DESTROY_EXCEPTION.format(e.message))
            }
        }
    }

    override fun collectBiddingData(adData: AdData?, context: Context, biddingDataCallback: BiddingDataCallback) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(AdMobConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(AdMobConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback, AdFormat.NATIVE, null)
    }

    // endregion

    // region Helper Methods

    internal fun setNativeAd(nativeAd: NativeAd?) {
        this.nativeAd = nativeAd
    }

    private fun getAdChoicesPlacement(adOptionsPosition: AdOptionsPosition): AdChoicesPlacement =
        when (adOptionsPosition) {
            AdOptionsPosition.TOP_LEFT -> AdChoicesPlacement.TOP_LEFT
            AdOptionsPosition.TOP_RIGHT -> AdChoicesPlacement.TOP_RIGHT
            AdOptionsPosition.BOTTOM_LEFT -> AdChoicesPlacement.BOTTOM_LEFT
            AdOptionsPosition.BOTTOM_RIGHT -> AdChoicesPlacement.BOTTOM_RIGHT
        }

    // endregion
}
