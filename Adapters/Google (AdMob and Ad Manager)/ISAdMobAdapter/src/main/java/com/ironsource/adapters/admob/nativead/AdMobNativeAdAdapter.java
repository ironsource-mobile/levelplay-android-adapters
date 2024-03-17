package com.ironsource.adapters.admob.nativead;

import static com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_BOTTOM_LEFT;
import static com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_BOTTOM_RIGHT;
import static com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_LEFT;
import static com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_RIGHT;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adapter.AbstractNativeAdAdapter;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdOptionsPosition;
import com.ironsource.mediationsdk.adunit.adapter.utility.NativeAdProperties;
import com.ironsource.mediationsdk.ads.nativead.interfaces.NativeAdSmashListener;
import com.ironsource.mediationsdk.bidding.BiddingDataCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class AdMobNativeAdAdapter extends AbstractNativeAdAdapter<AdMobAdapter> {

    protected WeakReference<NativeAd> mAd;
    private NativeAdSmashListener mSmashListener;

    public AdMobNativeAdAdapter(AdMobAdapter adapter) {
        super(adapter);
    }

    @Override
    public void initNativeAds(String appKey, String userId, @NonNull JSONObject config, @NonNull NativeAdSmashListener listener) {
        initNativeAdsInternal(config, listener);
    }

    @Override
    public void initNativeAdForBidding(String appKey, String userId, @NonNull JSONObject config, @NonNull NativeAdSmashListener listener) {
        initNativeAdsInternal(config, listener);
    }

    public void initNativeAdsInternal(@NonNull final JSONObject config, @NonNull final NativeAdSmashListener listener) {
        final String adUnitIdKey = getAdapter().getAdUnitIdKey();
        final String adUnitId = getConfigStringValueFromKey(config, adUnitIdKey);

        if (TextUtils.isEmpty(adUnitId)) {
            IronSourceError error = ErrorBuilder.buildInitFailedError(getAdUnitIdMissingErrorString(adUnitIdKey), IronSourceConstants.NATIVE_AD_UNIT);
            listener.onNativeAdInitFailed(error);
            return;
        }

        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
        mSmashListener = listener;

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (getAdapter().getInitState() == AdMobAdapter.InitState.INIT_STATE_SUCCESS) {
                    IronLog.INTERNAL.verbose("onNativeAdInitSuccess - adUnitId = " + adUnitId);
                    listener.onNativeAdInitSuccess();
                } else if (getAdapter().getInitState() == AdMobAdapter.InitState.INIT_STATE_FAILED) {
                    IronLog.INTERNAL.verbose("onNativeAdInitFailed - adUnitId = " + adUnitId);
                    listener.onNativeAdInitFailed(ErrorBuilder.buildInitFailedError("AdMob sdk init failed", IronSourceConstants.NATIVE_AD_UNIT));
                } else {
                    getAdapter().initSDK(config);
                }
            }
        });
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        if (mSmashListener != null) {
            mSmashListener.onNativeAdInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        if (mSmashListener != null) {
            mSmashListener.onNativeAdInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.NATIVE_AD_UNIT));
        }
    }

    public void loadNativeAd(@NonNull final JSONObject config, final JSONObject adData, @NonNull final NativeAdSmashListener listener) {
        loadNativeAdInternal(config, adData, null, listener);
    }

    @Override
    public void loadNativeAdForBidding(@NonNull JSONObject config, JSONObject adData, String serverData, @NonNull NativeAdSmashListener listener) {
        loadNativeAdInternal(config, adData, serverData, listener);
    }

    public void loadNativeAdInternal(@NonNull final JSONObject config, final JSONObject adData, final String serverData, @NonNull final NativeAdSmashListener listener) {
        final String adUnitIdKey = getAdapter().getAdUnitIdKey();
        final String adUnitId = getConfigStringValueFromKey(config, adUnitIdKey);

        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
        final NativeAdProperties nativeAdProperties = getNativeAdProperties(config);
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    AdOptionsPosition adOptionsPosition = nativeAdProperties.getAdOptionsPosition();
                    final AdMobNativeAdListener adMobNativeAdListener = new AdMobNativeAdListener(AdMobNativeAdAdapter.this, adUnitId, listener);
                    AdLoader adLoader = new AdLoader.Builder(ContextProvider.getInstance().getApplicationContext(), adUnitId)
                            .forNativeAd(adMobNativeAdListener)
                            .withAdListener(adMobNativeAdListener)
                            .withNativeAdOptions(new NativeAdOptions.Builder()
                                    .setAdChoicesPlacement(getAdChoicesPosition(adOptionsPosition))
                                    .build())
                            .build();
                    adLoader.loadAd(getAdapter().createAdRequest(adData, serverData));
                } catch (Exception e) {
                    IronSourceError error = ErrorBuilder.buildLoadFailedError("AdMobAdapter loadNativeAd exception " + e.getMessage());
                    listener.onNativeAdLoadFailed(error);
                }
            }
        });
    }

    @Override
    public void destroyNativeAd(@NonNull JSONObject config) {
        final String adUnitId = getConfigStringValueFromKey(config, getAdapter().getAdUnitIdKey());
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {

                if (mAd == null || mAd.get() == null) {
                    return;
                }

                try {
                    mSmashListener = null;
                    NativeAd nativeAd = mAd.get();
                    nativeAd.destroy();
                    mAd = null;

                } catch (Exception e) {
                    IronLog.INTERNAL.error("destroyNativeAd failed for adUnitId - " + adUnitId + " with an exception = " + e);
                }
            }
        });
    }

    @Override
    public void collectNativeAdBiddingData(@NonNull JSONObject config, JSONObject adData, @NonNull BiddingDataCallback biddingDataCallback) {
        getAdapter().collectBiddingData(biddingDataCallback, AdFormat.NATIVE, null);
    }

    private int getAdChoicesPosition(AdOptionsPosition adOptionsPosition) {
        switch (adOptionsPosition) {
            case TOP_LEFT:
                return ADCHOICES_TOP_LEFT;
            case TOP_RIGHT:
                return ADCHOICES_TOP_RIGHT;
            case BOTTOM_LEFT:
                return ADCHOICES_BOTTOM_LEFT;
            case BOTTOM_RIGHT:
                return ADCHOICES_BOTTOM_RIGHT;
        }
        return ADCHOICES_BOTTOM_LEFT;
    }

    // release memory will not be called for native ads at the moment
    @Override
    public void releaseMemory(@NonNull IronSource.AD_UNIT adUnit, JSONObject config) {
    }
}
