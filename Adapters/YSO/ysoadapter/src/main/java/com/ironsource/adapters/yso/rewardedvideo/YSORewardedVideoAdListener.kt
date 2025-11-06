package com.ironsource.adapters.yso.rewardedvideo

import android.view.View
import com.ironsource.adapters.yso.YSOAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ysocorp.ysonetwork.YNManager
import com.ysocorp.ysonetwork.enums.YNEnumActionError
import java.lang.ref.WeakReference

class YSORewardedVideoAdListener (
  private val mListener: RewardedVideoSmashListener,
  private val mPlacementKey: String,
  private val mAdapter: WeakReference<YSORewardedVideoAdapter>,
) : YNManager.ActionLoad, YNManager.ActionDisplay {

  override fun onLoad(error: YNEnumActionError) {
    if (error == YNEnumActionError.None) {
      IronLog.ADAPTER_CALLBACK.verbose("placementKey = $mPlacementKey")
      mAdapter.get()?.setRewardedVideoAdAvailability(true)
      mListener.onRewardedVideoAvailabilityChanged(true)
    } else {
      IronLog.ADAPTER_CALLBACK.verbose("Failed to load, error = ${error.name}, placementKey = $mPlacementKey")
      mAdapter.get()?.setRewardedVideoAdAvailability(false)
      mListener.onRewardedVideoAvailabilityChanged(false)
      mListener.onRewardedVideoLoadFailed(YSOAdapter.getLoadError(error))
    }
  }

  override fun onDisplay(view: View?) {
    IronLog.ADAPTER_CALLBACK.verbose("placementKey = $mPlacementKey")
    mListener.onRewardedVideoAdOpened()
    mListener.onRewardedVideoAdStarted()
  }

  override fun onClick() {
    IronLog.ADAPTER_CALLBACK.verbose("placementKey = $mPlacementKey")
    mListener.onRewardedVideoAdClicked()
  }

  override fun onClose(display: Boolean, complete: Boolean) {
    IronLog.ADAPTER_CALLBACK.verbose("placementKey = $mPlacementKey")
    if(!display){
      IronLog.ADAPTER_CALLBACK.verbose("calling onRewardedVideoAdShowFailed for placementKey = $mPlacementKey")
      mListener.onRewardedVideoAdShowFailed(IronSourceError(IronSourceError.ERROR_CODE_GENERIC, "Rewarded Video ad failed to display"))
      return
    }
    if(complete){
      IronLog.ADAPTER_CALLBACK.verbose("calling onRewardedVideoAdRewarded for placementKey = $mPlacementKey")
      mListener.onRewardedVideoAdRewarded()
    }
    IronLog.ADAPTER_CALLBACK.verbose("calling onRewardedVideoAdClosed for placementKey = $mPlacementKey")
    mListener.onRewardedVideoAdClosed()
  }
}