package com.ironsource.adapters.moloco.rewardedvideo

import com.ironsource.adapters.moloco.MolocoAdapter
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.ironsource.mediationsdk.utils.ErrorBuilder
import com.ironsource.mediationsdk.utils.IronSourceConstants
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.RewardedInterstitialAd
import org.json.JSONObject
import java.lang.ref.WeakReference

class MolocoRewardedVideoAdapter(adapter: MolocoAdapter) :
    AbstractRewardedVideoAdapter<MolocoAdapter>(adapter) {

    private var mListener : RewardedVideoSmashListener? = null
    private var mAdLoadListener : MolocoRewardedVideoAdLoadListener? = null
    private var mAdShowListener : MolocoRewardedVideoAdShowListener? = null
    private var mAd: RewardedInterstitialAd? = null

    override fun initRewardedVideoWithCallback(
        appKey: String?,
        userId: String?,
        config: JSONObject,
        listener: RewardedVideoSmashListener
    ) {
        val adUnitIdKey = MolocoAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)
        val appKey = getConfigStringValueFromKey(config, MolocoAdapter.getAppKey())

        if (adUnitId.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(adUnitId))
            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(adUnitId),
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        if (appKey.isEmpty()) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(appKey))
            listener.onRewardedVideoInitFailed(
                ErrorBuilder.buildInitFailedError(
                    getAdUnitIdMissingErrorString(appKey),
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
            return
        }

        IronLog.ADAPTER_API.verbose("adUnitId = $adUnitId, appKey = $appKey")

        //save rewarded video listener
        mListener = listener

        when (adapter.getInitState()) {
            MolocoAdapter.Companion.InitState.INIT_STATE_SUCCESS -> {
                listener.onRewardedVideoInitSuccess()
            }
            MolocoAdapter.Companion.InitState.INIT_STATE_FAILED -> {
                listener.onRewardedVideoInitFailed(
                    ErrorBuilder.buildInitFailedError(
                        "Moloco sdk init failed",
                        IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                    )
                )
            }
            MolocoAdapter.Companion.InitState.INIT_STATE_NONE,
            MolocoAdapter.Companion.InitState.INIT_STATE_IN_PROGRESS -> {
                adapter.initSdk(appKey)
            }
        }
    }

    override fun onNetworkInitCallbackSuccess() {
        mListener?.onRewardedVideoInitSuccess()
    }

    override fun onNetworkInitCallbackFailed(error: String?) {
        mListener?.onRewardedVideoInitFailed(
            ErrorBuilder.buildInitFailedError(
                error,
                IronSourceConstants.REWARDED_VIDEO_AD_UNIT
            )
        )
    }

    override fun loadRewardedVideoForBidding(
        config: JSONObject,
        adData: JSONObject?,
        serverData: String?,
        listener: RewardedVideoSmashListener
    ) {
        IronLog.ADAPTER_API.verbose()

        if (serverData.isNullOrEmpty()) {
            val error = "serverData is empty"
            IronLog.INTERNAL.error(error)
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildLoadFailedError(error))
            return
        }

        val rewardedVideoAdListener = MolocoRewardedVideoAdLoadListener(listener, WeakReference(this))
        mAdLoadListener = rewardedVideoAdListener
        val adUnitIdKey = MolocoAdapter.getAdUnitIdKey()
        val adUnitId = getConfigStringValueFromKey(config, adUnitIdKey)
        Moloco.createRewardedInterstitial(adUnitId) { rewardedAd, error ->
            if(error != null){
                mListener?.onRewardedVideoLoadFailed(
                    IronSourceError(error.errorCode,error.description)
                )
            } else {
                rewardedAd?.let { ad ->
                    mAd = ad
                    mAd?.load(serverData, mAdLoadListener)
                } ?: run {
                    listener.onRewardedVideoLoadFailed(
                        ErrorBuilder.buildLoadFailedError(
                            MolocoAdapter.INVALID_CONFIGURATION
                        )
                    )
                }
            }
        }
    }

    override fun showRewardedVideo(config: JSONObject, listener: RewardedVideoSmashListener) {
        IronLog.ADAPTER_API.verbose()

        if (!isRewardedVideoAvailable(config)) {
            listener.onRewardedVideoAdShowFailed(
                ErrorBuilder.buildNoAdsToShowError(
                    IronSourceConstants.REWARDED_VIDEO_AD_UNIT
                )
            )
        } else {
            val rewardedAdShowListener = MolocoRewardedVideoAdShowListener(listener, WeakReference(this))
            mAdShowListener = rewardedAdShowListener
            mAd?.show(mAdShowListener)
        }

    }

    override fun isRewardedVideoAvailable(config: JSONObject): Boolean {
        return mAd != null && mAd?.isLoaded == true
    }

    override fun collectRewardedVideoBiddingData(
        config: JSONObject,
        adData: JSONObject?,
        biddingDataCallback: BiddingDataCallback
    ) {
        adapter.collectBiddingData(biddingDataCallback)
    }

    //region memory handling

    override fun releaseMemory(adUnit: IronSource.AD_UNIT, config: JSONObject?) {
        IronLog.INTERNAL.verbose()
        destroyRewardedVideoAd()
        mAdLoadListener = null
        mAdShowListener = null
        mListener = null
        }

    //endregion

    // region Helpers

    internal fun destroyRewardedVideoAd() {
        mAd?.destroy()
        mAd = null
    }

    //endregion

}