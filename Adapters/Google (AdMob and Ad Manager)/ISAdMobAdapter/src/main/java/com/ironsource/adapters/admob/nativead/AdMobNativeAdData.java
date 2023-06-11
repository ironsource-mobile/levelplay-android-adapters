package com.ironsource.adapters.admob.nativead;

import com.google.android.gms.ads.nativead.NativeAd;
import com.ironsource.mediationsdk.ads.nativead.AdapterNativeAdData;
import com.ironsource.mediationsdk.logger.IronLog;

public class AdMobNativeAdData extends AdapterNativeAdData {

    private NativeAd mNativeAd;

    public AdMobNativeAdData(NativeAd nativeAd) {
        this.mNativeAd = nativeAd;
    }

    @Override
    public String getTitle() {
        IronLog.ADAPTER_CALLBACK.verbose("headline = " + mNativeAd.getHeadline());

        return mNativeAd.getHeadline();
    }

    @Override
    public String getAdvertiser() {
        IronLog.ADAPTER_CALLBACK.verbose("advertiser = " + mNativeAd.getAdvertiser());

        return mNativeAd.getAdvertiser();
    }

    @Override
    public String getBody() {
        IronLog.ADAPTER_CALLBACK.verbose("body = " + mNativeAd.getBody());
        return mNativeAd.getBody();
    }

    @Override
    public String getCallToAction() {
        IronLog.ADAPTER_CALLBACK.verbose("cta = " + mNativeAd.getCallToAction());

        return mNativeAd.getCallToAction();
    }

    @Override
    public Image getIcon() {
        NativeAd.Image icon = mNativeAd.getIcon();

        if (icon != null) {
            IronLog.ADAPTER_CALLBACK.verbose("icon uri = " + icon.getUri());
            return new Image(icon.getDrawable(), icon.getUri());
        }
        return null;
    }
}
