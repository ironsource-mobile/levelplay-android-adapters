package com.ironsource.adapters.bidmachine.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.*
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import io.bidmachine.AdsFormat
import io.bidmachine.banner.BannerRequest
import io.bidmachine.banner.BannerSize
import io.bidmachine.banner.BannerView
import org.json.JSONObject
import java.lang.ref.WeakReference

class BidMachineBannerAdapter(adapter: BidMachineAdapter) :
    AbstractBannerAdapter<BidMachineAdapter>(adapter) {

    private var mBannerListener : BannerSmashListener? = null
    private var mBannerAdListener : BidMachineBannerAdListener? = null
    private var mBannerViewAd: BannerView? = null
    private var mBannerRequest: BannerRequest? = null

    override fun initBannerForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        val sourceIdKey = BidMachineAdapter.getSourceIdKey()
        val sourceId = config.optString(sourceIdKey)

        if (sourceId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(sourceIdKey))
            listener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(sourceIdKey),
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }

        //save banner listener
        mBannerListener = listener

        when (adapter.getInitState()) {
            BidMachineAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onBannerInitSuccess()
            }
            BidMachineAdapter.Companion.InitState.INIT_STATE_NONE,
            BidMachineAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(sourceId)
            }
        }
    }

    override fun loadBannerForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        bannerSize: ISBannerSize?,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (bannerSize == null) {
            IronLog.INTERNAL.error("banner size is null")
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName))
            return
        }

        val bidMachineBannerSize = getBannerSize(bannerSize)
        if (bidMachineBannerSize == null) {
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName))
            return
        }

        val context = ContextProvider.getInstance().applicationContext
        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(context, bidMachineBannerSize.width),
            AdapterUtils.dpToPixels(context, bidMachineBannerSize.height),
            Gravity.CENTER
        )

        val bannerAdListener = BidMachineBannerAdListener(listener, WeakReference(this), layoutParams)
        mBannerAdListener = bannerAdListener

        val bannerView = BannerView(ContextProvider.getInstance().applicationContext)
        bannerView.setListener(bannerAdListener)

        val placementId = config.optString(BidMachineAdapter.getPlacementIdKey())
        val bannerRequestBuilder = BannerRequest.Builder()
            .setSize(bidMachineBannerSize)
            .setBidPayload(serverData)

        if(!placementId.isNullOrEmpty()) {
            bannerRequestBuilder.setPlacementId(placementId)
        }

        mBannerRequest = bannerRequestBuilder.build()

        postOnUIThread {
            if (bannerSize == null) {
                IronLog.INTERNAL.error("banner size is null")
                listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName))
                return@postOnUIThread
            }
            bannerView.load(mBannerRequest)
        }
    }

    override fun destroyBanner(config: JSONObject) {
        IronLog.ADAPTER_API.verbose()
        destroyBannerViewAd()
    }

    override fun collectBannerBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        val bannerSize = adData?.opt(IronSourceConstants.BANNER_SIZE)
        if (bannerSize !is ISBannerSize) {
            val error = "Banner size is invalid or not of type ISBannerSize"
            IronLog.INTERNAL.verbose(error)
            biddingDataCallback.onFailure("$error - BidMachine")
            return
        }
        val bidMachineBannerSize = getBannerSize(bannerSize)
        if (bidMachineBannerSize == null) {
            val error = "Unsupported or null banner size"
            IronLog.INTERNAL.verbose(error)
            biddingDataCallback.onFailure("$error - BidMachine")
            return
        }
        val format = when (bidMachineBannerSize) {
            BannerSize.Size_320x50 -> AdsFormat.Banner_320x50
            BannerSize.Size_300x250 -> AdsFormat.Banner_300x250
            BannerSize.Size_728x90 -> AdsFormat.Banner_728x90
        }

        adapter.collectBiddingData(biddingDataCallback, format, config)
    }

    //endregion

    // region Helpers

    private fun destroyBannerViewAd() {
        postOnUIThread {
            mBannerViewAd?.setListener(null)
            mBannerViewAd?.destroy()
        }
        mBannerViewAd = null
    }

    internal fun setBannerView(ad: BannerView) {
        mBannerViewAd = ad
    }

    private fun getBannerSize(bannerSize: ISBannerSize): BannerSize? {

        if(bannerSize == null) {
            IronLog.INTERNAL.verbose("Banner size is null")
            return null
        }

        return when (bannerSize.description) {
            "BANNER" -> BannerSize.Size_320x50
            "RECTANGLE" -> BannerSize.Size_300x250
            "LEADERBOARD" -> BannerSize.Size_728x90
            "SMART" ->
                (if (AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext)) {
                    BannerSize.Size_728x90
                } else {
                    BannerSize.Size_320x50
                })
            else -> null
        }
    }

    //endregion
}