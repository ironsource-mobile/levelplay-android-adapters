package com.ironsource.adapters.admob.nativead;

import android.view.View;
import android.view.ViewGroup;

import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayMediaView;
import com.ironsource.mediationsdk.ads.nativead.internal.NativeAdViewHolder;
import com.ironsource.mediationsdk.adunit.adapter.internal.nativead.AdapterNativeAdViewBinder;
import com.ironsource.mediationsdk.logger.IronLog;

public class AdMobNativeAdViewBinder extends AdapterNativeAdViewBinder {

    private final NativeAd mNativeAd;
    private NativeAdView mNativeAdView;

    public AdMobNativeAdViewBinder(NativeAd nativeAd) {
        mNativeAd = nativeAd;
    }

    @Override
    public void setNativeAdView(View nativeAdView) {
        if (nativeAdView == null) {
            IronLog.INTERNAL.error("nativeAdView is null");
            return;
        }
        mNativeAdView = new NativeAdView(nativeAdView.getContext());

        NativeAdViewHolder nativeAdViewHolder = getNativeAdViewHolder();

        mNativeAdView.setHeadlineView(nativeAdViewHolder.getTitleView());
        mNativeAdView.setAdvertiserView(nativeAdViewHolder.getAdvertiserView());
        mNativeAdView.setIconView(nativeAdViewHolder.getIconView());
        mNativeAdView.setBodyView(nativeAdViewHolder.getBodyView());
        MediaView adMobMediaView = null;
        LevelPlayMediaView levelPlayMediaView = nativeAdViewHolder.getMediaView();
        if (levelPlayMediaView != null) {
            adMobMediaView = new MediaView(levelPlayMediaView.getContext());
            levelPlayMediaView.addView(adMobMediaView);
        }
        mNativeAdView.setCallToActionView(nativeAdViewHolder.getCallToActionView());

        mNativeAdView.addView(nativeAdView);
        mNativeAdView.registerNativeAd(mNativeAd, adMobMediaView);
    }

    @Override
    public ViewGroup getNetworkNativeAdView() {
        return mNativeAdView;
    }

}
