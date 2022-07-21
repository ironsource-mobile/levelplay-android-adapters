package com.ironsource.adapters.applovin;

import android.app.Activity;
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

    // Handle init callback for all adapter instances
    private static HashSet<INetworkInitCallbackListener> initCallbackListeners = new HashSet<>();
    private static InitState mInitState = InitState.INIT_STATE_NONE;
    private static AtomicBoolean mWasInitCalled = new AtomicBoolean(false);

    // Rewarded video collections
    protected ConcurrentHashMap<String, AppLovinIncentivizedInterstitial> mZoneIdToRewardedVideoAd;
    protected ConcurrentHashMap<String, RewardedVideoSmashListener> mZoneIdToRewardedVideoSmashListener;
    protected ConcurrentHashMap<String, AppLovinRewardedVideoListener> mZoneIdToAppLovinRewardedVideoListener;
    protected CopyOnWriteArraySet<String> mRewardedVideoZoneIdsForInitCallbacks;

    // Interstitial maps
    protected ConcurrentHashMap<String, AppLovinAd> mZoneIdToInterstitialAd;
    protected ConcurrentHashMap<String, InterstitialSmashListener> mZoneIdToInterstitialSmashListener;
    protected ConcurrentHashMap<String, AppLovinInterstitialListener> mZoneIdToAppLovinInterstitialListener;
    protected ConcurrentHashMap<String, Boolean> mZoneIdToInterstitialAdReadyStatus;

    // Banner maps
    protected ConcurrentHashMap<String, AppLovinAdView> mZoneIdToBannerAd;
    protected ConcurrentHashMap<String, BannerSmashListener> mZoneIdToBannerSmashListener;
    protected ConcurrentHashMap<String, AppLovinBannerListener> mZoneIdToAppLovinBannerListener;
    protected ConcurrentHashMap<String, FrameLayout.LayoutParams> mZoneIdToBannerLayout;
    protected ConcurrentHashMap<String, AppLovinAdSize> mZoneIdToBannerSize;

    private enum InitState {
        INIT_STATE_NONE,
        INIT_STATE_IN_PROGRESS,
        INIT_STATE_SUCCESS
    }

    // Meta data flags
    private static final String META_DATA_APPLOVIN_AGE_RESTRICTION_KEY = "AppLovin_AgeRestrictedUser";
    private static Boolean mConsentCollectingUserData = null;
    private static Boolean mCCPACollectingUserData = null;
    private static Boolean mAgeRestrictionData = null;

    // AppLovin sdk instance
    private static AppLovinSdk mAppLovinSdk;

    // Region Adapter Methods
    public static AppLovinAdapter startAdapter(String providerName) {
        return new AppLovinAdapter(providerName);
    }

    private AppLovinAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose("");

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
    public static IntegrationData getIntegrationData(Activity activity) {
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
    //endregion

    //region Initializations Methods And Callbacks
    private void initSdk(final String sdkKey, String userId) {
        // add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this);
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS;

            // get sdk setting
            AppLovinSdkSettings appLovinSdkSettings = getAppLovinSDKSetting();

            // create AppLovin sdk instance
            mAppLovinSdk = AppLovinSdk.getInstance(sdkKey, appLovinSdkSettings, ContextProvider.getInstance().getCurrentActiveActivity());

            // set user ID
            if (!TextUtils.isEmpty(userId)) {
                IronLog.ADAPTER_API.verbose("setUserIdentifier to " + userId);
                mAppLovinSdk.setUserIdentifier(userId);
            }

            IronLog.ADAPTER_API.verbose("sdkKey = " + sdkKey);

            // init AppLovin sdk
            mAppLovinSdk.initializeSdk(new AppLovinSdk.SdkInitializationListener() {
                // AppLovin's initialization callback currently doesn't give any indication to initialization failure.
                // Once this callback is called we will treat the initialization as successful
                @Override
                public void onSdkInitialized(AppLovinSdkConfiguration appLovinSdkConfiguration) {
                    IronLog.ADAPTER_API.verbose("");

                    // set init state success
                    mInitState = InitState.INIT_STATE_SUCCESS;

                    for (INetworkInitCallbackListener adapter : initCallbackListeners) {
                        adapter.onNetworkInitCallbackSuccess();
                    }

                    initCallbackListeners.clear();
                }
            });

            // set consent
            if (mConsentCollectingUserData != null) {
                setConsent(mConsentCollectingUserData);
            }

            // set ccpa
            if (mCCPACollectingUserData != null) {
                setCCPAValue(mCCPACollectingUserData);
            }

            // set age restriction
            if (mAgeRestrictionData != null) {
                setAgeRestrictionValue(mAgeRestrictionData);
            }

        }
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose("");

        // rewarded listeners
        for (String zoneId : mZoneIdToRewardedVideoSmashListener.keySet()) {
            if (mRewardedVideoZoneIdsForInitCallbacks.contains(zoneId)) {
                mZoneIdToRewardedVideoSmashListener.get(zoneId).onRewardedVideoInitSuccess();
            } else {
                loadRewardedVideo(zoneId, mZoneIdToRewardedVideoSmashListener.get(zoneId));
            }
        }

        // interstitial listeners
        for (InterstitialSmashListener listener : mZoneIdToInterstitialSmashListener.values()) {
            listener.onInterstitialInitSuccess();
        }

        // banners listeners
        for (BannerSmashListener listener : mZoneIdToBannerSmashListener.values()) {
            listener.onBannerInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {

    }

    @Override
    public void onNetworkInitCallbackLoadSuccess(String zoneId) {

    }
    //endregion

    //region Rewarded Video API
    // Used for flows when the mediation needs to get a callback for init
    @Override
    public void initRewardedVideoWithCallback(String appKey, final String userId, JSONObject config, final RewardedVideoSmashListener listener) {
        final String zoneId = getZoneId(config);
        final String sdkKey = config.optString(SDK_KEY);

        // verify sdk key
        if (TextUtils.isEmpty(sdkKey)) {
            IronLog.INTERNAL.error("error - missing param = " + SDK_KEY);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + SDK_KEY, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId + ", sdkKey = " + sdkKey);

        // add to rewarded video listener map
        mZoneIdToRewardedVideoSmashListener.put(zoneId, listener);
        // add to rewarded video init callback ArraySet
        mRewardedVideoZoneIdsForInitCallbacks.add(zoneId);

        // check AppLovin sdk init state
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
    public void initAndLoadRewardedVideo(String appKey, final String userId, JSONObject config, final RewardedVideoSmashListener listener) {
        final String zoneId = getZoneId(config);
        final String sdkKey = config.optString(SDK_KEY);

        // verify sdk key
        if (TextUtils.isEmpty(sdkKey)) {
            IronLog.INTERNAL.error("error - missing param = " + SDK_KEY);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId + ", sdkKey = " + sdkKey);

        // add to rewarded video listener map
        mZoneIdToRewardedVideoSmashListener.put(zoneId, listener);

        // check AppLovin sdk init state
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
                loadRewardedVideo(zoneId, listener);
                break;
        }
    }

    @Override
    public void fetchRewardedVideoForAutomaticLoad(final JSONObject config, final RewardedVideoSmashListener listener) {
        IronLog.ADAPTER_API.verbose("");
        final String zoneId = getZoneId(config);
        loadRewardedVideo(zoneId, listener);
    }

    private void loadRewardedVideo(String zoneId, RewardedVideoSmashListener listener) {
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        AppLovinIncentivizedInterstitial rewardedVideoAd;
        if (mZoneIdToRewardedVideoAd.containsKey(zoneId)) {
            // get ad
            rewardedVideoAd = mZoneIdToRewardedVideoAd.get(zoneId);
        } else {
            // create ad
            if (!zoneId.equals(DEFAULT_ZONE_ID)) {
                rewardedVideoAd = AppLovinIncentivizedInterstitial.create(zoneId, mAppLovinSdk);
            } else { // default empty zone id
                rewardedVideoAd = AppLovinIncentivizedInterstitial.create(mAppLovinSdk);
            }

            // add to list
            mZoneIdToRewardedVideoAd.put(zoneId, rewardedVideoAd);
        }

        // create AppLovin rewarded video listener
        AppLovinRewardedVideoListener rewardedVideoListener = new AppLovinRewardedVideoListener(AppLovinAdapter.this, listener, zoneId);
        mZoneIdToAppLovinRewardedVideoListener.put(zoneId, rewardedVideoListener);

        // load ad
        rewardedVideoAd.preload(rewardedVideoListener);
    }

    @Override
    public void showRewardedVideo(JSONObject config, RewardedVideoSmashListener listener) {
        String zoneId = getZoneId(config);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        // get ad
        AppLovinIncentivizedInterstitial rewardedVideoAd = mZoneIdToRewardedVideoAd.get(zoneId);
        // change rewarded video availability to false
        listener.onRewardedVideoAvailabilityChanged(false);

        // check ad availability
        if (!isRewardedVideoAvailable(config)) {
            IronLog.ADAPTER_API.error("no ad to show - " + (rewardedVideoAd == null ? "ad is null" : "ad not ready to display"));
            listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        // set dynamic user id
        if (!TextUtils.isEmpty(getDynamicUserId())) {
            mAppLovinSdk.setUserIdentifier(getDynamicUserId());
        }

        // get AppLovin rewarded video ad listener
        AppLovinRewardedVideoListener rewardedVideoListener = mZoneIdToAppLovinRewardedVideoListener.get(zoneId);

        // show ad
        rewardedVideoAd.show(ContextProvider.getInstance().getCurrentActiveActivity(), rewardedVideoListener, rewardedVideoListener, rewardedVideoListener, rewardedVideoListener);
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

        // verify sdk key
        if (TextUtils.isEmpty(sdkKey)) {
            IronLog.INTERNAL.error("error - missing param = " + SDK_KEY);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + SDK_KEY, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId + ", sdkKey = " + sdkKey);

        // add to interstitial listener map
        mZoneIdToInterstitialSmashListener.put(zoneId, listener);

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
                listener.onInterstitialInitSuccess();
                break;
        }
    }

    @Override
    public void loadInterstitial(JSONObject config, final InterstitialSmashListener listener) {
        final String zoneId = getZoneId(config);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        // create AppLovin interstitial listener
        AppLovinInterstitialListener interstitialListener = new AppLovinInterstitialListener(AppLovinAdapter.this, listener, zoneId);

        // save it on map to use it for the show listener
        mZoneIdToAppLovinInterstitialListener.put(zoneId, interstitialListener);

        // load ad
        if (!zoneId.equals(DEFAULT_ZONE_ID)) {
            mAppLovinSdk.getAdService().loadNextAdForZoneId(zoneId, interstitialListener);
        } else { // default empty zone id
            mAppLovinSdk.getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, interstitialListener);
        }
    }

    @Override
    public void showInterstitial(JSONObject config, InterstitialSmashListener listener) {
        String zoneId = getZoneId(config);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        // get ad
        AppLovinAd interstitialAd = mZoneIdToInterstitialAd.get(zoneId);

        // verify ad
        if (!isInterstitialReady(config)) {
            IronLog.ADAPTER_API.error("no ad to show - " + (interstitialAd == null ? "ad is null" : "ad not ready to display"));
            listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        // create ad dialog
        AppLovinInterstitialAdDialog interstitialAdDialog = AppLovinInterstitialAd.create(mAppLovinSdk, ContextProvider.getInstance().getCurrentActiveActivity());

        // getting AppLovin interstitial listener
        AppLovinInterstitialListener interstitialListener = mZoneIdToAppLovinInterstitialListener.get(zoneId);

        // set listeners
        interstitialAdDialog.setAdClickListener(interstitialListener);
        interstitialAdDialog.setAdDisplayListener(interstitialListener);
        interstitialAdDialog.setAdVideoPlaybackListener(interstitialListener);

        // show ad
        interstitialAdDialog.showAndRender(interstitialAd);

        // update ad status
        mZoneIdToInterstitialAdReadyStatus.put(zoneId, false);
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

        // verify sdk key
        if (TextUtils.isEmpty(sdkKey)) {
            IronLog.INTERNAL.error("error - missing param = " + SDK_KEY);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + SDK_KEY, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId + ", sdkKey = " + sdkKey);

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
    public void loadBanner(final IronSourceBannerLayout banner, final JSONObject config, final BannerSmashListener listener) {

        // check banner
        if (banner == null) {
            IronLog.INTERNAL.error("banner is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner is null"));
            return;
        }

        final String zoneId = getZoneId(config);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        // get banner size
        final AppLovinAdSize bannerSize = calculateBannerSize(banner.getSize(), AdapterUtils.isLargeScreen(banner.getActivity()));
        if (bannerSize == null) {
            IronLog.ADAPTER_API.error("unsupported banner size");
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getProviderName()));
            return;
        }

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // create ad view
                    FrameLayout.LayoutParams layoutParams = calcLayoutParams(banner.getSize(), bannerSize, banner.getActivity());
                    AppLovinAdView adView = new AppLovinAdView(mAppLovinSdk, bannerSize, banner.getActivity());

                    // create banner listener
                    AppLovinBannerListener applovinListener = new AppLovinBannerListener(AppLovinAdapter.this, listener, zoneId, layoutParams);

                    // add to maps
                    mZoneIdToBannerAd.put(zoneId, adView);
                    mZoneIdToBannerLayout.put(zoneId, layoutParams);
                    mZoneIdToAppLovinBannerListener.put(zoneId, applovinListener);
                    mZoneIdToBannerSize.put(zoneId, bannerSize);

                    // load ad
                    if (!zoneId.equals(DEFAULT_ZONE_ID)) {
                        mAppLovinSdk.getAdService().loadNextAdForZoneId(zoneId, applovinListener);
                    } else { // default empty zone id
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
    public void reloadBanner(final IronSourceBannerLayout banner, final JSONObject config, final BannerSmashListener listener) {
        IronLog.INTERNAL.warning("Unsupported method");
    }

    // destroy banner ad and clear banner ad map
    @Override
    public void destroyBanner(JSONObject config) {
        String zoneId = getZoneId(config);
        final AppLovinAdView adView = mZoneIdToBannerAd.get(zoneId);
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (adView != null) {
                    adView.destroy();
                }
            }
        });
        mZoneIdToBannerAd.remove(zoneId);
    }
    //endregion

    //region Memory Handling
    @Override
    public void releaseMemory(IronSource.AD_UNIT adUnit, JSONObject config) {
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
    protected void setConsent(boolean consent) {
        mConsentCollectingUserData = consent;

        if (mWasInitCalled.get()) {
            IronLog.ADAPTER_API.verbose("consent = " + consent);
            AppLovinPrivacySettings.setHasUserConsent(consent, ContextProvider.getInstance().getCurrentActiveActivity());
        }
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
            mCCPACollectingUserData = MetaDataUtils.getMetaDataBooleanValue(value);
        }

        String formattedValue = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN);
        if (isAgeRestrictionMetaData(key, formattedValue)) {
            mAgeRestrictionData = MetaDataUtils.getMetaDataBooleanValue(formattedValue);
        }
    }

    private void setAgeRestrictionValue(final boolean value) {
        if (mWasInitCalled.get()) {
            IronLog.ADAPTER_API.verbose("value = " + value);
            AppLovinPrivacySettings.setIsAgeRestrictedUser(value, ContextProvider.getInstance().getApplicationContext());
        }
    }

    // This method checks if the Meta Data key is the age restriction key and the value is valid
    static public boolean isAgeRestrictionMetaData(String key, String value) {
        return (key.equalsIgnoreCase(META_DATA_APPLOVIN_AGE_RESTRICTION_KEY) && value.length() > 0);
    }

    private void setCCPAValue(final boolean value) {
        if (mWasInitCalled.get()) {
            IronLog.ADAPTER_API.verbose("value = " + value);
            AppLovinPrivacySettings.setDoNotSell(value, ContextProvider.getInstance().getApplicationContext());
        }
    }

    private AppLovinSdkSettings getAppLovinSDKSetting() {
        AppLovinSdkSettings appLovinSdkSettings = new AppLovinSdkSettings(ContextProvider.getInstance().getApplicationContext());
        appLovinSdkSettings.setVerboseLogging(isAdaptersDebugEnabled());
        return appLovinSdkSettings;
    }
    //endregion

    //region Helpers
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

    private String getZoneId(JSONObject config) {
        return !TextUtils.isEmpty(config.optString(ZONE_ID)) ? config.optString(ZONE_ID) : DEFAULT_ZONE_ID;
    }

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

    private FrameLayout.LayoutParams calcLayoutParams(ISBannerSize isSize, AppLovinAdSize appLovinSize, Activity activity) {
        int widthDp = 320; // default banner size
        if (isSize.getDescription().equals("RECTANGLE")) {
            widthDp = 300;
        } else if (isSize.getDescription().equals("SMART") && AdapterUtils.isLargeScreen(activity)) {
            widthDp = 728;
        }

        int widthPixel = AdapterUtils.dpToPixels(activity, widthDp);
        int heightPixel = AdapterUtils.dpToPixels(activity, appLovinSize.getHeight());
        return new FrameLayout.LayoutParams(widthPixel, heightPixel, Gravity.CENTER);
    }

    //endregion
}



