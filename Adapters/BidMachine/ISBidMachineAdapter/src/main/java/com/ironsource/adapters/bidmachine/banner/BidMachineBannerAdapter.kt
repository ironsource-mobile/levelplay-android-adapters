package com.ironsource.adapters.bidmachine.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.*
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter
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
        banner: IronSourceBannerLayout,
        listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (banner == null) {
            IronLog.INTERNAL.verbose("banner is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName));
            return
        }

        val bannerSize = getBannerSize(banner.size)
        if (bannerSize == null) {
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName))
            return
        }

        val context = ContextProvider.getInstance().applicationContext
        val layoutParams = FrameLayout.LayoutParams(
            AdapterUtils.dpToPixels(context, bannerSize.width),
            AdapterUtils.dpToPixels(context, bannerSize.height),
            Gravity.CENTER
        )

        val bannerAdListener = BidMachineBannerAdListener(listener, WeakReference(this), layoutParams)
        mBannerAdListener = bannerAdListener

        val bannerView = BannerView(ContextProvider.getInstance().applicationContext)
        bannerView.setListener(bannerAdListener)

        mBannerRequest = BannerRequest.Builder()
            .setSize(bannerSize)
            .setBidPayload(serverData)
            .build()

        postOnUIThread {
            if (banner == null) {
                IronLog.INTERNAL.verbose("banner is null");
                listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName));
                return@postOnUIThread
            }
            bannerView.load(mBannerRequest)
        }
    }

    override fun destroyBanner(config: JSONObject) {
        IronLog.ADAPTER_API.verbose()
        destroyBannerViewAd()
    }

    override fun getBannerBiddingData(
        config: JSONObject,
        adData: JSONObject?
    ): MutableMap<String?, Any?>? {
        return adapter.getBiddingData(AdsFormat.Banner)
    }

    //endregion

    //region memory handling

    override fun releaseMemory(
        adUnit: IronSource.AD_UNIT,
        config: JSONObject?
    ) {
        IronLog.INTERNAL.verbose("adUnit = $adUnit")

        if (adUnit == IronSource.AD_UNIT.BANNER) {
            destroyBannerViewAd()
        }
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
            IronLog.INTERNAL.verbose("Banner size is null");
            return null;
        }

        return when (bannerSize.description) {
            "BANNER" -> BannerSize.Size_320x50
            "RECTANGLE" -> BannerSize.Size_300x250
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