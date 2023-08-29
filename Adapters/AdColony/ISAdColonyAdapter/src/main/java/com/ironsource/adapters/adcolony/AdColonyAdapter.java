package com.ironsource.adapters.adcolony;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyAdSize;
import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.AbstractAdapter;
import com.ironsource.mediationsdk.AdapterUtils;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IntegrationData;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.LoadWhileShowSupportState;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.metadata.MetaData;
import com.ironsource.mediationsdk.metadata.MetaDataUtils;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.ironsource.mediationsdk.utils.IronSourceConstants;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


class AdColonyAdapter extends AbstractAdapter {

    //AdColony requires a request agent name
    private final String MEDIATION_NAME = "ironSource";

    //adapter version
    private static final String VERSION = BuildConfig.VERSION_NAME;

    private static final String GitHash = BuildConfig.GitHash;
    private final String APP_ID = "appID";
    private final String ZONE_ID = "zoneId";
    private final String ADM = "adm";

    //init flag
    private static AtomicBoolean mAlreadyInitiated = new AtomicBoolean(false);

    // Meta data flags
    private static final String META_DATA_ADCOLONY_COPPA = "AdColony_COPPA";
    private static final String META_DATA_ADCOLONY_CHILD_DIRECTED = "AdColony_App_Child_Directed";

    // AdColony options
    private static AdColonyAppOptions mAdColonyOptions = new AdColonyAppOptions();

    // Rewarded Video

    private ConcurrentHashMap<String, AdColonyRewardedVideoAdListener> mZoneIdToRewardedVideoListener;
    protected ConcurrentHashMap<String, AdColonyInterstitial> mZoneIdToRewardedVideoAdObject;

    // Interstitial

    private ConcurrentHashMap<String, AdColonyInterstitialAdListener> mZoneIdToInterstitialListener;
    protected ConcurrentHashMap<String, AdColonyInterstitial> mZoneIdToInterstitialAdObject;

    // Banner

    protected ConcurrentHashMap<String, AdColonyAdView> mZoneIdToBannerAdView;
    protected ConcurrentHashMap<String, IronSourceBannerLayout> mZoneIdToBannerLayout;
    protected ConcurrentHashMap<String, AdColonyBannerAdListener> mZoneIdToBannerAdListener;

    //region Adapter Methods
    public static AdColonyAdapter startAdapter(String providerName) {
        return new AdColonyAdapter(providerName);
    }

    private AdColonyAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose();

        // rewarded video
        mZoneIdToRewardedVideoListener = new ConcurrentHashMap<>();
        mZoneIdToRewardedVideoAdObject = new ConcurrentHashMap<>();

        // interstitial
        mZoneIdToInterstitialListener = new ConcurrentHashMap<>();
        mZoneIdToInterstitialAdObject = new ConcurrentHashMap<>();

        // banner
        mZoneIdToBannerLayout = new ConcurrentHashMap<>();
        mZoneIdToBannerAdView = new ConcurrentHashMap<>();
        mZoneIdToBannerAdListener = new ConcurrentHashMap<>();

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_NETWORK;
    }

    // get the network and adapter integration data
    public static IntegrationData getIntegrationData(Context context) {
        return new IntegrationData("AdColony", VERSION);
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
        return AdColony.getSDKVersion();
    }

    public boolean isUsingActivityBeforeImpression(@NotNull IronSource.AD_UNIT adUnit) {
        return false;
    }
    //endregion

    //region Initializations methods and callbacks

    private void initSDK(String userId, String appId) {
        if (mAlreadyInitiated.compareAndSet(false, true)) {
            IronLog.ADAPTER_API.verbose("appId = " + appId);

            if (!TextUtils.isEmpty(userId)) {
                IronLog.ADAPTER_API.verbose("setUserID to " + userId);
                mAdColonyOptions.setUserID(userId);
            }

            mAdColonyOptions.setMediationNetwork(MEDIATION_NAME, VERSION);

            //no need for init callbacks because AdColony doesn't have an init failed callback
            AdColony.configure((Application) ContextProvider.getInstance().getApplicationContext(), mAdColonyOptions, appId);
        }
    }

    //region Rewarded Video API
    // used for flows when the mediation needs to get a callback for init
    @Override
    public void initRewardedVideoWithCallback(String appKey, String userId, JSONObject config, final RewardedVideoSmashListener listener) {
        final String appId = config.optString(APP_ID);
        final String zoneId = config.optString(ZONE_ID);

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("error - missing param = " + APP_ID);
            IronSourceError error = ErrorBuilder.buildInitFailedError("Missing params - " + APP_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT);
            listener.onRewardedVideoInitFailed(error);
            return;
        }

        if (TextUtils.isEmpty(zoneId)) {
            IronLog.INTERNAL.error("error - missing param = " + ZONE_ID);
            IronSourceError error = ErrorBuilder.buildInitFailedError("Missing params - " + ZONE_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT);
            listener.onRewardedVideoInitFailed(error);
            return;
        }

        IronLog.ADAPTER_API.verbose("appId = " + appId + " zoneId = " + zoneId);

        // init SDK
        initSDK(userId, appId);

        // call init success
        listener.onRewardedVideoInitSuccess();
    }

    // used for flows when the mediation doesn't need to get a callback for init
    @Override
    public void initAndLoadRewardedVideo(String appKey, String userId, final JSONObject config, JSONObject adData, final RewardedVideoSmashListener listener) {

        final String appId = config.optString(APP_ID);
        final String zoneId = config.optString(ZONE_ID);

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("error - missing param = " + APP_ID);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        if (TextUtils.isEmpty(zoneId)) {
            IronLog.INTERNAL.error("error - missing param = " + ZONE_ID);
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        IronLog.ADAPTER_API.verbose("appId = " + appId + " zoneId = " + zoneId);

        // init SDK
        initSDK(userId, appId);

        // load rewarded video ad
        loadRewardedVideo(config, adData, listener);
    }

    @Override
    public void loadRewardedVideoForBidding(JSONObject config, JSONObject adData, final String serverData, final RewardedVideoSmashListener listener) {
        final String zoneId = config.optString(ZONE_ID);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);
        AdColonyAdOptions adOptions = new AdColonyAdOptions().setOption(ADM, serverData);
        AdColonyRewardedVideoAdListener rewardedVideoListener = new AdColonyRewardedVideoAdListener(AdColonyAdapter.this, listener, zoneId);
        mZoneIdToRewardedVideoListener.put(zoneId, rewardedVideoListener);
        AdColony.requestInterstitial(zoneId, rewardedVideoListener, adOptions);
    }

    @Override
    public void loadRewardedVideo(JSONObject config, JSONObject adData, RewardedVideoSmashListener listener) {
        final String zoneId = config.optString(ZONE_ID);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);
        AdColonyRewardedVideoAdListener rewardedVideoListener = new AdColonyRewardedVideoAdListener(AdColonyAdapter.this, listener, zoneId);
        mZoneIdToRewardedVideoListener.put(zoneId, rewardedVideoListener);
        AdColony.requestInterstitial(zoneId, rewardedVideoListener);
    }

    @Override
    public void showRewardedVideo(JSONObject config, RewardedVideoSmashListener listener) {
        final String zoneId = config.optString(ZONE_ID);
        AdColonyInterstitial rewardedVideoAd = mZoneIdToRewardedVideoAdObject.get(zoneId);

        if (isRewardedVideoAvailable(config)) {
            IronLog.ADAPTER_API.verbose("show zoneId = " + zoneId);
            AdColonyRewardedVideoAdListener rewardedVideoListener = mZoneIdToRewardedVideoListener.get(zoneId);
            AdColony.setRewardListener(rewardedVideoListener);
            rewardedVideoAd.show();
        } else {
            IronLog.INTERNAL.error("ad is expired");
            listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
        }
    }

    @Override
    public boolean isRewardedVideoAvailable(JSONObject config) {
        final String zoneId = config.optString(ZONE_ID);
        AdColonyInterstitial rewardedVideoAd = mZoneIdToRewardedVideoAdObject.get(zoneId);
        boolean isRewardedVideoAvailable = (rewardedVideoAd != null) && !rewardedVideoAd.isExpired();
        IronLog.ADAPTER_API.verbose("isRewardedVideoAvailable = " + isRewardedVideoAvailable);
        return isRewardedVideoAvailable;
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
        initInterstitialInternal(appKey, userId, config, listener);
    }

    @Override
    public void initInterstitial(String appKey, final String userId, final JSONObject config, final InterstitialSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        initInterstitialInternal(appKey, userId, config, listener);
    }

    private void initInterstitialInternal(String appKey, final String userId, final JSONObject config, final InterstitialSmashListener listener) {
        final String appId = config.optString(APP_ID);
        final String zoneId = config.optString(ZONE_ID);

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("error - missing param = " + APP_ID);
            IronSourceError error = ErrorBuilder.buildInitFailedError("Missing params - " + APP_ID, IronSourceConstants.INTERSTITIAL_AD_UNIT);
            listener.onInterstitialInitFailed(error);
            return;
        }

        if (TextUtils.isEmpty(zoneId)) {
            IronLog.INTERNAL.error("error - missing param = " + ZONE_ID);
            IronSourceError error = ErrorBuilder.buildInitFailedError("Missing params - " + ZONE_ID, IronSourceConstants.INTERSTITIAL_AD_UNIT);
            listener.onInterstitialInitFailed(error);
            return;
        }

        IronLog.ADAPTER_API.verbose("appId = " + appId + " zoneId = " + zoneId);

        // init SDK
        initSDK(userId, appId);

        // call init success
        listener.onInterstitialInitSuccess();
    }

    @Override
    public void loadInterstitialForBidding(final JSONObject config, final JSONObject adData, final String serverData, final InterstitialSmashListener listener) {
        final String zoneId = config.optString(ZONE_ID);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);
        AdColonyAdOptions adOptions = new AdColonyAdOptions().setOption(ADM, serverData);
        AdColonyInterstitialAdListener interstitialListener = new AdColonyInterstitialAdListener(AdColonyAdapter.this, listener, zoneId);
        mZoneIdToInterstitialListener.put(zoneId, interstitialListener);
        AdColony.requestInterstitial(zoneId, interstitialListener, adOptions);
    }

    @Override
    public void loadInterstitial(JSONObject config, JSONObject adData, InterstitialSmashListener listener) {
        final String zoneId = config.optString(ZONE_ID);
        IronLog.ADAPTER_API.verbose("zoneId " + zoneId);
        AdColonyInterstitialAdListener interstitialListener = new AdColonyInterstitialAdListener(AdColonyAdapter.this, listener, zoneId);
        mZoneIdToInterstitialListener.put(zoneId, interstitialListener);
        AdColony.requestInterstitial(zoneId, interstitialListener);
    }

    @Override
    public void showInterstitial(JSONObject config, InterstitialSmashListener listener) {
        final String zoneId = config.optString(ZONE_ID);
        AdColonyInterstitial interstitialAd = mZoneIdToInterstitialAdObject.get(zoneId);

        if (isInterstitialReady(config)) {
            IronLog.ADAPTER_API.verbose("show zoneId = " + zoneId);
            interstitialAd.show();
        } else {
            IronLog.INTERNAL.error("ad is expired");
            listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    @Override
    public boolean isInterstitialReady(JSONObject config) {
        final String zoneId = config.optString(ZONE_ID);
        AdColonyInterstitial interstitialAd = mZoneIdToInterstitialAdObject.get(zoneId);
        boolean isInterstitialAvailable = (interstitialAd != null) && !interstitialAd.isExpired();
        IronLog.ADAPTER_API.verbose("isInterstitialAvailable = " + isInterstitialAvailable);
        return isInterstitialAvailable;
    }

    @Override
    public Map<String, Object> getInterstitialBiddingData(JSONObject config, JSONObject adData) {
        return getBiddingData();
    }
    //endregion

    //region Banner API
    @Override
    public void initBanners(String appKey, final String userId, final JSONObject config, final BannerSmashListener listener) {
        initBannersInternal(userId, config, listener);
    }

    @Override
    public void initBannerForBidding(String appKey, String userId, JSONObject config, BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        initBannersInternal(userId, config, listener);
    }

    private void initBannersInternal(final String userId, final JSONObject config, final BannerSmashListener listener) {
        final String appId = config.optString(APP_ID);
        final String zoneId = config.optString(ZONE_ID);

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("error - missing param = " + APP_ID);
            IronSourceError error = ErrorBuilder.buildInitFailedError("Missing params - " + APP_ID, IronSourceConstants.BANNER_AD_UNIT);
            listener.onBannerInitFailed(error);
            return;
        }

        if (TextUtils.isEmpty(zoneId)) {
            IronLog.INTERNAL.error("error - missing param = " + ZONE_ID);
            IronSourceError error = ErrorBuilder.buildInitFailedError("Missing params - " + ZONE_ID, IronSourceConstants.BANNER_AD_UNIT);
            return;
        }

        // init SDK
        initSDK(userId, appId);

        // call init success
        listener.onBannerInitSuccess();
    }

    @Override
    public void loadBannerForBidding(JSONObject config, JSONObject adData, String serverData, IronSourceBannerLayout banner, BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        AdColonyAdOptions adOptions = new AdColonyAdOptions().setOption(ADM, serverData);
        loadBannerInternal(banner, config, listener, adOptions);
    }

    @Override
    public void loadBanner(final JSONObject config, final JSONObject adData, final IronSourceBannerLayout banner, final BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose();
        loadBannerInternal(banner, config, listener, null);
    }

    private void loadBannerInternal(IronSourceBannerLayout banner, JSONObject config, BannerSmashListener listener, AdColonyAdOptions adOptions) {
        // validate banner layout
        if (banner == null) {
            IronLog.ADAPTER_API.error("banner layout is null");
            listener.onBannerAdLoadFailed(ErrorBuilder.buildNoConfigurationAvailableError("banner layout is null"));
            return;
        }

        // verify size
        ISBannerSize ironSourceBannerSize = banner.getSize();

        if (!isBannerSizeSupported(ironSourceBannerSize)) {
            IronLog.INTERNAL.error("loadBanner - size not supported, size = " + ironSourceBannerSize.getDescription());
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getProviderName()));
            return;
        }

        final String zoneId = config.optString(ZONE_ID);

        // add banner layout
        mZoneIdToBannerLayout.put(zoneId, banner);

        // get size
        AdColonyAdSize bannerSize = getBannerSize(ironSourceBannerSize);

        FrameLayout.LayoutParams layoutParams = getBannerLayoutParams(ironSourceBannerSize);

        AdColonyBannerAdListener bannerListener = new AdColonyBannerAdListener(AdColonyAdapter.this, listener, zoneId, layoutParams);

        mZoneIdToBannerAdListener.put(zoneId, bannerListener);

        // request banner from AdColony
        IronLog.ADAPTER_API.verbose("zone id " + zoneId);
        AdColony.requestAdView(zoneId, bannerListener, bannerSize, adOptions);
    }

    // destroy banner ad and clear banner ad map
    @Override
    public void destroyBanner(final JSONObject config) {
        final String zoneId = config.optString(ZONE_ID);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        // remove from layout map
        mZoneIdToBannerLayout.remove(zoneId);
        AdColonyAdView bannerView = mZoneIdToBannerAdView.get(zoneId);

        if (bannerView != null) {
            // destroy banner
            bannerView.destroy();

            // remove banner layout from map
            mZoneIdToBannerAdView.remove(zoneId);

            // remove banner ad listener from map
            mZoneIdToBannerAdListener.remove(zoneId);

            // remove
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
        if (adUnit == IronSource.AD_UNIT.BANNER) {
            // release banner ads
            destroyBanner(config);
        }
    }
    //endregion

    //region legal
    @Override
    protected void setConsent(boolean consent) {
        mAdColonyOptions.setPrivacyConsentString(AdColonyAppOptions.GDPR, consent ? "1" : "0");
        mAdColonyOptions.setPrivacyFrameworkRequired(AdColonyAppOptions.GDPR, true);

        if (mAlreadyInitiated.get()) {
            IronLog.ADAPTER_API.verbose("consent = " + consent);
            AdColony.setAppOptions(mAdColonyOptions);
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
            setCCPAValue(value);
        } else {
            String formattedValue = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN);

            if (MetaDataUtils.isValidMetaData(key, META_DATA_ADCOLONY_COPPA, formattedValue)) {
                setCOPPAValue(formattedValue);
            } else if (MetaDataUtils.isValidMetaData(key, META_DATA_ADCOLONY_CHILD_DIRECTED, formattedValue)) {
                setChildDirectedValue(formattedValue);
            }
        }
    }

    private void setCCPAValue(final String value) {
        //When "do_not_sell" is true --> report Consent = false
        //When "do_not_sell" is false --> report Consent= true
        boolean isCCPAOptedIn = !MetaDataUtils.getMetaDataBooleanValue(value);
        String consentString = isCCPAOptedIn ? "1" : "0";
        IronLog.ADAPTER_API.verbose("value = " + value + " consentString = " + consentString);
        mAdColonyOptions.setPrivacyFrameworkRequired(AdColonyAppOptions.CCPA, true)
                .setPrivacyConsentString(AdColonyAppOptions.CCPA, consentString);

        if (mAlreadyInitiated.get()) {
            IronLog.ADAPTER_API.verbose("consent = " + consentString);
            AdColony.setAppOptions(mAdColonyOptions);
        }
    }

    private void setCOPPAValue(final String value) {
        IronLog.ADAPTER_API.verbose("value = " + value);
        boolean isCOPPAOptedIn = MetaDataUtils.getMetaDataBooleanValue(value);
        mAdColonyOptions.setPrivacyFrameworkRequired(AdColonyAppOptions.COPPA, isCOPPAOptedIn);

        if (mAlreadyInitiated.get()) {
            IronLog.ADAPTER_API.verbose("coppa = " + isCOPPAOptedIn);
            AdColony.setAppOptions(mAdColonyOptions);
        }
    }

    private void setChildDirectedValue(final String value) {
        IronLog.ADAPTER_API.verbose("value = " + value);
        boolean isChildDirected = MetaDataUtils.getMetaDataBooleanValue(value);
        mAdColonyOptions.setIsChildDirectedApp(isChildDirected);

        if (mAlreadyInitiated.get()) {
            IronLog.ADAPTER_API.verbose("isChildDirected = " + isChildDirected);
            AdColony.setAppOptions(mAdColonyOptions);
        }
    }

    // region Helpers

    private Map<String, Object> getBiddingData() {
        String bidderToken = AdColony.collectSignals();
        String returnedToken = (!TextUtils.isEmpty(bidderToken)) ? bidderToken : "";
        String sdkVersion = getCoreSDKVersion();

        IronLog.ADAPTER_API.verbose("token = " + returnedToken);
        IronLog.ADAPTER_API.verbose("sdkVersion = " + sdkVersion);

        Map<String, Object> ret = new HashMap<>();
        ret.put("sdkVersion", sdkVersion);
        ret.put("token", returnedToken);
        return ret;
    }


    private boolean isBannerSizeSupported(ISBannerSize size) {
        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
            case "RECTANGLE":
            case "SMART":
            case "CUSTOM":
                return true;
        }

        return false;
    }

    private AdColonyAdSize getBannerSize(ISBannerSize size) {
        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
                return AdColonyAdSize.BANNER;
            case "RECTANGLE":
                return AdColonyAdSize.MEDIUM_RECTANGLE;
            case "SMART":
                return AdapterUtils.isLargeScreen(ContextProvider.getInstance().getApplicationContext()) ? AdColonyAdSize.LEADERBOARD : AdColonyAdSize.BANNER;
            case "CUSTOM":
                return new AdColonyAdSize(size.getWidth(), size.getHeight());
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
                layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(context, size.getWidth()), AdapterUtils.dpToPixels(context, size.getHeight()));
                break;
        }

        // set gravity
        layoutParams.gravity = Gravity.CENTER;

        return layoutParams;
    }

    //endregion


}