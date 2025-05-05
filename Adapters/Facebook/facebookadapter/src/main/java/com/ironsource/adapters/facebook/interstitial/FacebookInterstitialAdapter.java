package com.ironsource.adapters.facebook.interstitial;

import android.text.TextUtils;


import androidx.annotation.NonNull;

import com.facebook.ads.CacheFlag;
import com.facebook.ads.InterstitialAd;
import com.ironsource.adapters.facebook.FacebookAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;

import org.json.JSONObject;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FacebookInterstitialAdapter extends AbstractInterstitialAdapter<FacebookAdapter> {

    private final ConcurrentHashMap<String, InterstitialSmashListener> mPlacementIdToSmashListener;
    private final ConcurrentHashMap<String, InterstitialAd> mPlacementIdToAd;
    private final ConcurrentHashMap<String, FacebookInterstitialAdListener> mPlacementIdToFacebookAdListener;
    protected ConcurrentHashMap<String, Boolean> mAdsAvailability;
    protected ConcurrentHashMap<String, Boolean> mPlacementIdToShowAttempts; //Show attempts per placement id, used to identify show errors


    public FacebookInterstitialAdapter(FacebookAdapter adapter) {
        super(adapter);

        mPlacementIdToSmashListener = new ConcurrentHashMap<>();
        mPlacementIdToAd = new ConcurrentHashMap<>();
        mPlacementIdToFacebookAdListener = new ConcurrentHashMap<>();
        mAdsAvailability = new ConcurrentHashMap<>();
        mPlacementIdToShowAttempts = new ConcurrentHashMap<>();
    }

    @Override
    public void initInterstitial(String appKey, String userId, @NonNull final JSONObject config,
                                 @NonNull final InterstitialSmashListener listener) {
        initInterstitialInternal(config, listener);
    }

    @Override
    public void initInterstitialForBidding(String appKey, String userId, @NonNull JSONObject config, @NonNull InterstitialSmashListener listener) {
        initInterstitialInternal(config, listener);
    }

    private void initInterstitialInternal(@NonNull JSONObject config, @NonNull InterstitialSmashListener listener) {
        final String placementIdKey = getAdapter().getPlacementIdKey();
        final String allPlacementIdsKey = getAdapter().getAllPlacementIdsKey();
        final String placementId = getConfigStringValueFromKey(config, placementIdKey);
        final String allPlacementIds = getConfigStringValueFromKey(config, allPlacementIdsKey);

        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(placementIdKey));
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(getAdUnitIdMissingErrorString(placementIdKey), IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(allPlacementIds)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(allPlacementIdsKey));
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(getAdUnitIdMissingErrorString(allPlacementIdsKey), IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId);


        // add to interstitial listener map
        mPlacementIdToSmashListener.put(placementId, listener);

        if (getAdapter().getInitState() == FacebookAdapter.InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose("onInterstitialInitSuccess - placementId = " + placementId);
            listener.onInterstitialInitSuccess();
        } else if (getAdapter().getInitState() == FacebookAdapter.InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.verbose("onInterstitialInitFailed - placementId = " + placementId);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Meta SDK init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT));
        } else {
            getAdapter().initSDK(allPlacementIds);
        }
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        for (InterstitialSmashListener listener : mPlacementIdToSmashListener.values()) {
            listener.onInterstitialInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        for (InterstitialSmashListener listener : mPlacementIdToSmashListener.values()) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    @Override
    public void loadInterstitial(@NonNull final JSONObject config, final JSONObject adData, @NonNull final InterstitialSmashListener listener) {
        loadInterstitialInternal(config, null, listener);
    }


    @Override
    public void loadInterstitialForBidding(@NonNull final JSONObject config, final JSONObject adData, final String serverData, @NonNull final InterstitialSmashListener listener) {
        loadInterstitialInternal(config, serverData, listener);
    }

    private void loadInterstitialInternal(@NonNull JSONObject config, final String serverData, @NonNull final InterstitialSmashListener listener) {
        final String placementIdKey = getAdapter().getPlacementIdKey();
        final String placementId = getConfigStringValueFromKey(config, placementIdKey);

        mPlacementIdToShowAttempts.put(placementId, false);
        mAdsAvailability.put(placementId, false);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    IronLog.ADAPTER_API.verbose("placementId = " + placementId);

                    if (mPlacementIdToAd.containsKey(placementId)) {
                        IronLog.ADAPTER_API.verbose("destroying previous ad with placementId " + placementId);
                        mPlacementIdToAd.get(placementId).destroy();
                        mPlacementIdToAd.remove(placementId);
                    }

                    InterstitialAd interstitialAd = new InterstitialAd(ContextProvider.getInstance().getApplicationContext(), placementId);

                    FacebookInterstitialAdListener interstitialAdListener = new FacebookInterstitialAdListener(FacebookInterstitialAdapter.this, placementId, listener);
                    mPlacementIdToFacebookAdListener.put(placementId, interstitialAdListener);

                    InterstitialAd.InterstitialAdLoadConfigBuilder configBuilder = interstitialAd.buildLoadAdConfig();
                    EnumSet<CacheFlag> cacheFlags = getAdapter().getCacheFlags();
                    configBuilder.withCacheFlags(cacheFlags);
                    configBuilder.withAdListener(interstitialAdListener);

                    if (!TextUtils.isEmpty(serverData)) {
                        // add server data to Interstitial bidder instance
                        configBuilder.withBid(serverData);
                    }

                    IronLog.ADAPTER_API.verbose("loading placementId = " + placementId + " with facebook cache flags = " + cacheFlags.toString());

                    interstitialAd.loadAd(configBuilder.build());
                    mPlacementIdToAd.put(placementId, interstitialAd);
                } catch (Exception e) {
                    listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError(e.getLocalizedMessage()));
                }
            }
        });
    }

    public void showInterstitial(@NonNull final JSONObject config,
                                 @NonNull final InterstitialSmashListener listener) {
        final String placementId = getConfigStringValueFromKey(config, getAdapter().getPlacementIdKey());
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        mAdsAvailability.put(placementId, false);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    InterstitialAd interstitialAd = mPlacementIdToAd.get(placementId);
                    // make sure the ad is loaded and has not expired
                    if (interstitialAd != null && interstitialAd.isAdLoaded() && !interstitialAd.isAdInvalidated()) {
                        mPlacementIdToShowAttempts.put(placementId, true);
                        interstitialAd.show();
                    } else {
                        listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
                    }
                } catch (Exception ex) {
                    IronLog.INTERNAL.error("ex.getMessage() = " + ex.getMessage());
                    listener.onInterstitialAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.INTERSTITIAL_AD_UNIT, ex.getMessage()));
                }
            }
        });


    }

    public final boolean isInterstitialReady(@NonNull final JSONObject config) {
        final String placementId = getConfigStringValueFromKey(config, getAdapter().getPlacementIdKey());
        return mAdsAvailability.containsKey(placementId) && Boolean.TRUE.equals(mAdsAvailability.get(placementId));

    }

    @Override
    public Map<String, Object> getInterstitialBiddingData(@NonNull JSONObject config, JSONObject adData) {
        return getAdapter().getBiddingData();
    }

    @Override
    public void releaseMemory(@NonNull IronSource.AD_UNIT adUnit, JSONObject config) {
        // release interstitial ads
        for (InterstitialAd interstitialAd : mPlacementIdToAd.values()) {
            interstitialAd.destroy();
        }
        mPlacementIdToAd.clear();
        mPlacementIdToFacebookAdListener.clear();
        mPlacementIdToSmashListener.clear();
        mAdsAvailability.clear();
        mPlacementIdToShowAttempts.clear();
    }
}
