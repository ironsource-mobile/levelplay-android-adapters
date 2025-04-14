package com.ironsource.adapters.admob.banner;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.ironsource.adapters.admob.R;

public class AdMobNativeBannerViewBinder {

    private NativeAdView mAdView;
    private NativeAd mNativeAd;
    private NativeTemplateType mTemplateType;

    public void bindView(NativeAd nativeAd, NativeAdView nativeAdView, NativeTemplateType templateType) {

        mAdView = nativeAdView;
        mNativeAd = nativeAd;
        mTemplateType = templateType;
        populateView();
        mAdView.setNativeAd(nativeAd);
    }

    private void populateView() {
        populateIconView();
        populateHeadlineView();
        populateAdvertiserView();
        populateBodyView();
        populateMediaView();
        populateCallToActionView();
    }

    private void populateIconView() {

        ImageView iconView = mAdView.findViewById(R.id.ad_app_icon);
        if (iconView != null) {
            mAdView.setIconView(iconView);
            if (mNativeAd.getIcon() != null && mNativeAd.getIcon().getDrawable() != null) {
                iconView.setImageDrawable(mNativeAd.getIcon().getDrawable());
                mAdView.getIconView().setVisibility(VISIBLE);
            } else {
                mAdView.getIconView().setVisibility(GONE);
            }
        }
    }
    private void populateHeadlineView() {

        TextView headlineView = mAdView.findViewById(R.id.ad_headline);
        if (headlineView != null) {
            if (mNativeAd.getHeadline() != null) {
                mAdView.setHeadlineView(headlineView);
                headlineView.setText(mNativeAd.getHeadline());
                headlineView.setVisibility(VISIBLE);
            } else {
                headlineView.setVisibility(GONE);
            }
        }
    }

    private void populateAdvertiserView() {

        TextView advertiserView = mAdView.findViewById(R.id.ad_advertiser);
        if (advertiserView != null) {
            if (mNativeAd.getAdvertiser() != null) {
                mAdView.setAdvertiserView(advertiserView);
                advertiserView.setText(mNativeAd.getAdvertiser());
                advertiserView.setVisibility(VISIBLE);
            } else {
                advertiserView.setVisibility(GONE);
            }
        }

    }

    private void populateBodyView() {

        TextView bodyView = mAdView.findViewById(R.id.ad_body);
        if (bodyView != null) {
            if (mNativeAd.getBody() != null) {
                mAdView.setBodyView(bodyView);
                bodyView.setText(mNativeAd.getBody());
                bodyView.setVisibility(VISIBLE);
            } else {
                bodyView.setVisibility(GONE);
            }
        }
    }

    private void populateMediaView() {

        MediaView mediaView = mAdView.findViewById(R.id.ad_media);
        if (mediaView != null) {
            if (mNativeAd.getMediaContent() != null) {
                boolean shouldHideMedia = mNativeAd.getMediaContent().hasVideoContent() && mTemplateType.shouldHideVideoContent();
                mAdView.setMediaView(mediaView);
                mediaView.setMediaContent(mNativeAd.getMediaContent());
                mediaView.setVisibility(shouldHideMedia ? GONE : VISIBLE);
            } else {
                mediaView.setVisibility(GONE);
            }
        }
    }

    private void populateCallToActionView() {

        Button callToActionView = mAdView.findViewById(R.id.ad_call_to_action);
        if (callToActionView != null) {
            if (mNativeAd.getCallToAction() == null || mTemplateType.shouldHideCallToAction()) {
                callToActionView.setVisibility(GONE);
            } else {
                mAdView.setCallToActionView(callToActionView);
                callToActionView.setText(mNativeAd.getCallToAction());
                callToActionView.setVisibility(VISIBLE);
            }
        }
    }
}