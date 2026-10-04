@file:Suppress("NOTHING_TO_OVERRIDE", "ABSTRACT_MEMBER_NOT_IMPLEMENTED", "ACCIDENTAL_OVERRIDE")

package com.ironsource.adapters.facebook.nativead

import android.graphics.drawable.Drawable
import android.net.Uri
import com.facebook.ads.NativeAd
import com.ironsource.adapters.facebook.FacebookConstants
import com.ironsource.mediationsdk.ads.nativead.AdapterNativeAdData
import com.ironsource.mediationsdk.ads.nativead.interfaces.NativeAdDataInterface.Image
import com.ironsource.mediationsdk.logger.IronLog

class FacebookNativeAdData(
    private val nativeAd: NativeAd,
    private var iconDrawable: Drawable?
) : AdapterNativeAdData() {

    override val title: String?
        get() {
            IronLog.ADAPTER_CALLBACK.verbose(FacebookConstants.Logs.NATIVE_TITLE.format(nativeAd.adHeadline))
            return nativeAd.adHeadline
        }

    override val advertiser: String?
        get() {
            IronLog.ADAPTER_CALLBACK.verbose(FacebookConstants.Logs.NATIVE_ADVERTISER.format(nativeAd.advertiserName))
            return nativeAd.advertiserName
        }

    override val body: String?
        get() {
            IronLog.ADAPTER_CALLBACK.verbose(FacebookConstants.Logs.NATIVE_BODY.format(nativeAd.adBodyText))
            return nativeAd.adBodyText
        }

    override val callToAction: String?
        get() {
            IronLog.ADAPTER_CALLBACK.verbose(FacebookConstants.Logs.NATIVE_CTA.format(nativeAd.adCallToAction))
            return nativeAd.adCallToAction
        }

    override val icon: Image?
        get() {
            val uri = nativeAd.adIcon?.url?.let { Uri.parse(it) }
            IronLog.ADAPTER_CALLBACK.verbose(FacebookConstants.Logs.NATIVE_ICON_URI.format(uri))

            nativeAd.preloadedIconViewDrawable?.let { iconDrawable = it }
            return Image(iconDrawable, uri)
        }
}
