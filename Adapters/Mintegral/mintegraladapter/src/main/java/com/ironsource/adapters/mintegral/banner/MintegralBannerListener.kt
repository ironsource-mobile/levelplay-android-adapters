package com.ironsource.adapters.mintegral.banner

import android.widget.FrameLayout
import com.ironsource.adapters.mintegral.MintegralConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.BannerAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.logger.IronLog
import com.mbridge.msdk.out.BannerAdWithCodeListener
import com.mbridge.msdk.out.MBBannerView
import com.mbridge.msdk.out.MBridgeIds

class MintegralBannerListener(
    private val listener: BannerAdListener,
    private val bannerView: MBBannerView,
    private val layoutParams: FrameLayout.LayoutParams
) : BannerAdWithCodeListener() {

    override fun onLoadSuccessed(bridgeIds: MBridgeIds?) {
        val creativeId = bannerView.creativeIdWithUnitId
        IronLog.ADAPTER_CALLBACK.verbose(MintegralConstants.Logs.CREATIVE_ID.format(creativeId))

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess(bannerView, layoutParams)
        } else {
            val extraData: Map<String, Any> = mapOf(MintegralConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(bannerView, layoutParams, extraData)
        }
    }

    override fun onLoadFailedWithCode(bridgeIds: MBridgeIds?, errorCode: Int, errorMsg: String?) {
        IronLog.ADAPTER_CALLBACK.verbose(MintegralConstants.Logs.ERROR_CODE_MSG.format(errorCode, errorMsg))

        val errorType = if (errorCode == MintegralConstants.MINTEGRAL_NO_FILL_ERROR_CODE) {
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
        } else {
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
        }

        listener.onAdLoadFailed(errorType, errorCode, errorMsg)
    }

    override fun onLogImpression(bridgeIds: MBridgeIds?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    override fun onClick(bridgeIds: MBridgeIds?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    override fun onLeaveApp(bridgeIds: MBridgeIds?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdLeftApplication()
    }

    override fun showFullScreen(bridgeIds: MBridgeIds?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenPresented()
    }

    override fun closeFullScreen(bridgeIds: MBridgeIds?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdScreenDismissed()
    }

    override fun onCloseBanner(bridgeIds: MBridgeIds?) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}
