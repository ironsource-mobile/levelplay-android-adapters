package com.ironsource.adapters.facebook.rewardedvideo;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.facebook.ads.RewardData;
import com.facebook.ads.RewardedVideoAd;
import com.ironsource.adapters.facebook.FacebookAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.adapter.AbstractRewardedVideoAdapter;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FacebookRewardedVideoAdapter extends AbstractRewardedVideoAdapter<FacebookAdapter> {

    private final ConcurrentHashMap<String, RewardedVideoSmashListener> mPlacementIdToSmashListener;
    private final ConcurrentHashMap<String, RewardedVideoAd> mPlacementIdToAd;
    private final ConcurrentHashMap<String, FacebookRewardedVideoAdListener> mPlacementIdToFacebookAdListener;
    protected ConcurrentHashMap<String, Boolean> mAdsAvailability;
    protected ConcurrentHashMap<String, Boolean> mPlacementIdToShowAttempts; //Show attempts per placement id, used to identify show errors


    public FacebookRewardedVideoAdapter(FacebookAdapter adapter) {
        super(adapter);
        mPlacementIdToSmashListener = new ConcurrentHashMap<>();
        mPlacementIdToAd = new ConcurrentHashMap<>();
        mPlacementIdToFacebookAdListener = new ConcurrentHashMap<>();
        mAdsAvailability = new ConcurrentHashMap<>();
        mPlacementIdToShowAttempts = new ConcurrentHashMap<>();
    }

    public void initRewardedVideoWithCallback(String appKey, String userId,
                                              @NonNull final JSONObject config, @NonNull final RewardedVideoSmashListener listener) {
        final String placementIdKey = getAdapter().getPlacementIdKey();
        final String allPlacementIdsKey = getAdapter().getAllPlacementIdsKey();
        final String placementId = getConfigStringValueFromKey(config, placementIdKey);
        final String allPlacementIds = getConfigStringValueFromKey(config, allPlacementIdsKey);

        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(placementIdKey));
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(getAdUnitIdMissingErrorString(placementIdKey), IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }
        if (TextUtils.isEmpty(allPlacementIds)) {
            IronLog.INTERNAL.error(getAdUnitIdMissingErrorString(allPlacementIdsKey));
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(getAdUnitIdMissingErrorString(allPlacementIdsKey), IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // add to rewarded video listener map
        mPlacementIdToSmashListener.put(placementId, listener);

        if (getAdapter().getInitState() == FacebookAdapter.InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.verbose("onRewardedVideoInitSuccess - placementId = " + placementId);
            listener.onRewardedVideoInitSuccess();
        } else if (getAdapter().getInitState() == FacebookAdapter.InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.verbose("onRewardedVideoInitFailed - placementId = " + placementId);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Meta SDK init failed", IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
        } else {
            getAdapter().initSDK(allPlacementIds);
        }

    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        for (RewardedVideoSmashListener listener : mPlacementIdToSmashListener.values()) {
            listener.onRewardedVideoInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        for (RewardedVideoSmashListener listener : mPlacementIdToSmashListener.values()) {
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
        }
    }

    @Override
    public void loadRewardedVideoForBidding(@NonNull final JSONObject config, final JSONObject adData, final String serverData, @NonNull final RewardedVideoSmashListener listener) {
        final String placementId = getConfigStringValueFromKey(config, getAdapter().getPlacementIdKey());

        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        mAdsAvailability.put(placementId, false);
        mPlacementIdToShowAttempts.put(placementId, false);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mPlacementIdToAd.containsKey(placementId)) {
                        IronLog.ADAPTER_API.verbose("destroying previous ad with placementId " + placementId);
                        mPlacementIdToAd.get(placementId).destroy();
                        mPlacementIdToAd.remove(placementId);
                    }

                    RewardedVideoAd rewardedVideoAd = new RewardedVideoAd(ContextProvider.getInstance().getApplicationContext(), placementId);

                    FacebookRewardedVideoAdListener rewardedVideoAdListener = new FacebookRewardedVideoAdListener(FacebookRewardedVideoAdapter.this, placementId, listener);
                    mPlacementIdToFacebookAdListener.put(placementId, rewardedVideoAdListener);

                    RewardedVideoAd.RewardedVideoAdLoadConfigBuilder configBuilder = rewardedVideoAd.buildLoadAdConfig();
                    configBuilder.withAdListener(rewardedVideoAdListener);

                    if (!TextUtils.isEmpty(serverData)) {
                        // add server data to rewarded video bidder instance
                        configBuilder.withBid(serverData);
                    }

                    // set dynamic user id
                    if (!TextUtils.isEmpty(getAdapter().getDynamicUserId())) {
                        configBuilder.withRewardData(new RewardData(getAdapter().getDynamicUserId(), ""));
                    }

                    mPlacementIdToAd.put(placementId, rewardedVideoAd);
                    rewardedVideoAd.loadAd(configBuilder.build());
                } catch (Exception ex) {
                    listener.onRewardedVideoAvailabilityChanged(false);
                }
            }
        });
    }

    public void showRewardedVideo(@NonNull final JSONObject config,
                                  @NonNull final RewardedVideoSmashListener listener) {
        final String placementId = getConfigStringValueFromKey(config, getAdapter().getPlacementIdKey());
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
              try {
                RewardedVideoAd rewardedVideoAd = mPlacementIdToAd.get(placementId);
                // make sure the ad is loaded and has not expired
                if (rewardedVideoAd != null && rewardedVideoAd.isAdLoaded() && !rewardedVideoAd.isAdInvalidated()) {
                    mPlacementIdToShowAttempts.put(placementId, true);
                    rewardedVideoAd.show();
                } else {
                    listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                }
                // change rewarded video availability to false
                mAdsAvailability.put(placementId, false);
              } catch (Exception ex) {
                  IronLog.INTERNAL.error("ex.getMessage() = " + ex.getMessage());
                  listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT, ex.getMessage()));
              }
            }
        });
    }

    @Override
    public boolean isRewardedVideoAvailable(@NonNull JSONObject config) {
        final String placementId = getConfigStringValueFromKey(config, getAdapter().getPlacementIdKey());
        return mAdsAvailability.containsKey(placementId) && Boolean.TRUE.equals(mAdsAvailability.get(placementId));
    }

    @Override
    public Map<String, Object> getRewardedVideoBiddingData(@NonNull JSONObject config, JSONObject adData) {
        return getAdapter().getBiddingData();
    }
}
