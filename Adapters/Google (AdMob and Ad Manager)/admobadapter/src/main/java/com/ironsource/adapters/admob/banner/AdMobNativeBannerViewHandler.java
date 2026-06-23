package com.ironsource.adapters.admob.banner;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;
import com.ironsource.adapters.admob.R;
import com.ironsource.mediationsdk.AdapterUtils;
import com.ironsource.mediationsdk.ISBannerSize;

import org.json.JSONObject;

enum NativeTemplateType {

    NB_TMP_BASIC(R.layout.ad_mob_native_banner_template_basic_layout, true, true, AdChoicesPlacement.TOP_RIGHT, NativeAd.NativeMediaAspectRatio.ANY),
    NB_TMP_BASIC_LARGE(R.layout.ad_mob_native_banner_template_basic_layout, false, true, AdChoicesPlacement.TOP_RIGHT, NativeAd.NativeMediaAspectRatio.ANY),
    NB_TMP_ICON_TEXT(R.layout.ad_mob_native_banner_template_icon_text_layout, true, true, AdChoicesPlacement.TOP_RIGHT, NativeAd.NativeMediaAspectRatio.ANY),
    NB_TMP_TEXT_CTA(R.layout.ad_mob_native_banner_template_text_cta_layout, false, true, AdChoicesPlacement.BOTTOM_LEFT, NativeAd.NativeMediaAspectRatio.ANY),
    NB_TMP_RECT(R.layout.ad_mob_native_banner_template_rect_layout, false, false, AdChoicesPlacement.TOP_RIGHT, NativeAd.NativeMediaAspectRatio.ANY);

    private final int mLayoutId;
    private final boolean mHideCallToAction;
    private final boolean mHideVideoContent;
    private final AdChoicesPlacement mAdChoicesPlacement;
    private final NativeAd.NativeMediaAspectRatio mMediaAspectRatio;

    private static final String NATIVE_TEMPLATE_NAME = "nativeBannerTemplateName";

    NativeTemplateType(int layoutId, boolean hideCallToAction, boolean hideVideoContent, AdChoicesPlacement adChoicesPlacement, NativeAd.NativeMediaAspectRatio mediaAspectRatio) {
        this.mLayoutId = layoutId;
        this.mHideCallToAction = hideCallToAction;
        this.mHideVideoContent = hideVideoContent;
        this.mAdChoicesPlacement = adChoicesPlacement;
        this.mMediaAspectRatio = mediaAspectRatio;
    }

    public int getLayoutId() {
        return mLayoutId;
    }

    public boolean shouldHideCallToAction() {
        return mHideCallToAction;
    }

    public boolean shouldHideVideoContent() {
        return mHideVideoContent;
    }

    public AdChoicesPlacement getAdChoicesPlacement() {
        return mAdChoicesPlacement;
    }

    public NativeAd.NativeMediaAspectRatio getMediaAspectRatio() {
        return mMediaAspectRatio;
    }

    public static NativeTemplateType createTemplateType(JSONObject config, ISBannerSize bannerSize) {
        switch (bannerSize.getDescription()) {
            case "BANNER":
            case "SMART":
                String templateName = config.optString(NATIVE_TEMPLATE_NAME, NativeTemplateType.NB_TMP_ICON_TEXT.toString());
                try {
                    return NativeTemplateType.valueOf(templateName);
                } catch (IllegalArgumentException e) {
                    return NativeTemplateType.NB_TMP_ICON_TEXT;
                }
            case "LARGE":
                return NativeTemplateType.NB_TMP_BASIC_LARGE;
            case "RECTANGLE":
                return NativeTemplateType.NB_TMP_RECT;
            default:
                return NativeTemplateType.NB_TMP_BASIC;
        }
    }
}

public class AdMobNativeBannerViewHandler {

    private final NativeAdView mAdView;
    private FrameLayout.LayoutParams mLayoutParams;
    private NativeTemplateType mTemplateType;

    public AdMobNativeBannerViewHandler(ISBannerSize bannerSize, NativeTemplateType templateType, Context context) {

        mTemplateType = templateType;

        switch (bannerSize.getDescription()) {
            case "BANNER":
            case "SMART":
                mLayoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(context, 320), AdapterUtils.dpToPixels(context, 50));
                break;
            case "LARGE":
                mLayoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(context, 320), AdapterUtils.dpToPixels(context, 90));
                break;
            case "RECTANGLE":
                mLayoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(context, 300), AdapterUtils.dpToPixels(context, 250));
                break;
        }
        mLayoutParams.gravity = Gravity.CENTER;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAdView = (NativeAdView) inflater.inflate(mTemplateType.getLayoutId(), null);
    }

    public FrameLayout.LayoutParams getLayoutParams() {
        return mLayoutParams;
    }

    public NativeAdView getNativeAdView() {
        return mAdView;
    }
}
