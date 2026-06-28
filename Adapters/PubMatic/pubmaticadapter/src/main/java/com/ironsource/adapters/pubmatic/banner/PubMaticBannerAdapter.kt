package com.ironsource.adapters.pubmatic.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ironsource.adapters.pubmatic.PubMaticAdapter
import com.ironsource.adapters.pubmatic.PubMaticConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseBanner
import com.pubmatic.sdk.common.POBAdFormat
import com.pubmatic.sdk.common.POBAdSize
import com.pubmatic.sdk.openwrap.banner.POBBannerView

class PubMaticBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<PubMaticAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: POBBannerView? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
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

        val appContext = activity.applicationContext
        val adSize = getBannerSize(appContext, bannerSize)
        if (adSize == null) {
            IronLog.INTERNAL.error(PubMaticConstants.Logs.UNSUPPORTED_BANNER_SIZE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                PubMaticConstants.Logs.UNSUPPORTED_BANNER_SIZE
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

        bannerAdView = POBBannerView(appContext).apply {
            setListener(PubMaticBannerListener(listener, this))
        }

        mainHandler.post {
            bannerAdView?.loadAd(serverData, PubMaticAdapter.BIDDING_HOST)
            bannerAdView?.pauseAutoRefresh()
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            bannerAdView?.destroy()
            bannerAdView = null
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

        val bannerSize = adData?.adUnitData?.get(PubMaticConstants.BANNER_SIZE_KEY)
        val pubMaticBannerSize = (bannerSize as? ISBannerSize)?.let { getBannerSize(context.applicationContext, it) }
        if (pubMaticBannerSize == null) {
            IronLog.INTERNAL.verbose(PubMaticConstants.Logs.UNSUPPORTED_BANNER_SIZE)
            biddingDataCallback.onFailure("${PubMaticConstants.Logs.UNSUPPORTED_BANNER_SIZE} - ${PubMaticConstants.NETWORK_NAME}")
            return
        }

        val adFormat = when (pubMaticBannerSize) {
            POBAdSize.BANNER_SIZE_300x250 -> POBAdFormat.MREC
            else -> POBAdFormat.BANNER
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback, adFormat)
    }

    // endregion

    // region Helper Methods

    private fun getBannerSize(context: Context, bannerSize: ISBannerSize): POBAdSize? {
        return when (bannerSize.description) {
            PubMaticConstants.BANNER_SIZE_BANNER -> POBAdSize.BANNER_SIZE_320x50
            PubMaticConstants.BANNER_SIZE_LARGE -> POBAdSize.BANNER_SIZE_320x100
            PubMaticConstants.BANNER_SIZE_RECTANGLE -> POBAdSize.BANNER_SIZE_300x250
            PubMaticConstants.BANNER_SIZE_SMART ->
                if (AdapterUtils.isLargeScreen(context)) {
                    POBAdSize.BANNER_SIZE_728x90
                } else {
                    POBAdSize.BANNER_SIZE_320x50
                }
            else -> {
                IronLog.INTERNAL.verbose(PubMaticConstants.Logs.BANNER_SIZE_NULL)
                null
            }
        }
    }

    // endregion
}
