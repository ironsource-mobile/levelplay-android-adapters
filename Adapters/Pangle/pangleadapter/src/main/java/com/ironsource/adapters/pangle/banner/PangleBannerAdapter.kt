package com.ironsource.adapters.pangle.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerSize
import com.ironsource.adapters.pangle.PangleAdapter
import com.ironsource.adapters.pangle.PangleConstants
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

class PangleBannerAdapter(networkSettings: NetworkSettings) : LevelPlayBaseBanner<PangleAdapter>(networkSettings) {

    private var bannerAd: PAGBannerAd? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // region Adapter Methods

    override fun loadAd(adData: AdData, activity: Activity, bannerSize: ISBannerSize, listener: BannerAdListener) {
        val slotId = adData.getString(PangleConstants.SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose(PangleConstants.Logs.SLOT_ID.format(slotId))

        if (slotId.isNullOrEmpty()) {
            val errorMessage = PangleConstants.Logs.MISSING_PARAM.format(PangleConstants.SLOT_ID_KEY)
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
            IronLog.INTERNAL.error(PangleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                PangleConstants.Logs.NETWORK_ADAPTER_IS_NULL
            )
            return
        }

        if (networkAdapter.isCoppaChildUser()) {
            val errorMessage = PangleConstants.Logs.CHILD_USER_ERROR.format(PangleConstants.PANGLE_NOT_ALLOW_CHILD_ERROR_MSG)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                PangleConstants.PANGLE_NOT_ALLOW_CHILD_ERROR_CODE,
                errorMessage
            )
            return
        }

        val appContext = activity.applicationContext
        val pangleBannerSize = getBannerSize(appContext, bannerSize)
        val layoutParams = getBannerLayoutParams(appContext, bannerSize)
        val bannerListener = PangleBannerListener(listener, WeakReference(this), layoutParams)
        val bannerRequest = PAGBannerRequest(pangleBannerSize).apply { adString = adData.serverData }

        mainHandler.post {
            PAGBannerAd.loadAd(slotId, bannerRequest, bannerListener)
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()

        mainHandler.post {
            // The listener needs to be set to null prior to destroying the banner to prevent a memory leak
            bannerAd?.setAdInteractionListener(null)
            // Destroy banner
            bannerAd?.destroy()
            bannerAd = null
        }
    }

    override fun collectBiddingData(adData: AdData?, context: Context, biddingDataCallback: BiddingDataCallback) {
        val slotId = adData?.getString(PangleConstants.SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose(PangleConstants.Logs.SLOT_ID.format(slotId ?: ""))

        val networkAdapter = getNetworkAdapter()
        if (networkAdapter == null) {
            IronLog.INTERNAL.error(PangleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(PangleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(context, slotId, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    internal fun setBannerAd(bannerAd: PAGBannerAd?) {
        this.bannerAd = bannerAd
    }

    private fun getBannerSize(context: Context, bannerSize: ISBannerSize): PAGBannerSize {
        return when (bannerSize.description) {
            PangleConstants.BANNER_SIZE_BANNER -> PAGBannerSize.BANNER_W_320_H_50
            PangleConstants.BANNER_SIZE_RECTANGLE -> PAGBannerSize.BANNER_W_300_H_250
            PangleConstants.BANNER_SIZE_SMART ->
                if (AdapterUtils.isLargeScreen(context)) {
                    PAGBannerSize.BANNER_W_728_H_90
                } else {
                    PAGBannerSize.BANNER_W_320_H_50
                }
            else -> PAGBannerSize(0, 0)
        }
    }

    private fun getBannerLayoutParams(context: Context, size: ISBannerSize): FrameLayout.LayoutParams {
        val layoutParams = when (size.description) {
            PangleConstants.BANNER_SIZE_BANNER -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, PangleConstants.BANNER_WIDTH),
                AdapterUtils.dpToPixels(context, PangleConstants.BANNER_HEIGHT)
            )
            PangleConstants.BANNER_SIZE_RECTANGLE -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, PangleConstants.RECTANGLE_WIDTH),
                AdapterUtils.dpToPixels(context, PangleConstants.RECTANGLE_HEIGHT)
            )
            PangleConstants.BANNER_SIZE_SMART ->
                if (AdapterUtils.isLargeScreen(context)) {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, PangleConstants.LARGE_WIDTH),
                        AdapterUtils.dpToPixels(context, PangleConstants.LARGE_HEIGHT)
                    )
                } else {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, PangleConstants.BANNER_WIDTH),
                        AdapterUtils.dpToPixels(context, PangleConstants.BANNER_HEIGHT)
                    )
                }
            else -> FrameLayout.LayoutParams(0, 0)
        }

        // Set gravity
        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    // endregion
}
