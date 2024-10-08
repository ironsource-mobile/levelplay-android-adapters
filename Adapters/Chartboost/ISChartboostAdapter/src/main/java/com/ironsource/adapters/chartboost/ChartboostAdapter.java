package com.ironsource.adapters.chartboost;

import static com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.LoggingLevel;
import com.chartboost.sdk.Mediation;
import com.chartboost.sdk.ads.Banner;
import com.chartboost.sdk.ads.Interstitial;
import com.chartboost.sdk.ads.Rewarded;
import com.chartboost.sdk.callbacks.StartCallback;
import com.chartboost.sdk.events.StartError;
import com.chartboost.sdk.privacy.model.CCPA;
import com.chartboost.sdk.privacy.model.COPPA;
import com.chartboost.sdk.privacy.model.DataUseConsent;
import com.chartboost.sdk.privacy.model.GDPR;
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
import com.ironsource.mediationsdk.utils.IronSourceUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

class ChartboostAdapter extends AbstractAdapter implements INetworkInitCallbackListener {

    // Mediation info
    private final String MEDIATION_NAME = "ironSource";
    private static Mediation mMediationInfo = null;

    // Adapter version
    private static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String GitHash = BuildConfig.GitHash;

    // Chartboost keys
    private final String APP_ID = "appID";
    private final String APP_SIGNATURE = "appSignature";
    private final String AD_LOCATION = "adLocation";

    // Meta data flags
    private static final String CHARTBOOST_COPPA_FLAG = "chartboost_coppa";
    private static Boolean mConsentCollectingUserData = null;
    private static Boolean mDoNotSellCollectingUserData = null;
    private static Boolean mCoppaUserData = null;

    // Rewarded video collections
    private ConcurrentHashMap<String, RewardedVideoSmashListener> mLocationIdToRewardedVideoListener;
    private ConcurrentHashMap<String, ChartboostRewardedVideoAdListener> mLocationIdToRewardedVideoAdListener;
    private ConcurrentHashMap<String, Rewarded> mLocationIdToRewardedVideoAd;
    private CopyOnWriteArraySet<String> mRewardedVideoLocationIdsForInitCallbacks;

    // Interstitial maps
    private ConcurrentHashMap<String, InterstitialSmashListener> mLocationIdToInterstitialListener;
    private ConcurrentHashMap<String, ChartboostInterstitialAdListener> mLocationIdToInterstitialAdListener;
    private ConcurrentHashMap<String, Interstitial> mLocationIdToInterstitialAd;

    // Banner maps
    private ConcurrentHashMap<String, BannerSmashListener> mLocationIdToBannerListener;
    private ConcurrentHashMap<String, ChartboostBannerAdListener> mLocationIdToBannerAdListener;
    private ConcurrentHashMap<String, IronSourceBannerLayout> mLocationIdToBannerLayout;
    protected ConcurrentHashMap<String, Banner> mLocationIdToBannerAd;

    // init state possible values
    public enum InitState {
        INIT_STATE_NONE,
        INIT_STATE_IN_PROGRESS,
        INIT_STATE_SUCCESS,
        INIT_STATE_FAILED
    }

    // Handle init callback for all adapter instances
    private static AtomicBoolean mWasInitCalled = new AtomicBoolean(false);
    protected static InitState mInitState = InitState.INIT_STATE_NONE;
    protected static HashSet<INetworkInitCallbackListener> initCallbackListeners = new HashSet<>();

    //region Adapter Methods

    public static ChartboostAdapter startAdapter(String providerName) {
        return new ChartboostAdapter(providerName);
    }

    private ChartboostAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose();

        // rewarded video
        mLocationIdToRewardedVideoListener = new ConcurrentHashMap<>();
        mLocationIdToRewardedVideoAdListener = new ConcurrentHashMap<>();
        mLocationIdToRewardedVideoAd = new ConcurrentHashMap<>();
        mRewardedVideoLocationIdsForInitCallbacks = new CopyOnWriteArraySet<>();

        // interstitial
        mLocationIdToInterstitialListener = new ConcurrentHashMap<>();
        mLocationIdToInterstitialAdListener = new ConcurrentHashMap<>();
        mLocationIdToInterstitialAd = new ConcurrentHashMap<>();

        // banner
        mLocationIdToBannerListener = new ConcurrentHashMap<>();
        mLocationIdToBannerAdListener = new ConcurrentHashMap<>();
        mLocationIdToBannerAd = new ConcurrentHashMap<>();
        mLocationIdToBannerLayout = new ConcurrentHashMap<>();

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_NETWORK;
    }

    // Get the network and adapter integration data
    public static IntegrationData getIntegrationData(Context context) {
        return new IntegrationData("Chartboost", VERSION);
    }

    // get adapter version
    @Override
    public String getVersion() {
        return VERSION;
    }

    //get network sdk version
    @Override
    public String getCoreSDKVersion() {
        return getAdapterSDKVersion();
    }

    public static String getAdapterSDKVersion() {
        return Chartboost.getSDKVersion();
    }

    public boolean isUsingActivityBeforeImpression(@NotNull IronSource.AD_UNIT adUnit) {
        return false;
    }

    //endregion

    //region Initializations methods and callbacks
    private void initSDK(final String appId, final String appSignature) {
        // add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this);
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            IronLog.ADAPTER_API.verbose("appId = " + appId + ", appSignature = " + appSignature);

            mInitState = InitState.INIT_STATE_IN_PROGRESS;

            // set consent data
            if (mConsentCollectingUserData != null) {
                setConsent(mConsentCollectingUserData);
            }

            // set CCPA data
            if (mDoNotSellCollectingUserData != null) {
                setCCPAValue(mDoNotSellCollectingUserData);
            }

            // set COPPA data
            if(mCoppaUserData != null) {
                setCOPPAValue(mCoppaUserData);
            }

            // set log level
            Chartboost.setLoggingLevel(isAdaptersDebugEnabled() ? LoggingLevel.ALL : LoggingLevel.NONE);

            // init Chartboost SDK
            Chartboost.startWithAppId(ContextProvider.getInstance().getApplicationContext(), appId, appSignature, new StartCallback() {
                @Override
                public void onStartCompleted(@Nullable StartError startError) {
                    if (startError == null) {
                        initializationSuccess();
                    } else {
                        initializationFailure();
                    }
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

    private void initializationFailure() {
        IronLog.ADAPTER_CALLBACK.verbose();

        mInitState = InitState.INIT_STATE_FAILED;

        //iterate over all the adapter instances and report init failed
        for (INetworkInitCallbackListener adapter : initCallbackListeners) {
            adapter.onNetworkInitCallbackFailed("Chartboost sdk init failed");
        }

        initCallbackListeners.clear();
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        // Rewarded Video
        for (String locationId : mLocationIdToRewardedVideoListener.keySet()) {
            RewardedVideoSmashListener listener = mLocationIdToRewardedVideoListener.get(locationId);
            if (mRewardedVideoLocationIdsForInitCallbacks.contains(locationId)) {
                listener.onRewardedVideoInitSuccess();
            } else {
                loadRewardedVideoInternal(locationId, null, listener);
            }
        }

        // Interstitial
        for (InterstitialSmashListener listener : mLocationIdToInterstitialListener.values()) {
            listener.onInterstitialInitSuccess();
        }

        // Banner
        for (BannerSmashListener listener : mLocationIdToBannerListener.values()) {
            listener.onBannerInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        // Rewarded Video
        for (String locationId : mLocationIdToRewardedVideoListener.keySet()) {
            RewardedVideoSmashListener listener = mLocationIdToRewardedVideoListener.get(locationId);
            if (mRewardedVideoLocationIdsForInitCallbacks.contains(locationId)) {
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            } else {
                listener.onRewardedVideoAvailabilityChanged(false);
            }
        }

        // Interstitial
        for (InterstitialSmashListener listener : mLocationIdToInterstitialListener.values()) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }

        // Banner
        for (BannerSmashListener listener : mLocationIdToBannerListener.values()) {
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.BANNER_AD_UNIT));
        }
    }

    //endregion

    //region Rewarded Video API

    @Override
    // used for flows when the mediation needs to get a callback for init
    public void initRewardedVideoWithCallback(String appKey, String userId, JSONObject config, RewardedVideoSmashListener listener) {
        final String locationId = config.optString(AD_LOCATION);
        final String appId = config.optString(APP_ID);
        final String appSignature = config.optString(APP_SIGNATURE);

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("Missing param - " + APP_ID);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + APP_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(appSignature)) {
            IronLog.INTERNAL.error("Missing param - " + APP_SIGNATURE);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + APP_SIGNATURE, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(locationId)) {
            IronLog.INTERNAL.error("Missing param - " + AD_LOCATION);
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + AD_LOCATION, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("locationId = " + locationId);

        //add to rewarded video listener map
        mLocationIdToRewardedVideoListener.put(locationId, listener);

        //add to rewarded video init callback map
        mRewardedVideoLocationIdsForInitCallbacks.add(locationId);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(appId, appSignature);
                break;
            case INIT_STATE_SUCCESS:
                listener.onRewardedVideoInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - locationId = " + locationId);
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Chartboost sdk init failed", IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                break;
        }
    }

    @Override
    // used for flows when the mediation doesn't need to get a callback for init
    public void initAndLoadRewardedVideo(String appKey, String userId, JSONObject config, JSONObject adData, RewardedVideoSmashListener listener) {
        final String locationId = config.optString(AD_LOCATION);
        final String appId = config.optString(APP_ID);
        final String appSignature = config.optString(APP_SIGNATURE);

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("Missing param - " + APP_ID);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        if (TextUtils.isEmpty(appSignature)) {
            IronLog.INTERNAL.error("Missing param - " + APP_SIGNATURE);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        if (TextUtils.isEmpty(locationId)) {
            IronLog.INTERNAL.error("Missing param - " + AD_LOCATION);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        IronLog.ADAPTER_API.verbose("locationId = " + locationId);

        //add to rewarded video listener map
        mLocationIdToRewardedVideoListener.put(locationId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(appId, appSignature);
                break;
            case INIT_STATE_SUCCESS:
                loadRewardedVideoInternal(locationId, null, listener);
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - locationId = " + locationId);
                listener.onRewardedVideoAvailabilityChanged(false);
                break;
        }
    }

    @Override
    public void loadRewardedVideo(final JSONObject config, JSONObject adData, final RewardedVideoSmashListener listener) {
        final String locationId = config.optString(AD_LOCATION);
        IronLog.ADAPTER_API.verbose("locationId = <locationId>");
        loadRewardedVideoInternal(locationId, null, listener);
    }

    @Override
    public void loadRewardedVideoForBidding(JSONObject config, JSONObject adData, String serverData, RewardedVideoSmashListener listener) {
        final String locationId = config.optString(AD_LOCATION);
        IronLog.ADAPTER_API.verbose("locationId = <locationId>");
        loadRewardedVideoInternal(locationId, serverData, listener);
    }

    private void loadRewardedVideoInternal(final String locationId, final String serverData, final RewardedVideoSmashListener listener) {
        IronLog.ADAPTER_API.verbose("locationId = " + locationId);

        ChartboostRewardedVideoAdListener rewardedVideoAdListener = new ChartboostRewardedVideoAdListener(listener, locationId);
        mLocationIdToRewardedVideoAdListener.put(locationId, rewardedVideoAdListener);

        Rewarded rewardedVideoAd = new Rewarded(locationId, rewardedVideoAdListener, getMediation());
        mLocationIdToRewardedVideoAd.put(locationId, rewardedVideoAd);

        if (serverData == null) {
            rewardedVideoAd.cache();
        } else {
            rewardedVideoAd.cache(serverData);
        }
    }

    @Override
    public void showRewardedVideo(JSONObject config, RewardedVideoSmashListener listener) {
        final String locationId = config.optString(AD_LOCATION);
        IronLog.ADAPTER_API.verbose("locationId = " + locationId);

        if (isRewardedVideoAvailable(config)) {
            Rewarded rewardedVideoAd = mLocationIdToRewardedVideoAd.get(locationId);
            rewardedVideoAd.show();
        } else {
            listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
        }
    }

    @Override
    public boolean isRewardedVideoAvailable(JSONObject config) {
        final String locationId = config.optString(AD_LOCATION);
        Rewarded rewardedVideoAd = mLocationIdToRewardedVideoAd.get(locationId);
        return rewardedVideoAd != null && rewardedVideoAd.isCached();
    }

    @Override
    public Map<String, Object> getRewardedVideoBiddingData(JSONObject config, JSONObject adData) {
        return getBiddingData();
    }

    //endregion

    //region Interstitial API
    @Override
    public void initInterstitial(String appKey, String userId, JSONObject config, InterstitialSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        initInterstitialInternal(config, listener);
    }

    @Override
    public void initInterstitialForBidding(String appKey, String userId, JSONObject config, InterstitialSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        initInterstitialInternal(config, listener);
    }

    public void initInterstitialInternal(JSONObject config, InterstitialSmashListener listener){
        final String locationId = config.optString(AD_LOCATION);
        final String appId = config.optString(APP_ID);
        final String appSignature = config.optString(APP_SIGNATURE);

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("missing param - " + APP_ID);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + APP_ID, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(appSignature)) {
            IronLog.INTERNAL.error("missing param - " + APP_SIGNATURE);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + APP_SIGNATURE, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(locationId)) {
            IronLog.INTERNAL.error("missing param - " + AD_LOCATION);
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + AD_LOCATION, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("locationId = " + locationId);

        //add to interstitial listener map
        mLocationIdToInterstitialListener.put(locationId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(appId, appSignature);
                break;
            case INIT_STATE_SUCCESS:
                listener.onInterstitialInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - locationId = " + locationId);
                listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Chartboost sdk init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT));
                break;
        }
    }


    @Override
    public void loadInterstitial(JSONObject config, JSONObject adData, InterstitialSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        loadInterstitialInternal(config, null, listener);
    }

    @Override
    public void loadInterstitialForBidding(JSONObject config, JSONObject adData, String serverData, InterstitialSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        loadInterstitialInternal(config, serverData, listener);
    }

    private void loadInterstitialInternal(JSONObject config, String serverData, InterstitialSmashListener listener){
        final String locationId = config.optString(AD_LOCATION);
        IronLog.ADAPTER_API.verbose("locationId = " + locationId);

        ChartboostInterstitialAdListener interstitialAdListener = new ChartboostInterstitialAdListener(listener, locationId);
        mLocationIdToInterstitialAdListener.put(locationId, interstitialAdListener);

        Interstitial interstitialAd = new Interstitial(locationId, interstitialAdListener, getMediation());
        mLocationIdToInterstitialAd.put(locationId, interstitialAd);

        if (serverData == null) {
            interstitialAd.cache();
        } else {
            interstitialAd.cache(serverData);
        }
    }

    @Override
    public void showInterstitial(JSONObject config, InterstitialSmashListener listener) {
        final String locationId = config.optString(AD_LOCATION);
        IronLog.ADAPTER_API.verbose("locationId = " + locationId);

        if (isInterstitialReady(config)) {
            Interstitial interstitialAd = mLocationIdToInterstitialAd.get(locationId);
            interstitialAd.show();
        } else {
            listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    @Override
    public boolean isInterstitialReady(JSONObject config) {
        final String locationId = config.optString(AD_LOCATION);
        Interstitial interstitialAd = mLocationIdToInterstitialAd.get(locationId);
        return interstitialAd != null && interstitialAd.isCached();
    }

    @Override
    public Map<String, Object> getInterstitialBiddingData(JSONObject config, JSONObject adData) {
        return getBiddingData();
    }

    //endregion

    //region Banner API
    @Override
    public void initBanners(String appKey, String userId, JSONObject config, final BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        initBannersInternal(config, listener);
    }

    @Override
    public void initBannerForBidding(String appKey, String userId, JSONObject config, final BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        initBannersInternal(config, listener);
    }

    private void initBannersInternal(JSONObject config, final BannerSmashListener listener){
        final String locationId = config.optString(AD_LOCATION);
        final String appId = config.optString(APP_ID);
        final String appSignature = config.optString(APP_SIGNATURE);

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("missing param - " + APP_ID);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + APP_ID, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(appSignature)) {
            IronLog.INTERNAL.error("missing param - " + APP_SIGNATURE);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + APP_SIGNATURE, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        if (TextUtils.isEmpty(locationId)) {
            IronLog.INTERNAL.error("missing param - " + AD_LOCATION);
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing param - " + AD_LOCATION, IronSourceConstants.BANNER_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("locationId = " + locationId);

        //add to banner listener map
        mLocationIdToBannerListener.put(locationId, listener);

        switch (mInitState) {
            case INIT_STATE_NONE:
            case INIT_STATE_IN_PROGRESS:
                initSDK(appId, appSignature);
                break;
            case INIT_STATE_SUCCESS:
                listener.onBannerInitSuccess();
                break;
            case INIT_STATE_FAILED:
                IronLog.INTERNAL.verbose("init failed - locationId = " + locationId);
                listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Chartboost sdk init failed", IronSourceConstants.BANNER_AD_UNIT));
                break;
        }
    }

    @Override
    public void loadBanner(final JSONObject config, final JSONObject adData, final IronSourceBannerLayout banner, final BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        loadBannerInternal(config, banner, null, listener);
    }

    @Override
    public void loadBannerForBidding(JSONObject config, JSONObject adData, String serverData, IronSourceBannerLayout banner, BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        loadBannerInternal(config, banner, serverData, listener);

    }

    private void loadBannerInternal(JSONObject config,  IronSourceBannerLayout banner, String serverData, BannerSmashListener listener){
        final String locationId = config.optString(AD_LOCATION);
        IronLog.ADAPTER_API.verbose("locationId = " + locationId);

        if (banner == null) {
            IronLog.ADAPTER_API.error("banner layout is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner layout is null"));
            return;
        }

        // get size
        Banner.BannerSize bannerSize = getBannerSize(banner.getSize());

        // verify if size is null
        if (bannerSize == null) {
            IronLog.INTERNAL.error("size not supported, size is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getProviderName()));
            return;
        }

        // add banner layout to map
        mLocationIdToBannerLayout.put(locationId, banner);

        // get banner
        Banner chartboostBanner = getChartboostBanner(banner, locationId, listener);

        if (serverData == null) {
            chartboostBanner.cache();
        } else {
            chartboostBanner.cache(serverData);
        }
    }

    @Override
    public Map<String, Object> getBannerBiddingData(JSONObject config, JSONObject adData) {
        return getBiddingData();
    }

    //endregion

    //region legal
    @Override
    protected void setConsent(boolean consent) {
        if (mWasInitCalled.get()) {
            IronLog.ADAPTER_API.verbose("consent = " + (consent ? "BEHAVIORAL" : "NON_BEHAVIORAL"));
            GDPR.GDPR_CONSENT chartboostConsent = consent ? GDPR.GDPR_CONSENT.BEHAVIORAL : GDPR.GDPR_CONSENT.NON_BEHAVIORAL;
            Chartboost.addDataUseConsent(ContextProvider.getInstance().getApplicationContext(), new GDPR(chartboostConsent));
        } else {
            mConsentCollectingUserData = consent;
        }
    }

    @Override
    protected void setMetaData(String key, List<String> values) {
        if (values.isEmpty()) {
            return;
        }

        // this is a list of 1 value.
        String value = values.get(0);
        IronLog.ADAPTER_API.verbose("key = " + key + ", value = " + value);

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            mDoNotSellCollectingUserData = MetaDataUtils.getMetaDataBooleanValue(value);
        } else {
            String formattedValue = MetaDataUtils.formatValueForType(value, META_DATA_VALUE_BOOLEAN);

            if (MetaDataUtils.isValidMetaData(key, CHARTBOOST_COPPA_FLAG, formattedValue)) {
                mCoppaUserData = MetaDataUtils.getMetaDataBooleanValue(formattedValue);
            }
        }
    }

    private void setCCPAValue(final boolean value) {
        IronLog.ADAPTER_API.verbose("value = " + (value ? "OPT_OUT_SALE" : "OPT_IN_SALE"));
        DataUseConsent dataUseConsent = new CCPA(value? CCPA.CCPA_CONSENT.OPT_OUT_SALE : CCPA.CCPA_CONSENT.OPT_IN_SALE);
        Chartboost.addDataUseConsent(ContextProvider.getInstance().getApplicationContext(), dataUseConsent);
    }

    private void setCOPPAValue(final boolean isUserCoppa) {
        IronLog.ADAPTER_API.verbose("value = " + isUserCoppa);
        DataUseConsent dataUseConsent = new COPPA(isUserCoppa);
        Chartboost.addDataUseConsent(ContextProvider.getInstance().getApplicationContext(), dataUseConsent);
    }

    //endregion

    // region Helpers

    private Map<String, Object> getBiddingData() {
        if (mInitState != InitState.INIT_STATE_SUCCESS) {
            IronLog.INTERNAL.error("returning null as token since init isn't completed");
            return null;
        }
        String bidderToken = Chartboost.getBidderToken();
        String returnedToken = (!TextUtils.isEmpty(bidderToken)) ? bidderToken : "";
        IronLog.ADAPTER_API.verbose("token = " + returnedToken);
        Map<String, Object> ret = new HashMap<>();
        ret.put("token", returnedToken);
        return ret;
    }

    private Banner.BannerSize getBannerSize(ISBannerSize size) {
        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
                return Banner.BannerSize.STANDARD;
            case "RECTANGLE":
                return Banner.BannerSize.MEDIUM;
            case "SMART":
                return AdapterUtils.isLargeScreen(ContextProvider.getInstance().getApplicationContext()) ?Banner.BannerSize.LEADERBOARD : Banner.BannerSize.STANDARD;
            case "CUSTOM":
                if (size.getHeight() >= 40 && size.getHeight() <= 60) {
                    return Banner.BannerSize.STANDARD;
                }
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

    private Banner getChartboostBanner(IronSourceBannerLayout banner, String locationId, BannerSmashListener listener) {
        // get banner from the map
        Banner chartboostBanner = mLocationIdToBannerAd.get(locationId);

        // check if null, if so, create a new one
        if (chartboostBanner == null) {
            IronLog.ADAPTER_API.verbose("creating banner");

            // get size
            Banner.BannerSize bannerSize = getBannerSize(banner.getSize());

            FrameLayout.LayoutParams bannerLayoutParams = getBannerLayoutParams(banner.getSize());

            ChartboostBannerAdListener bannerAdListener = new ChartboostBannerAdListener(ChartboostAdapter.this, listener, locationId, bannerLayoutParams);
            mLocationIdToBannerAdListener.put(locationId, bannerAdListener);

            // create banner
            chartboostBanner = new Banner(ContextProvider.getInstance().getApplicationContext(), locationId, bannerSize, bannerAdListener, getMediation());

            // add layout params
            chartboostBanner.setLayoutParams(bannerLayoutParams);

            // add banner layout to map
            mLocationIdToBannerAd.put(locationId, chartboostBanner);
        }

        return chartboostBanner;
    }

    @Override
    public void destroyBanner(JSONObject config) {
        final String locationId = config.optString(AD_LOCATION);
        IronLog.ADAPTER_API.verbose("locationId = " + locationId);

        Banner banner = mLocationIdToBannerAd.get(locationId);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (banner != null) {
                    // destroy banner
                    banner.detach();

                    // remove banner view from map
                    mLocationIdToBannerAd.remove(locationId);

                    // remove banner layout from map
                    mLocationIdToBannerLayout.remove(locationId);

                    // remove banner listener from map
                    mLocationIdToBannerListener.remove(locationId);

                    // remove banner ad listener from map
                    mLocationIdToBannerAdListener.remove(locationId);
                }
            }
        });
    }

    private Mediation getMediation() {
        if (mMediationInfo == null) {
            mMediationInfo = new Mediation(MEDIATION_NAME, IronSourceUtils.getSDKVersion() , VERSION);
        }

        return mMediationInfo;
    }

    @Override
    public void releaseMemory(IronSource.AD_UNIT adUnit, JSONObject config) {
        IronLog.INTERNAL.verbose("adUnit = " + adUnit);

        if (adUnit == IronSource.AD_UNIT.REWARDED_VIDEO) {
            // release rewarded ads
            mLocationIdToRewardedVideoListener.clear();
            mLocationIdToRewardedVideoAdListener.clear();
            mLocationIdToRewardedVideoAd.clear();
            mRewardedVideoLocationIdsForInitCallbacks.clear();

        } else if (adUnit == IronSource.AD_UNIT.INTERSTITIAL) {
            // release interstitial ads
            mLocationIdToInterstitialListener.clear();
            mLocationIdToInterstitialAdListener.clear();
            mLocationIdToInterstitialAd.clear();

        } else if (adUnit == IronSource.AD_UNIT.BANNER) {
            // release banner ads
            postOnUIThread(new Runnable() {
                @Override
                public void run() {
                    for (Banner banner : mLocationIdToBannerAd.values()) {
                        banner.detach();
                    }
                    mLocationIdToBannerAd.clear();
                    mLocationIdToBannerListener.clear();
                    mLocationIdToBannerAdListener.clear();
                    mLocationIdToBannerLayout.clear();
                }
            });
        }
    }
    //endregion
}