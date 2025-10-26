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
import io.bidmachine.AdPlacementConfig
import io.bidmachine.BannerAdSize
import io.bidmachine.banner.BannerRequest
import io.bidmachine.banner.BannerView
import org.json.JSONObject
import java.lang.ref.WeakReference

class BidMachineBannerAdapter(adapter: BidMachineAdapter) :
    AbstractBannerAdapter<BidMachineAdapter>(adapter) {

    private var mBannerListener : BannerSmashListener? = null
    private var mBannerAdListener : BidMachineBannerAdListener? = null
    private var mBannerViewAd: BannerView? = null
    private var mBannerRequest: BannerRequest? = null

    companion object {
        private const val BANNER_SIZE_IS_NULL_ERROR_MSG = "banner size is null, banner has been destroyed"
    }

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
            IronLog.INTERNAL.error(BANNER_SIZE_IS_NULL_ERROR_MSG)
            listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(BANNER_SIZE_IS_NULL_ERROR_MSG))
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
        val adPlacementConfig = createBannerPlacementConfig(config, bidMachineBannerSize)
        val bannerRequestBuilder = BannerRequest.Builder(adPlacementConfig)
            .setBidPayload(serverData)

        mBannerRequest = bannerRequestBuilder.build()

        postOnUIThread {
            if (bannerSize == null) {
                IronLog.INTERNAL.error(BANNER_SIZE_IS_NULL_ERROR_MSG)
                listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(BANNER_SIZE_IS_NULL_ERROR_MSG))
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
        val adPlacementConfig = createBannerPlacementConfig(config, bidMachineBannerSize)
        adapter.collectBiddingData(biddingDataCallback, adPlacementConfig)
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

    private fun getBannerSize(bannerSize: ISBannerSize): BannerAdSize? {

        if(bannerSize == null) {
            IronLog.INTERNAL.verbose("Banner size is null")
            return null
        }

        return when (bannerSize.description) {
            "BANNER" -> BannerAdSize.Banner
            "RECTANGLE" -> BannerAdSize.MediumRectangle
            "LEADERBOARD" -> BannerAdSize.Leaderboard
            "SMART" ->
                (if (AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext)) {
                    BannerAdSize.Leaderboard
                } else {
                    BannerAdSize.Banner
                })
            else -> null
        }
    }

    private fun createBannerPlacementConfig(config: JSONObject, bannerSize: BannerAdSize): AdPlacementConfig {
        val placementId = config.optString(BidMachineAdapter.getPlacementIdKey())
        val adPlacementConfigBuilder = AdPlacementConfig.bannerBuilder(bannerSize)
        if(!placementId.isNullOrEmpty()) {
            adPlacementConfigBuilder.withPlacementId(placementId)
        }
        return adPlacementConfigBuilder.build()
    }

    //endregion
}