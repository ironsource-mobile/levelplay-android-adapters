package com.ironsource.adapters.facebook;

import static com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.facebook.ads.AdSettings;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.CacheFlag;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAd.InterstitialAdLoadConfigBuilder;
import com.facebook.ads.RewardData;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAd.RewardedVideoAdLoadConfigBuilder;
import com.facebook.ads.BidderTokenProvider;
import com.ironsource.environment.ContextProvider;
import com.ironsource.environment.StringUtils;
import com.ironsource.mediationsdk.AbstractAdapter;
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
import com.ironsource.mediationsdk.AdapterUtils;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import com.ironsource.mediationsdk.utils.IronSourceUtils;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

public class FacebookAdapter extends AbstractAdapter implements INetworkInitCallbackListener, AudienceNetworkAds.InitListener {

    // Meta mediation service name
    private static final String MEDIATION_NAME = "ironSource";

    // Adapter version
    private static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String GitHash = BuildConfig.GitHash;

    // Meta network keys
    protected final String PLACEMENT_ID = "placementId";
    protected final String ALL_PLACEMENT_IDS = "placementIds";
    protected final static String META_NETWORK_NAME = "Facebook";

    // MetaData flags
    protected final String FACEBOOK_INTERSTITIAL_CACHE_FLAG = "facebook_is_cacheflag";
    protected final String META_INTERSTITIAL_CACHE_FLAG = "meta_is_cacheflag";
    protected final String META_MIXED_AUDIENCE = "meta_mixed_audience";

    // Rewarded Video
    protected ConcurrentHashMap<String, RewardedVideoSmashListener> mRewardedVideoPlacementIdToSmashListener;
    protected ConcurrentHashMap<String, RewardedVideoAd> mRewardedVideoPlacementIdToAd;
    protected ConcurrentHashMap<String, FacebookRewardedVideoAdListener> mRewardedVideoPlacementIdToFacebookAdListener;
    protected ConcurrentHashMap<String, Boolean> mRewardedVideoAdsAvailability;
    protected CopyOnWriteArraySet<String> mRewardedVideoPlacementIdsForInitCallbacks;
    protected ConcurrentHashMap<String, Boolean> mRewardedVideoPlacementIdShowCalled; //flags for show failed occurred on OnError callback

    // Interstitial
    protected ConcurrentHashMap<String, InterstitialSmashListener> mInterstitialPlacementIdToSmashListener;
    protected ConcurrentHashMap<String, InterstitialAd> mInterstitialPlacementIdToAd;
    protected ConcurrentHashMap<String, FacebookInterstitialAdListener> mInterstitialPlacementIdToFacebookAdListener;
    protected ConcurrentHashMap<String, Boolean> mInterstitialAdsAvailability;
    protected ConcurrentHashMap<String, Boolean> mInterstitialPlacementIdShowCalled; //flags for show failed occurred on OnError callback
    protected static EnumSet<CacheFlag> mInterstitialFacebookCacheFlags = EnumSet.allOf(CacheFlag.class); // collected cache flags

    // Banner
    protected ConcurrentHashMap<String, BannerSmashListener> mBannerPlacementIdToSmashListener;
    protected ConcurrentHashMap<String, AdView> mBannerPlacementIdToAd;

    // Init state possible values
    protected enum InitState {
        INIT_STATE_NONE,
        INIT_STATE_IN_PROGRESS,
        INIT_STATE_SUCCESS,
        INIT_STATE_FAILED
    }

    // Handle init callback for all adapter instances
    protected static HashSet<INetworkInitCallbackListener> initCallbackListeners = new HashSet<>();
    protected static InitState mInitState = InitState.INIT_STATE_NONE;
    protected static AtomicBoolean mWasInitCalled = new AtomicBoolean(false);

    //region Adapter Methods

    public static FacebookAdapter startAdapter(String providerName) {
        return new FacebookAdapter(providerName);
    }

    private FacebookAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose();

        // Rewarded video
        mRewardedVideoPlacementIdToSmashListener = new ConcurrentHashMap<>();
        mRewardedVideoPlacementIdToAd = new ConcurrentHashMap<>();
        mRewardedVideoPlacementIdToFacebookAdListener = new ConcurrentHashMap<>();
        mRewardedVideoAdsAvailability = new ConcurrentHashMap<>();
        mRewardedVideoPlacementIdsForInitCallbacks = new CopyOnWriteArraySet<>();
        mRewardedVideoPlacementIdShowCalled = new ConcurrentHashMap<>();

        // Interstitial
        mInterstitialPlacementIdToSmashListener = new ConcurrentHashMap<>();
        mInterstitialPlacementIdToAd = new ConcurrentHashMap<>();
        mInterstitialPlacementIdToFacebookAdListener = new ConcurrentHashMap<>();
        mInterstitialAdsAvailability = new ConcurrentHashMap<>();
        mInterstitialPlacementIdShowCalled = new ConcurrentHashMap<>();

        // Banner
        mBannerPlacementIdToSmashListener = new ConcurrentHashMap<>();
        mBannerPlacementIdToAd = new ConcurrentHashMap<>();

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE;
    }

    // Get the network and adapter integration data
    public static IntegrationData getIntegrationData(Context context) {
        return new IntegrationData(META_NETWORK_NAME, VERSION);
    }

    // Get adapter version
    @Override
    public String getVersion() {
        return VERSION;
    }

    // Get network sdk version
    @Override
    public String getCoreSDKVersion() {
        return getAdapterSDKVersion();
    }

    public static String getAdapterSDKVersion() {
        return com.facebook.ads.BuildConfig.VERSION_NAME;
    }
    //endregion

    //region Initializations methods and callbacks
    private void initSDK(String allPlacementIds) {
        // add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this);
        }

        // init SDK should be called only once
        if (mWasInitCalled.compareAndSet(false, true)) {
            final List<String> allPlacementIdsArr = Arrays.asList(allPlacementIds.split(","));
            IronLog.ADAPTER_API.verbose("Initialize Meta with placement ids = " + allPlacementIdsArr.toString());
            AudienceNetworkAds.buildInitSettings(ContextProvider.getInstance().getApplicationContext())
                    .withInitListener(this)
                    .withMediationService(getMediationServiceInfo())
                    .withPlacementIds(allPlacementIdsArr)
                    .initialize();
        }
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        // rewarded video listeners
        for (String placementId : mRewardedVideoPlacementIdToSmashListener.keySet()) {
            RewardedVideoSmashListener listener = mRewardedVideoPlacementIdToSmashListener.get(placementId);

            if (mRewardedVideoPlacementIdsForInitCallbacks.contains(placementId)) {
                listener.onRewardedVideoInitSuccess();
            } else {
                loadRewardedVideoInternal(placementId, null, listener);
            }
        }

        // interstitial listeners
        for (InterstitialSmashListener listener : mInterstitialPlacementIdToSmashListener.values()) {
            listener.onInterstitialInitSuccess();
        }

        // banners listeners
        for (BannerSmashListener listener : mBannerPlacementIdToSmashListener.values()) {
            listener.onBannerInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        // rewarded video listeners
        for (String placementId : mRewardedVideoPlacementIdToSmashListener.keySet()) {
            if (mRewardedVideoPlacementIdsForInitCallbacks.contains(placementId)) {
                mRewardedVideoPlacementIdToSmashListener.get(placementId).onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            } else {
                mRewardedVideoPlacementIdToSmashListener.get(placementId).onRewardedVideoAvailabilityChanged(false);
            }
        }

        // interstitial listeners
        for (InterstitialSmashListener listener : mInterstitialPlacementIdToSmashListener.values()) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }

        // banners listeners
        for (BannerSmashListener listener : mBannerPlacementIdToSmashListener.values()) {
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.BANNER_AD_UNIT));
        }
    }

    @Override
    public void onInitialized(AudienceNetworkAds.InitResult result) {
        IronLog.ADAPTER_CALLBACK.verbose("init SDK is completed with status: " + result.isSuccess() + ", " + result.getMessage());

        if (result.isSuccess()) {
            mInitState = InitState.INIT_STATE_SUCCESS;

            for (INetworkInitCallbackListener adapter : initCallbackListeners) {
                adapter.onNetworkInitCallbackSuccess();
            }

        } else {
            mInitState = InitState.INIT_STATE_FAILED;
            for (INetworkInitCallbackListener adapter : initCallbackListeners) {
                adapter.onNetworkInitCallbackFailed(result.getMessage());
            }
        }

        initCallbackListeners.clear();
    }
    //endregion

    //region Rewarded Video API

    // used for flows when the mediation needs to get a callback for init
    @Override
    public void initRewardedVideoWithCallback(String appKey, String userId, JSONObject config, RewardedVideoSmashListener listener) {
        final String placementId = config.optString(PLACEMENT_ID);
        final String allPlacementIds = config.optString(ALL_PLACEMENT_IDS);

        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("missing params - " + PLACEMENT_ID);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + PLACEMENT_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }
        if (TextUtils.isEmpty(allPlacementIds)) {
            IronLog.INTERNAL.error("missing params - " + ALL_PLACEMENT_IDS);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + ALL_PLACEMENT_IDS, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // add to rewarded video listener map
        mRewardedVideoPlacementIdToSmashListener.put(placementId, listener);

        // add placementId to init callback map
        mRewardedVideoPlacementIdsForInitCallbacks.add(placementId);


        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(allPlacementIds);
                break;
            case INIT_STATE_SUCCESS:
                listener.onRewardedVideoInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Meta SDK init failed", IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                break;
        }
    }

    // used for flows when the mediation doesn't need to get a callback for init
    @Override
    public void initAndLoadRewardedVideo(String appKey, String userId, JSONObject config, JSONObject adData, RewardedVideoSmashListener listener) {
        final String placementId = config.optString(PLACEMENT_ID);
        final String allPlacementIds = config.optString(ALL_PLACEMENT_IDS);

        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("missing params = " + PLACEMENT_ID);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        if (TextUtils.isEmpty(allPlacementIds)) {
            IronLog.INTERNAL.error("missing params = " + ALL_PLACEMENT_IDS);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        //add to rewarded video listener map
        mRewardedVideoPlacementIdToSmashListener.put(placementId, listener);


        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(allPlacementIds);
                break;
            case INIT_STATE_SUCCESS:
                loadRewardedVideoInternal(placementId, null, listener);
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                listener.onRewardedVideoAvailabilityChanged(false);
                break;
        }

    }

    @Override
    public void loadRewardedVideoForBidding(JSONObject config, JSONObject adData, final String serverData, final RewardedVideoSmashListener listener) {
        final String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        loadRewardedVideoInternal(placementId, serverData, listener);
    }

    @Override
    public void loadRewardedVideo(final JSONObject config, JSONObject adData, final RewardedVideoSmashListener listener) {
        final String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        loadRewardedVideoInternal(placementId, null, listener);
    }

    private void loadRewardedVideoInternal(final String placementId, final String serverData, final RewardedVideoSmashListener listener) {
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        mRewardedVideoAdsAvailability.put(placementId, false);
        mRewardedVideoPlacementIdShowCalled.put(placementId, false);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mRewardedVideoPlacementIdToAd.containsKey(placementId)) {
                        IronLog.ADAPTER_API.verbose("destroying previous ad with placement id " + placementId);
                        mRewardedVideoPlacementIdToAd.get(placementId).destroy();
                        mRewardedVideoPlacementIdToAd.remove(placementId);
                    }

                    RewardedVideoAd rewardedVideoAd = new RewardedVideoAd(ContextProvider.getInstance().getApplicationContext(), placementId);

                    FacebookRewardedVideoAdListener rewardedVideoAdListener = new FacebookRewardedVideoAdListener(FacebookAdapter.this, listener, placementId);
                    mRewardedVideoPlacementIdToFacebookAdListener.put(placementId, rewardedVideoAdListener);

                    RewardedVideoAdLoadConfigBuilder configBuilder = rewardedVideoAd.buildLoadAdConfig();
                    configBuilder.withAdListener(rewardedVideoAdListener);

                    if (!TextUtils.isEmpty(serverData)) {
                        // add server data to rewarded video bidder instance
                        configBuilder.withBid(serverData);
                    }

                    // set dynamic user id
                    if (!TextUtils.isEmpty(getDynamicUserId())) {
                        configBuilder.withRewardData(new RewardData(getDynamicUserId(), ""));
                    }

                    mRewardedVideoPlacementIdToAd.put(placementId, rewardedVideoAd);
                    rewardedVideoAd.loadAd(configBuilder.build());
                } catch (Exception ex) {
                    if (mRewardedVideoPlacementIdToSmashListener.containsKey(placementId)) {
                        mRewardedVideoPlacementIdToSmashListener.get(placementId).onRewardedVideoAvailabilityChanged(false);
                    }
                }
            }
        });
    }

    @Override
    public void showRewardedVideo(JSONObject config, final RewardedVideoSmashListener listener) {
        final String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        postOnUIThread(new Runnable() {
            @Override
            public void run() {

                try {
                    // change rewarded video availability to false
                    mRewardedVideoAdsAvailability.put(placementId, false);
                    RewardedVideoAd rewardedVideoAd = mRewardedVideoPlacementIdToAd.get(placementId);
                    // make sure the ad is loaded and has not expired
                    if (rewardedVideoAd != null && rewardedVideoAd.isAdLoaded() && !rewardedVideoAd.isAdInvalidated()) {
                        mRewardedVideoPlacementIdShowCalled.put(placementId, true);
                        rewardedVideoAd.show();
                    } else {
                        listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                    }
                } catch (Exception ex) {
                    IronLog.INTERNAL.error("ex.getMessage() = " + ex.getMessage());
                    listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildShowFailedError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT, ex.getMessage()));
                }
            }
        });
    }

    @Override
    public boolean isRewardedVideoAvailable(JSONObject config) {
        String adUnitId = config.optString(PLACEMENT_ID);
        return mRewardedVideoAdsAvailability.containsKey(adUnitId) && mRewardedVideoAdsAvailability.get(adUnitId);
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
        final String placementId = config.optString(PLACEMENT_ID);
        final String allPlacementIds = config.optString(ALL_PLACEMENT_IDS);

        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("missing params = " + PLACEMENT_ID);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + PLACEMENT_ID, IronSourceConstants.INTERSTITIAL_AD_UNIT));

            return;
        }

        if (TextUtils.isEmpty(allPlacementIds)) {
            IronLog.INTERNAL.error("missing params = " + ALL_PLACEMENT_IDS);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + ALL_PLACEMENT_IDS, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // add to interstitial listener map
        mInterstitialPlacementIdToSmashListener.put(placementId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(allPlacementIds);
                break;
            case INIT_STATE_SUCCESS:
                listener.onInterstitialInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Meta SDK init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT));
                break;
        }
    }

    // load interstitial for bidding
    @Override
    public void loadInterstitialForBidding(final JSONObject config, final JSONObject adData, final String serverData, final InterstitialSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        loadInterstitialInternal(config, serverData, listener);
    }

    @Override
    public void loadInterstitial(final JSONObject config, final JSONObject adData, final InterstitialSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        loadInterstitialInternal(config, null, listener);
    }

    private void loadInterstitialInternal(final JSONObject config, final String serverData, final InterstitialSmashListener listener) {
        final String placementId = config.optString(PLACEMENT_ID);

        mInterstitialPlacementIdShowCalled.put(placementId, false);
        mInterstitialAdsAvailability.put(placementId, false);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mInterstitialPlacementIdToAd.containsKey(placementId)) {
                        IronLog.ADAPTER_API.verbose("destroying previous ad with placement id " + placementId);
                        mInterstitialPlacementIdToAd.get(placementId).destroy();
                        mInterstitialPlacementIdToAd.remove(placementId);
                    }

                    InterstitialAd interstitialAd = new InterstitialAd(ContextProvider.getInstance().getApplicationContext(), placementId);

                    FacebookInterstitialAdListener interstitialAdListener = new FacebookInterstitialAdListener(FacebookAdapter.this, listener, placementId);
                    mInterstitialPlacementIdToFacebookAdListener.put(placementId, interstitialAdListener);

                    InterstitialAdLoadConfigBuilder configBuilder = interstitialAd.buildLoadAdConfig();
                    configBuilder.withCacheFlags(mInterstitialFacebookCacheFlags);
                    configBuilder.withAdListener(interstitialAdListener);

                    if (!TextUtils.isEmpty(serverData)) {
                        // add server data to Interstitial bidder instance
                        configBuilder.withBid(serverData);
                    }

                    IronLog.ADAPTER_API.verbose("loading placementId = " + placementId + " with facebook cache flags = " + mInterstitialFacebookCacheFlags.toString());

                    interstitialAd.loadAd(configBuilder.build());
                    mInterstitialPlacementIdToAd.put(placementId, interstitialAd);
                } catch (Exception e) {
                    listener.onInterstitialAdLoadFailed(ErrorBuilder.buildLoadFailedError(e.getLocalizedMessage()));
                }
            }
        });
    }

    @Override
    public void showInterstitial(JSONObject config, final InterstitialSmashListener listener) {
        final String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        mInterstitialAdsAvailability.put(placementId, false);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    InterstitialAd interstitialAd = mInterstitialPlacementIdToAd.get(placementId);
                    // make sure the ad is loaded and has not expired
                    if (interstitialAd != null && interstitialAd.isAdLoaded() && !interstitialAd.isAdInvalidated()) {
                        mInterstitialPlacementIdShowCalled.put(placementId, true);
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

    @Override
    public boolean isInterstitialReady(JSONObject config) {
        String adUnitId = config.optString(PLACEMENT_ID);
        return mInterstitialAdsAvailability.containsKey(adUnitId) && mInterstitialAdsAvailability.get(adUnitId);
    }

    @Override
    public Map<String, Object> getInterstitialBiddingData(JSONObject config, JSONObject adData) {
        return getBiddingData();
    }
    //endregion

    //region Banner API

    @Override
    public void initBannerForBidding(String appKey, String userId, JSONObject config, BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        initBannersInternal(config, listener);
    }

    @Override
    public void initBanners(String appKey, String userId, JSONObject config, BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        initBannersInternal(config, listener);
    }

    private void initBannersInternal(JSONObject config, BannerSmashListener listener) {
        final String placementId = config.optString(PLACEMENT_ID);
        final String allPlacementIds = config.optString(ALL_PLACEMENT_IDS);

        if (TextUtils.isEmpty(placementId)) {
            IronLog.INTERNAL.error("missing params = " + PLACEMENT_ID);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + PLACEMENT_ID, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(allPlacementIds)) {
            IronLog.INTERNAL.error("missing params = " + ALL_PLACEMENT_IDS);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + ALL_PLACEMENT_IDS, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId);
        // add to banner listener map
        mBannerPlacementIdToSmashListener.put(placementId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(allPlacementIds);
                break;
            case INIT_STATE_SUCCESS:
                listener.onBannerInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Meta SDK init failed", IronSourceConstants.BANNER_AD_UNIT));
                break;
        }

    }

    @Override
    public void loadBannerForBidding(final JSONObject config, final JSONObject adData, final String serverData, final IronSourceBannerLayout banner, final BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        loadBannerInternal(banner, config, listener, serverData);
    }

    @Override
    public void loadBanner(final JSONObject config, final JSONObject adData, final IronSourceBannerLayout banner, final BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        loadBannerInternal(banner, config, listener, null);
    }

    private void loadBannerInternal(final IronSourceBannerLayout banner, JSONObject config, final BannerSmashListener listener, final String serverData) {
        final String placementId = config.optString(PLACEMENT_ID);

        // check banner
        if (banner == null) {
            IronLog.INTERNAL.error("banner is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner is null"));
            return;
        }

        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // check size
        final AdSize adSize = calculateBannerSize(banner.getSize(), ContextProvider.getInstance().getApplicationContext());

        if (adSize == null) {
            IronLog.INTERNAL.error("loadBanner - size not supported, size = " + banner.getSize().getDescription());
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getProviderName()));
            return;
        }

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    AdView adView = new AdView(ContextProvider.getInstance().getApplicationContext(), placementId, adSize);
                    FrameLayout.LayoutParams layoutParams = calcLayoutParams(banner.getSize(), ContextProvider.getInstance().getApplicationContext());

                    // create banner
                    FacebookBannerAdListener bannerAdListener = new FacebookBannerAdListener(FacebookAdapter.this, listener, placementId, layoutParams);
                    AdView.AdViewLoadConfigBuilder configBuilder = adView.buildLoadAdConfig();
                    configBuilder.withAdListener(bannerAdListener);

                    if (serverData != null) {
                        // add server data to banner bidder instance
                        configBuilder.withBid(serverData);
                    }

                    mBannerPlacementIdToAd.put(placementId, adView);
                    adView.loadAd(configBuilder.build());
                } catch (Exception e) {
                    IronSourceError error = ErrorBuilder.buildLoadFailedError("Meta loadBanner exception " + e.getMessage());
                    listener.onBannerAdLoadFailed(error);
                }
            }
        });
    }

    @Override
    public void destroyBanner(final JSONObject config) {
        final String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mBannerPlacementIdToAd.containsKey(placementId)) {
                        mBannerPlacementIdToAd.get(placementId).destroy();
                        mBannerPlacementIdToAd.remove(placementId);
                    }
                } catch (Exception e) {
                    IronLog.INTERNAL.error("destroyBanner failed for placementId - " + placementId + " with an exception = " + e);
                }
            }
        });
    }

    public Map<String, Object> getBannerBiddingData(JSONObject config, JSONObject adData) {
        return getBiddingData();
    }

    //endregion

    // region memory handling

    @Override
    public void releaseMemory(IronSource.AD_UNIT adUnit, JSONObject config) {
        IronLog.INTERNAL.verbose("adUnit = " + adUnit);

        if (adUnit == IronSource.AD_UNIT.REWARDED_VIDEO) {
            // release rewarded ads
            for (RewardedVideoAd rewardedVideoAd : mRewardedVideoPlacementIdToAd.values()) {
                rewardedVideoAd.destroy();
            }
            mRewardedVideoPlacementIdToAd.clear();
            mRewardedVideoPlacementIdToFacebookAdListener.clear();
            mRewardedVideoPlacementIdToSmashListener.clear();
            mRewardedVideoAdsAvailability.clear();
            mRewardedVideoPlacementIdsForInitCallbacks.clear();

        } else if (adUnit == IronSource.AD_UNIT.INTERSTITIAL) {
            // release interstitial ads
            for (InterstitialAd interstitialAd : mInterstitialPlacementIdToAd.values()) {
                interstitialAd.destroy();
            }
            mInterstitialPlacementIdToAd.clear();
            mInterstitialPlacementIdToFacebookAdListener.clear();
            mInterstitialPlacementIdToSmashListener.clear();
            mInterstitialAdsAvailability.clear();

        } else if (adUnit == IronSource.AD_UNIT.BANNER) {
            // release banner ads
            for (AdView adView : mBannerPlacementIdToAd.values()) {
                adView.destroy();
            }
            mBannerPlacementIdToAd.clear();
            mBannerPlacementIdToSmashListener.clear();
        }
    }
    //endregion

    //region legal
    @Override
    protected void setMetaData(String key, List<String> values) {
        if (values.isEmpty()) {
            return;
        }

        switch (StringUtils.toLowerCase(key)) {
            case FACEBOOK_INTERSTITIAL_CACHE_FLAG:
            case META_INTERSTITIAL_CACHE_FLAG:
                IronLog.ADAPTER_API.verbose("key = " + key + ", values = " + values);
                mInterstitialFacebookCacheFlags.clear();

                try {
                    for (String value : values) {
                        CacheFlag flag = getFacebookCacheFlag(value);
                        IronLog.ADAPTER_API.verbose("flag for value " + value + " is " + flag.name());
                        mInterstitialFacebookCacheFlags.add(flag);
                    }
                } catch (Exception e) {
                    IronLog.INTERNAL.error("flag is unknown or all, set all as default");
                    mInterstitialFacebookCacheFlags = getFacebookAllCacheFlags();
                }
                break;

            case META_MIXED_AUDIENCE:
                // this is a list of 1 value
                String value = values.get(0);
                IronLog.ADAPTER_API.verbose("key = " + key + ", value = " + value);

                String formattedValue = MetaDataUtils.formatValueForType(value, META_DATA_VALUE_BOOLEAN);
                if (isValidMixedAudienceMetaData(formattedValue)) {
                    setMixedAudience(MetaDataUtils.getMetaDataBooleanValue(formattedValue));
                }
                break;
        }
    }

    private CacheFlag getFacebookCacheFlag(String value) {
        IronLog.ADAPTER_API.verbose("value = " + value);
        return CacheFlag.valueOf(StringUtils.toUpperCase(value));
    }

    private EnumSet<CacheFlag> getFacebookAllCacheFlags() {
        IronLog.ADAPTER_API.verbose();
        return EnumSet.allOf(CacheFlag.class);
    }

    private void setMixedAudience(boolean isMixedAudience) {
        IronLog.ADAPTER_API.verbose("isMixedAudience = " + isMixedAudience);
        AdSettings.setMixedAudience(isMixedAudience);
    }

    private boolean isValidMixedAudienceMetaData(String value) {
        return !TextUtils.isEmpty(value);
    }

    //endregion

    //region Helpers
    private AdSize calculateBannerSize(ISBannerSize size, Context context) {
        switch (size.getDescription()) {
            case "BANNER":
                return AdSize.BANNER_HEIGHT_50;

            case "LARGE":
                return AdSize.BANNER_HEIGHT_90;

            case "RECTANGLE":
                return AdSize.RECTANGLE_HEIGHT_250;

            case "SMART":
                return AdapterUtils.isLargeScreen(context) ? AdSize.BANNER_HEIGHT_90 : AdSize.BANNER_HEIGHT_50;

            case "CUSTOM":
                if (size.getHeight() == 50) {
                    return AdSize.BANNER_HEIGHT_50;
                } else if (size.getHeight() == 90) {
                    return AdSize.BANNER_HEIGHT_90;
                } else if (size.getHeight() == 250) {
                    return AdSize.RECTANGLE_HEIGHT_250;
                }
                break;
        }
        return null;
    }

    protected FrameLayout.LayoutParams calcLayoutParams(ISBannerSize size, Context context) {
        int widthDp = 320;
        if (size.getDescription().equals("RECTANGLE")) {
            widthDp = 300;
        } else if (size.getDescription().equals("SMART") && AdapterUtils.isLargeScreen(context)) {
            widthDp = 728;
        }

        int widthPixel = AdapterUtils.dpToPixels(context, widthDp);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(widthPixel, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        return layoutParams;
    }

    private String getMediationServiceInfo() {
        String mediationServiceInfo = String.format("%s_%s:%s", MEDIATION_NAME, IronSourceUtils.getSDKVersion(), VERSION);
        IronLog.INTERNAL.verbose("mediationServiceInfo = " + mediationServiceInfo);
        return mediationServiceInfo;
    }

    private Map<String, Object> getBiddingData() {
        if (mInitState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.verbose("returning null as token since init failed");
            return null;
        }

        String bidderToken = BidderTokenProvider.getBidderToken(ContextProvider.getInstance().getApplicationContext());
        String returnedToken = (!TextUtils.isEmpty(bidderToken)) ? bidderToken : "";
        IronLog.ADAPTER_API.verbose("token = " + returnedToken);
        Map<String, Object> ret = new HashMap<>();
        ret.put("token", returnedToken);
        return ret;
    }
    //endregion


}
