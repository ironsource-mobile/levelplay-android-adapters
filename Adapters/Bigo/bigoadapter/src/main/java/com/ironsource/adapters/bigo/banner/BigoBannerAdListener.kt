package com.ironsource.adapters.bigo.banner

import android.widget.FrameLayout
import com.ironsource.adapters.bigo.BigoAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdInteractionListener
import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.BannerAd
import java.lang.ref.WeakReference

class BigoBannerAdListener(
    private val mAdapter: WeakReference<BigoBannerAdapter>,
    private val mListener: BannerSmashListener,
    private val mLayoutParams: FrameLayout.LayoutParams) : AdInteractionListener, AdLoadListener<BannerAd> {

    /**
     * Called when ad request succeeds
     *
     * @param ad - Banner ad
     */
    override fun onAdLoaded(ad: BannerAd) {
        IronLog.ADAPTER_CALLBACK.verbose()

        mListener.onBannerAdLoaded(
            ad.adView(),
            mLayoutParams
        )

        mAdapter.get()?.setBannerView(ad)

    }

    /**
     * Called when something wrong during ad loading
     *
     * @param error - bigo ad error
     */
    override fun onError(error: AdError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${error.code}, errorMessage = ${error.message}")
        mListener.onBannerAdLoadFailed(BigoAdapter.getLoadError(error))
    }

    /**
     * There's something wrong when using this ad
     *
     * @param error - bigo ad error
     */
    override fun onAdError(error: AdError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to show, errorCode = ${error.code}, errorMessage = ${error.message}")
        mListener.onBannerAdLoadFailed(BigoAdapter.getLoadError(error))
    }

    /**
     * Indicates that the ad has been displayed successfully on the device screen.
     *
     */
    override fun onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdShown()
    }

    /**
     * Indicates that the ad is clicked by the user.
     *
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdClicked()
    }

    /**
     * When the fullscreen ad covers the screen.
     *
     */
    override fun onAdOpened() {
        // Bigo banner ads should not trigger this callback.
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * When the fullsceen ad closes.
     *
     */
    override fun onAdClosed() {
        // Bigo banner ads should not trigger this callback.
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}