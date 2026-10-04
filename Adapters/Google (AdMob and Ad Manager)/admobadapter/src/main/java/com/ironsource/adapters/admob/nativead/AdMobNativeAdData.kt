@file:Suppress("NOTHING_TO_OVERRIDE", "ABSTRACT_MEMBER_NOT_IMPLEMENTED", "ACCIDENTAL_OVERRIDE")

package com.ironsource.adapters.admob.nativead

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.ironsource.mediationsdk.ads.nativead.AdapterNativeAdData
import com.ironsource.mediationsdk.ads.nativead.interfaces.NativeAdDataInterface

class AdMobNativeAdData(private val nativeAd: NativeAd) : AdapterNativeAdData() {

    override val title: String?
        get() = nativeAd.headline

    override val advertiser: String?
        get() = nativeAd.advertiser

    override val body: String?
        get() = nativeAd.body

    override val callToAction: String?
        get() = nativeAd.callToAction

    override val icon: NativeAdDataInterface.Image?
        get() = nativeAd.icon?.let { NativeAdDataInterface.Image(it.drawable, it.uri) }
}
