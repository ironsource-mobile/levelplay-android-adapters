@file:Suppress("NOTHING_TO_OVERRIDE", "ABSTRACT_MEMBER_NOT_IMPLEMENTED", "ACCIDENTAL_OVERRIDE")

package com.ironsource.adapters.facebook.nativead

import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.facebook.ads.AdOptionsView
import com.facebook.ads.MediaView
import com.facebook.ads.NativeAd
import com.facebook.ads.NativeAdLayout
import com.ironsource.adapters.facebook.FacebookConstants
import com.ironsource.mediationsdk.adunit.adapter.internal.nativead.AdapterNativeAdViewBinder
import com.ironsource.mediationsdk.adunit.adapter.utility.AdOptionsPosition
import com.ironsource.mediationsdk.logger.IronLog

class FacebookNativeAdViewBinder(
    private val nativeAd: NativeAd,
    private val adOptionsPosition: AdOptionsPosition
) : AdapterNativeAdViewBinder() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var nativeAdLayout: NativeAdLayout? = null

    override val networkNativeAdView: ViewGroup?
        get() = nativeAdLayout

    override fun setNativeAdView(nativeAdView: View?) {
        if (nativeAdView == null) {
            IronLog.INTERNAL.error(FacebookConstants.Logs.NATIVE_AD_VIEW_IS_NULL)
            return
        }

        val context = nativeAdView.context
        val nativeAdLayout = NativeAdLayout(context)
        this.nativeAdLayout = nativeAdLayout

        val viewsToRegister = ArrayList<View>()
        nativeAdViewHolder.titleView?.let { viewsToRegister.add(it) }
        nativeAdViewHolder.advertiserView?.let { viewsToRegister.add(it) }
        nativeAdViewHolder.iconView?.let { viewsToRegister.add(it) }
        nativeAdViewHolder.bodyView?.let { viewsToRegister.add(it) }
        nativeAdViewHolder.callToActionView?.let { viewsToRegister.add(it) }

        mainHandler.post {
            val facebookMediaView = MediaView(context)
            nativeAdViewHolder.mediaView?.addView(facebookMediaView)
            val adOptions = AdOptionsView(context, nativeAd, nativeAdLayout)
            nativeAdLayout.addView(adOptions, getAdOptionsLayoutParams())
            nativeAdLayout.addView(nativeAdView)
            nativeAd.registerViewForInteraction(nativeAdView, facebookMediaView, viewsToRegister)
        }
    }

    private fun getAdOptionsLayoutParams(): FrameLayout.LayoutParams {
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = when (adOptionsPosition) {
            AdOptionsPosition.TOP_LEFT -> Gravity.TOP or Gravity.LEFT
            AdOptionsPosition.TOP_RIGHT -> Gravity.TOP or Gravity.RIGHT
            AdOptionsPosition.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.LEFT
            AdOptionsPosition.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.RIGHT
        }
        return layoutParams
    }
}
