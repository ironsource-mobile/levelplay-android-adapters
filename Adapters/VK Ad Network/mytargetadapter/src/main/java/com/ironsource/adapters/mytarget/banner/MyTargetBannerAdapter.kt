package com.ironsource.adapters.mytarget.banner

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.mytarget.MyTargetAdapter
import com.ironsource.adapters.mytarget.MyTargetConstants
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.model.NetworkSettings
import com.my.target.ads.MyTargetView
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseBanner

class MyTargetBannerAdapter(networkSettings: NetworkSettings) :
    LevelPlayBaseBanner<MyTargetAdapter>(networkSettings) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerAdView: MyTargetView? = null

    // region Adapter Methods

    override fun loadAd(
        adData: AdData,
        activity: Activity,
        bannerSize: ISBannerSize,
        listener: BannerAdListener
    ) {
        val slotId = adData.getString(MyTargetConstants.SLOT_ID_KEY)
        IronLog.ADAPTER_API.verbose(MyTargetConstants.Logs.SLOT_ID.format(slotId ?: ""))

        val slotIdInt = slotId?.toIntOrNull()
        if (slotIdInt == null) {
            val errorMessage = MyTargetConstants.Logs.ERROR_PARSING_PLACEMENT
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val appContext = activity.applicationContext

        val myTargetBannerSize = getBannerSize(bannerSize, appContext)
        if (myTargetBannerSize == null) {
            val errorMessage = MyTargetConstants.Logs.UNSUPPORTED_BANNER_SIZE
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                errorMessage
            )
            return
        }

        val serverData = adData.serverData
        if (serverData.isNullOrEmpty()) {
            val errorMessage = MyTargetConstants.Logs.SERVER_DATA_IS_EMPTY
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS,
                errorMessage
            )
            return
        }

        val layoutParams = getBannerLayoutParams(bannerSize, appContext)

        bannerAdView = MyTargetView(appContext).apply {
            setSlotId(slotIdInt)
            setAdSize(myTargetBannerSize)
            setListener(MyTargetBannerListener(listener, layoutParams))
            loadFromBid(serverData)
        }
    }

    override fun destroyAd(adData: AdData) {
        IronLog.ADAPTER_API.verbose()
        bannerAdView?.let {
            mainHandler.post { it.destroy() }
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
            IronLog.INTERNAL.error(MyTargetConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            biddingDataCallback.onFailure(MyTargetConstants.Logs.NETWORK_ADAPTER_IS_NULL)
            return
        }

        networkAdapter.collectBiddingData(context, biddingDataCallback)
    }

    // endregion

    // region Helper Methods

    private fun getBannerSize(bannerSize: ISBannerSize, context: Context): MyTargetView.AdSize? =
        when (bannerSize.description) {
            MyTargetConstants.BANNER_SIZE_DESCRIPTION -> MyTargetView.AdSize.ADSIZE_320x50
            MyTargetConstants.RECTANGLE_SIZE_DESCRIPTION -> MyTargetView.AdSize.ADSIZE_300x250
            MyTargetConstants.SMART_SIZE_DESCRIPTION ->
                if (AdapterUtils.isLargeScreen(context)) MyTargetView.AdSize.ADSIZE_728x90 else MyTargetView.AdSize.ADSIZE_320x50
            else -> null
        }

    private fun getBannerLayoutParams(bannerSize: ISBannerSize, context: Context): FrameLayout.LayoutParams {
        val layoutParams = when (bannerSize.description) {
            MyTargetConstants.BANNER_SIZE_DESCRIPTION -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, MyTargetConstants.BANNER_WIDTH),
                AdapterUtils.dpToPixels(context, MyTargetConstants.BANNER_HEIGHT)
            )

            MyTargetConstants.RECTANGLE_SIZE_DESCRIPTION -> FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, MyTargetConstants.RECTANGLE_WIDTH),
                AdapterUtils.dpToPixels(context, MyTargetConstants.RECTANGLE_HEIGHT)
            )

            MyTargetConstants.SMART_SIZE_DESCRIPTION ->
                if (AdapterUtils.isLargeScreen(context)) {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, MyTargetConstants.LARGE_WIDTH),
                        AdapterUtils.dpToPixels(context, MyTargetConstants.LARGE_HEIGHT)
                    )
                } else {
                    FrameLayout.LayoutParams(
                        AdapterUtils.dpToPixels(context, MyTargetConstants.BANNER_WIDTH),
                        AdapterUtils.dpToPixels(context, MyTargetConstants.BANNER_HEIGHT)
                    )
                }

            else -> FrameLayout.LayoutParams(0, 0)
        }

        layoutParams.gravity = Gravity.CENTER
        return layoutParams
    }

    // endregion
}
