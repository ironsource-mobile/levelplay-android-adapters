package com.ironsource.adapters.mobilefuse.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.mobilefuse.MobileFuseAdapter
import com.ironsource.adapters.mobilefuse.MobileFuseConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.mobilefuse.sdk.MobileFuseBannerAd
import com.mobilefuse.sdk.MobileFuseBannerAd.AdSize
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseBanner

class MobileFuseBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<MobileFuseAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAd: MobileFuseBannerAd? = null

    // region LevelPlay Banner API

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val placementId = adData.getString(MobileFuseConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MobileFuseConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        if (placementId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(MobileFuseConstants.Logs.PLACEMENT_ID_EMPTY)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                MobileFuseConstants.Logs.PLACEMENT_ID_EMPTY
            )
            return
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            IronLog.INTERNAL.error(MobileFuseConstants.SERVER_DATA_EMPTY)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                MobileFuseConstants.SERVER_DATA_EMPTY
            )
            return
        }

        val appContext = activity.applicationContext
        val mobileFuseBannerSize = getBannerSize(bannerSize, AdapterUtils.isLargeScreen(appContext))
        if (mobileFuseBannerSize == null) {
            IronLog.INTERNAL.error(MobileFuseConstants.UNSUPPORTED_BANNER_SIZE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                MobileFuseConstants.UNSUPPORTED_BANNER_SIZE
            )
            return
        }

        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(appContext, bannerSize.width),
            AdapterUtils.dpToPixels(appContext, bannerSize.height),
            Gravity.CENTER
        )

        val ad = MobileFuseBannerAd(
            appContext,
            placementId,
            mobileFuseBannerSize
        )
        bannerAd = ad

        val bannerAdListener = MobileFuseBannerListener(
            listener,
            ad,
            layoutParams
        )

        ad.setListener(bannerAdListener)
        ad.autorefreshEnabled = false
        ad.setMuted(true)
        ad.loadAdFromBiddingToken(serverData)
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            bannerAd?.setListener(null)
            bannerAd?.destroy()
            bannerAd = null
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
            IronLog.INTERNAL.error(MobileFuseConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(MobileFuseConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    private fun getBannerSize(size: ISBannerSize, isLargeScreen: Boolean): AdSize? {
        return when (size.description) {
            ISBannerSize.BANNER.description-> AdSize.BANNER_320x50
            ISBannerSize.RECTANGLE.description-> AdSize.BANNER_300x250
            ISBannerSize.SMART.description-> if (isLargeScreen) AdSize.BANNER_728x90 else AdSize.BANNER_320x50
            else -> null
        }
    }

    // endregion
}
