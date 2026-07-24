package com.ironsource.adapters.smaato.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.smaato.SmaatoAdapter
import com.ironsource.adapters.smaato.SmaatoConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.smaato.sdk.banner.ad.AutoReloadInterval
import com.smaato.sdk.banner.ad.BannerAdSize
import com.smaato.sdk.banner.widget.BannerView
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseBanner

class SmaatoBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<SmaatoAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: BannerView? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val adSpaceId = adData.getString(SmaatoConstants.AD_SPACE_ID_KEY)
        IronLog.ADAPTER_API.verbose(SmaatoConstants.Logs.AD_SPACE_ID.format(adSpaceId ?: ""))

        if (adSpaceId.isNullOrEmpty()) {
            val errorMessage = SmaatoConstants.Logs.MISSING_PARAM.format(SmaatoConstants.AD_SPACE_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = SmaatoConstants.Logs.MISSING_PARAM.format(SmaatoConstants.SERVER_DATA)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val context = activity.applicationContext
        val smaatoBannerSize = getBannerSize(context, bannerSize)
        if (smaatoBannerSize == null) {
            IronLog.INTERNAL.error(SmaatoConstants.Logs.UNSUPPORTED_BANNER_SIZE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                SmaatoConstants.Logs.UNSUPPORTED_BANNER_SIZE
            )
            return
        }

        val adRequestParams = SmaatoAdapter.getAdRequest(serverData)
        if (adRequestParams == null) {
            IronLog.INTERNAL.error(SmaatoConstants.Logs.INVALID_AD_REQUEST)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                SmaatoConstants.Logs.INVALID_AD_REQUEST
            )
            return
        }

        val layoutParams = getBannerLayoutParams(context, smaatoBannerSize)

        val bannerView = BannerView(context).apply {
            setEventListener(SmaatoBannerListener(listener, layoutParams))
            autoReloadInterval = AutoReloadInterval.DISABLED
        }
        bannerAdView = bannerView

        mainHandler.post {
            bannerView.loadAd(adSpaceId, smaatoBannerSize, adRequestParams)
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            bannerAdView?.setEventListener(null)
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
            IronLog.INTERNAL.error(SmaatoConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(SmaatoConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    private fun getBannerSize(context: Context, bannerSize: ISBannerSize): BannerAdSize? {
        return when (bannerSize.description) {
            SmaatoConstants.BANNER_SIZE_BANNER -> BannerAdSize.XX_LARGE_320x50
            SmaatoConstants.BANNER_SIZE_RECTANGLE -> BannerAdSize.MEDIUM_RECTANGLE_300x250
            SmaatoConstants.BANNER_SIZE_SMART ->
                if (AdapterUtils.isLargeScreen(context)) {
                    BannerAdSize.LEADERBOARD_728x90
                } else {
                    BannerAdSize.XX_LARGE_320x50
                }
            else -> null
        }
    }

    private fun getBannerLayoutParams(context: Context, size: BannerAdSize): FrameLayout.LayoutParams {
        val layoutParams = when (size) {
            BannerAdSize.MEDIUM_RECTANGLE_300x250 -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, SmaatoConstants.RECTANGLE_WIDTH),
                AdapterUtils.dpToPixels(context, SmaatoConstants.RECTANGLE_HEIGHT)
            )
            BannerAdSize.LEADERBOARD_728x90 -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, SmaatoConstants.LEADERBOARD_WIDTH),
                AdapterUtils.dpToPixels(context, SmaatoConstants.LEADERBOARD_HEIGHT)
            )
            else -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, SmaatoConstants.BANNER_WIDTH),
                AdapterUtils.dpToPixels(context, SmaatoConstants.BANNER_HEIGHT)
            )
        }
        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    // endregion
}
