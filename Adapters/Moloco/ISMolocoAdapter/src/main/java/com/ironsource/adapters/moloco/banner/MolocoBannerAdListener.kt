package com.ironsource.adapters.moloco.banner

import android.widget.FrameLayout
import com.ironsource.adapters.moloco.MolocoAdapter
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.BannerSmashListener
import com.moloco.sdk.publisher.AdLoad
import com.moloco.sdk.publisher.Banner
import com.moloco.sdk.publisher.BannerAdShowListener
import com.moloco.sdk.publisher.MolocoAd
import com.moloco.sdk.publisher.MolocoAdError

class MolocoBannerAdListener(
    private val mListener: BannerSmashListener,
    private val mLayoutParams: FrameLayout.LayoutParams,
    private val mBannerAdView: Banner?
    ) : AdLoad.Listener, BannerAdShowListener {

    /**
     * Called when Ad was loaded and ready to be displayed
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdLoadSuccess(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdLoaded(mBannerAdView, mLayoutParams)
    }

    /**
     * Called when Ad failed to load
     *
     * @param molocoAdError - MolocoAdError with additional info about error
     */
    override fun onAdLoadFailed(molocoAdError: MolocoAdError) {
        val errorCode = MolocoAdError.ErrorType.AD_LOAD_FAILED.errorCode
        IronLog.ADAPTER_CALLBACK.verbose("Failed to load, errorCode = ${errorCode}, errorMessage = ${molocoAdError.description}")
        val bannerError = MolocoAdapter.getLoadErrorAndCheckNoFill(
            molocoAdError,
            IronSourceError.ERROR_BN_LOAD_NO_FILL
        )
        mListener.onBannerAdLoadFailed(bannerError)
    }


    /**
     * Called when an ad starts displaying. Impression can be recorded.
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdShowSuccess(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdShown()
    }

    /**
     * Called when Ad show failed
     *
     * @param molocoAdError - MolocoAdError with additional info about error
     */
    override fun onAdShowFailed(molocoAdError: MolocoAdError) {
        val errorCode = MolocoAdError.ErrorType.AD_SHOW_ERROR
        IronLog.ADAPTER_CALLBACK.verbose("Failed to show, errorCode = ${errorCode}, errorMessage = ${molocoAdError.description}")
    }

    /**
     * Called when Ad has been clicked
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdClicked(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
        mListener.onBannerAdClicked()
    }

    /**
     * Called when an ad is hidden.
     *
     * @param molocoAd - MolocoAd instance
     */
    override fun onAdHidden(molocoAd: MolocoAd) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

}