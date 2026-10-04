@file:Suppress("NOTHING_TO_OVERRIDE", "ABSTRACT_MEMBER_NOT_IMPLEMENTED", "ACCIDENTAL_OVERRIDE")

package com.ironsource.adapters.admob.nativead

import android.view.View
import android.view.ViewGroup
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.ironsource.adapters.admob.AdMobConstants
import com.ironsource.mediationsdk.adunit.adapter.internal.nativead.AdapterNativeAdViewBinder
import com.ironsource.mediationsdk.logger.IronLog

class AdMobNativeAdViewBinder(private val nativeAd: NativeAd) : AdapterNativeAdViewBinder() {

    private var adMobNativeAdView: NativeAdView? = null

    override fun setNativeAdView(nativeAdView: View?) {
        if (nativeAdView == null) {
            IronLog.INTERNAL.error(AdMobConstants.Logs.AD_VIEW_IS_NULL)
            return
        }

        val networkNativeAdView = NativeAdView(nativeAdView.context)
        networkNativeAdView.headlineView = nativeAdViewHolder.titleView
        networkNativeAdView.advertiserView = nativeAdViewHolder.advertiserView
        networkNativeAdView.iconView = nativeAdViewHolder.iconView
        networkNativeAdView.bodyView = nativeAdViewHolder.bodyView

        var adMobMediaView: MediaView? = null
        nativeAdViewHolder.mediaView?.let { levelPlayMediaView ->
            adMobMediaView = MediaView(levelPlayMediaView.context)
            levelPlayMediaView.addView(adMobMediaView)
        }

        networkNativeAdView.callToActionView = nativeAdViewHolder.callToActionView
        networkNativeAdView.addView(nativeAdView)
        networkNativeAdView.registerNativeAd(nativeAd, adMobMediaView)

        adMobNativeAdView = networkNativeAdView
    }

    override val networkNativeAdView: ViewGroup?
        get() = adMobNativeAdView
}
