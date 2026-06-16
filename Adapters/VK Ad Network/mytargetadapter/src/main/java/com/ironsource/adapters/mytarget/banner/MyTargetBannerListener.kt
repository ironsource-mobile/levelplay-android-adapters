package com.ironsource.adapters.mytarget.banner

import android.widget.FrameLayout
import com.ironsource.adapters.mytarget.MyTargetConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.logger.IronLog
import com.my.target.ads.MyTargetView
import com.my.target.common.models.IAdLoadingError

class MyTargetBannerListener(
    private val listener: BannerAdListener,
    private val layoutParams: FrameLayout.LayoutParams
) : MyTargetView.MyTargetViewListener {

    /**
     * Called when the banner ad has been loaded and is ready to be displayed.
     * @param myTargetView - the banner view that was loaded.
     */
    override fun onLoad(myTargetView: MyTargetView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLoadSuccess(myTargetView, layoutParams)
    }

    /**
     * Called when the banner ad failed to load.
     * @param iAdLoadingError - the error details suggesting the cause of failure.
     * @param myTargetView - the banner view that failed to load.
     */
    override fun onNoAd(iAdLoadingError: IAdLoadingError, myTargetView: MyTargetView) {
        IronLog.ADAPTER_CALLBACK.error(MyTargetConstants.Logs.FAILED_TO_LOAD.format(iAdLoadingError.code, iAdLoadingError.message))
        listener.onAdLoadFailed(AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL, iAdLoadingError.code, iAdLoadingError.message)
    }

    /**
     * Called when the banner ad is shown (impression).
     * @param myTargetView - the banner view that was shown.
     */
    override fun onShow(myTargetView: MyTargetView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the user clicks on the banner ad.
     * @param myTargetView - the banner view that was clicked.
     */
    override fun onClick(myTargetView: MyTargetView) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }
}
