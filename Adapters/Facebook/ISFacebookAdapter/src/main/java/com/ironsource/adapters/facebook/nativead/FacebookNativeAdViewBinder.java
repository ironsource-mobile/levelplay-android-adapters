package com.ironsource.adapters.facebook.nativead;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.facebook.ads.AdOptionsView;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.ironsource.adapters.facebook.FacebookAdapter;
import com.ironsource.mediationsdk.ads.nativead.LevelPlayMediaView;
import com.ironsource.mediationsdk.ads.nativead.internal.NativeAdViewHolder;
import com.ironsource.mediationsdk.adunit.adapter.internal.nativead.AdapterNativeAdViewBinder;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdOptionsPosition;
import com.ironsource.mediationsdk.logger.IronLog;

import java.util.ArrayList;
import java.util.List;

public class FacebookNativeAdViewBinder extends AdapterNativeAdViewBinder {

    private final NativeAd mNativeAd;
    private NativeAdLayout mNativeAdLayout;
    private final AdOptionsPosition mAdOptionsPosition;

    public FacebookNativeAdViewBinder(NativeAd nativeAd, AdOptionsPosition adOptionsPosition) {
        mNativeAd = nativeAd;
        mAdOptionsPosition = adOptionsPosition;
    }

    @Override
    public void setNativeAdView(View nativeAdView) {
        if (nativeAdView == null) {
            IronLog.INTERNAL.error("nativeAdView is null");
            return;
        }
        Context context = nativeAdView.getContext();
        mNativeAdLayout = new NativeAdLayout(context);
        List<View> viewsToRegister = new ArrayList<>();

        NativeAdViewHolder nativeAdViewHolder = getNativeAdViewHolder();


        if (nativeAdViewHolder.getTitleView() != null)
            viewsToRegister.add(nativeAdViewHolder.getTitleView());
        if (nativeAdViewHolder.getAdvertiserView() != null)
            viewsToRegister.add(nativeAdViewHolder.getAdvertiserView());
        if (nativeAdViewHolder.getIconView() != null)
            viewsToRegister.add(nativeAdViewHolder.getIconView());
        if (nativeAdViewHolder.getBodyView() != null)
            viewsToRegister.add(nativeAdViewHolder.getBodyView());
        if (nativeAdViewHolder.getCallToActionView() != null)
            viewsToRegister.add(nativeAdViewHolder.getCallToActionView());

        FacebookAdapter.postOnUIThread(new Runnable() {
            @Override
            public void run() {
                LevelPlayMediaView levelPlayMediaView = nativeAdViewHolder.getMediaView();
                MediaView facebookMediaView = new MediaView(context);
                if (levelPlayMediaView != null) {
                    levelPlayMediaView.addView(facebookMediaView);
                }
                View adOptions = new AdOptionsView(context, mNativeAd, mNativeAdLayout);
                mNativeAdLayout.addView(adOptions, getAdOptionsLayoutParams());
                mNativeAdLayout.addView(nativeAdView);
                mNativeAd.registerViewForInteraction(nativeAdView, facebookMediaView, viewsToRegister);
            }
        });
    }

    private FrameLayout.LayoutParams getAdOptionsLayoutParams() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        switch (mAdOptionsPosition) {
            case TOP_LEFT:
                layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
                break;
            case TOP_RIGHT:
                layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
                break;
            case BOTTOM_LEFT:
                layoutParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
                break;
            default:
                layoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        }
        return layoutParams;
    }

    @Override
    public ViewGroup getNetworkNativeAdView() {
        return mNativeAdLayout;
    }

}
