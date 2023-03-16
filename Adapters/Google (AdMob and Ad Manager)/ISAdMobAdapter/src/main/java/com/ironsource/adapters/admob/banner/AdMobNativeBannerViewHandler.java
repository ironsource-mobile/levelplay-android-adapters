package com.ironsource.adapters.admob.banner;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.google.android.gms.ads.nativead.NativeAdView;
import com.ironsource.adapters.admob.R;
import com.ironsource.mediationsdk.AdapterUtils;
import com.ironsource.mediationsdk.ISBannerSize;

public class AdMobNativeBannerViewHandler {

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

    public AdMobNativeBannerViewHandler(ISBannerSize bannerSize, Context context) {

        TemplateType templateType = TemplateType.SMALL;

        switch (bannerSize.getDescription()) {
            case "BANNER":
            case "SMART":
                mLayoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(context, 320), AdapterUtils.dpToPixels(context, 50));
                mShouldHideCallToAction = true;
                break;
            case "LARGE":
                mLayoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(context, 320), AdapterUtils.dpToPixels(context, 90));
                break;
            case "RECTANGLE":
                mLayoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(context, 300), AdapterUtils.dpToPixels(context, 250));
                templateType = TemplateType.MEDIUM;
                mShouldHideVideoContent = false;
                break;
        }
        mLayoutParams.gravity = Gravity.CENTER;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAdView = (NativeAdView) inflater.inflate(templateType.getTemplateVal(), null);
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
