package com.ironsource.adapters.facebook.nativead;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdBase;
import com.facebook.ads.NativeAdListener;
import com.ironsource.adapters.facebook.FacebookAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adapter.AbstractNativeAdAdapter;
import com.ironsource.mediationsdk.adunit.adapter.utility.NativeAdProperties;
import com.ironsource.mediationsdk.ads.nativead.interfaces.NativeAdSmashListener;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Map;

public class FacebookNativeAdAdapter extends AbstractNativeAdAdapter<FacebookAdapter> {

    private WeakReference<NativeAd> mAd;
    private NativeAdSmashListener mSmashListener;

    public FacebookNativeAdAdapter(FacebookAdapter adapter) {
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

    private void initNativeAdsInternal(@NonNull JSONObject config, @NonNull final NativeAdSmashListener listener) {
        final String placementIdKey = getAdapter().getPlacementIdKey();
        final String allPlacementIdsKey = getAdapter().getAllPlacementIdsKey();
        final String placementId = getConfigStringValueFromKey(config, placementIdKey);
        final String allPlacementIds = getConfigStringValueFromKey(config, allPlacementIdsKey);

        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(placementIdKey));
            IronSourceError error = ErrorBuilder.buildInitFailedError(getAdUnitIdMissingErrorString(placementIdKey), IronSourceConstants.NATIVE_AD_UNIT);
            listener.onNativeAdInitFailed(error);
            return;
        }

        if (TextUtils.isEmpty(allPlacementIds)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(allPlacementIdsKey));
            IronSourceError error = ErrorBuilder.buildInitFailedError(getAdUnitIdMissingErrorString(allPlacementIdsKey), IronSourceConstants.NATIVE_AD_UNIT);
            listener.onNativeAdInitFailed(error);
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        mSmashListener = listener;

        if (getAdapter().getInitState() == FacebookAdapter.InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose("onNativeAdInitSuccess - placementId = " + placementId);
            listener.onNativeAdInitSuccess();
        } else if (getAdapter().getInitState() == FacebookAdapter.InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.verbose("onNativeAdInitFailed - placementId = " + placementId);
            listener.onNativeAdInitFailed(ErrorBuilder.buildInitFailedError("Meta SDK init failed", IronSourceConstants.NATIVE_AD_UNIT));
        } else {
            getAdapter().initSDK(allPlacementIds);
        }
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

    @Override
    public void loadNativeAd(@NonNull JSONObject config, JSONObject adData, @NonNull NativeAdSmashListener listener) {
        loadNativeAdInternal(config, null, listener);
    }

    @Override
    public void loadNativeAdForBidding(@NonNull JSONObject config, JSONObject adData, String serverData, @NonNull NativeAdSmashListener listener) {
        loadNativeAdInternal(config, serverData, listener);
    }

    private void loadNativeAdInternal(@NonNull final JSONObject config, final String serverData, @NonNull final NativeAdSmashListener listener) {
        final String placementId = getConfigStringValueFromKey(config, getAdapter().getPlacementIdKey());
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        final NativeAdProperties nativeAdProperties = getNativeAdProperties(config);
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {

                    final Context context = ContextProvider.getInstance().getApplicationContext();
                    final NativeAd nativeAd = new NativeAd(context, placementId);

                    NativeAdListener facebookNativeAdListener = new FacebookNativeAdListener(context, placementId, nativeAdProperties.getAdOptionsPosition(), listener);

                    NativeAdBase.NativeAdLoadConfigBuilder nativeLoadAdConfigBuilder = nativeAd.buildLoadAdConfig().withAdListener(facebookNativeAdListener);

                    if (serverData != null) {
                        nativeLoadAdConfigBuilder.withBid(serverData);
                    }

                    mAd = new WeakReference<>(nativeAd);
                    nativeAd.loadAd(nativeLoadAdConfigBuilder.build());
                } catch (Exception e) {
                    IronSourceError error = ErrorBuilder.buildLoadFailedError("Meta loadNative exception " + e.getMessage());
                    listener.onNativeAdLoadFailed(error);
                }
            }
        });
    }

    @Override
    public void destroyNativeAd(@NonNull JSONObject config) {
        final String placementId = getConfigStringValueFromKey(config, getAdapter().getPlacementIdKey());
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {

                if (mAd == null || mAd.get() == null) {
                    return;
                }

                try {
                    NativeAd nativeAd = mAd.get();
                    nativeAd.destroy();
                    mAd = null;
                } catch (Exception e) {
                    IronLog.INTERNAL.error("destroyNativeAd failed for placementId - " + placementId + " with an exception = " + e);
                }
            }
        });
    }

    // release memory will not be called for native ads at the moment
    @Override
    public void releaseMemory(@NonNull IronSource.AD_UNIT adUnit, JSONObject config) {
    }

    @Override
    public Map<String, Object> getNativeAdBiddingData(@NonNull JSONObject config, JSONObject adData) {
        return getAdapter().getBiddingData();
    }
}
