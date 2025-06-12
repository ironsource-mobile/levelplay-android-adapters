package com.ironsource.adapters.verve.banner

import android.widget.FrameLayout
import com.ironsource.adapters.verve.VerveAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import net.pubnative.lite.sdk.views.HyBidAdView
import java.lang.ref.WeakReference

class VerveBannerAdListener(
        private val mListener: BannerSmashListener,
        private val mAdapter: WeakReference<VerveBannerAdapter>,
        private val mAdView: HyBidAdView?,
        private val mLayoutParams: FrameLayout.LayoutParams
) :  HyBidAdView.Listener{

    /**
     * Called when Ad was loaded and ready to be displayed
     *
     */
    override fun onAdLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mAdView?.let { mAdapter.get()?.setBannerView(it) }
        mListener.onBannerAdLoaded(mAdView, mLayoutParams)
    }

    /**
     * Called when Ad failed to load
     *
     * @param error - Throwable error
     */
    override fun onAdLoadFailed(error: Throwable?) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorMessage = ${error?.message}")
        mListener.onBannerAdLoadFailed(VerveAdapter.getLoadError(error))
    }

    /**
     * Called when Ad Impression has been tracked
     *
     */
    override fun onAdImpression() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdShown()
    }

    /**
     * Called when Ad has been clicked.
     *
     */
    override fun onAdClick() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdClicked()
    }

}