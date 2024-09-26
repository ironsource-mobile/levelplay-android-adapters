package com.ironsource.adapters.ogury.banner

import android.widget.FrameLayout
import com.ironsource.adapters.ogury.OguryAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.ogury.core.OguryError
import com.ogury.ed.OguryBannerAdListener
import com.ogury.ed.OguryBannerAdView
import java.lang.ref.WeakReference

class OguryBannerAdListener(
    private val mListener: BannerSmashListener,
    private val mAdapter: WeakReference<OguryBannerAdapter>,
    private val mAdView: OguryBannerAdView?,
    private val mLayoutParams: FrameLayout.LayoutParams
) :  OguryBannerAdListener {

    override fun onAdLoaded() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mAdView?.let { mAdapter.get()?.setBannerView(it) }
        mListener.onBannerAdLoaded(mAdView, mLayoutParams)
    }

    /**
     * The ad failed to load or display.
     *
     */
    override fun onAdError(error: OguryError) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorMessage = ${error.message}, " +
            "errorCode = ${error.errorCode}, errorCause = ${error.cause}")
        mListener.onBannerAdLoadFailed(OguryAdapter.getLoadError(error))
    }

    /**
     * The ad has been displayed on the screen.
     *
     */
    override fun onAdDisplayed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdShown()
    }

    /**
     * The ad has been clicked by the user.
     *
     */
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdClicked()
    }

    /**
     * The ad has been closed by the user.
     *
     */
    override fun onAdClosed() {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdScreenDismissed()
    }

}