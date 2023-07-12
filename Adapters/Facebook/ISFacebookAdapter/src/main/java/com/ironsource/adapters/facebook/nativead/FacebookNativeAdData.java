package com.ironsource.adapters.facebook.nativead;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.facebook.ads.NativeAd;
import com.ironsource.mediationsdk.ads.nativead.AdapterNativeAdData;
import com.ironsource.mediationsdk.logger.IronLog;

public class FacebookNativeAdData extends AdapterNativeAdData {

    private Drawable mIconDrawable;
    private final NativeAd mNativeAd;

    protected FacebookNativeAdData(NativeAd nativeAd, Drawable iconDrawable) {
        mNativeAd = nativeAd;
        mIconDrawable = iconDrawable;
    }

    @Override
    public String getTitle() {
        IronLog.ADAPTER_CALLBACK.verbose("headline = " + mNativeAd.getAdHeadline());

        return mNativeAd.getAdHeadline();
    }

    @Override
    public String getAdvertiser() {
        IronLog.ADAPTER_CALLBACK.verbose("advertiser = " + mNativeAd.getAdvertiserName());

        return mNativeAd.getAdvertiserName();
    }

    @Override
    public String getBody() {
        IronLog.ADAPTER_CALLBACK.verbose("body = " + mNativeAd.getAdBodyText());

        return mNativeAd.getAdBodyText();
    }

    @Override
    public String getCallToAction() {
        IronLog.ADAPTER_CALLBACK.verbose("cta = " + mNativeAd.getAdCallToAction());

        return mNativeAd.getAdCallToAction();
    }

    @Override
    public Image getIcon() {
        Uri uri = mNativeAd.getAdIcon() != null ? Uri.parse(mNativeAd.getAdIcon().getUrl()) : null;
        IronLog.ADAPTER_CALLBACK.verbose("icon uri = " + uri);

        if (mNativeAd.getPreloadedIconViewDrawable() != null) {
            mIconDrawable = mNativeAd.getPreloadedIconViewDrawable();
        }
        return new Image(mIconDrawable, uri);
    }


}
