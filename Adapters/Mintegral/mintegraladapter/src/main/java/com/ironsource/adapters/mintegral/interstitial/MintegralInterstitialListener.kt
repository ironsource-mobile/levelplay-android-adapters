package com.ironsource.adapters.mintegral.interstitial

import com.ironsource.adapters.mintegral.MintegralConstants
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.logger.IronLog
import com.mbridge.msdk.newinterstitial.out.MBBidNewInterstitialHandler
import com.mbridge.msdk.newinterstitial.out.NewInterstitialWithCodeListener
import com.mbridge.msdk.out.MBridgeIds
import com.mbridge.msdk.out.RewardInfo

class MintegralInterstitialListener(
    private val listener: InterstitialAdListener,
    private val interstitialHandler: MBBidNewInterstitialHandler
) : NewInterstitialWithCodeListener() {

    override fun onResourceLoadSuccess(bridgeIds: MBridgeIds?) {
        val creativeId = interstitialHandler.creativeIdWithUnitId
        IronLog.ADAPTER_CALLBACK.verbose(MintegralConstants.Logs.CREATIVE_ID.format(creativeId))

        if (creativeId.isNullOrEmpty()) {
            listener.onAdLoadSuccess()
        } else {
            val extraData: Map<String, Any> = mapOf(MintegralConstants.CREATIVE_ID_KEY to creativeId)
            listener.onAdLoadSuccess(extraData)
        }
    }

    override fun onLoadCampaignSuccess(bridgeIds: MBridgeIds?) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    override fun onResourceLoadFailWithCode(bridgeIds: MBridgeIds?, errorCode: Int, errorMsg: String?) {
        IronLog.ADAPTER_CALLBACK.verbose(MintegralConstants.Logs.ERROR_CODE_MSG.format(errorCode, errorMsg))

        val errorType = if (errorCode == MintegralConstants.MINTEGRAL_NO_FILL_ERROR_CODE) {
            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
        } else {
            AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
        }

        listener.onAdLoadFailed(errorType, errorCode, errorMsg)
    }

    override fun onAdShow(bridgeIds: MBridgeIds?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    override fun onAdClose(bridgeIds: MBridgeIds?, rewardInfo: RewardInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClosed()
    }

    override fun onShowFailWithCode(bridgeIds: MBridgeIds?, errorCode: Int, errorMsg: String?) {
        IronLog.ADAPTER_CALLBACK.verbose(MintegralConstants.Logs.ERROR_CODE_MSG.format(errorCode, errorMsg))
        listener.onAdShowFailed(errorCode, errorMsg)
    }

    override fun onAdClicked(bridgeIds: MBridgeIds?) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    override fun onVideoComplete(bridgeIds: MBridgeIds?) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    override fun onAdCloseWithNIReward(bridgeIds: MBridgeIds?, rewardInfo: RewardInfo?) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    override fun onEndcardShow(bridgeIds: MBridgeIds?) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }
}
