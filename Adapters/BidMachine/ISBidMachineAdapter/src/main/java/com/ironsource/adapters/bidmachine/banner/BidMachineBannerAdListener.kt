package com.ironsource.adapters.bidmachine.banner

import android.widget.FrameLayout
import com.ironsource.adapters.bidmachine.BidMachineAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import io.bidmachine.banner.BannerListener
import io.bidmachine.banner.BannerView
import io.bidmachine.utils.BMError
import java.lang.ref.WeakReference

class BidMachineBannerAdListener(
    private val mListener: BannerSmashListener,
    private val mAdapter: WeakReference<BidMachineBannerAdapter>,
    private val mLayoutParams: FrameLayout.LayoutParams
) : BannerListener {

    /**
     * Called when Ad was loaded and ready to be displayed
     *
     * @param ad - BannerView instance
     */
    override fun onAdLoaded(ad: BannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mAdapter.get()?.setBannerView(ad)
        mListener.onBannerAdLoaded(ad, mLayoutParams)
    }

    /**
     * Called when Ad failed to load
     *
     * @param ad    - BannerView instance
     * @param error - BMError with additional info about error
     */
    override fun onAdLoadFailed(ad: BannerView, error: BMError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${error.code}, errorMessage = ${error.message}")
        val bannerError = BidMachineAdapter.getLoadErrorAndCheckNoFill(
            error,
            IronSourceError.ERROR_BN_LOAD_NO_FILL
        )
        mListener.onBannerAdLoadFailed(bannerError)
    }

    /**
     * Called when Ad Impression has been tracked
     *
     * @param ad - BannerView instance
     */
    override fun onAdImpression(ad: BannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdShown()
    }

    /**
     * Called when Ad show failed
     *
     * @param ad    - BannerView instance
     * @param error - BMError with additional info about error
     */
    override fun onAdShowFailed(ad: BannerView, error: BMError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to show, errorCode = ${error.code}, errorMessage = ${error.message}")
    }

    /**
     * Called when Ad has been clicked
     *
     * @param ad - BannerView instance
     */
    override fun onAdClicked(ad: BannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdClicked()
    }

    /**
     * Called when Ad expired
     *
     * @param ad - BannerView instance
     */
    override fun onAdExpired(ad: BannerView) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}