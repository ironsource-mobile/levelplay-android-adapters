package com.ironsource.adapters.ogury.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.adapters.ogury.OguryConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.ogury.ad.OguryBannerAdSize
import com.ogury.ad.OguryBannerAdView
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseBanner

class OguryBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<OguryAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: OguryBannerAdView? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val adUnitId = adData.getString(OguryConstants.AD_UNIT_ID_KEY)
        IronLog.ADAPTER_API.verbose(OguryConstants.Logs.AD_UNIT_ID.format(adUnitId ?: ""))

        val context = activity.applicationContext
        val oguryBannerSize = getBannerSize(context, bannerSize)
        if (oguryBannerSize == null) {
            IronLog.INTERNAL.error(OguryConstants.Logs.UNSUPPORTED_BANNER_SIZE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                OguryConstants.Logs.UNSUPPORTED_BANNER_SIZE
            )
            return
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            IronLog.INTERNAL.error(OguryConstants.Logs.SERVER_DATA_EMPTY)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                OguryConstants.Logs.SERVER_DATA_EMPTY
            )
            return
        }

        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(context, bannerSize.width),
            AdapterUtils.dpToPixels(context, bannerSize.height),
            Gravity.CENTER
        )

        mainHandler.post {
            bannerAdView = OguryBannerAdView(context, adUnitId, oguryBannerSize, OguryAdapter.mediation).apply {
                setListener(OguryBannerListener(listener, this, layoutParams))
                load(serverData)
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
            IronLog.INTERNAL.error(OguryConstants.Logs.ADAPTER_UNAVAILABLE)
            biddingDataCallback.onFailure(OguryConstants.Logs.ADAPTER_UNAVAILABLE)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    private fun getBannerSize(context: Context, bannerSize: ISBannerSize): OguryBannerAdSize? =
        when (bannerSize.description) {
            ISBannerSize.BANNER.description -> OguryBannerAdSize.SMALL_BANNER_320x50
            ISBannerSize.RECTANGLE.description -> OguryBannerAdSize.MREC_300x250
            ISBannerSize.SMART.description ->
                if (AdapterUtils.isLargeScreen(context)) null else OguryBannerAdSize.SMALL_BANNER_320x50
            else -> null
        }

    // endregion
}
