package com.ironsource.adapters.pangle

import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionListener
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import java.lang.ref.WeakReference

class PangleInterstitialAdListener(private val mListener: InterstitialSmashListener?,
                                   private val mAdapter: WeakReference<PangleAdapter>?,
                                   private val mSlotId: String) : PAGInterstitialAdLoadListener, PAGInterstitialAdInteractionListener {

    // PAGInterstitialAdLoadListener

    //This method is executed when an ad material is loaded successfully.
    override fun onAdLoaded(interstitialAd: PAGInterstitialAd) {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId")
        mAdapter?.get()?.setInterstitialAd(mSlotId, interstitialAd)
        mAdapter?.get()?.setInterstitialAdAvailability(mSlotId, true)
        mListener?.onInterstitialAdReady()
    }

    //This method is invoked when an ad fails to load. It includes an error parameter of type Error that indicates what type of failure occurred.
    override fun onError(code: Int, message: String) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load slotId = $mSlotId, error code = $code, message = $message")
        mAdapter?.get()?.setInterstitialAdAvailability(mSlotId, false)
        val errorCode = if (code == PangleAdapter.PANGLE_NO_FILL_ERROR_CODE) IronSourceError.ERROR_IS_LOAD_NO_FILL else code
        mListener?.onInterstitialAdLoadFailed(IronSourceError(errorCode, message))
    }

    // PAGInterstitialAdInteractionListener

    //This method is invoked when the ad is displayed, covering the device's screen.
    override fun onAdShowed() {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId")
        mListener?.onInterstitialAdOpened()
        mListener?.onInterstitialAdShowSucceeded()
    }

    //This method is invoked when the ad is clicked by the user.
    override fun onAdClicked() {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId")
        mListener?.onInterstitialAdClicked()
    }

    //This method is invoked when the ad disappears.
    override fun onAdDismissed() {
        IronLog.ADAPTER_CALLBACK.verbose("slotId = $mSlotId")
        mListener?.onInterstitialAdClosed()
    }
}