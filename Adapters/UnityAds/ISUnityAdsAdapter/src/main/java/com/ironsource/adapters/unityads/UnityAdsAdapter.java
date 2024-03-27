package com.ironsource.adapters.unityads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.AbstractAdapter;
import com.ironsource.mediationsdk.AdapterUtils;
import com.ironsource.mediationsdk.INetworkInitCallbackListener;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IntegrationData;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.LoadWhileShowSupportState;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.metadata.MetaDataUtils;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import com.ironsource.mediationsdk.utils.IronSourceUtils;
import com.unity3d.ads.IUnityAdsTokenListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsLoadOptions;
import com.unity3d.ads.UnityAdsShowOptions;
import com.unity3d.ads.metadata.MediationMetaData;
import com.unity3d.ads.metadata.MetaData;
import com.unity3d.ads.metadata.PlayerMetaData;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;
import com.unity3d.ads.IUnityAdsInitializationListener;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN;


class UnityAdsAdapter extends AbstractAdapter implements IUnityAdsInitializationListener, INetworkInitCallbackListener {

    // UnityAds Mediation MetaData
    private final String MEDIATION_NAME = "ironSource";
    private final String ADAPTER_VERSION_KEY = "adapter_version";

    // Adapter version
    private static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String GitHash = BuildConfig.GitHash;

    // UnityAds keys
    private final String GAME_ID = "sourceId";
    private final String PLACEMENT_ID = "zoneId";

    // Rewarded video collections
    private ConcurrentHashMap<String, RewardedVideoSmashListener> mPlacementIdToRewardedVideoSmashListener;
    private ConcurrentHashMap<String, UnityAdsRewardedVideoListener> mPlacementIdToRewardedVideoListener;
    private ConcurrentHashMap<String, String> mRewardedVideoPlacementIdToLoadedAdObjectId;
    protected ConcurrentHashMap<String, Boolean> mRewardedVideoAdsAvailability;
    private CopyOnWriteArraySet<String> mRewardedVideoPlacementIdsForInitCallbacks;

    // Interstitial maps
    private ConcurrentHashMap<String, InterstitialSmashListener> mPlacementIdToInterstitialSmashListener;
    private ConcurrentHashMap<String, UnityAdsInterstitialListener> mPlacementIdToInterstitialListener;
    private ConcurrentHashMap<String, String> mInterstitialPlacementIdToLoadedAdObjectId;
    protected ConcurrentHashMap<String, Boolean> mInterstitialAdsAvailability;

    // Banner maps
    private ConcurrentHashMap<String, BannerSmashListener> mPlacementIdToBannerSmashListener;
    private ConcurrentHashMap<String, UnityAdsBannerListener> mPlacementIdToBannerListener;
    private ConcurrentHashMap<String, BannerView> mPlacementIdToBannerAd;

    // init state possible values
    private enum InitState {
        INIT_STATE_NONE,
        INIT_STATE_IN_PROGRESS,
        INIT_STATE_SUCCESS,
        INIT_STATE_FAILED
    }

    // handle init callback for all adapter instances
    private static HashSet<INetworkInitCallbackListener> initCallbackListeners = new HashSet<>();
    private static InitState mInitState = InitState.INIT_STATE_NONE;
    private static AtomicBoolean mWasInitCalled = new AtomicBoolean(false);
    ;

    // Meta data flags
    private final String CONSENT_GDPR = "gdpr.consent";
    private final String CONSENT_CCPA = "privacy.consent";
    private final String UNITYADS_COPPA = "user.nonBehavioral";
    private final String UNITYADS_METADATA_COPPA_KEY = "unityads_coppa";
    private final String GAME_DESIGNATION = "mode";
    private final String MIXED_AUDIENCE = "mixed";
    private final String ADS_GATEWAY_ENABLED = "adsGatewayV2";

    // UnityAds asynchronous token
    private static String asyncToken = null;

    // Feature flag key to disable the network's capability to load a Rewarded Video ad
    // while another Rewarded Video ad of that network is showing
    private final String LWS_SUPPORT_STATE = "isSupportedLWS";

    final private Object mUnityAdsStorageLock = new Object();

    private boolean isAdsGateway = false;

    //region Adapter Methods
    public static UnityAdsAdapter startAdapter(String providerName) {
        return new UnityAdsAdapter(providerName);
    }

    private UnityAdsAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose();

        // Rewarded video
        mPlacementIdToRewardedVideoSmashListener = new ConcurrentHashMap<>();
        mPlacementIdToRewardedVideoListener = new ConcurrentHashMap<>();
        mRewardedVideoPlacementIdToLoadedAdObjectId = new ConcurrentHashMap<>();
        mRewardedVideoAdsAvailability = new ConcurrentHashMap<>();
        mRewardedVideoPlacementIdsForInitCallbacks = new CopyOnWriteArraySet<>();

        // Interstitial
        mPlacementIdToInterstitialSmashListener = new ConcurrentHashMap<>();
        mPlacementIdToInterstitialListener = new ConcurrentHashMap<>();
        mInterstitialPlacementIdToLoadedAdObjectId = new ConcurrentHashMap<>();
        mInterstitialAdsAvailability = new ConcurrentHashMap<>();

        // Banner
        mPlacementIdToBannerSmashListener = new ConcurrentHashMap<>();
        mPlacementIdToBannerListener = new ConcurrentHashMap<>();
        mPlacementIdToBannerAd = new ConcurrentHashMap<>();
    }

    // get the network and adapter integration data
    public static IntegrationData getIntegrationData(Context context) {
        return new IntegrationData("UnityAds", VERSION);
    }

    // get adapter version
    @Override
    public String getVersion() {
        return VERSION;
    }

    // get network sdk version
    @Override
    public String getCoreSDKVersion() {
        return UnityAds.getVersion();
    }

    public static String getAdapterSDKVersion() {
        return UnityAds.getVersion();
    }

    public boolean isUsingActivityBeforeImpression(@NotNull IronSource.AD_UNIT adUnit) {
        return false;
    }

    //endregion

    //region Initializations methods and callbacks
    private void initSDK(String gameId, JSONObject config) {
        // add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this);
        }
        //Init SDK should be called only once
        if (mWasInitCalled.compareAndSet(false, true)) {
            IronLog.ADAPTER_API.verbose();
            mInitState = InitState.INIT_STATE_IN_PROGRESS;

            synchronized (mUnityAdsStorageLock) {
                MediationMetaData mediationMetaData = new MediationMetaData(ContextProvider.getInstance().getApplicationContext());
                mediationMetaData.setName(MEDIATION_NAME);
                // mediation version
                mediationMetaData.setVersion(IronSourceUtils.getSDKVersion());
                // adapter version
                mediationMetaData.set(ADAPTER_VERSION_KEY, BuildConfig.VERSION_NAME);
                mediationMetaData.commit();
            }

            isAdsGateway = config.optBoolean(ADS_GATEWAY_ENABLED, false);

            setUnityAdsMetaData(ADS_GATEWAY_ENABLED, isAdsGateway);

            UnityAds.setDebugMode(isAdaptersDebugEnabled());
            UnityAds.initialize(ContextProvider.getInstance().getApplicationContext(), gameId, false, this);

            // trying to fetch async token for the first load
            if (!isAdsGateway) {
                getAsyncToken();
            }
        }
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose();

        // rewarded video listeners
        for (String placementId : mPlacementIdToRewardedVideoSmashListener.keySet()) {
            RewardedVideoSmashListener listener = mPlacementIdToRewardedVideoSmashListener.get(placementId);

            if (mRewardedVideoPlacementIdsForInitCallbacks.contains(placementId)) {
                listener.onRewardedVideoInitSuccess();
            } else {
                loadRewardedVideoInternal(placementId, null, listener);
            }
        }

        // interstitial listeners
        for (InterstitialSmashListener listener : mPlacementIdToInterstitialSmashListener.values()) {
            listener.onInterstitialInitSuccess();
        }

        // banners listeners
        for (BannerSmashListener listener : mPlacementIdToBannerSmashListener.values()) {
            listener.onBannerInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        IronLog.ADAPTER_CALLBACK.verbose();

        // rewarded video listeners
        for (String placementId : mPlacementIdToRewardedVideoSmashListener.keySet()) {
            RewardedVideoSmashListener listener = mPlacementIdToRewardedVideoSmashListener.get(placementId);

            if (mRewardedVideoPlacementIdsForInitCallbacks.contains(placementId)) {
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            } else {
                listener.onRewardedVideoAvailabilityChanged(false);
            }
        }

        // interstitial listeners
        for (InterstitialSmashListener listener : mPlacementIdToInterstitialSmashListener.values()) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }

        // banners listeners
        for (BannerSmashListener listener : mPlacementIdToBannerSmashListener.values()) {
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.BANNER_AD_UNIT));
        }
    }

    @Override
    public void onInitializationComplete() {
        IronLog.ADAPTER_CALLBACK.verbose();
        mInitState = InitState.INIT_STATE_SUCCESS;

        for (INetworkInitCallbackListener adapter : initCallbackListeners) {
            adapter.onNetworkInitCallbackSuccess();
        }

        initCallbackListeners.clear();
    }

    @Override
    public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
        IronLog.ADAPTER_CALLBACK.verbose();
        mInitState = InitState.INIT_STATE_FAILED;
        String initError = getUnityAdsInitializationErrorCode(error) + message;

        for (INetworkInitCallbackListener adapter : initCallbackListeners) {
            adapter.onNetworkInitCallbackFailed(initError);
        }

        initCallbackListeners.clear();
    }

    //endregion

    //region Rewarded Video API

    // Used for flows when the mediation needs to get a callback for init
    @Override
    public void initRewardedVideoWithCallback(String appKey, String userId, JSONObject config, RewardedVideoSmashListener listener) {
        String gameId = config.optString(GAME_ID);
        String placementId = config.optString(PLACEMENT_ID);

        // check if OS is supported
        if (!isOSSupported()) {
            IronSourceError error = errorForUnsupportedAdapter(IronSourceConstants.REWARDED_VIDEO_AD_UNIT);
            IronLog.INTERNAL.error(error.getErrorMessage());
            listener.onRewardedVideoInitFailed(error);
            return;
        }

        // check gameId
        if (TextUtils.isEmpty(gameId)) {
            IronLog.INTERNAL.error("missing params = " + GAME_ID);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + GAME_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        // check placementId
        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("missing params = " + PLACEMENT_ID);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + PLACEMENT_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("gameId = " + gameId + ", placementId = " + placementId);

        // add to rewarded video listener map
        mPlacementIdToRewardedVideoSmashListener.put(placementId, listener);

        // add placementId to init callback map
        mRewardedVideoPlacementIdsForInitCallbacks.add(placementId);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(gameId, config);
                break;
            case INIT_STATE_SUCCESS:
                listener.onRewardedVideoInitSuccess();
                break;
            case INIT_STATE_FAILED:
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("UnityAds SDK init failed", IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                break;

        }
    }

    // used for flows when the mediation doesn't need to get a callback for init
    @Override
    public void initAndLoadRewardedVideo(String appKey, String userId, JSONObject config, JSONObject adData, RewardedVideoSmashListener listener) {
        String gameId = config.optString(GAME_ID);
        String placementId = config.optString(PLACEMENT_ID);

        // check if OS is supported
        if (!isOSSupported()) {
            IronSourceError error = errorForUnsupportedAdapter(IronSourceConstants.REWARDED_VIDEO_AD_UNIT);
            IronLog.INTERNAL.error(error.getErrorMessage());
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        // check gameId
        if (TextUtils.isEmpty(gameId)) {
            IronLog.INTERNAL.error("missing params = " + GAME_ID);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        // check placementId
        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("missing params = " + PLACEMENT_ID);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        IronLog.ADAPTER_API.verbose("gameId = " + gameId + ", placementId = " + placementId);

        //add to rewarded video listener map
        mPlacementIdToRewardedVideoSmashListener.put(placementId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(gameId, config);
                break;
            case INIT_STATE_SUCCESS:
                loadRewardedVideoInternal(placementId, null, listener);
                break;
            case INIT_STATE_FAILED:
                listener.onRewardedVideoAvailabilityChanged(false);
                break;
        }
    }

    @Override
    public void loadRewardedVideoForBidding(JSONObject config, JSONObject adData, String serverData, RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        loadRewardedVideoInternal(placementId, serverData, listener);
    }

    @Override
    public void loadRewardedVideo(final JSONObject config, JSONObject adData, RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        loadRewardedVideoInternal(placementId, null, listener);
    }

    private void loadRewardedVideoInternal(String placementId, String serverData, RewardedVideoSmashListener listener) {
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        mRewardedVideoAdsAvailability.put(placementId, false);
        UnityAdsRewardedVideoListener rewardedVideoAdListener = new UnityAdsRewardedVideoListener(UnityAdsAdapter.this, listener, placementId);
        mPlacementIdToRewardedVideoListener.put(placementId, rewardedVideoAdListener);

        UnityAdsLoadOptions loadOptions = new UnityAdsLoadOptions();

        // objectId is used to identify a loaded ad and to show it
        String mObjectId = UUID.randomUUID().toString();
        loadOptions.setObjectId(mObjectId);

        if (!TextUtils.isEmpty(serverData)) {
            // add adMarkup for bidder instances
            loadOptions.setAdMarkup(serverData);
        }

        mRewardedVideoPlacementIdToLoadedAdObjectId.put(placementId, mObjectId);

        UnityAds.load(placementId, loadOptions, rewardedVideoAdListener);

    }

    @Override
    public void showRewardedVideo(JSONObject config, RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        if (isRewardedVideoAvailable(config)) {
            mRewardedVideoAdsAvailability.put(placementId, false);

            if (!TextUtils.isEmpty(getDynamicUserId())) {
                synchronized (mUnityAdsStorageLock) {
                    PlayerMetaData playerMetaData = new PlayerMetaData(ContextProvider.getInstance().getApplicationContext());
                    playerMetaData.setServerId(getDynamicUserId());
                    playerMetaData.commit();
                }
            }

            UnityAdsRewardedVideoListener rewardedVideoAdListener = mPlacementIdToRewardedVideoListener.get(placementId);

            String objectId = mRewardedVideoPlacementIdToLoadedAdObjectId.get(placementId);
            UnityAdsShowOptions showOptions = new UnityAdsShowOptions();
            showOptions.setObjectId(objectId);

            UnityAds.show(ContextProvider.getInstance().getCurrentActiveActivity(), placementId, showOptions, rewardedVideoAdListener);
        } else {
            listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
        }


    }

    @Override
    public boolean isRewardedVideoAvailable(JSONObject config) {
        if (!isOSSupported()) {
            IronSourceError error = errorForUnsupportedAdapter(IronSourceConstants.REWARDED_VIDEO_AD_UNIT);
            IronLog.INTERNAL.error(error.getErrorMessage());
            return false;
        }

        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        return mRewardedVideoAdsAvailability.containsKey(placementId) && mRewardedVideoAdsAvailability.get(placementId);
    }


    @Override
    public Map<String, Object> getRewardedVideoBiddingData(JSONObject config, JSONObject adData) {
        return getBiddingData();
    }

    //endregion

    //region Interstitial API

    @Override
    public void initInterstitialForBidding(String appKey, String userId, JSONObject config, InterstitialSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        initInterstitial(appKey, userId, config, listener);
    }

    @Override
    public void initInterstitial(String appKey, String userId, JSONObject config, InterstitialSmashListener listener) {
        String gameId = config.optString(GAME_ID);
        String placementId = config.optString(PLACEMENT_ID);

        // check if OS is supported
        if (!isOSSupported()) {
            IronSourceError error = errorForUnsupportedAdapter(IronSourceConstants.INTERSTITIAL_AD_UNIT);
            IronLog.INTERNAL.error(error.getErrorMessage());
            listener.onInterstitialInitFailed(error);
            return;
        }

        // check gameId
        if (TextUtils.isEmpty(gameId)) {
            IronLog.INTERNAL.error("missing params = " + GAME_ID);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + GAME_ID, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        // check placementId
        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("missing params = " + PLACEMENT_ID);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + PLACEMENT_ID, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("gameId = " + gameId + ", placementId = " + placementId);

        //add to interstitial listener map
        mPlacementIdToInterstitialSmashListener.put(placementId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(gameId, config);
                break;
            case INIT_STATE_SUCCESS:
                listener.onInterstitialInitSuccess();
                break;
            case INIT_STATE_FAILED:
                listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("UnityAds SDK init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT));
                break;
        }
    }

    @Override
    public void loadInterstitialForBidding(JSONObject config, JSONObject adData, String serverData, InterstitialSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        loadInterstitialInternal(config, listener, serverData, placementId);
    }

    @Override
    public void loadInterstitial(JSONObject config, JSONObject adData, InterstitialSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        loadInterstitialInternal(config, listener, null, placementId);
    }

    private void loadInterstitialInternal(JSONObject config, InterstitialSmashListener listener, String serverData, String placementId) {
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        mInterstitialAdsAvailability.put(placementId, false);

        UnityAdsInterstitialListener interstitialAdListener = new UnityAdsInterstitialListener(UnityAdsAdapter.this, listener, placementId);
        mPlacementIdToInterstitialListener.put(placementId, interstitialAdListener);

        UnityAdsLoadOptions loadOptions = new UnityAdsLoadOptions();

        // objectId is used to identify a loaded ad and to show it
        String mObjectId = UUID.randomUUID().toString();
        loadOptions.setObjectId(mObjectId);

        if (!TextUtils.isEmpty(serverData)) {
            // add adMarkup for bidder instances
            loadOptions.setAdMarkup(serverData);
        }

        mInterstitialPlacementIdToLoadedAdObjectId.put(placementId, mObjectId);

        UnityAds.load(placementId, loadOptions, interstitialAdListener);
    }

    @Override
    public void showInterstitial(JSONObject config, InterstitialSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        if (isInterstitialReady(config)) {
            mInterstitialAdsAvailability.put(placementId, false);

            Activity currentActiveActivity = ContextProvider.getInstance().getCurrentActiveActivity();
            UnityAdsInterstitialListener unityAdsInterstitialListener = mPlacementIdToInterstitialListener.get(placementId);

            String mObjectId = mInterstitialPlacementIdToLoadedAdObjectId.get(placementId);
            UnityAdsShowOptions showOptions = new UnityAdsShowOptions();
            showOptions.setObjectId(mObjectId);

            UnityAds.show(currentActiveActivity, placementId, showOptions, unityAdsInterstitialListener);

        } else {
            listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    @Override
    public boolean isInterstitialReady(JSONObject config) {
        if (!isOSSupported()) {
            IronSourceError error = errorForUnsupportedAdapter(IronSourceConstants.INTERSTITIAL_AD_UNIT);
            IronLog.INTERNAL.error(error.getErrorMessage());
            return false;
        }

        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        return mInterstitialAdsAvailability.containsKey(placementId) && mInterstitialAdsAvailability.get(placementId);
    }

    @Override
    public Map<String, Object> getInterstitialBiddingData(JSONObject config, JSONObject adData) {
        return getBiddingData();
    }
    //endregion

    //region Banner API
    @Override
    public void initBannerForBidding(String appKey, String userId, JSONObject config, BannerSmashListener listener) {
        initBanners(appKey, userId, config, listener);
    }

    @Override
    public void initBanners(String appKey, String userId, final JSONObject config, final BannerSmashListener listener) {
        String gameId = config.optString(GAME_ID);
        String placementId = config.optString(PLACEMENT_ID);

        // check if OS is supported
        if (!isOSSupported()) {
            IronSourceError error = errorForUnsupportedAdapter(IronSourceConstants.BANNER_AD_UNIT);
            IronLog.INTERNAL.error(error.getErrorMessage());
            listener.onBannerInitFailed(error);
            return;
        }

        // check gameId
        if (TextUtils.isEmpty(gameId)) {
            IronLog.INTERNAL.error("missing params = " + GAME_ID);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + GAME_ID, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        // check placementId
        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("missing params = " + PLACEMENT_ID);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + PLACEMENT_ID, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("gameId = " + gameId + ", placementId = " + placementId);

        //add to banner listener map
        mPlacementIdToBannerSmashListener.put(placementId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(gameId, config);
                break;
            case INIT_STATE_SUCCESS:
                listener.onBannerInitSuccess();
                break;
            case INIT_STATE_FAILED:
                listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("UnityAds SDK init failed", IronSourceConstants.BANNER_AD_UNIT));
                break;
        }
    }

    @Override
    public void loadBannerForBidding(JSONObject config, JSONObject adData, String serverData, IronSourceBannerLayout banner, BannerSmashListener listener) {
        loadBannerInternal(config,adData, banner,listener, serverData);
    }

    @Override
    public void loadBanner(JSONObject config, JSONObject adData, IronSourceBannerLayout banner, BannerSmashListener listener) {
        loadBannerInternal(config,adData, banner,listener, null);
    }

    public void loadBannerInternal(final JSONObject config, final JSONObject adData, final IronSourceBannerLayout banner, final BannerSmashListener listener, String serverData) {
        String placementId = config.optString(PLACEMENT_ID);

        // check banner
        if (banner == null) {
            IronLog.INTERNAL.error("banner is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner is null"));
            return;
        }

        // check size
        if (!isBannerSizeSupported(banner.getSize())) {
            IronLog.ADAPTER_API.verbose("size not supported, size = " + banner.getSize().getDescription());
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getProviderName()));
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // create banner
        BannerView bannerView = getBannerView(banner, placementId, listener);

        UnityAdsLoadOptions loadOptions = new UnityAdsLoadOptions();

        // objectId is used to identify a loaded ad and to show it
        String mObjectId = UUID.randomUUID().toString();
        loadOptions.setObjectId(mObjectId);

        if (!TextUtils.isEmpty(serverData)) {
            // add adMarkup for bidder instances
            loadOptions.setAdMarkup(serverData);
        }

        // load
        bannerView.load(loadOptions);
    }

    @Override
    public void destroyBanner(JSONObject config) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        if (mPlacementIdToBannerAd.get(placementId) != null) {
            mPlacementIdToBannerAd.get(placementId).destroy();
            mPlacementIdToBannerAd.remove(placementId);
        }
    }

    @Override
    public Map<String, Object> getBannerBiddingData(JSONObject config, JSONObject adData) {
        return getBiddingData();
    }

    //endregion

    // region memory handling

    @Override
    public void releaseMemory(IronSource.AD_UNIT adUnit, JSONObject config) {
        IronLog.INTERNAL.verbose("adUnit = " + adUnit);

        if (adUnit == IronSource.AD_UNIT.REWARDED_VIDEO) {
            mPlacementIdToRewardedVideoSmashListener.clear();
            mPlacementIdToRewardedVideoListener.clear();
            mRewardedVideoPlacementIdToLoadedAdObjectId.clear();
            mRewardedVideoAdsAvailability.clear();
            mRewardedVideoPlacementIdsForInitCallbacks.clear();
        } else if (adUnit == IronSource.AD_UNIT.INTERSTITIAL) {
            mPlacementIdToInterstitialSmashListener.clear();
            mPlacementIdToInterstitialListener.clear();
            mInterstitialPlacementIdToLoadedAdObjectId.clear();
            mInterstitialAdsAvailability.clear();
        } else if (adUnit == IronSource.AD_UNIT.BANNER) {
            for (BannerView adView : mPlacementIdToBannerAd.values()) {
                adView.destroy();
            }

            mPlacementIdToBannerSmashListener.clear();
            mPlacementIdToBannerListener.clear();
            mPlacementIdToBannerAd.clear();
        }
    }
    //endregion

    //region legal
    @Override
    protected void setConsent(boolean consent) {
        IronLog.ADAPTER_API.verbose("setConsent = " + consent);
        setUnityAdsMetaData(CONSENT_GDPR, consent);
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

            if (MetaDataUtils.isValidMetaData(key, UNITYADS_METADATA_COPPA_KEY, formattedValue)) {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue));
            }
        }
    }

    private void setUnityAdsMetaData(String key, boolean value) {
        IronLog.INTERNAL.verbose("key = " + key + "value = " + value);

        synchronized (mUnityAdsStorageLock) {
            MetaData metaData = new MetaData(ContextProvider.getInstance().getApplicationContext());
            metaData.set(key, value);

            // in case of COPPA we need to set an additional key
            if (key.equals(UNITYADS_COPPA)) {
                metaData.set(GAME_DESIGNATION, MIXED_AUDIENCE); // This is a mixed audience game.
            }

            metaData.commit();
        }

    }

    private void setCOPPAValue(boolean value) {
        IronLog.ADAPTER_API.verbose("value = " + value);
        setUnityAdsMetaData(UNITYADS_COPPA, value);
    }

    private void setCCPAValue(boolean value) {
        IronLog.ADAPTER_API.verbose("value = " + value);
        // The UnityAds CCPA API expects an indication if the user opts in to targeted advertising.
        // Given that this is opposite to the ironSource Mediation CCPA flag of do_not_sell
        // we will use the opposite value of what is passed to this method
        boolean optIn = !value;
        setUnityAdsMetaData(CONSENT_CCPA, optIn);
    }

    //endregion

    //region Banner Helpers

    //In case this method is called before the init we will try using the token that was received asynchronically
    private Map<String, Object> getBiddingData() {
        String bidderToken;
        if (isAdsGateway) {
            bidderToken = UnityAds.getToken();
        } else {
            if (InitState.INIT_STATE_SUCCESS == mInitState) {
                bidderToken = UnityAds.getToken();
            } else if (!TextUtils.isEmpty(asyncToken)) {
                bidderToken = asyncToken;
                // Fetching a fresh async token for the next load
                getAsyncToken();
            } else {
                IronLog.INTERNAL.verbose("returning null as token since init did not finish successfully and async token did not return");
                return null;
            }
        }

        String returnedToken = (!TextUtils.isEmpty(bidderToken)) ? bidderToken : "";
        IronLog.ADAPTER_API.verbose("token = " + returnedToken);
        Map<String, Object> ret = new HashMap<>();
        ret.put("token", returnedToken);
        return ret;
    }

    private Boolean isBannerSizeSupported(ISBannerSize size) {
        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
            case "RECTANGLE":
            case "SMART":
                return true;
            default:
                return false;
        }
    }

    private UnityBannerSize getBannerSize(ISBannerSize size, boolean isLargeScreen) {
        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
                return new UnityBannerSize(320, 50);
            case "RECTANGLE":
                return new UnityBannerSize(300, 250);
            case "SMART":
                return isLargeScreen ? new UnityBannerSize(728, 90) : new UnityBannerSize(320, 50);
        }

        return null;
    }

    private BannerView getBannerView(IronSourceBannerLayout banner, String placementId, final BannerSmashListener listener) {
        // Remove previously created banner view
        if (mPlacementIdToBannerAd.get(placementId) != null) {
            mPlacementIdToBannerAd.get(placementId).destroy();
            mPlacementIdToBannerAd.remove(placementId);
        }

        // get size
        UnityBannerSize unityBannerSize = getBannerSize(banner.getSize(),
                AdapterUtils.isLargeScreen(ContextProvider.getInstance().getApplicationContext()));

        // create banner
        BannerView bannerView = new BannerView(ContextProvider.getInstance().getCurrentActiveActivity(),
                placementId, unityBannerSize);

        // add listener
        UnityAdsBannerListener bannerAdListener = new UnityAdsBannerListener(UnityAdsAdapter.this, listener, placementId);
        mPlacementIdToBannerListener.put(placementId, bannerAdListener);
        bannerView.setListener(bannerAdListener);

        // add to map
        mPlacementIdToBannerAd.put(placementId, bannerView);

        return bannerView;
    }

    protected FrameLayout.LayoutParams createLayoutParams(UnityBannerSize size) {
        int widthPixel = AdapterUtils.dpToPixels(ContextProvider.getInstance().getApplicationContext(), size.getWidth());
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(widthPixel, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        return layoutParams;
    }

    private IronSourceError errorForUnsupportedAdapter(String adUnit) {
        IronSourceError error = ErrorBuilder.buildInitFailedError("UnityAds SDK version is not supported", adUnit);
        return error;
    }

    private boolean isOSSupported() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT;
    }

    //endregion

    //region Adapter Helpers
    private int getUnityAdsInitializationErrorCode(UnityAds.UnityAdsInitializationError error) {
        if (error != null) {
            for (UnityAds.UnityAdsInitializationError e : UnityAds.UnityAdsInitializationError.values()) {
                if (e.name().equalsIgnoreCase(error.toString())) {
                    return UnityAds.UnityAdsInitializationError.valueOf(error.toString()).ordinal();
                }
            }
        }

        return IronSourceError.ERROR_CODE_GENERIC;
    }

    protected int getUnityAdsLoadErrorCode(UnityAds.UnityAdsLoadError error) {
        if (error != null) {
            for (UnityAds.UnityAdsLoadError e : UnityAds.UnityAdsLoadError.values()) {
                if (e.name().equalsIgnoreCase(error.toString())) {
                    return UnityAds.UnityAdsLoadError.valueOf(error.toString()).ordinal();
                }
            }
        }
        return IronSourceError.ERROR_CODE_GENERIC;
    }

    protected int getUnityAdsShowErrorCode(UnityAds.UnityAdsShowError error) {
        if (error != null) {
            for (UnityAds.UnityAdsShowError e : UnityAds.UnityAdsShowError.values()) {
                if (e.name().equalsIgnoreCase(error.toString())) {
                    return UnityAds.UnityAdsShowError.valueOf(error.toString()).ordinal();
                }
            }
        }

        return IronSourceError.ERROR_CODE_GENERIC;
    }

    // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
    @Override
    public LoadWhileShowSupportState getLoadWhileShowSupportState(JSONObject mAdUnitSettings) {
        LoadWhileShowSupportState loadWhileShowSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE;

        if (mAdUnitSettings != null) {
            boolean isSupportedLWS = mAdUnitSettings.optBoolean(LWS_SUPPORT_STATE, true);

            if (!isSupportedLWS) {
                loadWhileShowSupportState = LoadWhileShowSupportState.NONE;
            }
        }

        return loadWhileShowSupportState;
    }

    public void getAsyncToken() {
        IronLog.INTERNAL.verbose();
        UnityAds.getToken(new IUnityAdsTokenListener() {
            @Override
            public void onUnityAdsTokenReady(String token) {
                IronLog.ADAPTER_CALLBACK.verbose("async token returned");
                asyncToken = token;
            }
        });
    }

    //endregion
}