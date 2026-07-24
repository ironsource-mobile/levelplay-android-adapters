package com.ironsource.adapters.aps.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.amazon.aps.ads.ApsAdController
import com.amazon.aps.ads.ApsAdView
import com.amazon.device.ads.AdRegistration
import com.amazon.device.ads.DTBAdResponse
import com.amazon.device.ads.DTBAdSize
import com.amazon.device.ads.SDKUtilities
import com.ironsource.adapters.aps.APSAdapter
import com.ironsource.adapters.aps.APSConstants
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
import java.lang.ref.WeakReference

class APSBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<APSAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: ApsAdView? = null
    private var adResponse: DTBAdResponse? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val uuid = adData.getString(APSConstants.UUID)
        IronLog.ADAPTER_API.verbose(APSConstants.Logs.UUID_LOG.format(uuid ?: ""))

        val adResponse = this.adResponse
        if (adResponse == null) {
            IronLog.INTERNAL.error(APSConstants.Logs.AD_RESPONSE_MISSING)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                APSConstants.Logs.AD_RESPONSE_MISSING
            )
            return
        }

        val appContext = activity.applicationContext
        val layoutParams = getBannerLayoutParams(appContext, bannerSize)
        if (layoutParams == null) {
            IronLog.INTERNAL.error(APSConstants.Logs.UNSUPPORTED_BANNER_SIZE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                APSConstants.Logs.UNSUPPORTED_BANNER_SIZE
            )
            return
        }

        val apsBannerListener = APSBannerListener(listener, WeakReference(this), layoutParams)
        val bidInfo = SDKUtilities.getBidInfo(adResponse)

        mainHandler.post {
            val bannerAdController = ApsAdController(activity, apsBannerListener)
            try {
                bannerAdController.fetchBannerAd(bidInfo, bannerSize.width, bannerSize.height)
                bannerAdView = bannerAdController.apsAdView
            } catch (e: Exception) {
                val errorMessage = APSConstants.Logs.LOAD_EXCEPTION.format(e.message)
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
            bannerAdView?.destroy()
            bannerAdView = null
        }
        adResponse = null
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        IronLog.ADAPTER_API.verbose()

        if (!AdRegistration.isInitialized()) {
            IronLog.ADAPTER_API.error(APSConstants.Logs.APS_NOT_INITIALIZED)
            biddingDataCallback.onFailure(APSConstants.Logs.APS_NOT_INITIALIZED)
            return
        }

        if (adData == null) {
            IronLog.INTERNAL.error(APSConstants.Logs.MISSING_AD_DATA)
            biddingDataCallback.onFailure(APSConstants.Logs.MISSING_AD_DATA)
            return
        }

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(APSConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(APSConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        val uuid = adData.getString(APSConstants.UUID)
        if (uuid.isNullOrEmpty()) {
            val errorMessage = APSConstants.Logs.MISSING_APS_CONFIGURATION.format(APSConstants.UUID)
            IronLog.ADAPTER_API.error(errorMessage)
            biddingDataCallback.onFailure(errorMessage)
            return
        }

        val dimensions = adData.configuration?.get(APSConstants.DIMENSIONS_KEY) as? Map<*, *>
        val width = (dimensions?.get(APSConstants.DIMENSION_WIDTH_KEY) as? Number)?.toInt() ?: 0
        val height = (dimensions?.get(APSConstants.DIMENSION_HEIGHT_KEY) as? Number)?.toInt() ?: 0

        if (width <= 0 || height <= 0) {
            val errorMessage = APSConstants.Logs.INVALID_AD_SIZE.format(width, height)
            IronLog.ADAPTER_API.error(errorMessage)
            biddingDataCallback.onFailure(errorMessage)
            return
        }

        val adSize = DTBAdSize(width, height, uuid)
        networkAdapter.collectBiddingData(adSize, biddingDataCallback) { this.adResponse = it }
    }

    // endregion

    // region Helper Methods

    internal fun getBannerView(): ApsAdView? = bannerAdView

    private fun getBannerLayoutParams(context: Context, size: ISBannerSize): FrameLayout.LayoutParams? {
        val layoutParams = when (size.description) {
            APSConstants.BANNER_SIZE_BANNER -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, APSConstants.BANNER_WIDTH),
                AdapterUtils.dpToPixels(context, APSConstants.BANNER_HEIGHT)
            )
            APSConstants.BANNER_SIZE_RECTANGLE -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, APSConstants.RECTANGLE_WIDTH),
                AdapterUtils.dpToPixels(context, APSConstants.RECTANGLE_HEIGHT)
            )
            APSConstants.BANNER_SIZE_SMART ->
                if (AdapterUtils.isLargeScreen(context)) {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, APSConstants.LEADERBOARD_WIDTH),
                        AdapterUtils.dpToPixels(context, APSConstants.LEADERBOARD_HEIGHT)
                    )
                } else {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, APSConstants.BANNER_WIDTH),
                        AdapterUtils.dpToPixels(context, APSConstants.BANNER_HEIGHT)
                    )
                }
            else -> return null
        }

        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    // endregion
}
