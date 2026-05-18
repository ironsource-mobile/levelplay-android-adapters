package com.ironsource.adapters.bigo.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.bigo.BigoAdapter
import com.ironsource.adapters.bigo.BigoConstants
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
import sg.bigo.ads.api.AdSize
import sg.bigo.ads.api.BannerAd
import sg.bigo.ads.api.BannerAdLoader
import sg.bigo.ads.api.BannerAdRequest
import java.lang.ref.WeakReference

class BigoBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<BigoAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerListener: BigoBannerListener? = null
    private var bannerAd: BannerAd? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val slotId = adData.getString(BigoConstants.SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose(BigoConstants.Logs.SLOT_ID.format(slotId ?: ""))

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = BigoConstants.Logs.SERVER_DATA_EMPTY
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val appContext = activity.applicationContext
        val bigoBannerSize = getBannerSize(bannerSize, appContext)
        if (bigoBannerSize == null) {
            val errorMessage = BigoConstants.Logs.UNSUPPORTED_BANNER_SIZE
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                errorMessage
            )
            return
        }

        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(appContext, bigoBannerSize.width),
            AdapterUtils.dpToPixels(appContext, bigoBannerSize.height),
            Gravity.CENTER
        )

        bannerListener = BigoBannerListener(listener, WeakReference(this), layoutParams)

        val bannerAdLoader = BannerAdLoader.Builder()
            .withAdLoadListener(bannerListener)
            .withExt(BigoAdapter.getMediationInfo())
            .build()

        val bannerAdRequest = BannerAdRequest.Builder()
            .withBid(serverData)
            .withSlotId(slotId)
            .withAdSizes(bigoBannerSize)
            .build()

        bannerAdLoader.loadAd(bannerAdRequest)
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        mainHandler.post {
            bannerAd?.setAdInteractionListener(null)
            bannerAd?.destroy()
            bannerAd = null
            bannerListener = null
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
            val errorMessage = BigoConstants.Logs.ADAPTER_UNAVAILABLE
            IronLog.INTERNAL.error(errorMessage)
            biddingDataCallback.onFailure(errorMessage)
            return
        }

        networkAdapter.collectBiddingData(biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    internal fun setBannerAd(ad: BannerAd) {
        ad.setAdInteractionListener(bannerListener)
        bannerAd = ad
    }

    private fun getBannerSize(bannerSize: ISBannerSize, context: Context): AdSize? {
        return when (bannerSize.description) {
            ISBannerSize.BANNER.description -> AdSize.BANNER
            ISBannerSize.RECTANGLE.description -> AdSize.MEDIUM_RECTANGLE
            ISBannerSize.SMART.description -> if (AdapterUtils.isLargeScreen(context)) {
                AdSize.LARGE_BANNER
            } else {
                AdSize.BANNER
            }
            else -> null
        }
    }

    // endregion
}
