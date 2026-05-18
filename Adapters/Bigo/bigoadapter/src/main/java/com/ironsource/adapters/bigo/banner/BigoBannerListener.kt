package com.ironsource.adapters.bigo.banner

import android.widget.FrameLayout
import com.ironsource.adapters.bigo.BigoConstants
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.logger.IronLog
import sg.bigo.ads.api.AdError
import sg.bigo.ads.api.AdInteractionListener
import sg.bigo.ads.api.AdLoadListener
import sg.bigo.ads.api.BannerAd
import java.lang.ref.WeakReference

class BigoBannerListener(
    private val listener: BannerAdListener,
    private val adapter: WeakReference<BigoBannerAdapter>,
    private val layoutParams: FrameLayout.LayoutParams
) : AdInteractionListener, AdLoadListener<BannerAd> {

    /**
     * Called when ad request succeeds
     * @param ad - Banner ad
     */
    override fun onAdLoaded(ad: BannerAd) {
        IronLog.ADAPTER_CALLBACK.verbose()

        adapter.get()?.setBannerAd(ad)
        listener.onAdLoadSuccess(ad.adView(), layoutParams)
    }

    /**
     * Called when something wrong during ad loading
     * @param error - Bigo ad error
     */
    override fun onError(error: AdError) {
        IronLog.ADAPTER_CALLBACK.error(BigoConstants.Logs.FAILED_TO_LOAD.format(error.code, error.message))
        listener.onAdLoadFailed(AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL, error.code, error.message)
    }

    /**
     * Called when something wrong when using this ad
     * @param error - Bigo ad error
     */
    override fun onAdError(error: AdError) {
        IronLog.ADAPTER_CALLBACK.error(BigoConstants.Logs.FAILED_TO_SHOW.format(error.code, error.message))
    }

    /**
     * Called when ad has been displayed successfully on the device screen
     */
    override fun onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when ad is clicked by the user
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the fullscreen ad covers the screen
     */
    override fun onAdOpened() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the fullscreen ad closes
     */
    override fun onAdClosed() {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}
