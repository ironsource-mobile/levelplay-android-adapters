package com.ironsource.adapters.admob.interstitial;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adapter.AbstractInterstitialAdapter;
import com.ironsource.mediationsdk.bidding.BiddingDataCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class AdMobInterstitialAdapter extends AbstractInterstitialAdapter<AdMobAdapter> {

    private final ConcurrentHashMap<String, InterstitialSmashListener> mAdUnitIdToListener;
    private final ConcurrentHashMap<String, InterstitialAd> mAdUnitIdToAd;
    private final ConcurrentHashMap<String, Boolean> mAdUnitIdToAdsAvailability; //used to check if an ad is available

    public AdMobInterstitialAdapter(AdMobAdapter adapter) {
        super(adapter);

        mAdUnitIdToListener = new ConcurrentHashMap<>();
        mAdUnitIdToAd = new ConcurrentHashMap<>();
        mAdUnitIdToAdsAvailability = new ConcurrentHashMap<>();
    }

    public void initInterstitial(String appKey, String userId, @NonNull final JSONObject config,
                                 @NonNull final InterstitialSmashListener listener) {
        initInterstitialInternal(config, listener);
    }

    public void initInterstitialForBidding(String appKey, String userId, @NonNull final JSONObject config,
                                           @NonNull final InterstitialSmashListener listener) {
        initInterstitialInternal(config, listener);
    }

    private void initInterstitialInternal(@NonNull final JSONObject config, @NonNull final InterstitialSmashListener listener) {
        final String adUnitIdKey = getAdapter().getAdUnitIdKey();
        final String adUnitId = getConfigStringValueFromKey(config, adUnitIdKey);

        if (TextUtils.isEmpty(adUnitId)) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(getAdUnitIdMissingErrorString(adUnitIdKey), IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        //add to interstitial listener map
        mAdUnitIdToListener.put(adUnitId, listener);

        if (getAdapter().getInitState() == AdMobAdapter.InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose("onInterstitialInitSuccess - adUnitId = " + adUnitId);
            listener.onInterstitialInitSuccess();
        } else if (getAdapter().getInitState() == AdMobAdapter.InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.verbose("onInterstitialInitFailed - adUnitId = " + adUnitId);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("AdMob sdk init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT));
        } else {
            getAdapter().initSDK(config);
        }
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        for (InterstitialSmashListener listener : mAdUnitIdToListener.values()) {
            listener.onInterstitialInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        for (InterstitialSmashListener listener : mAdUnitIdToListener.values()) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    public void loadInterstitial(@NonNull final JSONObject config, final JSONObject adData,
                                 @NonNull final InterstitialSmashListener listener) {
        loadInterstitialInternal(config, adData, null, listener);
    }


    @Override
    public void loadInterstitialForBidding(@NonNull final JSONObject config, final JSONObject adData, final String serverData, @NonNull final InterstitialSmashListener listener) {
        loadInterstitialInternal(config, adData, serverData, listener);
    }

    private void loadInterstitialInternal(@NonNull final JSONObject config, final JSONObject adData, final String serverData, @NonNull final InterstitialSmashListener listener) {
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                final String adUnitId = getConfigStringValueFromKey(config, getAdapter().getAdUnitIdKey());
                IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

                // set the interstitial ad availability to false before attempting to load
                mAdUnitIdToAdsAvailability.put(adUnitId, false);
                AdRequest adRequest = getAdapter().createAdRequest(adData, serverData);
                AdMobInterstitialAdLoadListener adMobInterstitialAdLoadListener = new AdMobInterstitialAdLoadListener(AdMobInterstitialAdapter.this, adUnitId, listener);
                InterstitialAd.load(ContextProvider.getInstance().getApplicationContext(), adUnitId, adRequest, adMobInterstitialAdLoadListener);
            }
        });
    }

    @Override
    public void showInterstitial(@NonNull final JSONObject config,
                                 @NonNull final InterstitialSmashListener listener) {
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                final String adUnitId = getConfigStringValueFromKey(config, getAdapter().getAdUnitIdKey());
                IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
                InterstitialAd interstitialAd = getInterstitialAd(adUnitId);
                // Show the ad if it's ready.
                if (isInterstitialReadyForAdUnitId(adUnitId) && interstitialAd != null) {
                    AdMobInterstitialAdShowListener adMobInterstitialAdShowListener = new AdMobInterstitialAdShowListener(adUnitId, listener);
                    interstitialAd.setFullScreenContentCallback(adMobInterstitialAdShowListener);
                    interstitialAd.show(ContextProvider.getInstance().getCurrentActiveActivity());
                } else {
                    IronLog.ADAPTER_API.error("Ad not ready to display");
                    listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
                }


                //change interstitial availability to false
                mAdUnitIdToAdsAvailability.put(adUnitId, false);
            }
        });


    }

    public final boolean isInterstitialReady(@NonNull final JSONObject config) {
        final String adUnitId = getConfigStringValueFromKey(config, getAdapter().getAdUnitIdKey());
        return isInterstitialReadyForAdUnitId(adUnitId);
    }

    private boolean isInterstitialReadyForAdUnitId(final String adUnitId) {
        if (TextUtils.isEmpty(adUnitId)) {
            return false;
        }
        return mAdUnitIdToAdsAvailability.containsKey(adUnitId) && mAdUnitIdToAdsAvailability.get(adUnitId);
    }

    @Override
    public void releaseMemory(@NonNull IronSource.AD_UNIT adUnit, JSONObject config) {
        for (InterstitialAd interstitialAd : mAdUnitIdToAd.values()) {
            interstitialAd.setFullScreenContentCallback(null);
        }
        // clear interstitial maps
        mAdUnitIdToAd.clear();
        mAdUnitIdToListener.clear();
        mAdUnitIdToAdsAvailability.clear();
    }

    @Override
    public void collectInterstitialBiddingData(@NonNull JSONObject config, JSONObject adData, @NotNull BiddingDataCallback biddingDataCallback) {
        getAdapter().collectBiddingData(biddingDataCallback, AdFormat.INTERSTITIAL, null);
    }

    public void onInterstitialAdLoaded(String adUnitId, InterstitialAd interstitialAd) {
        //add interstitial ad to maps
        mAdUnitIdToAd.put(adUnitId, interstitialAd);
        mAdUnitIdToAdsAvailability.put(adUnitId, true);
    }

    private InterstitialAd getInterstitialAd(String adUnitId) {
        if (TextUtils.isEmpty(adUnitId) || !mAdUnitIdToAd.containsKey(adUnitId)) {
            return null;
        }
        return mAdUnitIdToAd.get(adUnitId);
    }
}
