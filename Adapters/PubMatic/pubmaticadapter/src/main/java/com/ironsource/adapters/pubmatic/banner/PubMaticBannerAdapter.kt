package com.ironsource.adapters.pubmatic.banner

import com.ironsource.adapters.pubmatic.PubMaticAdapter
import com.ironsource.adapters.pubmatic.PubMaticAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.pubmatic.sdk.common.POBAdFormat
import com.pubmatic.sdk.common.POBAdSize
import com.pubmatic.sdk.openwrap.banner.POBBannerView
import org.json.JSONObject
import java.lang.ref.WeakReference

class PubMaticBannerAdapter(adapter: PubMaticAdapter) :
    AbstractBannerAdapter<PubMaticAdapter>(adapter) {

    private var mSmashListener : BannerSmashListener? = null
    private var mAdListener : PubMaticBannerAdListener? = null
    private var mAdView: POBBannerView? = null

    //region Banner API

    override fun initBannerForBidding(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: BannerSmashListener
    ) {
        val adUnitIdKey = PubMaticAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)
        if (adUnitId.isNullOrEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(adUnitIdKey))
            listener.onBannerInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(adUnitIdKey),
                    IronSourceConstants.BANNER_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("adUnitId = $adUnitId")

        //save banner listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            PubMaticAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onBannerInitSuccess()
            }
            PubMaticAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onBannerInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        LOG_INIT_FAILED,
                        IronSourceConstants.BANNER_AD_UNIT
                    )
                )
            }
            PubMaticAdapter.Companion.InitState.INIT_STATE_NONE,
            PubMaticAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(config)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mSmashListener?.onBannerInitSuccess()
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        mSmashListener?.onBannerInitFailed(
            ErrorBuilder.buildInitFailedError(
                error,
                IronSourceConstants.BANNER_AD_UNIT
            )
        )
    }

    override fun loadBannerForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        bannerSize: ISBannerSize?,
        listener: BannerSmashListener
    ) {
        val adUnitIdKey = PubMaticAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)

        IronLog.ADAPTER_API.verbose("adUnitId = $adUnitId")

        if (bannerSize == null) {
            IronLog.INTERNAL.verbose("banner is null")
            listener.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner is null"))
            return
        }

        val adSize = getBannerSize(bannerSize)
        if (adSize == null) {
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(adapter.providerName))
            return
        }

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        val context = ContextProvider.getInstance().applicationContext
        mAdView = POBBannerView(context)

        mAdListener = PubMaticBannerAdListener(
            listener,
            WeakReference(this),
            adUnitId,
            mAdView
        )
        mAdView?.setListener(mAdListener)
        postOnUIThread {
            if (bannerSize == null) {
                val errorMsg = "banner size is null, banner has been destroyed"
                IronLog.INTERNAL.verbose(errorMsg)
                listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(errorMsg))
                return@postOnUIThread
            }
            mAdView?.loadAd(serverData, PubMaticAdapter.BiddingHost) ?: run {
                listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError("Ad is null"))
            }
            mAdView?.pauseAutoRefresh()
        }
    }

    override fun destroyBanner(config: JSONObject) {
        IronLog.ADAPTER_API.verbose()
        destroyBannerViewAd()
        mSmashListener = null
        mAdListener = null
    }

    override fun collectBannerBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        val bannerSize = adData?.opt(IronSourceConstants.BANNER_SIZE) as? ISBannerSize
        val pubMaticBannerSize = getBannerSize(bannerSize)
        if (pubMaticBannerSize == null) {
            val error = "Unsupported or null banner size"
            IronLog.INTERNAL.verbose(error)
            biddingDataCallback.onFailure("$error - PubMatic")
            return
        }

        val format = when (pubMaticBannerSize) {
            POBAdSize.BANNER_SIZE_300x250 -> POBAdFormat.MREC
            else -> POBAdFormat.BANNER
        }
        adapter.collectBiddingData(biddingDataCallback, format)
    }

    //endregion

    // region Helpers

    private fun destroyBannerViewAd() {
        postOnUIThread {
            mAdView?.destroy()
            mAdView = null
        }
    }

    internal fun setBannerView(bannerAdView: POBBannerView) {
        mAdView = bannerAdView
    }

    private fun getBannerSize(bannerSize: ISBannerSize?): POBAdSize? {
        if (bannerSize == null) {
            IronLog.INTERNAL.verbose("Banner size is null")
            return null
        }

        return when (bannerSize.description) {
            ISBannerSize.BANNER.description -> POBAdSize.BANNER_SIZE_320x50
            ISBannerSize.LARGE.description -> POBAdSize.BANNER_SIZE_320x100
            ISBannerSize.RECTANGLE.description -> POBAdSize.BANNER_SIZE_300x250
            ISBannerSize.SMART.description -> if (AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext)) {
                POBAdSize.BANNER_SIZE_728x90
            } else {
                POBAdSize.BANNER_SIZE_320x50
            }
            else -> null
        }
    }

    //endregion

}
