package com.ironsource.adapters.verve.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.verve.VerveAdapter
import com.ironsource.adapters.verve.VerveConstants
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
import net.pubnative.lite.sdk.models.AdSize
import net.pubnative.lite.sdk.views.HyBidAdView

class VerveBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<VerveAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdListener: VerveBannerListener? = null
    private var adView: HyBidAdView? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        IronLog.ADAPTER_API.verbose()

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = VerveConstants.Logs.MISSING_PARAM.format(VerveConstants.SERVER_DATA)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val appContext = activity.applicationContext
        val verveBannerSize = getBannerSize(bannerSize, appContext)
        if (verveBannerSize == null) {
            IronLog.INTERNAL.error(VerveConstants.Logs.BANNER_SIZE_NULL)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                VerveConstants.Logs.BANNER_SIZE_NULL
            )
            return
        }

        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(appContext, verveBannerSize.width),
            AdapterUtils.dpToPixels(appContext, verveBannerSize.height),
            Gravity.CENTER
        )

        val bannerAdView = HyBidAdView(appContext, verveBannerSize)
        bannerAdView.apply {
            setAdSize(verveBannerSize)
            setMediation(true)
        }

        adView = bannerAdView
        val bannerListener = VerveBannerListener(listener, bannerAdView, layoutParams)
        bannerAdListener = bannerListener

        mainHandler.post {
            bannerAdView.renderAd(serverData, bannerAdListener)
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            adView?.destroy()
            adView = null
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
            biddingDataCallback.onFailure(VerveConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    private fun getBannerSize(bannerSize: ISBannerSize, context: Context): AdSize? {
        return when (bannerSize.description) {
            ISBannerSize.BANNER.description -> AdSize.SIZE_320x50
            ISBannerSize.RECTANGLE.description -> AdSize.SIZE_300x250
            ISBannerSize.SMART.description -> {
                if (AdapterUtils.isLargeScreen(context)) {
                    AdSize.SIZE_728x90
                } else {
                    AdSize.SIZE_320x50
                }
            }
            else -> null
        }
    }

    // endregion
}
