package com.ironsource.adapters.admob.rewardedvideo;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.ironsource.adapters.admob.AdMobAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter;
import com.ironsource.mediationsdk.bidding.BiddingDataCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class AdMobRewardedVideoAdapter extends AbstractRewardedVideoAdapter<AdMobAdapter> {

    private final ConcurrentHashMap<String, RewardedVideoSmashListener> mAdUnitIdToListener;
    private final ConcurrentHashMap<String, JSONObject> mAdUnitIdToAdData;
    private final ConcurrentHashMap<String, RewardedAd> mAdUnitIdToAd;
    private final ConcurrentHashMap<String, Boolean> mAdUnitIdToAdsAvailability; //used to check if an ad is available
    private final CopyOnWriteArraySet<String> mAdUnitIdsForInitCallbacks;

    public AdMobRewardedVideoAdapter(AdMobAdapter adapter) {
        super(adapter);

        mAdUnitIdToListener = new ConcurrentHashMap<>();
        mAdUnitIdToAdData = new ConcurrentHashMap<>();
        mAdUnitIdToAd = new ConcurrentHashMap<>();
        mAdUnitIdToAdsAvailability = new ConcurrentHashMap<>();
        mAdUnitIdsForInitCallbacks = new CopyOnWriteArraySet<>();

    }

    @Override
    public void initAndLoadRewardedVideo(String appKey, String userId, @NonNull final JSONObject config, final JSONObject adData, @NonNull final RewardedVideoSmashListener listener) {

        final String adUnitIdKey = getAdapter().getAdUnitIdKey();
        final String adUnitId = getConfigStringValueFromKey(config, adUnitIdKey);

        if (TextUtils.isEmpty(adUnitId)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(adUnitIdKey));
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        //add to rewarded video listener map
        mAdUnitIdToListener.put(adUnitId, listener);

        if (getAdapter().getInitState() == AdMobAdapter.InitState.INIT_STATE_SUCCESS) {
            loadRewardedVideoAdInternal(adUnitId, adData, null, listener);
        } else if (getAdapter().getInitState() == AdMobAdapter.InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.verbose("onRewardedVideoAvailabilityChanged(false) - adUnitId = " + adUnitId);
            listener.onRewardedVideoAvailabilityChanged(false);
        } else {
            if (adData != null) {
                mAdUnitIdToAdData.put(adUnitId, adData);
            }

            getAdapter().initSDK(config);
        }
    }

    @Override
    public void initRewardedVideoWithCallback(String appKey, String userId, @NonNull final JSONObject config, @NonNull final RewardedVideoSmashListener listener) {

        final String adUnitIdKey = getAdapter().getAdUnitIdKey();
        final String adUnitId = getConfigStringValueFromKey(config, adUnitIdKey);

        if (TextUtils.isEmpty(adUnitId)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(adUnitIdKey));
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(getAdUnitIdMissingErrorString(adUnitIdKey), IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        //add to rewarded video listener map
        mAdUnitIdToListener.put(adUnitId, listener);
        //add to rewarded video init callback map
        mAdUnitIdsForInitCallbacks.add(adUnitId);

        // check AdMob sdk init state
        if (getAdapter().getInitState() == AdMobAdapter.InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose("onRewardedVideoInitSuccess - adUnitId = " + adUnitId);
            listener.onRewardedVideoInitSuccess();
        } else if (getAdapter().getInitState() == AdMobAdapter.InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.verbose("init failed - adUnitId = " + adUnitId);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("AdMob sdk init failed", IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
        } else {
            getAdapter().initSDK(config);
        }
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        for (String adUnitId : mAdUnitIdToListener.keySet()) {
            RewardedVideoSmashListener listener = mAdUnitIdToListener.get(adUnitId);
            if (mAdUnitIdsForInitCallbacks.contains(adUnitId)) {
                listener.onRewardedVideoInitSuccess();
            } else {
                JSONObject adData = mAdUnitIdToAdData.get(adUnitId);
                loadRewardedVideoAdInternal(adUnitId, adData, null, listener);
            }
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        for (String adUnitId : mAdUnitIdToListener.keySet()) {
            RewardedVideoSmashListener listener = mAdUnitIdToListener.get(adUnitId);
            if (mAdUnitIdsForInitCallbacks.contains(adUnitId)) {
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            } else {
                listener.onRewardedVideoAvailabilityChanged(false);
            }
        }
    }

    @Override
    public void loadRewardedVideo(@NonNull final JSONObject config, final JSONObject adData, @NonNull final RewardedVideoSmashListener listener) {
        final String adUnitId = getConfigStringValueFromKey(config, getAdapter().getAdUnitIdKey());
        loadRewardedVideoAdInternal(adUnitId, adData, null, listener);
    }

    @Override
    public void loadRewardedVideoForBidding(@NonNull final JSONObject config, final JSONObject adData, final String serverData, @NonNull final RewardedVideoSmashListener listener) {
        final String adUnitId = getConfigStringValueFromKey(config, getAdapter().getAdUnitIdKey());
        loadRewardedVideoAdInternal(adUnitId, adData, serverData, listener);
    }

    private void loadRewardedVideoAdInternal(final String adUnitId, final JSONObject adData, final String serverData, @NonNull final RewardedVideoSmashListener listener) {
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

                // set the rewarded video availability to false before attempting to load
                mAdUnitIdToAdsAvailability.put(adUnitId, false);
                AdRequest adRequest = getAdapter().createAdRequest(adData, serverData);
                AdMobRewardedVideoAdLoadListener adMobRewardedVideoAdLoadListener = new AdMobRewardedVideoAdLoadListener(AdMobRewardedVideoAdapter.this, adUnitId, listener);
                RewardedAd.load(ContextProvider.getInstance().getApplicationContext(), adUnitId, adRequest, adMobRewardedVideoAdLoadListener);
            }
        });
    }

    @Override
    public void showRewardedVideo(@NonNull final JSONObject config,
                                  @NonNull final RewardedVideoSmashListener listener) {
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                final String adUnitId = getConfigStringValueFromKey(config, getAdapter().getAdUnitIdKey());
                final RewardedAd rewardedAd = getRewardedVideoAd(adUnitId);
                IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
                if (rewardedAd != null && isRewardedVideoAvailableForAdUnitId(adUnitId)) {

                    AdMobRewardedVideoAdShowListener adMobRewardedVideoAdShowListener = new AdMobRewardedVideoAdShowListener(adUnitId, listener);
                    rewardedAd.setFullScreenContentCallback(adMobRewardedVideoAdShowListener);
                    rewardedAd.show(ContextProvider.getInstance().getCurrentActiveActivity(), adMobRewardedVideoAdShowListener);
                } else {
                    listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                }
                //change rewarded video availability to false
                mAdUnitIdToAdsAvailability.put(adUnitId, false);
            }
        });
    }

    public boolean isRewardedVideoAvailable(@NonNull JSONObject config) {
        final String adUnitId = getConfigStringValueFromKey(config, getAdapter().getAdUnitIdKey());
        return isRewardedVideoAvailableForAdUnitId(adUnitId);
    }

    private boolean isRewardedVideoAvailableForAdUnitId(String adUnitId) {
        if (TextUtils.isEmpty(adUnitId)) {
            return false;
        }
        return mAdUnitIdToAdsAvailability.containsKey(adUnitId) && mAdUnitIdToAdsAvailability.get(adUnitId);
    }

    @Override
    public void releaseMemory(@NonNull IronSource.AD_UNIT adUnit, JSONObject config) {
        for (RewardedAd rewardedVideoAd : mAdUnitIdToAd.values()) {
            rewardedVideoAd.setFullScreenContentCallback(null);
        }
        // clear rewarded maps
        mAdUnitIdToAd.clear();
        mAdUnitIdToListener.clear();
        mAdUnitIdToAdsAvailability.clear();
        mAdUnitIdsForInitCallbacks.clear();
        mAdUnitIdToAdData.clear();
    }

    @Override
    public void collectRewardedVideoBiddingData(@NonNull JSONObject config, JSONObject adData, @NotNull final BiddingDataCallback biddingDataCallback) {
        getAdapter().collectBiddingData(biddingDataCallback, AdFormat.REWARDED, null);
    }

    public void onRewardedVideoAdLoaded(String adUnitId, RewardedAd rewardedAd) {
        //add rewarded ad to maps
        mAdUnitIdToAd.put(adUnitId, rewardedAd);
        mAdUnitIdToAdsAvailability.put(adUnitId, true);
    }

    private RewardedAd getRewardedVideoAd(String adUnitId) {
        if (TextUtils.isEmpty(adUnitId) || !mAdUnitIdToAd.containsKey(adUnitId)) {
            return null;
        }
        return mAdUnitIdToAd.get(adUnitId);
    }
}
