package com.ironsource.adapters.facebook.nativead

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.NativeAd
import com.facebook.ads.NativeAdListener
import com.ironsource.adapters.facebook.FacebookAdapter
import com.ironsource.adapters.facebook.FacebookConstants
import com.ironsource.environment.workerthread.WorkerManager
import com.ironsource.environment.workerthread.WorkerResult
import com.ironsource.mediationsdk.adunit.adapter.listener.NativeAdListener as LevelPlayNativeAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdOptionsPosition
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.logger.IronLog
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FacebookNativeAdListener(
    private val context: Context,
    private val adOptionsPosition: AdOptionsPosition,
    private val listener: LevelPlayNativeAdListener
) : NativeAdListener {

    private companion object {
        private const val ICON_DOWNLOAD_TIMEOUT_SECONDS = 3L
    }

    /**
     * Called when the native ad media was downloaded
     */
    override fun onMediaDownloaded(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
    }

    /**
     * Called when the native ad was loaded successfully
     * @param ad the loaded ad instance
     */
    override fun onAdLoaded(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        if (ad is NativeAd) {
            ad.unregisterView()
            downloadIconAndSendLoadSuccess(ad)
        } else {
            val errorMessage = FacebookConstants.Logs.NATIVE_AD_TYPE_MISMATCH.format(ad.javaClass.name)
            IronLog.INTERNAL.error(errorMessage)
            listener.onAdLoadFailed(
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL,
                AdapterErrors.ADAPTER_ERROR_INTERNAL,
                errorMessage
            )
        }
    }

    /**
     * Called when the native ad failed to load
     * @param ad the ad instance
     * @param adError the error describing the failure
     */
    override fun onError(ad: Ad, adError: AdError) {
        IronLog.ADAPTER_CALLBACK.error(FacebookConstants.Logs.FAILED_TO_LOAD.format(adError.errorCode, adError.errorMessage))
        listener.onAdLoadFailed(FacebookAdapter.getLoadErrorType(adError), adError.errorCode, adError.errorMessage)
    }

    /**
     * Called when an impression is recorded for the native ad
     */
    override fun onLoggingImpression(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdOpened()
    }

    /**
     * Called when the native ad was clicked
     */
    override fun onAdClicked(ad: Ad) {
        IronLog.ADAPTER_CALLBACK.verbose()
        listener.onAdClicked()
    }

    private fun downloadIconAndSendLoadSuccess(nativeAd: NativeAd) {
        val workerManager = WorkerManager<Drawable>(Executors.newSingleThreadExecutor())
        workerManager.addCallable(Callable {
            val iconUrl = nativeAd.adIcon?.url
            if (!iconUrl.isNullOrEmpty()) {
                URL(iconUrl).openStream().use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    return@Callable BitmapDrawable(context.resources, bitmap)
                }
            }
            null
        })

        workerManager.startWork(object : WorkerManager.WorkEndedListener<Drawable> {
            override fun onWorkCompleted(responsesList: List<WorkerResult<Drawable>>, totalWorkDurationMillis: Long) {
                val response = responsesList[0]
                val drawable = (response as? WorkerResult.Completed<Drawable>)?.data
                sendLoadSuccess(nativeAd, drawable)
            }

            override fun onWorkFailed(error: String) {
                IronLog.INTERNAL.verbose(FacebookConstants.Logs.NATIVE_ICON_DOWNLOAD_FAILED.format(error))
                sendLoadSuccess(nativeAd, null)
            }
        }, ICON_DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun sendLoadSuccess(nativeAd: NativeAd, iconDrawable: Drawable?) {
        val adapterNativeAdData = FacebookNativeAdData(nativeAd, iconDrawable)
        val nativeAdViewBinder = FacebookNativeAdViewBinder(nativeAd, adOptionsPosition)
        listener.onAdLoadSuccess(adapterNativeAdData, nativeAdViewBinder)
    }
}
