package com.ironsource.adapters.hyprmx.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.hyprmx.android.sdk.banner.HyprMXBannerSize
import com.hyprmx.android.sdk.banner.HyprMXBannerView
import com.ironsource.adapters.hyprmx.HyprMXAdapter
import com.ironsource.adapters.hyprmx.HyprMXConstants
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

class HyprMXBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<HyprMXAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: HyprMXBannerView? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val propertyId = adData.getString(HyprMXConstants.PROPERTY_ID_KEY)
        IronLog.ADAPTER_API.verbose(HyprMXConstants.Logs.PROPERTY_ID.format(propertyId ?: ""))

        if (propertyId.isNullOrEmpty()) {
            val errorMessage = HyprMXConstants.Logs.MISSING_PARAM.format(HyprMXConstants.PROPERTY_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        if (!isBannerSizeSupported(bannerSize)) {
            val errorMessage = HyprMXConstants.Logs.UNSUPPORTED_BANNER_SIZE.format(bannerSize.description)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                errorMessage
            )
            return
        }

        val context = activity.applicationContext
        val layoutParams = createBannerLayoutParams(context, bannerSize)
        val hyprMXBannerSize = createBannerSize(context, bannerSize)
        val serverData = adData.serverData

        mainHandler.post {
            val bannerView = HyprMXBannerView(context, null).apply {
                placementName = propertyId
                adSize = hyprMXBannerSize
            }
            bannerView.listener = HyprMXBannerListener(listener)
            bannerAdView = bannerView

            val onResult: (Boolean) -> Unit = { isAdAvailable ->
                if (isAdAvailable) {
                    listener.onAdLoadSuccess(bannerView, layoutParams)
                } else {
                    listener.onAdLoadFailed(
                        AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL,
                        AdapterErrors.ADAPTER_ERROR_INTERNAL,
                        HyprMXConstants.Logs.AD_NOT_AVAILABLE
                    )
                }
            }

            if (serverData.isNullOrEmpty()) {
                bannerView.loadAd(onResult)
            } else {
                bannerView.loadAd(serverData, onResult)
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
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(HyprMXConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(HyprMXConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    private fun isBannerSizeSupported(size: ISBannerSize): Boolean =
        when (size.description) {
            HyprMXConstants.BANNER_SIZE_BANNER,
            HyprMXConstants.BANNER_SIZE_RECTANGLE,
            HyprMXConstants.BANNER_SIZE_SMART -> true
            else -> false
        }

    private fun createBannerSize(context: Context, size: ISBannerSize): HyprMXBannerSize =
        when (size.description) {
            HyprMXConstants.BANNER_SIZE_BANNER -> HyprMXBannerSize.HyprMXAdSizeBanner
            HyprMXConstants.BANNER_SIZE_RECTANGLE -> HyprMXBannerSize.HyprMXAdSizeMediumRectangle
            HyprMXConstants.BANNER_SIZE_SMART -> {
                if (AdapterUtils.isLargeScreen(context)) {
                    HyprMXBannerSize.HyprMXAdSizeLeaderboard
                } else {
                    HyprMXBannerSize.HyprMXAdSizeBanner
                }
            }
            else -> HyprMXBannerSize.HyprMXAdSizeBanner
        }

    private fun createBannerLayoutParams(context: Context, size: ISBannerSize): FrameLayout.LayoutParams {
        val layoutParams = when (size.description) {
            HyprMXConstants.BANNER_SIZE_BANNER -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, HyprMXConstants.BANNER_WIDTH),
                AdapterUtils.dpToPixels(context, HyprMXConstants.BANNER_HEIGHT)
            )
            HyprMXConstants.BANNER_SIZE_RECTANGLE -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, HyprMXConstants.RECTANGLE_WIDTH),
                AdapterUtils.dpToPixels(context, HyprMXConstants.RECTANGLE_HEIGHT)
            )
            HyprMXConstants.BANNER_SIZE_SMART -> if (AdapterUtils.isLargeScreen(context)) {
                FrameLayout.LayoutParams(
                    AdapterUtils.dpToPixels(context, HyprMXConstants.LEADERBOARD_WIDTH),
                    AdapterUtils.dpToPixels(context, HyprMXConstants.LEADERBOARD_HEIGHT)
                )
            } else {
                FrameLayout.LayoutParams(
                    AdapterUtils.dpToPixels(context, HyprMXConstants.BANNER_WIDTH),
                    AdapterUtils.dpToPixels(context, HyprMXConstants.BANNER_HEIGHT)
                )
            }
            else -> FrameLayout.LayoutParams(0, 0)
        }

        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    // endregion
}
