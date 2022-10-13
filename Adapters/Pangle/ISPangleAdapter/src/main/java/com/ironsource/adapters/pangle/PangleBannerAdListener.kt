package com.ironsource.adapters.pangle

import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdInteractionListener
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import java.lang.ref.WeakReference

class PangleBannerAdListener(private val mListener: BannerSmashListener?,
                             private val mAdapter: WeakReference<PangleAdapter>?,
                             private val mSlotId: String,
                             private val mLayoutParams: FrameLayout.LayoutParams) : PAGBannerAdLoadListener, PAGBannerAdInteractionListener {

    // PAGBannerAdLoadListener

    //This method is executed when an ad material is loaded successfully.
    override fun onAdLoaded(bannerAd: PAGBannerAd) {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId")
        mAdapter?.get()?.setBannerAd(mSlotId, bannerAd)
        mListener?.onBannerAdLoaded(bannerAd.bannerView, mLayoutParams)
    }

    //This method is invoked when an ad fails to load. It includes an error parameter of type Error that indicates what type of failure occurred.
    override fun onError(code: Int, message: String) {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId, error code = $code, message = $message")
        val errorCode = if (code == PangleAdapter.PANGLE_NO_FILL_ERROR_CODE) IronSourceError.ERROR_BN_LOAD_NO_FILL else code
        mListener?.onBannerAdLoadFailed(IronSourceError(errorCode, message))
    }

    // PAGBannerAdInteractionListener

    //This method is invoked when the ad is displayed, covering the device's screen.
    override fun onAdShowed() {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId")
        mListener?.onBannerAdShown()
    }

    //This method is invoked when the ad is clicked by the user.
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId")
        mListener?.onBannerAdClicked()
    }

    //This method is invoked when the ad disappears.
    override fun onAdDismissed() {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId")
    }
}