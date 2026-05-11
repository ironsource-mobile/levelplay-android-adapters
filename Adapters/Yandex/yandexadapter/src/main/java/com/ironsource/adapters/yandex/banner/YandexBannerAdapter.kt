package com.ironsource.adapters.yandex.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.yandex.YandexAdapter
import com.ironsource.adapters.yandex.YandexConstants
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
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdType
import com.yandex.mobile.ads.common.BidderTokenRequest
import java.lang.ref.WeakReference

class YandexBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<YandexAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: BannerAdView? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
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

        val appContext = activity.applicationContext
        val yandexBannerSize = getBannerSize(appContext, bannerSize)
        if (yandexBannerSize == null) {
            IronLog.INTERNAL.error(YandexConstants.Logs.UNSUPPORTED_BANNER_SIZE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                YandexConstants.Logs.UNSUPPORTED_BANNER_SIZE
            )
            return
        }

        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(appContext, yandexBannerSize.width),
            AdapterUtils.dpToPixels(appContext, yandexBannerSize.height),
            Gravity.CENTER
        )

        bannerAdView = BannerAdView(appContext).apply {
            setAdSize(yandexBannerSize)
            setBannerAdEventListener(YandexBannerListener(listener, this, layoutParams))
        }

        val adRequest: AdRequest = AdRequest.Builder(adUnitId)
            .setBiddingData(serverData)
            .setParameters(networkAdapter.getConfigParams())
            .build()

        mainHandler.post {
            bannerAdView?.loadAd(adRequest)
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            bannerAdView?.setBannerAdEventListener(null)
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
            biddingDataCallback.onFailure(YandexConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        val bannerSize = adData?.adUnitData?.get(YandexConstants.BANNER_SIZE_KEY)
        if (bannerSize !is ISBannerSize) {
            IronLog.INTERNAL.verbose(YandexConstants.Logs.BANNER_SIZE_IS_NULL)
            biddingDataCallback.onFailure(YandexConstants.Logs.BANNER_SIZE_IS_NULL)
            return
        }

        val yandexBannerSize = getBannerSize(context.applicationContext, bannerSize)
        if (yandexBannerSize == null) {
            IronLog.INTERNAL.verbose(YandexConstants.Logs.UNSUPPORTED_BANNER_SIZE)
            biddingDataCallback.onFailure(YandexConstants.Logs.UNSUPPORTED_BANNER_SIZE)
            return
        }

        val bidderTokenRequest = BidderTokenRequest.banner(yandexBannerSize, null, networkAdapter.getConfigParams())

        networkAdapter.collectBiddingData(context, biddingDataCallback, bidderTokenRequest)
    }

    // endregion

    // region Helper Methods

    private fun getBannerSize(context: Context, bannerSize: ISBannerSize): BannerAdSize? {
        return when (bannerSize.description) {
            YandexConstants.BANNER_SIZE_BANNER -> BannerAdSize.fixed(context, YandexConstants.BANNER_WIDTH, YandexConstants.BANNER_HEIGHT)
            YandexConstants.BANNER_SIZE_LARGE -> BannerAdSize.fixed(context, YandexConstants.LARGE_BANNER_WIDTH, YandexConstants.LARGE_BANNER_HEIGHT)
            YandexConstants.BANNER_SIZE_RECTANGLE -> BannerAdSize.fixed(context, YandexConstants.RECTANGLE_WIDTH, YandexConstants.RECTANGLE_HEIGHT)
            YandexConstants.BANNER_SIZE_SMART ->
                (if (AdapterUtils.isLargeScreen(context)) {
                    BannerAdSize.fixed(context, YandexConstants.LEADERBOARD_WIDTH, YandexConstants.LEADERBOARD_HEIGHT)
                } else {
                    BannerAdSize.fixed(context, YandexConstants.BANNER_WIDTH, YandexConstants.BANNER_HEIGHT)
                })
            YandexConstants.BANNER_SIZE_CUSTOM -> {
                val width = bannerSize.width
                val height = bannerSize.height
                BannerAdSize.fixed(context, width, height)
            }
            else -> {
                IronLog.INTERNAL.verbose(YandexConstants.Logs.BANNER_SIZE_NULL_LOG)
                null
            }
        }
    }

    // endregion
}
