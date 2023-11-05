package com.ironsource.adapters.applovin;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.AbstractAdapter;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IntegrationData;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.INetworkInitCallbackListener;
import com.ironsource.mediationsdk.LoadWhileShowSupportState;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.metadata.MetaData;
import com.ironsource.mediationsdk.metadata.MetaDataUtils;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;
import com.ironsource.mediationsdk.AdapterUtils;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;


class AppLovinAdapter extends AbstractAdapter implements INetworkInitCallbackListener {

    // Adapter version
    private static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String GitHash = BuildConfig.GitHash;

    // AppLovin keys
    private static final String ZONE_ID = "zoneId";
    private static final String DEFAULT_ZONE_ID = "defaultZoneId";
    private static final String SDK_KEY = "sdkKey";

    // Meta data flags
    private static final String META_DATA_APPLOVIN_AGE_RESTRICTION_KEY = "AppLovin_AgeRestrictedUser";

    // Rewarded video collections
    protected final ConcurrentHashMap<String, AppLovinIncentivizedInterstitial> mZoneIdToRewardedVideoAd;
    protected final ConcurrentHashMap<String, RewardedVideoSmashListener> mZoneIdToRewardedVideoSmashListener;
    protected final ConcurrentHashMap<String, AppLovinRewardedVideoListener> mZoneIdToAppLovinRewardedVideoListener;
    protected final CopyOnWriteArraySet<String> mRewardedVideoZoneIdsForInitCallbacks;

    // Interstitial maps
    protected final ConcurrentHashMap<String, AppLovinAd> mZoneIdToInterstitialAd;
    protected final ConcurrentHashMap<String, InterstitialSmashListener> mZoneIdToInterstitialSmashListener;
    protected final ConcurrentHashMap<String, AppLovinInterstitialListener> mZoneIdToAppLovinInterstitialListener;
    protected final ConcurrentHashMap<String, Boolean> mZoneIdToInterstitialAdReadyStatus;

    // Banner maps
    protected final ConcurrentHashMap<String, AppLovinAdView> mZoneIdToBannerAd;
    protected final ConcurrentHashMap<String, BannerSmashListener> mZoneIdToBannerSmashListener;
    protected final ConcurrentHashMap<String, AppLovinBannerListener> mZoneIdToAppLovinBannerListener;
    protected final ConcurrentHashMap<String, FrameLayout.LayoutParams> mZoneIdToBannerLayout;
    protected final ConcurrentHashMap<String, AppLovinAdSize> mZoneIdToBannerSize;

    // init state possible values
    private enum InitState {
        INIT_STATE_NONE,
        INIT_STATE_IN_PROGRESS,
        INIT_STATE_SUCCESS
    }

    // Handle init callback for all adapter instances
    private static final AtomicBoolean mWasInitCalled = new AtomicBoolean(false);
    private static InitState mInitState = InitState.INIT_STATE_NONE;
    private static final HashSet<INetworkInitCallbackListener> initCallbackListeners = new HashSet<>();

    // AppLovin sdk instance
    private static AppLovinSdk mAppLovinSdk;

    // Region Adapter Methods

    public static AppLovinAdapter startAdapter(String providerName) {
        return new AppLovinAdapter(providerName);
    }

    private AppLovinAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose();

        // Rewarded video
        mZoneIdToAppLovinRewardedVideoListener = new ConcurrentHashMap<>();
        mZoneIdToRewardedVideoAd = new ConcurrentHashMap<>();
        mZoneIdToRewardedVideoSmashListener = new ConcurrentHashMap<>();
        mRewardedVideoZoneIdsForInitCallbacks = new CopyOnWriteArraySet<>();

        // Interstitial
        mZoneIdToAppLovinInterstitialListener = new ConcurrentHashMap<>();
        mZoneIdToInterstitialAd = new ConcurrentHashMap<>();
        mZoneIdToInterstitialSmashListener = new ConcurrentHashMap<>();
        mZoneIdToInterstitialAdReadyStatus = new ConcurrentHashMap<>();

        // Banner
        mZoneIdToAppLovinBannerListener = new ConcurrentHashMap<>();
        mZoneIdToBannerSmashListener = new ConcurrentHashMap<>();
        mZoneIdToBannerLayout = new ConcurrentHashMap<>();
        mZoneIdToBannerAd = new ConcurrentHashMap<>();
        mZoneIdToBannerSize = new ConcurrentHashMap<>();

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE;
    }

    // Get the network and adapter integration data
    public static IntegrationData getIntegrationData(Context context) {
        return new IntegrationData("AppLovin", VERSION);
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
        return AppLovinSdk.VERSION;
    }

    public boolean isUsingActivityBeforeImpression(@NotNull IronSource.AD_UNIT adUnit) {
        return false;
    }

    //endregion

    //region Initializations Methods And Callbacks

    private void initSdk(final String sdkKey, String userId) {
        // add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this);
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            IronLog.ADAPTER_API.verbose("sdkKey = " + sdkKey);

            mInitState = InitState.INIT_STATE_IN_PROGRESS;

            // get sdk setting
            AppLovinSdkSettings appLovinSdkSettings = getAppLovinSDKSetting();

            // create AppLovin sdk instance
            mAppLovinSdk = AppLovinSdk.getInstance(sdkKey, appLovinSdkSettings, ContextProvider.getInstance().getApplicationContext());

            // set ironSource as mediation
            mAppLovinSdk.setMediationProvider(AppLovinMediationProvider.IRONSOURCE);

            // set user ID
            if (!TextUtils.isEmpty(userId)) {
                IronLog.ADAPTER_API.verbose("setUserIdentifier to " + userId);
                mAppLovinSdk.setUserIdentifier(userId);
            }

            // init AppLovin sdk
            mAppLovinSdk.initializeSdk(new AppLovinSdk.SdkInitializationListener() {
                // AppLovin's initialization callback currently doesn't give any indication to initialization failure.
                // Once this callback is called we will treat the initialization as successful
                @Override
                public void onSdkInitialized(AppLovinSdkConfiguration appLovinSdkConfiguration) {
                   initializationSuccess();
                }
            });
        }
    }

    private void initializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose();

        mInitState = InitState.INIT_STATE_SUCCESS;

        //iterate over all the adapter instances and report init success
        for (INetworkInitCallbackListener adapter : initCallbackListeners) {
            adapter.onNetworkInitCallbackSuccess();
        }

        initCallbackListeners.clear();
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        // Rewarded Video
        for (String zoneId : mZoneIdToRewardedVideoSmashListener.keySet()) {
            RewardedVideoSmashListener listener = mZoneIdToRewardedVideoSmashListener.get(zoneId);
            if (mRewardedVideoZoneIdsForInitCallbacks.contains(zoneId)) {
                listener.onRewardedVideoInitSuccess();
            } else {
                loadRewardedVideoInternal(zoneId, listener);
            }
        }

        // Interstitial
        for (InterstitialSmashListener listener : mZoneIdToInterstitialSmashListener.values()) {
            listener.onInterstitialInitSuccess();
        }

        // Banner
        for (BannerSmashListener listener : mZoneIdToBannerSmashListener.values()) {
            listener.onBannerInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) { }

    //endregion

    //region Rewarded Video API

    // Used for flows when the mediation needs to get a callback for init
    @Override
    public void initRewardedVideoWithCallback(String appKey, final String userId, JSONObject config, final RewardedVideoSmashListener listener) {
        final String zoneId = getZoneId(config);
        final String sdkKey = config.optString(SDK_KEY);

        if (TextUtils.isEmpty(sdkKey)) {
            IronLog.INTERNAL.error("error - missing param - " + SDK_KEY);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + SDK_KEY, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(zoneId)) {
            IronLog.INTERNAL.error("Missing param - " + ZONE_ID);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + ZONE_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        // add to rewarded video listener map
        mZoneIdToRewardedVideoSmashListener.put(zoneId, listener);
        // add to rewarded video init callback ArraySet
        mRewardedVideoZoneIdsForInitCallbacks.add(zoneId);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS: {
                postOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        initSdk(sdkKey, userId);
                    }
                });
            }
            break;
            case INIT_STATE_SUCCESS:
                listener.onRewardedVideoInitSuccess();
                break;
        }
    }

    // Used for flows when the mediation doesn't need to get a callback for init
    @Override
    public void initAndLoadRewardedVideo(String appKey, final String userId, JSONObject config, JSONObject adData, final RewardedVideoSmashListener listener) {
        final String zoneId = getZoneId(config);
        final String sdkKey = config.optString(SDK_KEY);

        if (TextUtils.isEmpty(sdkKey)) {
            IronLog.INTERNAL.error("Missing param - " + SDK_KEY);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        if (TextUtils.isEmpty(zoneId)) {
            IronLog.INTERNAL.error("Missing param - " + ZONE_ID);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        // add to rewarded video listener map
        mZoneIdToRewardedVideoSmashListener.put(zoneId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS: {
                postOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        initSdk(sdkKey, userId);
                    }
                });
            }
            break;
            case INIT_STATE_SUCCESS:
                loadRewardedVideoInternal(zoneId, listener);
                break;
        }
    }

    @Override
    public void loadRewardedVideo(final JSONObject config, JSONObject adData, final RewardedVideoSmashListener listener) {
        final String zoneId = getZoneId(config);
        loadRewardedVideoInternal(zoneId, listener);
    }

    private void loadRewardedVideoInternal(String zoneId, RewardedVideoSmashListener listener) {
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        AppLovinIncentivizedInterstitial rewardedVideoAd;

        if (mZoneIdToRewardedVideoAd.containsKey(zoneId)) {
            rewardedVideoAd = mZoneIdToRewardedVideoAd.get(zoneId);
        } else {
            if (!zoneId.equals(DEFAULT_ZONE_ID)) {
                rewardedVideoAd = AppLovinIncentivizedInterstitial.create(zoneId, mAppLovinSdk);
            } else {
                rewardedVideoAd = AppLovinIncentivizedInterstitial.create(mAppLovinSdk);
            }
            mZoneIdToRewardedVideoAd.put(zoneId, rewardedVideoAd);
        }

        // create AppLovin rewarded video listener
        AppLovinRewardedVideoListener rewardedVideoListener = new AppLovinRewardedVideoListener(AppLovinAdapter.this, listener, zoneId);
        mZoneIdToAppLovinRewardedVideoListener.put(zoneId, rewardedVideoListener);

        // load rewarded video
        rewardedVideoAd.preload(rewardedVideoListener);
    }

    @Override
    public void showRewardedVideo(JSONObject config, RewardedVideoSmashListener listener) {
        String zoneId = getZoneId(config);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        // check ad availability
        if (isRewardedVideoAvailable(config)) {

            if (!TextUtils.isEmpty(getDynamicUserId())) {
                mAppLovinSdk.setUserIdentifier(getDynamicUserId());
            }

            AppLovinIncentivizedInterstitial rewardedVideoAd = mZoneIdToRewardedVideoAd.get(zoneId);
            AppLovinRewardedVideoListener rewardedVideoListener = mZoneIdToAppLovinRewardedVideoListener.get(zoneId);

            rewardedVideoAd.show(ContextProvider.getInstance().getCurrentActiveActivity(), rewardedVideoListener, rewardedVideoListener, rewardedVideoListener, rewardedVideoListener);

        } else {
            listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
        }
    }

    @Override
    public boolean isRewardedVideoAvailable(JSONObject config) {
        String zoneId = getZoneId(config);
        AppLovinIncentivizedInterstitial rewardedVideoAd = mZoneIdToRewardedVideoAd.get(zoneId);
        return rewardedVideoAd != null && rewardedVideoAd.isAdReadyToDisplay();
    }

    //endregion

    //region Interstitial API

    @Override
    public void initInterstitial(final String appKey, final String userId, final JSONObject config, final InterstitialSmashListener listener) {
        final String zoneId = getZoneId(config);
        final String sdkKey = config.optString(SDK_KEY);

        if (TextUtils.isEmpty(sdkKey)) {
            IronLog.INTERNAL.error("Missing param - " + SDK_KEY);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + SDK_KEY, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(zoneId)) {
            IronLog.INTERNAL.error("Missing param - " + ZONE_ID);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + ZONE_ID, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        // add to interstitial listener map
        mZoneIdToInterstitialSmashListener.put(zoneId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS: {
                postOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        initSdk(sdkKey, userId);
                    }
                });
            }
            break;
            case INIT_STATE_SUCCESS:
                listener.onInterstitialInitSuccess();
                break;
        }
    }

    @Override
    public void loadInterstitial(JSONObject config, JSONObject adData, final InterstitialSmashListener listener) {
        final String zoneId = getZoneId(config);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        AppLovinInterstitialListener interstitialListener = new AppLovinInterstitialListener(AppLovinAdapter.this, listener, zoneId);

        mZoneIdToAppLovinInterstitialListener.put(zoneId, interstitialListener);

        // load interstitial
        if (!zoneId.equals(DEFAULT_ZONE_ID)) {
            mAppLovinSdk.getAdService().loadNextAdForZoneId(zoneId, interstitialListener);
        } else { 
            mAppLovinSdk.getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, interstitialListener);
        }
    }

    @Override
    public void showInterstitial(JSONObject config, InterstitialSmashListener listener) {
        String zoneId = getZoneId(config);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        if (isInterstitialReady(config)) {
            AppLovinAd interstitialAd = mZoneIdToInterstitialAd.get(zoneId);
            AppLovinInterstitialListener interstitialListener = mZoneIdToAppLovinInterstitialListener.get(zoneId);

            AppLovinInterstitialAdDialog interstitialAdDialog = AppLovinInterstitialAd.create(mAppLovinSdk, ContextProvider.getInstance().getCurrentActiveActivity());

            interstitialAdDialog.setAdClickListener(interstitialListener);
            interstitialAdDialog.setAdDisplayListener(interstitialListener);
            interstitialAdDialog.setAdVideoPlaybackListener(interstitialListener);
            interstitialAdDialog.showAndRender(interstitialAd);

            mZoneIdToInterstitialAdReadyStatus.put(zoneId, false);
        } else {
            listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    @Override
    public boolean isInterstitialReady(JSONObject config) {
        String zoneId = getZoneId(config);
        return mZoneIdToInterstitialAd.containsKey(zoneId) && mZoneIdToInterstitialAdReadyStatus.containsKey(zoneId) && mZoneIdToInterstitialAdReadyStatus.get(zoneId);
    }

    //endregion

    //region Banner API

    @Override
    public void initBanners(final String appKey, final String userId, JSONObject config, final BannerSmashListener listener) {
        final String zoneId = getZoneId(config);
        final String sdkKey = config.optString(SDK_KEY);

        if (TextUtils.isEmpty(sdkKey)) {
            IronLog.INTERNAL.error("Missing param - " + SDK_KEY);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + SDK_KEY, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(zoneId)) {
            IronLog.INTERNAL.error("Missing param - " + ZONE_ID);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + ZONE_ID, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        //add banner to listener map
        mZoneIdToBannerSmashListener.put(zoneId, listener);

        //check AppLovin sdk init state
        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS: {
                postOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        initSdk(sdkKey, userId);
                    }
                });
            }
            break;
            case INIT_STATE_SUCCESS:
                listener.onBannerInitSuccess();
                break;
        }
    }

    @Override
    public void loadBanner(final JSONObject config, final JSONObject adData, final IronSourceBannerLayout banner, final BannerSmashListener listener) {
        final String zoneId = getZoneId(config);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        if (banner == null) {
            IronLog.INTERNAL.error("banner layout is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner layout is null"));
            return;
        }

        // get size
        final AppLovinAdSize bannerSize = calculateBannerSize(banner.getSize(), AdapterUtils.isLargeScreen(ContextProvider.getInstance().getApplicationContext()));

        // verify if size is null
        if (bannerSize == null) {
            IronLog.INTERNAL.error("size not supported, size is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getProviderName()));
            return;
        }

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // create ad view
                    FrameLayout.LayoutParams layoutParams = getBannerLayoutParams(banner.getSize());
                    // create banner listener
                    AppLovinBannerListener applovinListener = new AppLovinBannerListener(AppLovinAdapter.this, listener, zoneId, layoutParams);

                    // create ad view
                    AppLovinAdView adView = new AppLovinAdView(mAppLovinSdk, bannerSize, ContextProvider.getInstance().getApplicationContext());
                    adView.setAdDisplayListener(applovinListener);
                    adView.setAdClickListener(applovinListener);
                    adView.setAdViewEventListener(applovinListener);

                    // add to maps
                    mZoneIdToBannerAd.put(zoneId, adView);
                    mZoneIdToBannerLayout.put(zoneId, layoutParams);
                    mZoneIdToAppLovinBannerListener.put(zoneId, applovinListener);
                    mZoneIdToBannerSize.put(zoneId, bannerSize);

                    // load ad
                    if (!zoneId.equals(DEFAULT_ZONE_ID)) {
                        mAppLovinSdk.getAdService().loadNextAdForZoneId(zoneId, applovinListener);
                    } else {
                        mAppLovinSdk.getAdService().loadNextAd(bannerSize, applovinListener);
                    }

                } catch (Exception e) {
                    IronSourceError error = ErrorBuilder.buildLoadFailedError(getProviderName() + " loadBanner exception " + e.getMessage());
                    listener.onBannerAdLoadFailed(error);
                }
            }
        });
    }

    @Override
    public void destroyBanner(JSONObject config) {
        final String zoneId = getZoneId(config);
        final AppLovinAdView adView = mZoneIdToBannerAd.get(zoneId);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (adView != null) {
                    adView.destroy();
                }

                mZoneIdToBannerAd.remove(zoneId);
                mZoneIdToBannerLayout.remove(zoneId);
                mZoneIdToAppLovinBannerListener.remove(zoneId);
                mZoneIdToBannerSize.remove(zoneId);
            }
        });
    }

    //endregion

    //region Memory Handling

    @Override
    public void releaseMemory(IronSource.AD_UNIT adUnit, JSONObject config) {
        IronLog.INTERNAL.verbose("adUnit = " + adUnit);

        if (adUnit == IronSource.AD_UNIT.REWARDED_VIDEO) {
            mZoneIdToAppLovinRewardedVideoListener.clear();
            mZoneIdToRewardedVideoAd.clear();
            mZoneIdToRewardedVideoSmashListener.clear();
            mRewardedVideoZoneIdsForInitCallbacks.clear();

        } else if (adUnit == IronSource.AD_UNIT.INTERSTITIAL) {
            mZoneIdToAppLovinInterstitialListener.clear();
            mZoneIdToInterstitialAdReadyStatus.clear();
            mZoneIdToInterstitialAd.clear();
            mZoneIdToInterstitialSmashListener.clear();

        } else if (adUnit == IronSource.AD_UNIT.BANNER) {
            postOnUIThread(new Runnable() {
                @Override
                public void run() {
                    for (AppLovinAdView adView : mZoneIdToBannerAd.values()) {
                        adView.destroy();
                    }

                    mZoneIdToAppLovinBannerListener.clear();
                    mZoneIdToBannerSmashListener.clear();
                    mZoneIdToBannerLayout.clear();
                    mZoneIdToBannerAd.clear();
                }
            });

        }
    }

    //endregion

    //region Legal
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
            String formattedValue = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN);

            if (MetaDataUtils.isValidMetaData(key, META_DATA_APPLOVIN_AGE_RESTRICTION_KEY, formattedValue)) {
                setAgeRestrictionValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue));
            }
        }
    }

    @Override
    protected void setConsent(boolean consent) {
        IronLog.ADAPTER_API.verbose("consent = " + consent);
        AppLovinPrivacySettings.setHasUserConsent(consent, ContextProvider.getInstance().getApplicationContext());
    }

    private void setAgeRestrictionValue(final boolean value) {
            IronLog.ADAPTER_API.verbose("value = " + value);
            AppLovinPrivacySettings.setIsAgeRestrictedUser(value, ContextProvider.getInstance().getApplicationContext());
    }

    private void setCCPAValue(final boolean value) {
            IronLog.ADAPTER_API.verbose("value = " + value);
            AppLovinPrivacySettings.setDoNotSell(value, ContextProvider.getInstance().getApplicationContext());
    }

    //endregion

    //region Helpers

    private AppLovinAdSize calculateBannerSize(ISBannerSize bannerSize, boolean isLargeScreen) {
        if (bannerSize == null) {
            IronLog.ADAPTER_API.error(getProviderName() + " calculateLayoutParams - bannerSize is null");
            return null;
        }

        switch (bannerSize.getDescription()) {
            case "BANNER":
            case "LARGE":
                return AppLovinAdSize.BANNER;
            case "RECTANGLE":
                return AppLovinAdSize.MREC;
            case "SMART":
                return isLargeScreen ? AppLovinAdSize.LEADER : AppLovinAdSize.BANNER;
            case "CUSTOM":
                if (bannerSize.getHeight() >= 40 && bannerSize.getHeight() <= 60) {
                    return AppLovinAdSize.BANNER;
                }
                break;
        }
        return null;
    }

    private FrameLayout.LayoutParams getBannerLayoutParams(ISBannerSize size) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(0, 0);
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
            case "CUSTOM":
                if (size.getHeight() >= 40 && size.getHeight() <= 60) {
                    layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(context, 320), AdapterUtils.dpToPixels(context, 50));
                }
                break;
        }

        // set gravity
        layoutParams.gravity = Gravity.CENTER;

        return layoutParams;
    }

    private AppLovinSdkSettings getAppLovinSDKSetting() {
        AppLovinSdkSettings appLovinSdkSettings = new AppLovinSdkSettings(ContextProvider.getInstance().getApplicationContext());
        appLovinSdkSettings.setVerboseLogging(isAdaptersDebugEnabled());
        return appLovinSdkSettings;
    }

    private String getZoneId(JSONObject config) {
        return !TextUtils.isEmpty(config.optString(ZONE_ID)) ? config.optString(ZONE_ID) : DEFAULT_ZONE_ID;
    }

    protected String getErrorString(int errorCode) {
        switch (errorCode) {
            case AppLovinErrorCodes.SDK_DISABLED:
                return "The SDK is currently disabled.";
            case AppLovinErrorCodes.FETCH_AD_TIMEOUT:
                return "The network conditions prevented the SDK from receiving an ad.";
            case AppLovinErrorCodes.NO_NETWORK:
                return "The device had no network connectivity at the time of an ad request, either due to airplane mode or no service.";
            case AppLovinErrorCodes.NO_FILL:
                return "No ads are currently eligible for your device.";
            case AppLovinErrorCodes.UNABLE_TO_RENDER_AD:
                return "There has been a failure to render an ad on screen.";
            case AppLovinErrorCodes.INVALID_ZONE:
                return "The zone provided is invalid; the zone needs to be added to your AppLovin account or may still be propagating to our servers.";
            case AppLovinErrorCodes.INVALID_AD_TOKEN:
                return "The provided ad token is invalid; ad token must be returned from AppLovin S2S integration.";
            case AppLovinErrorCodes.UNSPECIFIED_ERROR:
                return "The system is in unexpected state.";
            case AppLovinErrorCodes.INCENTIVIZED_NO_AD_PRELOADED:
                return "The developer called for a rewarded video before one was available.";
            case AppLovinErrorCodes.INCENTIVIZED_UNKNOWN_SERVER_ERROR:
                return "An unknown server-side error occurred.";
            case AppLovinErrorCodes.INCENTIVIZED_SERVER_TIMEOUT:
                return "A reward validation requested timed out (usually due to poor connectivity).";
            case AppLovinErrorCodes.INCENTIVIZED_USER_CLOSED_VIDEO:
                return "The user exited out of the ad early. You may or may not wish to grant a reward depending on your preference.";
            case AppLovinErrorCodes.INVALID_RESPONSE:
                return "The AppLovin servers have returned an invalid response";
            case AppLovinErrorCodes.INVALID_URL:
                return "A postback URL you attempted to dispatch was empty or nil.";
            case AppLovinErrorCodes.UNABLE_TO_PRECACHE_RESOURCES:
                return "An attempt to cache a resource to the filesystem failed; the device may be out of space.";
            case AppLovinErrorCodes.UNABLE_TO_PRECACHE_IMAGE_RESOURCES:
                return "An attempt to cache an image resource to the filesystem failed; the device may be out of space.";
            case AppLovinErrorCodes.UNABLE_TO_PRECACHE_VIDEO_RESOURCES:
                return "An attempt to cache a video resource to the filesystem failed; the device may be out of space.";
            default:
                return "Unknown error";
        }
    }

    //endregion
}



