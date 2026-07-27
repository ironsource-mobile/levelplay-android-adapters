package com.ironsource.adapters.yso.rewarded

import android.view.View
import com.ironsource.adapters.yso.YSOConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.RewardedVideoAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import com.ysocorp.ysonetwork.YsoNetwork
import com.ysocorp.ysonetwork.enums.YNEnumActionError
import java.lang.ref.WeakReference

class YSORewardedListener(
    private val listener: RewardedVideoAdListener,
    private val adapter: WeakReference<YSORewardedAdapter>
) : YsoNetwork.ActionLoad, YsoNetwork.ActionDisplay {

    /**
     * Called when the ad finished loading or failed to load
     * @param error - YNEnumActionError.None on success, otherwise the load error
     */
    override fun onLoad(error: YNEnumActionError) {
        if (error == YNEnumActionError.None) {
            IronLog.ADAPTER_CALLBACK.verbose()
            adapter.get()?.setRewardedAdAvailability(true)
            listener.onAdLoadSuccess()
        } else {
            IronLog.ADAPTER_CALLBACK.error(YSOConstants.Logs.FAILED_TO_LOAD.format(error.ordinal, error.name))
            adapter.get()?.setRewardedAdAvailability(false)
            listener.onAdLoadFailed(AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL, error.ordinal, error.name)
        }
    }

    /**
     * Called when the ad is displayed
     * @param view - the ad view
     */
    override fun onDisplay(view: View?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the ad is clicked
     */
    override fun onClick() {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    /**
     * Called when the ad is closed
     * @param display - whether the ad was displayed
     * @param complete - whether the ad was watched to completion (eligible for reward)
     */
    override fun onClose(display: Boolean, complete: Boolean) {
        IronLog.ADAPTER_CALLBACK.verbose()
        if (!display) {
            IronLog.ADAPTER_CALLBACK.error(YSOConstants.Logs.AD_DISPLAY_FAILED)
            listener.onAdShowFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, YSOConstants.Logs.AD_DISPLAY_FAILED)
            return
        }
        if (complete) {
            listener.onAdRewarded()
        }
        listener.onAdClosed()
    }
}
