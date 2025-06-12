package com.ironsource.adapters.verve.banner

import android.view.Gravity
import android.widget.FrameLayout
import com.ironsource.adapters.verve.VerveAdapter
import com.ironsource.adapters.verve.VerveAdapter.Companion.LOG_INIT_FAILED
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AdapterUtils
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adapter.AbstractBannerAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import net.pubnative.lite.sdk.models.AdSize
import net.pubnative.lite.sdk.views.HyBidAdView
import org.json.JSONObject
import java.lang.ref.WeakReference

class VerveBannerAdapter(adapter: VerveAdapter) :
        AbstractBannerAdapter<VerveAdapter>(adapter) {
    private var mSmashListener : BannerSmashListener? = null
    private var mAdListener : VerveBannerAdListener? = null
    private var mAdView: HyBidAdView? = null

    //region Banner API

    override fun initBannerForBidding(
            appKey: String?,
            userId: String?,
            config: JSONObject,
            listener: BannerSmashListener
    ) {

        IronLog.ADAPTER_API.verbose()

        //save interstitial listener
        mSmashListener = listener

        when (adapter.getInitState()) {
            VerveAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onBannerInitSuccess()
            }
            VerveAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onBannerInitFailed(
                        ErrorBuilder.buildInitFailedError(
                                LOG_INIT_FAILED,
                                IronSourceConstants.BANNER_AD_UNIT
                        )
                )
            }
            VerveAdapter.Companion.InitState.INIT_STATE_NONE,
            VerveAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
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
            banner: IronSourceBannerLayout,
            listener: BannerSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        val bannerSize = getBannerSize(banner.size)
        if (bannerSize == null) {
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
        val layoutParams = FrameLayout.LayoutParams(
                AdapterUtils.dpToPixels(context, bannerSize.width),
                AdapterUtils.dpToPixels(context, bannerSize.height),
                Gravity.CENTER
        )

        val bannerAdView = HyBidAdView(
            context,
            bannerSize
        )

        bannerAdView.setAdSize(bannerSize)
        setBannerView(bannerAdView)

        val bannerAdListener = VerveBannerAdListener(
            listener,
            WeakReference(this),
            mAdView,
            layoutParams
        )

        mAdListener = bannerAdListener
        bannerAdView.setMediation(true)

        postOnUIThread {
            bannerAdView.renderAd(
                serverData,
                bannerAdListener
            )
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
        adapter.collectBiddingData(biddingDataCallback)
    }

    //endregion

    //region memory handling

    override fun releaseMemory(
            adUnit: IronSource.AD_UNIT,
            config: JSONObject?
    ) {
        IronLog.INTERNAL.verbose()
        destroyBannerViewAd()
        mSmashListener = null
        mAdListener = null
    }

    //endregion

    // region Helpers

    private fun destroyBannerViewAd() {
        postOnUIThread {
            mAdView?.destroy()
            mAdView = null
        }
    }

    internal fun setBannerView(bannerAdView: HyBidAdView) {
        mAdView = bannerAdView
    }


    private fun getBannerSize(bannerSize: ISBannerSize?): AdSize? {

        if(bannerSize == null) {
            IronLog.INTERNAL.verbose("Banner size is null")
            return null
        }

        return when (bannerSize.description) {
            "BANNER" -> AdSize.SIZE_320x50
            "RECTANGLE" -> AdSize.SIZE_300x250
            "SMART" ->
                (if (AdapterUtils.isLargeScreen(ContextProvider.getInstance().applicationContext)) {
                    AdSize.SIZE_728x90
                } else {
                    AdSize.SIZE_320x50
                })
            else -> null
        }
    }

    //endregion

}