package com.ironsource.adapters.tapjoy;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.AbstractAdapter;
import com.ironsource.mediationsdk.INetworkInitCallbackListener;
import com.ironsource.mediationsdk.IntegrationData;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.LoadWhileShowSupportState;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.metadata.MetaDataUtils;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import com.tapjoy.TJConnectListener;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJPrivacyPolicy;
import com.tapjoy.TJSetUserIDListener;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyAuctionFlags;
import com.tapjoy.TapjoyConnectFlag;
import com.tapjoy.TapjoyLog;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN;

public class TapjoyAdapter extends AbstractAdapter implements TJConnectListener, INetworkInitCallbackListener {
    // Tapjoy requires a request agent name
    private final String MEDIATION_NAME = "ironsource";

    // Adapter version
    private static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String GitHash = BuildConfig.GitHash;

    // Tapjoy keys
    private final String SDK_KEY = "sdkKey";
    private final String PLACEMENT_NAME = "placementName";

    // Meta data flags
    private final String META_DATA_TAPJOY_COPPA = "Tapjoy_COPPA";
    private final String META_DATA_TAPJOY_ADV_ID_OPT_OUT = "Tapjoy_optOutAdvertisingID";

    // Rewarded video maps
    protected ConcurrentHashMap<String, RewardedVideoSmashListener> mRewardedVideoPlacementToSmashListener;
    protected ConcurrentHashMap<String, TapjoyRewardedVideoAdListener> mRewardedVideoPlacementToTapjoyListener;
    protected ConcurrentHashMap<String, TJPlacement> mRewardedVideoPlacementToAd;
    protected ConcurrentHashMap<String, Boolean> mRewardedVideoPlacementToIsReady;
    private CopyOnWriteArraySet<String> mRewardedVideoPlacementsForInitCallbacks;

    // Interstitial maps
    protected ConcurrentHashMap<String, InterstitialSmashListener> mInterstitialPlacementToSmashListener;
    protected ConcurrentHashMap<String, TapjoyInterstitialAdListener> mInterstitialPlacementToTapjoyListener;
    protected ConcurrentHashMap<String, TJPlacement> mInterstitialPlacementToAd;
    protected ConcurrentHashMap<String, Boolean> mInterstitialPlacementToIsReady;

    // members for network
    private String mUserId;

    // Tapjoy instance
    private static TJPrivacyPolicy tjPrivacyPolicy = Tapjoy.getPrivacyPolicy();

    // init state possible values
    private enum InitState {
        INIT_STATE_NONE,
        INIT_STATE_IN_PROGRESS,
        INIT_STATE_SUCCESS,
        INIT_STATE_FAILED
    }

    // Handle init callback for all adapter instances
    private static AtomicBoolean mWasInitCalled = new AtomicBoolean(false);
    private static InitState mInitState = InitState.INIT_STATE_NONE;
    private static HashSet<INetworkInitCallbackListener> initCallbackListeners = new HashSet<>();

    //region Adapter Methods

    public static TapjoyAdapter startAdapter(String providerName) {
        return new TapjoyAdapter(providerName);
    }

    private TapjoyAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose();

        // Rewarded video
        mRewardedVideoPlacementToSmashListener = new ConcurrentHashMap<>();
        mRewardedVideoPlacementToTapjoyListener = new ConcurrentHashMap<>();
        mRewardedVideoPlacementToAd = new ConcurrentHashMap<>();
        mRewardedVideoPlacementToIsReady = new ConcurrentHashMap<>();
        mRewardedVideoPlacementsForInitCallbacks = new CopyOnWriteArraySet<>();

        // Interstitial
        mInterstitialPlacementToSmashListener = new ConcurrentHashMap<>();
        mInterstitialPlacementToTapjoyListener = new ConcurrentHashMap<>();
        mInterstitialPlacementToAd = new ConcurrentHashMap<>();
        mInterstitialPlacementToIsReady = new ConcurrentHashMap<>();

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE;
    }

    // Get the network and adapter integration data
    public static IntegrationData getIntegrationData(Context context) {
        return new IntegrationData("Tapjoy", VERSION);
    }

    @Override
    // get adapter version
    public String getVersion() {
        return VERSION;
    }

    @Override
    //get network sdk version
    public String getCoreSDKVersion() {
        return Tapjoy.getVersion();
    }

    public static String getAdapterSDKVersion() {
        return Tapjoy.getVersion();
    }
    //endregion

    //region Initializations methods and callbacks
    private void initSDK(final String userId, final String sdkKey) {
        // add self to init delegates only when init not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this);
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            IronLog.ADAPTER_API.verbose("initSDK - sdkKey = " + sdkKey);

            mInitState = InitState.INIT_STATE_IN_PROGRESS;

            mUserId = userId;

            final Hashtable<String, Object> connectFlags = new Hashtable<>();

            if (isAdaptersDebugEnabled()) {
                connectFlags.put(TapjoyConnectFlag.ENABLE_LOGGING, "true");
                Tapjoy.setDebugEnabled(true);
                TapjoyLog.setDebugEnabled(true);
            } else {
                connectFlags.put(TapjoyConnectFlag.ENABLE_LOGGING, "false");
                Tapjoy.setDebugEnabled(false);
                TapjoyLog.setDebugEnabled(false);
            }

            Tapjoy.connect(ContextProvider.getInstance().getApplicationContext(), sdkKey, connectFlags, this);
        }
    }

    @Override
    public void onConnectSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose("onConnectSuccess");

        mInitState = InitState.INIT_STATE_SUCCESS;

        for (INetworkInitCallbackListener adapter : initCallbackListeners) {
            adapter.onNetworkInitCallbackSuccess();
        }

        initCallbackListeners.clear();
    }

    @Override
    public void onConnectFailure() {
        IronLog.ADAPTER_CALLBACK.verbose("onConnectFailure");

        mInitState = InitState.INIT_STATE_FAILED;

        for (INetworkInitCallbackListener adapter : initCallbackListeners) {
            adapter.onNetworkInitCallbackFailed("Tapjoy sdk init failed");
        }

        initCallbackListeners.clear();
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        setUserID();

        Tapjoy.setActivity(ContextProvider.getInstance().getCurrentActiveActivity());

        for (String placementName : mRewardedVideoPlacementToSmashListener.keySet()) {
            RewardedVideoSmashListener listener = mRewardedVideoPlacementToSmashListener.get(placementName);

            if (mRewardedVideoPlacementsForInitCallbacks.contains(placementName)) {
                listener.onRewardedVideoInitSuccess();
            } else {
                loadRewardedVideoInternal(placementName, null, listener);
            }
        }

        for (InterstitialSmashListener listener : mInterstitialPlacementToSmashListener.values()) {
            listener.onInterstitialInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        for (String placementName : mRewardedVideoPlacementToSmashListener.keySet()) {
            RewardedVideoSmashListener listener = mRewardedVideoPlacementToSmashListener.get(placementName);

            if (mRewardedVideoPlacementsForInitCallbacks.contains(placementName)) {
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            } else {
                listener.onRewardedVideoAvailabilityChanged(false);
            }
        }

        for (InterstitialSmashListener listener : mInterstitialPlacementToSmashListener.values()) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    //endregion

    //region Rewarded Video API
    @Override
    // used for flows when the mediation needs to get a callback for init
    public void initRewardedVideoWithCallback(String appKey, String userId, JSONObject config, RewardedVideoSmashListener listener) {
        String sdkKey = config.optString(SDK_KEY);
        String placementName = config.optString(PLACEMENT_NAME);

        if (TextUtils.isEmpty(sdkKey)) {
            IronLog.INTERNAL.error("Missing params - " + SDK_KEY);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + SDK_KEY, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(placementName)) {
            IronLog.INTERNAL.error("Missing params - " + PLACEMENT_NAME);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + PLACEMENT_NAME, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("sdkKey = " + sdkKey + " placementName = " + placementName);

        //add to rewarded video listener map
        mRewardedVideoPlacementToSmashListener.put(placementName, listener);

        //add to rewarded video init callback map
        mRewardedVideoPlacementsForInitCallbacks.add(placementName);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(userId, sdkKey);
            break;
            case INIT_STATE_SUCCESS:
                listener.onRewardedVideoInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementName = " + placementName);
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Tapjoy sdk init failed", IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                break;
        }
    }

    @Override
    // used for flows when the mediation doesn't need to get a callback for init
    public void initAndLoadRewardedVideo(String appKey, String userId, JSONObject config, JSONObject adData, RewardedVideoSmashListener listener) {
        String sdkKey = config.optString(SDK_KEY);
        String placementName = config.optString(PLACEMENT_NAME);

        if (TextUtils.isEmpty(sdkKey)) {
            IronLog.INTERNAL.error("Missing params - " + SDK_KEY);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        if (TextUtils.isEmpty(placementName)) {
            IronLog.INTERNAL.error("Missing params - " + PLACEMENT_NAME);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        IronLog.ADAPTER_API.verbose("sdkKey = " + sdkKey + " placementName = " + placementName);

        //add to rewarded video listener map
        mRewardedVideoPlacementToSmashListener.put(placementName, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(userId, sdkKey);
                break;
            case INIT_STATE_SUCCESS:
                loadRewardedVideoInternal(placementName, null, listener);
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementName = " + placementName);
                listener.onRewardedVideoAvailabilityChanged(false);
                break;
        }
    }

    @Override
    public void loadRewardedVideoForBidding(JSONObject config, JSONObject adData, final String serverData, final RewardedVideoSmashListener listener) {
        final String placementName = config.optString(PLACEMENT_NAME);
        loadRewardedVideoInternal(placementName, serverData, listener);
    }

    @Override
    public void loadRewardedVideo(final JSONObject config, JSONObject adData, final RewardedVideoSmashListener listener) {
        final String placementName = config.optString(PLACEMENT_NAME);
        loadRewardedVideoInternal(placementName, null, listener);
    }

    private void loadRewardedVideoInternal(final String placementName, final String serverData, final RewardedVideoSmashListener listener) {
        IronLog.ADAPTER_API.verbose("placementName = " + placementName);

        mRewardedVideoPlacementToIsReady.put(placementName, false);

        TapjoyRewardedVideoAdListener rewardedVideoAdListener = new TapjoyRewardedVideoAdListener(TapjoyAdapter.this, listener, placementName);
        mRewardedVideoPlacementToTapjoyListener.put(placementName, rewardedVideoAdListener);
        TJPlacement placement;

        if (!TextUtils.isEmpty(serverData)) {
            placement = getTJBiddingPlacement(placementName, serverData, rewardedVideoAdListener);
        } else {
            placement = getTJPlacement(placementName, rewardedVideoAdListener);
        }

        if (placement != null) {
            placement.setVideoListener(rewardedVideoAdListener);
            mRewardedVideoPlacementToAd.put(placementName, placement);
            placement.requestContent();
        } else {
            listener.onRewardedVideoAvailabilityChanged(false);
        }
    }

    @Override
    public void showRewardedVideo(final JSONObject config, final RewardedVideoSmashListener listener) {
        final String placementName = config.optString(PLACEMENT_NAME);
        IronLog.ADAPTER_API.verbose("placementName = " + placementName);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {

                if (isRewardedVideoAvailable(config)) {
                    TJPlacement placement = mRewardedVideoPlacementToAd.get(placementName);
                    placement.showContent();
                } else if (listener != null) {
                    listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                }

                // change rewarded video availability to false
                mRewardedVideoPlacementToIsReady.put(placementName, false);
            }
        });
    }

    @Override
    public boolean isRewardedVideoAvailable(JSONObject config) {
        String placementName = config.optString(PLACEMENT_NAME);
        return mRewardedVideoPlacementToIsReady.containsKey(placementName) && mRewardedVideoPlacementToIsReady.get(placementName) &&
                mRewardedVideoPlacementToAd.containsKey(placementName);
    }

    @Override
    public Map<String, Object> getRewardedVideoBiddingData(JSONObject config, JSONObject adData) {
        return getBiddingData();
    }
    //endregion

    //region Interstitial API
    @Override
    public void initInterstitialForBidding(String appKey, String userId, JSONObject config, InterstitialSmashListener listener) {
        initInterstitial(appKey, userId, config, listener);
    }

    @Override
    public void initInterstitial(String appKey, String userId, JSONObject config, InterstitialSmashListener listener) {
        String sdkKey = config.optString(SDK_KEY);
        String placementName = config.optString(PLACEMENT_NAME);

        if (TextUtils.isEmpty(sdkKey)) {
            IronLog.INTERNAL.error("Missing params - " + SDK_KEY);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + SDK_KEY, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(placementName)) {
            IronLog.INTERNAL.error("Missing params - " + PLACEMENT_NAME);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + PLACEMENT_NAME, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("sdkKey = " + sdkKey + " placementName = " + placementName);

        //add to interstitial listener map
        mInterstitialPlacementToSmashListener.put(placementName, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(userId, sdkKey);
                break;
            case INIT_STATE_SUCCESS:
                listener.onInterstitialInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementName = " + placementName);
                listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Tapjoy sdk init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT));
                break;
        }
    }

    @Override
    public void loadInterstitialForBidding(final JSONObject config, final JSONObject adData, final String serverData, final InterstitialSmashListener listener) {
        String placementName = config.optString(PLACEMENT_NAME);
        loadInterstitialInternal(placementName, serverData, listener);
    }

    @Override
    public void loadInterstitial(final JSONObject config, final JSONObject adData, final InterstitialSmashListener listener) {
        String placementName = config.optString(PLACEMENT_NAME);
        loadInterstitialInternal(placementName, null, listener);
    }

    private void loadInterstitialInternal(final String placementName, final String serverData, final InterstitialSmashListener listener) {
        IronLog.ADAPTER_API.verbose("placementName = " + placementName);

        mInterstitialPlacementToIsReady.put(placementName, false);

        TapjoyInterstitialAdListener interstitialAdListener = new TapjoyInterstitialAdListener(TapjoyAdapter.this, listener, placementName);
        mInterstitialPlacementToTapjoyListener.put(placementName, interstitialAdListener);
        TJPlacement placement;

        if (!TextUtils.isEmpty(serverData)) {
            placement = getTJBiddingPlacement(placementName, serverData, interstitialAdListener);
        } else {
            placement = getTJPlacement(placementName, interstitialAdListener);
        }

        if (placement != null) {
            placement.setVideoListener(interstitialAdListener);
            mInterstitialPlacementToAd.put(placementName, placement);
            placement.requestContent();
        } else {
            listener.onInterstitialAdLoadFailed(new IronSourceError(IronSourceError.ERROR_CODE_GENERIC, "Load failed - TJPlacement is null"));
        }
    }

    @Override
    public void showInterstitial(final JSONObject config, final InterstitialSmashListener listener) {
        final String placementName = config.optString(PLACEMENT_NAME);
        IronLog.ADAPTER_API.verbose("placementName = " + placementName);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (isInterstitialReady(config)) {
                    TJPlacement placement = mInterstitialPlacementToAd.get(placementName);
                    placement.showContent();
                } else {
                    listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
                }

                mInterstitialPlacementToIsReady.put(placementName, false);
            }
        });
    }

    @Override
    public boolean isInterstitialReady(JSONObject config) {
        String placementName = config.optString(PLACEMENT_NAME);
        return mInterstitialPlacementToIsReady.containsKey(placementName) && mInterstitialPlacementToIsReady.get(placementName) &&
                mInterstitialPlacementToAd.containsKey(placementName);
    }

    @Override
    public Map<String, Object> getInterstitialBiddingData(JSONObject config, JSONObject adData) {
        return getBiddingData();
    }
    //endregion

    // region memory handling
    @Override
    public void releaseMemory(IronSource.AD_UNIT adUnit, JSONObject config) {
        IronLog.INTERNAL.verbose("adUnit = " + adUnit);

        if (adUnit == IronSource.AD_UNIT.REWARDED_VIDEO) {
            mRewardedVideoPlacementToSmashListener.clear();
            mRewardedVideoPlacementToTapjoyListener.clear();
            mRewardedVideoPlacementToAd.clear();
            mRewardedVideoPlacementToIsReady.clear();
            mRewardedVideoPlacementsForInitCallbacks.clear();
        } else if (adUnit == IronSource.AD_UNIT.INTERSTITIAL) {
            mInterstitialPlacementToAd.clear();
            mInterstitialPlacementToSmashListener.clear();
            mInterstitialPlacementToTapjoyListener.clear();
            mInterstitialPlacementToIsReady.clear();
        }
    }
    //endregion

    //region legal
    @Override
    protected void setConsent(boolean consent) {
        IronLog.ADAPTER_API.verbose("setUserConsent = " + consent);
        tjPrivacyPolicy.setUserConsent(consent ? "1" : "0");
        setGDPRValue();
    }

    @Override
    protected void setMetaData(String key, List<String> values) {
        if (values.isEmpty()) {
            return;
        }

        // this is a list of 1 value
        String value = values.get(0);
        IronLog.ADAPTER_API.verbose("key = " + key + ", value = " + value);

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value));
        } else {
            String formattedValue = MetaDataUtils.formatValueForType(value, META_DATA_VALUE_BOOLEAN);

            if (MetaDataUtils.isValidMetaData(key, META_DATA_TAPJOY_COPPA, formattedValue)) {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue));
            } else if (MetaDataUtils.isValidMetaData(key, META_DATA_TAPJOY_ADV_ID_OPT_OUT, formattedValue)) {
                setAdvIdOptOutValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue));
            }
        }
    }

    private void setCCPAValue(final boolean value) {
        String privacyValue = value ? "1YY-" : "1YN-";
        IronLog.ADAPTER_API.verbose("value = " + privacyValue);
        tjPrivacyPolicy.setUSPrivacy(privacyValue);
    }

    private void setGDPRValue() {
        IronLog.ADAPTER_API.verbose();
        tjPrivacyPolicy.setSubjectToGDPR(true);
    }

    private void setCOPPAValue(final boolean value) {
        IronLog.ADAPTER_API.verbose("value = " + value);
        tjPrivacyPolicy.setBelowConsentAge(value);
    }

    private void setAdvIdOptOutValue(final boolean isOptOut) {
        if (isOptOut) {
            IronLog.ADAPTER_API.verbose("value = true");
            Tapjoy.optOutAdvertisingID(ContextProvider.getInstance().getApplicationContext(), true);
        }
    }

    //endregion

    // region Helpers
    private void setUserID() {
        if (!TextUtils.isEmpty(mUserId)) {
            IronLog.ADAPTER_API.verbose("setUserID to " + mUserId);
            Tapjoy.setUserID(mUserId, new TJSetUserIDListener() {
                @Override
                public void onSetUserIDSuccess() {
                    IronLog.ADAPTER_CALLBACK.verbose("onSetUserIDSuccess");
                }

                @Override
                public void onSetUserIDFailure(String errorMessage) {
                    IronLog.ADAPTER_CALLBACK.verbose("onSetUserIDFailure - " + errorMessage);
                }
            });
        }
    }

    private Map<String, Object> getBiddingData() {
        if (mInitState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error("returning null as token since init failed");
            return null;
        }

        String bidderToken = Tapjoy.getUserToken();
        String returnedToken = (!TextUtils.isEmpty(bidderToken)) ? bidderToken : "";
        IronLog.ADAPTER_API.verbose("token = " + returnedToken);
        Map<String, Object> ret = new HashMap<>();
        ret.put("token", returnedToken);
        return ret;
    }

    /**
     * Some methods in getTJBiddingPlacement(String placementName, String serverData, TJPlacementListener tapjoyListener) must run on the same thread.
     * Make sure that all the methods who call getTJBiddingPlacement(String placementName, String serverData, TJPlacementListener tapjoyListener) are running on the same thread.
     */
    private TJPlacement getTJBiddingPlacement(String placementName, String serverData, TJPlacementListener tapjoyListener) {
        try {
            TJPlacement placement = Tapjoy.getPlacement(placementName, tapjoyListener);
            placement.setMediationName(MEDIATION_NAME);
            placement.setAdapterVersion(VERSION);

            HashMap<String, String> auctionData = new HashMap<>();

            JSONObject main = new JSONObject(serverData);

            String id = main.getString(TapjoyAuctionFlags.AUCTION_ID);
            auctionData.put(TapjoyAuctionFlags.AUCTION_ID, id);

            String extData = main.getString(TapjoyAuctionFlags.AUCTION_DATA);
            auctionData.put(TapjoyAuctionFlags.AUCTION_DATA, extData);

            placement.setAuctionData(auctionData);
            return placement;

        } catch (Exception e) {
            IronLog.INTERNAL.error("error - " + e.getMessage());
            return null;
        }
    }

    /**
     * Some methods in getTJPlacement(String placementName, TJPlacementListener tapjoyListener)() must run on the same thread.
     * Make sure that all the methods who call getTJPlacement(String placementName, TJPlacementListener tapjoyListener) are running on the same thread.
     */
    private TJPlacement getTJPlacement(String placementName, TJPlacementListener tapjoyListener) {
        TJPlacement placement = Tapjoy.getPlacement(placementName, tapjoyListener);
        if (placement != null) {
            placement.setMediationName(MEDIATION_NAME);
            placement.setAdapterVersion(VERSION);
            return placement;
        } else {
            IronLog.INTERNAL.error("error - TJPlacement is null");
            return null;
        }
    }
    //endregion

}
