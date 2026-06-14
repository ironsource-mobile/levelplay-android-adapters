package com.ironsource.adapters.vungle.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ironsource.adapters.vungle.VungleAdapter
import com.ironsource.adapters.vungle.VungleConstants
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
import com.vungle.ads.VungleAdSize
import com.vungle.ads.VungleAds
import com.vungle.ads.VungleBannerView
import com.vungle.ads.VungleMediationLogger

class VungleBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<VungleAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerView: VungleBannerView? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val placementId = adData.getString(VungleConstants.PLACEMENT_ID_KEY)
        IronLog.ADAPTER_API.verbose(VungleConstants.Logs.PLACEMENT_ID.format(placementId ?: ""))

        if (placementId.isNullOrEmpty()) {
            val errorMessage = VungleConstants.Logs.MISSING_PARAM.format(VungleConstants.PLACEMENT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val appContext = activity.applicationContext
        val vungleBannerSize = getBannerSize(appContext, bannerSize)
        if (vungleBannerSize == null) {
            IronLog.INTERNAL.error(VungleConstants.Logs.UNSUPPORTED_BANNER_SIZE)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                VungleConstants.Logs.UNSUPPORTED_BANNER_SIZE
            )
            return
        }

        bannerView = VungleBannerView(appContext, placementId, vungleBannerSize).apply {
            adListener = VungleBannerListener(listener, this)
            adapterAdFormat = VungleConstants.ADAPTER_FORMAT_BANNER
        }

        if (!VungleAds.isInline(placementId) && bannerSize.description == VungleConstants.BANNER_SIZE_CUSTOM) {
            bannerView?.let {
                it.adapterAdFormat = "${VungleConstants.ADAPTER_FORMAT_BANNER}-${bannerSize.description.lowercase()}"
                VungleMediationLogger.logError(it, VungleConstants.Logs.CUSTOM_SIZE_MISMATCH.format(bannerSize.width, bannerSize.height))
            }
        }

        bannerView?.load(adData.serverData)
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            bannerView?.finishAd()
            bannerView = null
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
            IronLog.INTERNAL.error(VungleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(VungleConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    private fun getBannerSize(context: Context, bannerSize: ISBannerSize): VungleAdSize? {
        return when (bannerSize.description) {
            VungleConstants.BANNER_SIZE_BANNER, VungleConstants.BANNER_SIZE_LARGE -> VungleAdSize.BANNER
            VungleConstants.BANNER_SIZE_RECTANGLE -> VungleAdSize.MREC
            VungleConstants.BANNER_SIZE_SMART ->
                if (AdapterUtils.isLargeScreen(context)) {
                    VungleAdSize.BANNER_LEADERBOARD
                } else {
                    VungleAdSize.BANNER
                }

            VungleConstants.BANNER_SIZE_CUSTOM -> VungleAdSize.getAdSizeWithWidthAndHeight(bannerSize.width, bannerSize.height)
            else -> null
        }
    }

    // endregion
}
