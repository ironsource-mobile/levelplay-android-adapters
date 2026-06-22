package com.ironsource.adapters.applovin.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.applovin.adview.AppLovinAdView
import com.applovin.sdk.AppLovinAdSize
import com.ironsource.adapters.applovin.AppLovinAdapter
import com.ironsource.adapters.applovin.AppLovinConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseBanner

class AppLovinBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<AppLovinAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: AppLovinAdView? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val zoneId = adData.getString(AppLovinConstants.ZONE_ID_KEY)
        IronLog.ADAPTER_API.verbose(AppLovinConstants.Logs.ZONE_ID.format(zoneId ?: ""))

        if (zoneId.isNullOrEmpty()) {
            val errorMessage = AppLovinConstants.Logs.MISSING_PARAM.format(AppLovinConstants.ZONE_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val appLovinSdk = AppLovinAdapter.appLovinSdk
        if (appLovinSdk == null) {
            IronLog.INTERNAL.error(AppLovinConstants.Logs.SDK_NOT_INITIALIZED)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                AppLovinConstants.Logs.SDK_NOT_INITIALIZED
            )
            return
        }

        val appContext = activity.applicationContext
        val appLovinBannerSize = getBannerSize(appContext, bannerSize)
        if (appLovinBannerSize == null) {
            IronLog.INTERNAL.error(AppLovinConstants.Logs.UNSUPPORTED_BANNER_SIZE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                AppLovinConstants.Logs.UNSUPPORTED_BANNER_SIZE
            )
            return
        }
        val layoutParams = getBannerLayoutParams(appContext, bannerSize)

        mainHandler.post {
            val adView = AppLovinAdView(appLovinBannerSize)
            val bannerListener = AppLovinBannerListener(listener, adView, layoutParams)
            adView.setAdDisplayListener(bannerListener)
            adView.setAdClickListener(bannerListener)
            adView.setAdViewEventListener(bannerListener)

            bannerAdView = adView
            appLovinSdk.adService.loadNextAdForZoneId(zoneId, bannerListener)
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            bannerAdView?.destroy()
            bannerAdView = null
        }
    }

    // endregion

    // region Helper Methods

    private fun getBannerSize(context: Context, bannerSize: ISBannerSize): AppLovinAdSize? {
        return when (bannerSize.description) {
            AppLovinConstants.BANNER_SIZE_BANNER,
            AppLovinConstants.BANNER_SIZE_LARGE -> AppLovinAdSize.BANNER

            AppLovinConstants.BANNER_SIZE_RECTANGLE -> AppLovinAdSize.MREC

            AppLovinConstants.BANNER_SIZE_SMART ->
                if (AdapterUtils.isLargeScreen(context)) AppLovinAdSize.LEADER else AppLovinAdSize.BANNER

            AppLovinConstants.BANNER_SIZE_CUSTOM ->
                if (bannerSize.height in AppLovinConstants.CUSTOM_BANNER_MIN_HEIGHT..AppLovinConstants.CUSTOM_BANNER_MAX_HEIGHT) {
                    AppLovinAdSize.BANNER
                } else {
                    null
                }

            else -> null
        }
    }

    private fun getBannerLayoutParams(context: Context, bannerSize: ISBannerSize): FrameLayout.LayoutParams {
        val layoutParams = when (bannerSize.description) {
            AppLovinConstants.BANNER_SIZE_BANNER,
            AppLovinConstants.BANNER_SIZE_LARGE -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, AppLovinConstants.BANNER_WIDTH),
                AdapterUtils.dpToPixels(context, AppLovinConstants.BANNER_HEIGHT)
            )

            AppLovinConstants.BANNER_SIZE_RECTANGLE -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, AppLovinConstants.RECTANGLE_WIDTH),
                AdapterUtils.dpToPixels(context, AppLovinConstants.RECTANGLE_HEIGHT)
            )

            AppLovinConstants.BANNER_SIZE_SMART ->
                if (AdapterUtils.isLargeScreen(context)) {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, AppLovinConstants.LARGE_WIDTH),
                        AdapterUtils.dpToPixels(context, AppLovinConstants.LARGE_HEIGHT)
                    )
                } else {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, AppLovinConstants.BANNER_WIDTH),
                        AdapterUtils.dpToPixels(context, AppLovinConstants.BANNER_HEIGHT)
                    )
                }

            AppLovinConstants.BANNER_SIZE_CUSTOM ->
                if (bannerSize.height in AppLovinConstants.CUSTOM_BANNER_MIN_HEIGHT..AppLovinConstants.CUSTOM_BANNER_MAX_HEIGHT) {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, AppLovinConstants.BANNER_WIDTH),
                        AdapterUtils.dpToPixels(context, AppLovinConstants.BANNER_HEIGHT)
                    )
                } else {
                    FrameLayout.LayoutParams(0, 0)
                }

            else -> FrameLayout.LayoutParams(0, 0)
        }

        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    // endregion
}
