package com.ironsource.adapters.facebook.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.facebook.ads.AdSize
import com.facebook.ads.AdView
import com.ironsource.adapters.facebook.FacebookAdapter
import com.ironsource.adapters.facebook.FacebookConstants
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

class FacebookBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<FacebookAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: AdView? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
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

        val context = activity.applicationContext
        val facebookAdSize = calculateBannerSize(bannerSize, context)
        if (facebookAdSize == null) {
            val errorMessage = FacebookConstants.Logs.UNSUPPORTED_BANNER_SIZE.format(bannerSize.description)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                errorMessage
            )
            return
        }

        val serverData = adData.serverData
        mainHandler.post {
            try {
                val bannerAdView = AdView(context, placementId, facebookAdSize)
                val layoutParams = calcLayoutParams(bannerSize, context)
                val configBuilder = bannerAdView.buildLoadAdConfig()
                    .withAdListener(FacebookBannerListener(listener, bannerAdView, layoutParams))
                if (!serverData.isNullOrEmpty()) {
                    configBuilder.withBid(serverData)
                }
                this.bannerAdView = bannerAdView
                bannerAdView.loadAd(configBuilder.build())
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
            IronLog.INTERNAL.error(FacebookConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(FacebookConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }
        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    private fun calculateBannerSize(bannerSize: ISBannerSize, context: Context): AdSize? {
        return when (bannerSize.description) {
            FacebookConstants.BANNER_SIZE_BANNER -> AdSize.BANNER_HEIGHT_50
            FacebookConstants.BANNER_SIZE_LARGE -> AdSize.BANNER_HEIGHT_90
            FacebookConstants.BANNER_SIZE_RECTANGLE -> AdSize.RECTANGLE_HEIGHT_250
            FacebookConstants.BANNER_SIZE_SMART ->
                if (AdapterUtils.isLargeScreen(context)) AdSize.BANNER_HEIGHT_90 else AdSize.BANNER_HEIGHT_50

            FacebookConstants.BANNER_SIZE_CUSTOM -> when (bannerSize.height) {
                FacebookConstants.BANNER_HEIGHT -> AdSize.BANNER_HEIGHT_50
                FacebookConstants.LARGE_HEIGHT -> AdSize.BANNER_HEIGHT_90
                FacebookConstants.RECTANGLE_HEIGHT -> AdSize.RECTANGLE_HEIGHT_250
                else -> null
            }

            else -> null
        }
    }

    private fun calcLayoutParams(bannerSize: ISBannerSize, context: Context): FrameLayout.LayoutParams {
        var widthDp = FacebookConstants.BANNER_WIDTH
        if (bannerSize.description == FacebookConstants.BANNER_SIZE_RECTANGLE) {
            widthDp = FacebookConstants.RECTANGLE_WIDTH
        } else if (bannerSize.description == FacebookConstants.BANNER_SIZE_SMART && AdapterUtils.isLargeScreen(context)) {
            widthDp = FacebookConstants.LARGE_WIDTH
        }

        val widthPixel = AdapterUtils.dpToPixels(context, widthDp)
        val layoutParams = FrameLayout.LayoutParams(widthPixel, FrameLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    // endregion
}
