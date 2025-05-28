package com.ironsource.adapters.yandex.banner

import android.widget.FrameLayout
import com.ironsource.adapters.yandex.YandexAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import java.lang.ref.WeakReference

class YandexBannerAdListener(
    private val mListener: BannerSmashListener,
    private val mAdapter: WeakReference<YandexBannerAdapter>,
    private val mAdView: BannerAdView,
    private val mLayoutParams: FrameLayout.LayoutParams
) : BannerAdEventListener {

    override fun onAdLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mAdapter.get()?.setBannerView(mAdView)
        mListener.onBannerAdLoaded(mAdView, mLayoutParams)
    }

    override fun onAdFailedToLoad(error: AdRequestError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${error.code}, errorMessage = ${error.description}")
        val bannerError = YandexAdapter.getLoadErrorAndCheckNoFill(
            error,
            IronSourceError.ERROR_BN_LOAD_NO_FILL
        )
        mListener.onBannerAdLoadFailed(bannerError)
    }

    override fun onImpression(impressionData: ImpressionData?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdShown()
    }

    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdClicked()
    }

    override fun onLeftApplication() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdLeftApplication()
    }

    override fun onReturnedToApplication() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}