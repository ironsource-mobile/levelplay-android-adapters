package com.ironsource.adapters.chartboost.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.chartboost.sdk.ads.Banner
import com.ironsource.adapters.chartboost.ChartboostAdapter
import com.ironsource.adapters.chartboost.ChartboostConstants
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

class ChartboostBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<ChartboostAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    internal var bannerAdView: Banner? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
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

        val context = activity.applicationContext
        val chartboostBannerSize = getBannerSize(context, bannerSize)
        if (chartboostBannerSize == null) {
            val errorMessage = ChartboostConstants.Logs.UNSUPPORTED_BANNER_SIZE.format(bannerSize.description)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                errorMessage
            )
            return
        }

        val layoutParams = getBannerLayoutParams(context, bannerSize)
        val serverData = adData.serverData

        mainHandler.post {
            val bannerListener = ChartboostBannerListener(listener, layoutParams, WeakReference(this))
            val bannerView = Banner(
                context,
                locationId,
                chartboostBannerSize,
                bannerListener,
                ChartboostAdapter.mediation
            ).apply { setLayoutParams(layoutParams) }
            bannerAdView = bannerView

            if (serverData.isNullOrEmpty()) {
                bannerView.cache()
            } else {
                bannerView.cache(serverData)
            }
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()

        mainHandler.post {
            bannerAdView?.detach()
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
            IronLog.INTERNAL.error(ChartboostConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(ChartboostConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    private fun getBannerSize(context: Context, size: ISBannerSize): Banner.BannerSize? =
        when (size.description) {
            ChartboostConstants.BANNER_SIZE_BANNER,
            ChartboostConstants.BANNER_SIZE_LARGE -> Banner.BannerSize.STANDARD
            ChartboostConstants.BANNER_SIZE_RECTANGLE -> Banner.BannerSize.MEDIUM
            ChartboostConstants.BANNER_SIZE_SMART -> if (AdapterUtils.isLargeScreen(context)) {
                Banner.BannerSize.LEADERBOARD
            } else {
                Banner.BannerSize.STANDARD
            }
            ChartboostConstants.BANNER_SIZE_CUSTOM -> if (isSupportedCustomHeight(size.height)) {
                Banner.BannerSize.STANDARD
            } else {
                null
            }
            else -> null
        }

    private fun getBannerLayoutParams(context: Context, size: ISBannerSize): FrameLayout.LayoutParams {
        val layoutParams = when (size.description) {
            ChartboostConstants.BANNER_SIZE_BANNER,
            ChartboostConstants.BANNER_SIZE_LARGE -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, ChartboostConstants.BANNER_WIDTH),
                AdapterUtils.dpToPixels(context, ChartboostConstants.BANNER_HEIGHT)
            )
            ChartboostConstants.BANNER_SIZE_RECTANGLE -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, ChartboostConstants.RECTANGLE_WIDTH),
                AdapterUtils.dpToPixels(context, ChartboostConstants.RECTANGLE_HEIGHT)
            )
            ChartboostConstants.BANNER_SIZE_SMART -> if (AdapterUtils.isLargeScreen(context)) {
                FrameLayout.LayoutParams(
                    AdapterUtils.dpToPixels(context, ChartboostConstants.LEADERBOARD_WIDTH),
                    AdapterUtils.dpToPixels(context, ChartboostConstants.LEADERBOARD_HEIGHT)
                )
            } else {
                FrameLayout.LayoutParams(
                    AdapterUtils.dpToPixels(context, ChartboostConstants.BANNER_WIDTH),
                    AdapterUtils.dpToPixels(context, ChartboostConstants.BANNER_HEIGHT)
                )
            }
            ChartboostConstants.BANNER_SIZE_CUSTOM -> if (isSupportedCustomHeight(size.height)) {
                FrameLayout.LayoutParams(
                    AdapterUtils.dpToPixels(context, ChartboostConstants.BANNER_WIDTH),
                    AdapterUtils.dpToPixels(context, ChartboostConstants.BANNER_HEIGHT)
                )
            } else {
                FrameLayout.LayoutParams(0, 0)
            }
            else -> FrameLayout.LayoutParams(0, 0)
        }

        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    private fun isSupportedCustomHeight(height: Int): Boolean =
        height in ChartboostConstants.CUSTOM_BANNER_MIN_HEIGHT..ChartboostConstants.CUSTOM_BANNER_MAX_HEIGHT

    // endregion
}
