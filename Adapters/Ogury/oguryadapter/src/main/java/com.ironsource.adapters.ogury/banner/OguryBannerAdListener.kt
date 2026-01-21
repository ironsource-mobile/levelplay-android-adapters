package com.ironsource.adapters.ogury.banner

import android.widget.FrameLayout
import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ogury.ad.OguryAdError
import com.ogury.ad.OguryBannerAdViewListener
import com.ogury.ad.OguryBannerAdView
import java.lang.ref.WeakReference

class OguryBannerAdListener(
    private val mListener: BannerSmashListener,
    private val mAdapter: WeakReference<OguryBannerAdapter>,
    private val mAdView: OguryBannerAdView?,
    private val mLayoutParams: FrameLayout.LayoutParams
) :  OguryBannerAdViewListener {

    /**
     * The SDK is ready to display the ad provided by the ad server.
     * @param ad - bannerAd instance
     */
    override fun onAdLoaded(ad: OguryBannerAdView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mAdView?.let { mAdapter.get()?.setBannerView(it) }
        mListener.onBannerAdLoaded(mAdView, mLayoutParams)
    }

    /**
     * The ad failed to load or display.
     * @param ad - bannerAd instance
     * @param error - Ogury Ad Error
     */
    override fun onAdError(ad: OguryBannerAdView, error: OguryAdError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorMessage = ${error.message}, " +
            "errorCode = ${error.code}")
        mListener.onBannerAdLoadFailed(OguryAdapter.getLoadError(error))
    }

    /**
     * The ad has been displayed on the screen.
     * @param ad - bannerAd instance
     */
    override fun onAdImpression(ad: OguryBannerAdView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdShown()
    }

    /**
     * The ad has been clicked by the user.
     * @param ad - bannerAd instance
     */
    override fun onAdClicked(ad: OguryBannerAdView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdClicked()
    }

    /**
     * The ad has been closed by the user.
     * @param ad - bannerAd instance
     */
    override fun onAdClosed(ad: OguryBannerAdView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdScreenDismissed()
    }

}