package com.ironsource.adapters.inmobi.banner

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.inmobi.ads.InMobiBanner
import com.ironsource.adapters.inmobi.InMobiAdapter
import com.ironsource.adapters.inmobi.InMobiConstants
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

class InMobiBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<InMobiAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: InMobiBanner? = null

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        // Fetch and validate placementId
        val placementId = adData.getString(InMobiConstants.PLACEMENT_ID_KEY)

        IronLog.ADAPTER_API.verbose(InMobiConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        if (adData.serverData.isNullOrEmpty()) {
            val errorMessage = InMobiConstants.Logs.MISSING_PARAM.format(InMobiConstants.SERVER_DATA_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        // Get banner size
        val dpSize = getBannerSize(bannerSize, AdapterUtils.isLargeScreen(activity.applicationContext))
        if (dpSize == null) {
            val errorMessage = InMobiConstants.Logs.UNSUPPORTED_BANNER_SIZE
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                errorMessage
            )
            return
        }

        // Build layoutParams
        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(activity.applicationContext, dpSize.width),
            AdapterUtils.dpToPixels(activity.applicationContext, dpSize.height)
        )
        layoutParams.gravity = Gravity.CENTER

        val placement = placementId?.toLongOrNull()
        if (placement == null) {
            val errorMessage = InMobiConstants.Logs.MISSING_PARAM.format(InMobiConstants.PLACEMENT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        mainHandler.post {
            bannerAdView = InMobiBanner(activity.applicationContext, placement)
            bannerAdView?.setListener(InMobiBannerListener(listener, layoutParams))
            bannerAdView?.setBannerSize(dpSize.width, dpSize.height)

            val bytes = adData.serverData.toByteArray(Charsets.UTF_8)
            bannerAdView?.load(bytes)
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()

        bannerAdView?.let {
            mainHandler.post {
                it.destroy()
            }
            bannerAdView = null
        }
    }

    override fun collectBiddingData(
        adData: AdData?,
        context: android.content.Context,
        biddingDataCallback: BiddingDataCallback
    ) {
        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            val errorMessage = InMobiConstants.Logs.NETWORK_ADAPTER_IS_NULL
            IronLog.INTERNAL.error(errorMessage)
            biddingDataCallback.onFailure(errorMessage)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // region Helper Methods

    private fun getBannerSize(banner: ISBannerSize, largeScreen: Boolean): Size? {
        return when (banner.description) {
            InMobiConstants.BANNER_SIZE_DESCRIPTION, InMobiConstants.LARGE_SIZE_DESCRIPTION -> Size(InMobiConstants.BANNER_WIDTH, InMobiConstants.BANNER_HEIGHT)
            InMobiConstants.RECTANGLE_SIZE_DESCRIPTION -> Size(InMobiConstants.RECTANGLE_WIDTH, InMobiConstants.RECTANGLE_HEIGHT)
            InMobiConstants.SMART_SIZE_DESCRIPTION -> if (largeScreen) {
                Size(InMobiConstants.LARGE_WIDTH, InMobiConstants.LARGE_HEIGHT)
            } else {
                Size(InMobiConstants.BANNER_WIDTH, InMobiConstants.BANNER_HEIGHT)
            }
            else -> Size(banner.width, banner.height)
        }
    }

    private class Size(val width: Int, val height: Int)

    // endregion
}
