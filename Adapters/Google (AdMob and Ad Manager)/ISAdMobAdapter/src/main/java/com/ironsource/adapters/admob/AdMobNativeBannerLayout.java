package com.ironsource.adapters.admob;

import android.app.Activity;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.google.android.gms.ads.nativead.NativeAdView;
import com.ironsource.mediationsdk.AdapterUtils;
import com.ironsource.mediationsdk.ISBannerSize;

public class AdMobNativeBannerLayout {

    enum TemplateType {

        SMALL (R.layout.ad_mob_native_banner_small_layout),
        MEDIUM(R.layout.ad_mob_native_banner_medium_layout);

        private final int val;
        TemplateType(int val) {
            this.val = val;
        }

        public int getTemplateVal() {
            return val;
        }
    }

    private FrameLayout.LayoutParams mLayoutParams;
    private boolean mShouldHideCallToAction = false;
    private boolean mShouldHideVideoContent = true;
    private final NativeAdView mAdView;

    public AdMobNativeBannerLayout(ISBannerSize bannerSize, Activity activity) {

        TemplateType templateType = TemplateType.SMALL;

        switch (bannerSize.getDescription()) {
            case "BANNER":
            case "SMART":
                mLayoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 320), AdapterUtils.dpToPixels(activity, 50));
                mShouldHideCallToAction = true;
                break;
            case "LARGE":
                mLayoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 320), AdapterUtils.dpToPixels(activity, 90));
                break;
            case "RECTANGLE":
                mLayoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 300), AdapterUtils.dpToPixels(activity, 250));
                templateType = TemplateType.MEDIUM;
                mShouldHideVideoContent = false;
                break;
        }
        mLayoutParams.gravity = Gravity.CENTER;
        mAdView = (NativeAdView) activity.getLayoutInflater().inflate(templateType.getTemplateVal(), null);
    }

    public FrameLayout.LayoutParams getLayoutParams() {
        return mLayoutParams;
    }

    public boolean shouldHideCallToAction() {
        return mShouldHideCallToAction;
    }

    public boolean shouldHideVideoContent() {
        return mShouldHideVideoContent;
    }

    public NativeAdView getNativeAdView() {
        return mAdView;
    }
}
