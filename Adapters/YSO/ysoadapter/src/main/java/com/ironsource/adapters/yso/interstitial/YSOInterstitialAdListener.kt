package com.ironsource.adapters.yso.interstitial

import android.view.View
import com.ironsource.adapters.yso.YSOAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener
import com.ysocorp.ysonetwork.YNManager
import com.ysocorp.ysonetwork.enums.YNEnumActionError
import java.lang.ref.WeakReference

class YSOInterstitialAdListener (
  private val mListener: InterstitialSmashListener,
  private val mPlacementKey: String,
  private val mAdapter: WeakReference<YSOInterstitialAdapter>,
) : YNManager.ActionLoad, YNManager.ActionDisplay  {

  override fun onLoad(error: YNEnumActionError) {
    if (error == YNEnumActionError.None) {
      IronLog.ADAPTER_CALLBACK.verbose("placementKey = $mPlacementKey")
      mAdapter.get()?.setInterstitialAdAvailability(true)
      mListener.onInterstitialAdReady()
    } else {
      IronLog.ADAPTER_CALLBACK.verbose("Failed to load, error = ${error.name}, placementKey = $mPlacementKey")
      mAdapter.get()?.setInterstitialAdAvailability(false)
      mListener.onInterstitialAdLoadFailed(
         YSOAdapter.getLoadError(error)
      )
    }
  }

  override fun onDisplay(view: View?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementKey = $mPlacementKey")
    mListener.onInterstitialAdOpened()
    mListener.onInterstitialAdShowSucceeded()
  }

  override fun onClick() {
    IronLog.ADAPTER_CALLBACK.verbose("placementKey = $mPlacementKey")
    mListener.onInterstitialAdClicked()
  }

  override fun onClose(display: Boolean, complete: Boolean) {
    IronLog.ADAPTER_CALLBACK.verbose("placementKey = $mPlacementKey")
    if(!display){
      IronLog.ADAPTER_CALLBACK.verbose("calling onInterstitialAdShowFailed for placementKey = $mPlacementKey")
      mListener.onInterstitialAdShowFailed(
        IronSourceError(IronSourceError.ERROR_CODE_GENERIC, "Interstitial ad failed to display")
      )
      return
    }
    IronLog.ADAPTER_CALLBACK.verbose("calling onInterstitialAdClosed for placementKey = $mPlacementKey")
    mListener.onInterstitialAdClosed()
  }
}