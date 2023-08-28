package com.ironsource.adapters.vungle;

import static com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN;

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
import com.ironsource.mediationsdk.metadata.MetaDataUtils;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import com.vungle.warren.AdConfig;
import com.vungle.warren.BannerAdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.InitCallback;
import com.vungle.warren.Plugin;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleApiClient;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.VungleSettings;
import com.vungle.warren.error.VungleException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

class VungleAdapter extends AbstractAdapter implements INetworkInitCallbackListener, InitCallback {

    // Adapter version
    private static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String GitHash = BuildConfig.GitHash;

    // Vungle keys
    private static final String APP_ID = "AppID";
    private static final String PLACEMENT_ID = "PlacementId";

    // Meta data flags
    private static final String VUNGLE_COPPA_FLAG = "vungle_coppa";
    private static final String ORIENTATION_FLAG = "vungle_adorientation";

    // Vungle Constants
    private static final String CONSENT_MESSAGE_VERSION = "1.0.0";
    private static final String ORIENTATION_PORTRAIT = "PORTRAIT";
    private static final String ORIENTATION_LANDSCAPE = "LANDSCAPE";
    private static final String ORIENTATION_AUTO_ROTATE = "AUTO_ROTATE";

    private static final String LWS_SUPPORT_STATE = "isSupportedLWSByInstance";

    // Rewarded video collections
    private ConcurrentHashMap<String, RewardedVideoSmashListener> mPlacementIdToRewardedVideoSmashListener;
    private ConcurrentHashMap<String, String> mPlacementIdToRewardedVideoServerData;
    private CopyOnWriteArraySet<String> mRewardedVideoPlacementIdsForInitCallbacks;

    // Interstitial maps
    private ConcurrentHashMap<String, InterstitialSmashListener> mPlacementIdToInterstitialSmashListener;
    private ConcurrentHashMap<String, String> mPlacementIdToInterstitialServerData;

    // Banner maps
    private ConcurrentHashMap<String, BannerSmashListener> mPlacementIdToBannerSmashListener;
    private ConcurrentHashMap<String, String> mPlacementIdToBannerServerData;
    protected ConcurrentHashMap<String, VungleBanner> mPlacementIdToBannerView;

    // members for network
    private static Boolean mConsent = null;
    private static Boolean mCCPA = null;
    private static String mAdOrientation = null;

    // init state possible values
    private enum InitState {
        INIT_STATE_NONE,
        INIT_STATE_IN_PROGRESS,
        INIT_STATE_SUCCESS,
        INIT_STATE_FAILED
    }

    // Handle init callback for all adapter instances
    private static AtomicBoolean mWasInitCalled = new AtomicBoolean(false);
    private static HashSet<INetworkInitCallbackListener> initCallbackListeners = new HashSet<>();
    private static InitState mInitState = InitState.INIT_STATE_NONE;

    //region Adapter Methods
    public static VungleAdapter startAdapter(String providerName) {
        return new VungleAdapter(providerName);
    }

    private VungleAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose();

        // Rewarded video
        mPlacementIdToRewardedVideoSmashListener = new ConcurrentHashMap<>();
        mPlacementIdToRewardedVideoServerData = new ConcurrentHashMap<>();
        mRewardedVideoPlacementIdsForInitCallbacks = new CopyOnWriteArraySet<>();

        // Interstitial
        mPlacementIdToInterstitialSmashListener = new ConcurrentHashMap<>();
        mPlacementIdToInterstitialServerData = new ConcurrentHashMap<>();

        // Banner
        mPlacementIdToBannerSmashListener = new ConcurrentHashMap<>();
        mPlacementIdToBannerServerData = new ConcurrentHashMap<>();
        mPlacementIdToBannerView = new ConcurrentHashMap<>();
    }

    // Get the network and adapter integration data
    public static IntegrationData getIntegrationData(Context context) {
        return new IntegrationData("Vungle", VERSION);
    }

    @Override
    // get adapter version
    public String getVersion() {
        return VERSION;
    }

    @Override
    //get network sdk version
    public String getCoreSDKVersion() {
        return getAdapterSDKVersion();
    }

    public static String getAdapterSDKVersion() {
        return com.vungle.warren.BuildConfig.VERSION_NAME;
    }

    public boolean isUsingActivityBeforeImpression(@NotNull IronSource.AD_UNIT adUnit) {
        return false;
    }

    //endregion

    //region Initializations methods and callbacks
    private void initSDK(final String appId) {
        // add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(VungleAdapter.this);
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            IronLog.ADAPTER_API.verbose("appId = " + appId);

            mInitState = InitState.INIT_STATE_IN_PROGRESS;

            Plugin.addWrapperInfo(VungleApiClient.WrapperFramework.ironsource, getVersion());

            VungleSettings vungleSettings = new VungleSettings.Builder()
                    .disableBannerRefresh()
                    .build();

            Vungle.init(appId, ContextProvider.getInstance().getApplicationContext(), this, vungleSettings);
        }
    }

    @Override
    public void onSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose("Succeeded to initialize SDK");

        mInitState = InitState.INIT_STATE_SUCCESS;

        if (mConsent != null) {
            setConsent(mConsent);
        }

        if (mCCPA != null) {
            setCCPAValue(mCCPA);
        }

        for (INetworkInitCallbackListener adapter : initCallbackListeners) {
            adapter.onNetworkInitCallbackSuccess();
        }

        initCallbackListeners.clear();
    }

    @Override
    public void onError(VungleException exception) {
        IronLog.ADAPTER_CALLBACK.verbose("Failed to initialize SDK");

        mInitState = InitState.INIT_STATE_FAILED;

        for (INetworkInitCallbackListener adapter : initCallbackListeners) {
            adapter.onNetworkInitCallbackFailed("Vungle sdk init failed - " + exception.getLocalizedMessage());
        }

        initCallbackListeners.clear();
    }

    @Override
    public void onAutoCacheAdAvailable(String placementId) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId " + placementId);
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        // rewarded video listener
        for (String placementId : mPlacementIdToRewardedVideoSmashListener.keySet()) {
            RewardedVideoSmashListener listener = mPlacementIdToRewardedVideoSmashListener.get(placementId);

            if (mRewardedVideoPlacementIdsForInitCallbacks.contains(placementId)) {
                listener.onRewardedVideoInitSuccess();
            } else {
                loadRewardedVideoInternal(placementId, listener, null);
            }
        }

        // interstitial listener
        for (InterstitialSmashListener listener : mPlacementIdToInterstitialSmashListener.values()) {
            listener.onInterstitialInitSuccess();
        }

        // banner listener
        for (BannerSmashListener listener : mPlacementIdToBannerSmashListener.values()) {
            listener.onBannerInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        // rewarded video listener
        for (String placementId : mPlacementIdToRewardedVideoSmashListener.keySet()) {
            RewardedVideoSmashListener listener = mPlacementIdToRewardedVideoSmashListener.get(placementId);

            if (mRewardedVideoPlacementIdsForInitCallbacks.contains(placementId)) {
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            } else {
                listener.onRewardedVideoAvailabilityChanged(false);
            }
        }

        // interstitial listener
        for (InterstitialSmashListener listener : mPlacementIdToInterstitialSmashListener.values()) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }

        // banner listener
        for (BannerSmashListener listener : mPlacementIdToBannerSmashListener.values()) {
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.BANNER_AD_UNIT));
        }
    }

    //endregion

    //region Rewarded Video API
    @Override
    // Used for flows when the mediation needs to get a callback for init
    public void initRewardedVideoWithCallback(String appKey, String userId, JSONObject config, RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        String appId = config.optString(APP_ID);

        // Configuration Validation
        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("Missing param - " + PLACEMENT_ID);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + PLACEMENT_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("Missing param - " + APP_ID);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + APP_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId + ", appId = " + appId);

        //add to rewarded video listener map
        mPlacementIdToRewardedVideoSmashListener.put(placementId, listener);
        mRewardedVideoPlacementIdsForInitCallbacks.add(placementId);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(appId);
                break;
            case INIT_STATE_SUCCESS:
                listener.onRewardedVideoInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Vungle SDK init failed", IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                break;
        }
    }

    @Override
    // used for flows when the mediation doesn't need to get a callback for init
    public void initAndLoadRewardedVideo(String appKey, String userId, JSONObject config, JSONObject adData, RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        String appId = config.optString(APP_ID);

        // Configuration Validation
        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("Missing param - " + PLACEMENT_ID);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("Missing param - " + APP_ID);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId + ", appId = " + appId);

        //add to rewarded video listener map
        mPlacementIdToRewardedVideoSmashListener.put(placementId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(appId);
                break;
            case INIT_STATE_SUCCESS:
                if (isRewardedVideoAdAvailableInternal(placementId)) {
                    IronLog.ADAPTER_API.verbose("ad already cached for placement Id " + placementId);
                    listener.onRewardedVideoAvailabilityChanged(true);
                } else {
                    loadRewardedVideoInternal(placementId, listener, null);
                }
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                listener.onRewardedVideoAvailabilityChanged(false);
                break;
        }
    }

    @Override
    public void loadRewardedVideoForBidding(JSONObject config, JSONObject adData, String serverData, final RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        mPlacementIdToRewardedVideoServerData.put(placementId, serverData);
        loadRewardedVideoInternal(placementId, listener, serverData);
    }

    @Override
    public void loadRewardedVideo(final JSONObject config, JSONObject adData, final RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);

        if (isRewardedVideoAdAvailableInternal(placementId)) {
            IronLog.ADAPTER_API.verbose("ad already cached for placement Id " + placementId);
            listener.onRewardedVideoAvailabilityChanged(true);
        } else {
            loadRewardedVideoInternal(placementId, listener, null);
        }
    }

    private void loadRewardedVideoInternal(String placementId, RewardedVideoSmashListener listener, String serverData) {
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // added to rewarded video listener map as it is required for the availability check
        mPlacementIdToRewardedVideoSmashListener.put(placementId, listener);

        // create Vungle load listener
        VungleRewardedVideoLoadListener vungleLoadListener = new VungleRewardedVideoLoadListener(listener);

        if (!TextUtils.isEmpty(serverData)) {
            // Load rewarded video for bidding instance
            Vungle.loadAd(placementId, serverData, createAdConfig(), vungleLoadListener);
        } else {
            // Load rewarded video for non bidding instance
            Vungle.loadAd(placementId, vungleLoadListener);
        }
    }

    @Override
    public void showRewardedVideo(JSONObject config, final RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // if we can play
        if (isRewardedVideoAdAvailableInternal(placementId)) {
            // dynamic user id
            if (!TextUtils.isEmpty(getDynamicUserId())) {
                Vungle.setIncentivizedFields(getDynamicUserId(), null, null, null, null);
            }

            // create Vungle play listener
            VungleRewardedVideoPlayListener vunglePlayListener = new VungleRewardedVideoPlayListener(listener);

            // get ad config
            AdConfig adConfig = createAdConfig();

            // get server data
            String serverData = mPlacementIdToRewardedVideoServerData.get(placementId);

            if (!TextUtils.isEmpty(serverData)) {
                // Show rewarded video for bidding instance
                Vungle.playAd(placementId, serverData, adConfig, vunglePlayListener);
            } else {
                // Show rewarded video for non bidding instance
                Vungle.playAd(placementId, adConfig, vunglePlayListener);
            }
        } else {
            IronLog.INTERNAL.error("There is no ad available for placementId = " + placementId);
            listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
        }
    }

    @Override
    public boolean isRewardedVideoAvailable(JSONObject config) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // Vungle cache ads that were loaded in the last week.
        // This means that Vungle.canPlayAd() could return true for placements that we didn't try to load during this session.
        // This is the reason we also check if the placementId is contained in the ConcurrentHashMap
        return Vungle.isInitialized() && mPlacementIdToRewardedVideoSmashListener.containsKey(placementId) && isRewardedVideoAdAvailableInternal(placementId);
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
    public void initInterstitial(final String appKey, final String userId, final JSONObject config, InterstitialSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        String appId = config.optString(APP_ID);

        // Configuration Validation
        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("Missing param - " + PLACEMENT_ID);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + PLACEMENT_ID, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("Missing param - " + APP_ID);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + APP_ID, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId + ", appId = " + appId);

        //add to interstitial listener map
        mPlacementIdToInterstitialSmashListener.put(placementId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(appId);
                break;
            case INIT_STATE_SUCCESS:
                listener.onInterstitialInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Vungle SDK init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT));
                break;
        }
    }

    @Override
    public void loadInterstitialForBidding(JSONObject config, JSONObject adData, String serverData, InterstitialSmashListener listener) {
        final String placementId = config.optString(PLACEMENT_ID);
        mPlacementIdToInterstitialServerData.put(placementId, serverData);
        loadInterstitialInternal(placementId, listener, serverData);
    }

    @Override
    public void loadInterstitial(JSONObject config, JSONObject adData, final InterstitialSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        loadInterstitialInternal(placementId, listener, null);
    }

    private void loadInterstitialInternal(final String placementId, final InterstitialSmashListener listener, String serverData) {
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // added to interstitial listener map as it is required for the availability check
        mPlacementIdToInterstitialSmashListener.put(placementId, listener);

        VungleInterstitialLoadListener vungleLoadListener = new VungleInterstitialLoadListener(listener);

        if (!TextUtils.isEmpty(serverData)) {
            // Load interstitial for bidding instance
            Vungle.loadAd(placementId, serverData, createAdConfig(), vungleLoadListener);
        } else {
            // Load interstitial for non bidding instance
            Vungle.loadAd(placementId, vungleLoadListener);
        }
    }

    @Override
    public void showInterstitial(JSONObject config, final InterstitialSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // if we can play
        if (isInterstitialAdAvailableInternal(placementId)) {
            // create Vungle play listener
            VungleInterstitialPlayListener vunglePlayListener = new VungleInterstitialPlayListener(listener);

            // get ad config
            AdConfig adConfig = createAdConfig();

            // get server data
            String serverData = mPlacementIdToInterstitialServerData.get(placementId);

            if (!TextUtils.isEmpty(serverData)) {
                // Show interstitial for bidding instance
                Vungle.playAd(placementId, serverData, adConfig, vunglePlayListener);
            } else {
                // Show interstitial for non bidding instance
                Vungle.playAd(placementId, adConfig, vunglePlayListener);
            }
        } else {
            IronLog.INTERNAL.error("There is no ad available for placementId = " + placementId);
            listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    @Override
    public boolean isInterstitialReady(JSONObject config) {
        String placementId = config.optString(PLACEMENT_ID);

        // Vungle cache ads that were loaded in the last week.
        // This means that Vungle.canPlayAd() could return true for placements that we didn't try to load during this session.
        // This is the reason we also check if the placementId is contained in the ConcurrentHashMap
        return Vungle.isInitialized() && mPlacementIdToInterstitialSmashListener.containsKey(placementId) && isInterstitialAdAvailableInternal(placementId);
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
    public void initBanners(String appKey, String userId, JSONObject config, BannerSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        String appId = config.optString(APP_ID);

        // Configuration Validation
        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("Missing param - " + PLACEMENT_ID);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + PLACEMENT_ID, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("Missing param - " + APP_ID);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + APP_ID, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId + ", appId = " + appId);

        //add to banner listener map
        mPlacementIdToBannerSmashListener.put(placementId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(appId);
                break;
            case INIT_STATE_SUCCESS:
                listener.onBannerInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Vungle SDK init failed", IronSourceConstants.BANNER_AD_UNIT));
                break;
        }
    }

    @Override
    public void loadBannerForBidding(JSONObject config, JSONObject adData, String serverData, IronSourceBannerLayout banner, BannerSmashListener listener) {
        final String placementId = config.optString(PLACEMENT_ID);
        mPlacementIdToBannerServerData.put(placementId, serverData);
        loadBannerInternal(placementId, banner, listener, serverData);
    }

    @Override
    public void loadBanner(JSONObject config, JSONObject adData, final IronSourceBannerLayout banner, final BannerSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        loadBannerInternal(placementId, banner, listener, null);
    }

    private void loadBannerInternal(final String placementId, final IronSourceBannerLayout banner, final BannerSmashListener listener, final String serverData) {
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        if (banner == null) {
            IronLog.INTERNAL.verbose("banner is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getProviderName()));
            return;
        }

        // verify size
        if (!isBannerSizeSupported(banner.getSize())) {
            IronLog.ADAPTER_API.verbose("size not supported, size = " + banner.getSize().getDescription());
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getProviderName()));
            return;
        }

        //add to banner listener map
        mPlacementIdToBannerSmashListener.put(placementId, listener);

        // run on main thread
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                ISBannerSize size = banner.getSize();
                if (size == null) {
                    IronLog.INTERNAL.error("size not supported, size is null");
                    listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getProviderName()));
                    return;
                }

                // get size
                AdConfig.AdSize bannerSize = getBannerSize(size);
                BannerAdConfig bannerAdConfig = new BannerAdConfig(bannerSize);
                IronLog.ADAPTER_API.verbose("bannerSize = " + bannerSize);

                // create Vungle load listener
                VungleBannerLoadListener vungleLoadListener = new VungleBannerLoadListener(
                        VungleAdapter.this,
                        listener,
                        size
                );

                if (!TextUtils.isEmpty(serverData)) {
                    // Load banner for bidding instance
                    Banners.loadBanner(
                            placementId,
                            serverData,
                            bannerAdConfig,
                            vungleLoadListener
                    );
                } else {
                    // Load banner for non bidding instance
                    Banners.loadBanner(
                            placementId,
                            bannerAdConfig,
                            vungleLoadListener
                    );
                }
            }
        });
    }

    @Override
    public void destroyBanner(JSONObject config) {
        final String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // run on main thread
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (mPlacementIdToBannerView.containsKey(placementId)) {
                    // get banner
                    VungleBanner banner = mPlacementIdToBannerView.get(placementId);

                    // Vungle destroy
                    banner.destroyAd();

                    // remove from map
                    mPlacementIdToBannerView.remove(placementId);
                }

            }
        });
    }

    @Override
    public Map<String, Object> getBannerBiddingData(JSONObject config, JSONObject adData) {
        return getBiddingData();
    }

    //endregion

    // region memory handling
    @Override
    public void releaseMemory(IronSource.AD_UNIT adUnit, JSONObject config) {
        if (adUnit == IronSource.AD_UNIT.REWARDED_VIDEO) {
            mPlacementIdToRewardedVideoSmashListener.clear();
            mPlacementIdToRewardedVideoServerData.clear();
            mRewardedVideoPlacementIdsForInitCallbacks.clear();
        } else if (adUnit == IronSource.AD_UNIT.INTERSTITIAL) {
            mPlacementIdToInterstitialSmashListener.clear();
            mPlacementIdToInterstitialServerData.clear();
        } else if (adUnit == IronSource.AD_UNIT.BANNER) {
            for (VungleBanner adView : mPlacementIdToBannerView.values()) {
                adView.destroyAd();
            }
            mPlacementIdToBannerView.clear();
            mPlacementIdToBannerSmashListener.clear();
            mPlacementIdToBannerServerData.clear();
        }
    }
    //endregion

    // region progressive loading handling
    @Override
    // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
    public LoadWhileShowSupportState getLoadWhileShowSupportState(JSONObject mAdUnitSettings) {
        LoadWhileShowSupportState loadWhileShowSupportState = mLWSSupportState;

        if (mAdUnitSettings != null) {
            boolean isSupportedLWSByInstance = mAdUnitSettings.optBoolean(LWS_SUPPORT_STATE);

            if (isSupportedLWSByInstance) {
                loadWhileShowSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE;
            } else {
                loadWhileShowSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_NETWORK;
            }
        }

        return loadWhileShowSupportState;
    }
    //endregion

    //region legal
    @Override
    protected void setConsent(boolean consent) {
        IronLog.ADAPTER_API.verbose("consent = " + consent);

        if (mInitState == InitState.INIT_STATE_SUCCESS) {
            Vungle.updateConsentStatus(consent ? Vungle.Consent.OPTED_IN : Vungle.Consent.OPTED_OUT, CONSENT_MESSAGE_VERSION);
        } else {
            mConsent = consent;
        }
    }

    protected void setMetaData(String key, List<String> values) {
        if (values.isEmpty()) {
            return;
        }

        // this is a list of 1 value
        String value = values.get(0);
        IronLog.ADAPTER_API.verbose("key = " + key + ", value = " + value);

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value));
        } else if (MetaDataUtils.isValidMetaData(key, ORIENTATION_FLAG, value)) {
            mAdOrientation = value;
        } else {
            String formattedValue = MetaDataUtils.formatValueForType(value, META_DATA_VALUE_BOOLEAN);

            if (MetaDataUtils.isValidMetaData(key, VUNGLE_COPPA_FLAG, formattedValue)) {
                setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue));
            }
        }
    }

    private void setCCPAValue(final boolean ccpa) {
        if (mInitState == InitState.INIT_STATE_SUCCESS) {
            // The Vungle CCPA API expects an indication if the user opts in to targeted advertising.
            // Given that this is opposite to the ironSource Mediation CCPA flag of do_not_sell
            // we will use the opposite value of what is passed to this method
            boolean optIn = !ccpa;
            Vungle.Consent status = optIn ? Vungle.Consent.OPTED_IN : Vungle.Consent.OPTED_OUT;
            IronLog.ADAPTER_API.verbose("key = Vungle.Consent" + ", value = " +  status.name());
            Vungle.updateCCPAStatus(status);
        } else {
            mCCPA = ccpa;
        }
    }

    private void setCOPPAValue(final boolean isUserCoppa) {
        if (mInitState == InitState.INIT_STATE_NONE) {
            IronLog.ADAPTER_API.verbose("coppa = " + isUserCoppa);
            Vungle.updateUserCoppaStatus(isUserCoppa);
        } else {
            IronLog.INTERNAL.verbose("COPPA value can be set only before the initialization of Vungle");
        }
    }

    //endregion

    //region Helpers
    private Map<String, Object> getBiddingData() {
        if (mInitState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error("Returning null as token since init failed");
            return null;
        }

        String bidderToken = Vungle.getAvailableBidTokens(ContextProvider.getInstance().getApplicationContext());
        String returnedToken = (!TextUtils.isEmpty(bidderToken)) ? bidderToken : "";
        String sdkVersion = getCoreSDKVersion();
        IronLog.ADAPTER_API.verbose("sdkVersion = " + sdkVersion);
        IronLog.ADAPTER_API.verbose("token = " + returnedToken);
        Map<String, Object> ret = new HashMap<>();
        ret.put("sdkVersion", sdkVersion);
        ret.put("token", returnedToken);
        return ret;
    }

    private AdConfig createAdConfig() {
        AdConfig adconfig = new AdConfig();

        //set orientation configuration
        if (mAdOrientation != null) {
            switch (mAdOrientation) {
                case ORIENTATION_PORTRAIT:
                    adconfig.setAdOrientation(AdConfig.PORTRAIT);
                    break;
                case ORIENTATION_LANDSCAPE:
                    adconfig.setAdOrientation(AdConfig.LANDSCAPE);
                    break;
                case ORIENTATION_AUTO_ROTATE:
                    adconfig.setAdOrientation(AdConfig.AUTO_ROTATE);
                    break;
            }

            IronLog.INTERNAL.verbose("setAdOrientation to " + adconfig.getAdOrientation());
        }

        return adconfig;
    }

    private boolean isBannerSizeSupported(ISBannerSize size) {
        if(size == null) {
            IronLog.INTERNAL.verbose("Banner size is null");
            return false;
        }
        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
            case "RECTANGLE":
            case "SMART":
                return true;
        }
        return false;
    }

    protected AdConfig.AdSize getBannerSize(ISBannerSize size) {
        if(size == null) {
            IronLog.INTERNAL.verbose("Banner size is null");
            return null;
        }
        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
                return AdConfig.AdSize.BANNER;
            case "RECTANGLE":
                return AdConfig.AdSize.VUNGLE_MREC;
            case "SMART":
                return AdapterUtils.isLargeScreen(ContextProvider.getInstance().getApplicationContext()) ? AdConfig.AdSize.BANNER_LEADERBOARD : AdConfig.AdSize.BANNER;
        }
        return null;
    }

    protected VungleBanner createVungleBannerAdView(String placementId, AdConfig.AdSize bannerSize) {
        BannerAdConfig adConfig = new BannerAdConfig(bannerSize);
        boolean isRectangle = bannerSize == AdConfig.AdSize.VUNGLE_MREC;

        if (isRectangle) {
            // Vungle MREC plays with sound enabled as a default
            adConfig.setMuted(true);
        }

        // get server data
        String serverData = mPlacementIdToBannerServerData.get(placementId);

        // get banner smash listener
        BannerSmashListener smashListener = mPlacementIdToBannerSmashListener.get(placementId);

        // create Vungle play listener
        VungleBannerPlayListener playListener = new VungleBannerPlayListener(smashListener);

        // get banner view
        VungleBanner vungleBanner;

        if (!TextUtils.isEmpty(serverData)) {
            vungleBanner = Banners.getBanner(placementId, serverData, adConfig, playListener);
        } else {
            vungleBanner = Banners.getBanner(placementId, adConfig, playListener);
        }

        if (vungleBanner != null) {
            mPlacementIdToBannerView.put(placementId, vungleBanner);
        }

        return vungleBanner;
    }

    protected FrameLayout.LayoutParams getBannerLayoutParams(ISBannerSize size) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(0,0);

        Context context = ContextProvider.getInstance().getApplicationContext();

        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
                layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(context, 320), AdapterUtils.dpToPixels(context, 50));
                break;
            case "RECTANGLE":
                layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(context, 300), AdapterUtils.dpToPixels(context, 250));
                break;
            case "SMART":
                if (AdapterUtils.isLargeScreen(context)) {
                    layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(context, 728), AdapterUtils.dpToPixels(context, 90));
                } else {
                    layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(context, 320), AdapterUtils.dpToPixels(context, 50));
                }

                break;
        }

        // set gravity
        layoutParams.gravity = Gravity.CENTER;

        return layoutParams;
    }

    protected boolean isRewardedVideoAdAvailableInternal(String placementId) {
        if (mPlacementIdToRewardedVideoServerData.containsKey(placementId)) {
            // get rewarded video server data
            String serverData = mPlacementIdToRewardedVideoServerData.get(placementId);

            // return if ad available or not
            return Vungle.canPlayAd(placementId, serverData);
        }

        return Vungle.canPlayAd(placementId);
    }

    protected boolean isInterstitialAdAvailableInternal(String placementId) {
        if (mPlacementIdToInterstitialServerData.containsKey(placementId)) {
            // get interstitial server data
            String serverData = mPlacementIdToInterstitialServerData.get(placementId);

            // return if ad available or not
            return Vungle.canPlayAd(placementId, serverData);
        }

        return Vungle.canPlayAd(placementId);
    }

    protected boolean isBannerAdAvailableInternal(String placementId, AdConfig.AdSize adSize) {
        if (mPlacementIdToBannerServerData.containsKey(placementId)) {
            // get banner server data
            String serverData = mPlacementIdToBannerServerData.get(placementId);

            // return if ad available or not
            return Banners.canPlayAd(placementId, serverData, adSize);
        }

        return Banners.canPlayAd(placementId, adSize);
    }

    //endregion
}