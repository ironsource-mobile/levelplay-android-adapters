package com.ironsource.adapters.moloco.rewardedvideo

import com.ironsource.adapters.moloco.MolocoAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener
import com.moloco.sdk.publisher.AdLoad
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError
import java.lang.ref.WeakReference

class MolocoRewardedVideoAdLoadListener(
    private val mListener: RewardedVideoSmashListener,
    private val mAdapter: WeakReference<MolocoRewardedVideoAdapter>
) : AdLoad.Listener {

    /**
     * Called when Ad was loaded and ready to be displayed
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdLoadSuccess(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onRewardedVideoAvailabilityChanged(true)
    }

    /**
     * Called when Ad failed to load
     *
     * @param molocoAdError - MolocoAdError with additional info about error
     */
    override fun onAdLoadFailed(molocoAdError: MolocoAdError) {
        val errorCode = MolocoAdError.ErrorType.AD_LOAD_FAILED.errorCode
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${errorCode}, errorMessage = ${molocoAdError.description}")
        mListener.onRewardedVideoAvailabilityChanged(false)
        mListener.onRewardedVideoLoadFailed(
            MolocoAdapter.getLoadErrorAndCheckNoFill(
                molocoAdError,
                IronSourceError.ERROR_RV_LOAD_NO_FILL
            )
        )
        mAdapter.get()?.destroyRewardedVideoAd()
    }

    }