package com.ironsource.adapters.adcolony;

import android.app.Activity;
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

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/*
britt.mullen from AdColony said on 15/3/2019
"none of those must be invoked from the UI thread"

Android
AdColony.setAppOptions()
AdColony.getAppOptions()
AdColony.configure()
AdColony.setRewardListener()
AdColony.requestInterstitial()
AdColonyInterstitial.isExpired()
AdColonyInterstitial.show()

iOS
[AdColony setAppOptions:
[AdColony configureWithAppID:
[AdColony requestInterstitialInZone:
[AdColonyZone setReward:
AdColonyInterstitial.expired
[interstititalAd showWithPresentingViewController:
 */

class AdColonyAdapter extends AbstractAdapter {
    private static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String GitHash = BuildConfig.GitHash;

    private static AtomicBoolean mAlreadyInitiated = new AtomicBoolean(false);
    private static AdColonyAppOptions mAdColonyOptions = new AdColonyAppOptions();

    private static final String META_DATA_ADCOLONY_COPPA = "AdColony_COPPA";
    private static final String META_DATA_ADCOLONY_CHILD_DIRECTED = "AdColony_App_Child_Directed";

    private final String APP_ID = "appID";
    private final String ZONE_ID = "zoneId";
    private final String ALL_ZONE_IDS = "zoneIds";
    private final String ADM = "adm";
    private final String MEDIATION_NAME = "ironSource";

    // Rewarded Video
    private ConcurrentHashMap<String, RewardedVideoSmashListener> mZoneIdToRewardedVideoSmashListener; // Rewarded smash listener
    private ConcurrentHashMap<String, AdColonyRewardedVideoAdListener> mZoneIdToRewardedVideoListener;
    protected ConcurrentHashMap<String, AdColonyInterstitial> mZoneIdToRewardedVideoAdObject;

    // Interstitial
    private ConcurrentHashMap<String, InterstitialSmashListener> mZoneIdToInterstitialSmashListener;
    private ConcurrentHashMap<String, AdColonyInterstitialAdListener> mZoneIdToInterstitialListener; // Interstitial smash listener
    protected ConcurrentHashMap<String, AdColonyInterstitial> mZoneIdToInterstitialAdObject;

    // Banner
    private ConcurrentHashMap<String, BannerSmashListener> mZoneIdToBannerSmashListener; // Banner smash listener
    private ConcurrentHashMap<String, AdColonyBannerAdListener> mZoneIdToBannerListener;
    protected ConcurrentHashMap<String, AdColonyAdView> mZoneIdToBannerAdView; // used for banner
    protected ConcurrentHashMap<String, IronSourceBannerLayout> mZoneIdToBannerLayout;

    public static AdColonyAdapter startAdapter(String providerName) {
        return new AdColonyAdapter(providerName);
    }

    private AdColonyAdapter(String providerName) {
        super(providerName);

        mZoneIdToRewardedVideoSmashListener = new ConcurrentHashMap<>();
        mZoneIdToRewardedVideoListener = new ConcurrentHashMap<>();
        mZoneIdToRewardedVideoAdObject = new ConcurrentHashMap<>();

        mZoneIdToInterstitialSmashListener = new ConcurrentHashMap<>();
        mZoneIdToInterstitialListener = new ConcurrentHashMap<>();
        mZoneIdToInterstitialAdObject = new ConcurrentHashMap<>();

        mZoneIdToBannerSmashListener = new ConcurrentHashMap<>();
        mZoneIdToBannerListener = new ConcurrentHashMap<>();
        mZoneIdToBannerLayout = new ConcurrentHashMap<>();
        mZoneIdToBannerAdView = new ConcurrentHashMap<>();

        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_NETWORK;
    }

    public static IntegrationData getIntegrationData(Activity activity) {
        IntegrationData ret = new IntegrationData("AdColony", VERSION);
        ret.activities = new String[]{
                "com.adcolony.sdk.AdColonyInterstitialActivity",
                "com.adcolony.sdk.AdColonyAdViewActivity"
        };
        return ret;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getCoreSDKVersion() {
        return AdColony.getSDKVersion();
    }


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
        } else if (isCOPPAMetaData(key)) {
            String formattedValue = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN);

            if (formattedValue.length() > 0) {
                setCOPPAValue(formattedValue);
            }
        } else if (isChildDirectedMetaData(key)) {
            String formattedValue = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN);

            if (formattedValue.length() > 0) {
                setChildDirectedValue(formattedValue);
            }
        }
    }

    private boolean isCOPPAMetaData(String key) {
        return (key.equalsIgnoreCase(META_DATA_ADCOLONY_COPPA));
    }

    private boolean isChildDirectedMetaData(String key) {
        return (key.equalsIgnoreCase(META_DATA_ADCOLONY_CHILD_DIRECTED));
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
        AdColony.setAppOptions(mAdColonyOptions);
    }

    // ********** Init **********

    private void init(String userId, String appId, String allZoneIds) {
        if (mAlreadyInitiated.compareAndSet(false, true)) {
            IronLog.ADAPTER_API.verbose("appId = " + appId);

            if (!TextUtils.isEmpty(userId)) {
                IronLog.ADAPTER_API.verbose("setUserID to " + userId);
                mAdColonyOptions.setUserID(userId);
            }

            mAdColonyOptions.setMediationNetwork(MEDIATION_NAME, VERSION);
            final String[] allZoneIdsArray = allZoneIds.split(",");
            AdColony.configure(ContextProvider.getInstance().getCurrentActiveActivity().getApplication(), mAdColonyOptions, appId, allZoneIdsArray);
        }
    }

    //region Rewarded Video API
    @Override
    public Map<String, Object> getRewardedVideoBiddingData(JSONObject config) {
        return getBiddingData();
    }

    @Override
    public void initRewardedVideoWithCallback(String appKey, String userId, JSONObject config, final RewardedVideoSmashListener listener) {
        IronLog.ADAPTER_API.verbose("");

        initRewardedVideoInternal(config, userId, true, listener, new ResultListener() {
            @Override
            public void onSuccess() {
                listener.onRewardedVideoInitSuccess();
            }

            @Override
            public void onFail(IronSourceError error) {
                listener.onRewardedVideoInitFailed(error);
            }
        });
    }

    @Override
    public void initAndLoadRewardedVideo(String appKey, String userId, final JSONObject config, final RewardedVideoSmashListener listener) {
        IronLog.ADAPTER_API.verbose("");

        initRewardedVideoInternal(config, userId, true, listener, new ResultListener() {
            @Override
            public void onSuccess() {
                String zoneId = config.optString(ZONE_ID);
                loadRewardedVideoInternal(zoneId);
            }

            @Override
            public void onFail(IronSourceError error) {
                listener.onRewardedVideoAvailabilityChanged(false);
            }
        });
    }

    private void initRewardedVideoInternal(final JSONObject config, final String userId, final boolean isBidder, final RewardedVideoSmashListener listener, final ResultListener initListener) {
        // validate input
        validateInitParams(config, IronSourceConstants.REWARDED_VIDEO_AD_UNIT, new ResultListener() {
            @Override
            public void onSuccess() {

                try {
                    final String appId = config.optString(APP_ID);
                    final String zoneId = config.optString(ZONE_ID);
                    final String allZoneIds = config.optString(ALL_ZONE_IDS);

                    AdColonyRewardedVideoAdListener rewardedVideoListener = new AdColonyRewardedVideoAdListener(AdColonyAdapter.this, listener, zoneId);
                    mZoneIdToRewardedVideoListener.put(zoneId, rewardedVideoListener);
                    mZoneIdToRewardedVideoSmashListener.put(zoneId, listener);

                    // init SDK
                    init(userId, appId, allZoneIds);

                    // call init listener
                    initListener.onSuccess();

                } catch (Exception ex) {
                    IronLog.INTERNAL.error("exception while trying to init rv " + ex);
                    initListener.onFail(ErrorBuilder.buildInitFailedError(ex.getMessage(), IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                }
            }

            @Override
            public void onFail(IronSourceError error) {
                initListener.onFail(error);
            }
        });
    }

    @Override
    public void loadRewardedVideoForBidding(JSONObject config, final RewardedVideoSmashListener listener, final String serverData) {
        final String zoneId = config.optString(ZONE_ID);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);
        AdColonyAdOptions adOptions = new AdColonyAdOptions().setOption(ADM, serverData);
        AdColonyRewardedVideoAdListener rewardedVideoListener = mZoneIdToRewardedVideoListener.get(zoneId);
        AdColony.requestInterstitial(zoneId, rewardedVideoListener, adOptions);
    }

    @Override
    public void fetchRewardedVideoForAutomaticLoad(JSONObject config, RewardedVideoSmashListener listener) {
        final String zoneId = config.optString(ZONE_ID);
        loadRewardedVideoInternal(zoneId);
    }

    private void loadRewardedVideoInternal(final String zoneId) {
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);
        AdColonyRewardedVideoAdListener rewardedVideoListener = mZoneIdToRewardedVideoListener.get(zoneId);
        AdColony.requestInterstitial(zoneId, rewardedVideoListener);
    }

    @Override
    public void showRewardedVideo(JSONObject config, RewardedVideoSmashListener listener) {
        try {
            final String zoneId = config.optString(ZONE_ID);
            AdColonyInterstitial rewardedVideoAd = mZoneIdToRewardedVideoAdObject.get(zoneId);

            if (rewardedVideoAd != null && !rewardedVideoAd.isExpired()) {
                IronLog.ADAPTER_API.verbose("show zoneId =" + zoneId);
                AdColonyRewardedVideoAdListener rewardedVideoListener = mZoneIdToRewardedVideoListener.get(zoneId);
                AdColony.setRewardListener(rewardedVideoListener);
                rewardedVideoAd.show();
            } else {
                IronLog.INTERNAL.error("ad is expired");
                listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            }
        } catch (Exception ex) {
            IronLog.INTERNAL.error("exception while trying to show ad " + ex);
            listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
        }

        listener.onRewardedVideoAvailabilityChanged(false);
    }

    @Override
    public boolean isRewardedVideoAvailable(JSONObject config) {
        try {
            String zoneId = config.optString(ZONE_ID);

            if (TextUtils.isEmpty(zoneId)) {
                return false;
            }

            if (mZoneIdToRewardedVideoAdObject.get(zoneId) == null) {
                return false;
            }

            boolean isRewardedVideoAvailable = !mZoneIdToRewardedVideoAdObject.get(zoneId).isExpired();
            IronLog.ADAPTER_API.verbose("isRewardedVideoAvailable=" + isRewardedVideoAvailable);
            return isRewardedVideoAvailable;
        } catch (Exception ex) {
            IronLog.INTERNAL.error("exception = " + ex);
            return false;
        }
    }
    //endregion

    //region Interstitial API
    @Override
    public Map<String, Object> getInterstitialBiddingData(JSONObject config) {
        return getBiddingData();
    }

    @Override
    public void initInterstitialForBidding(String appKey, String userId, JSONObject config, InterstitialSmashListener listener) {
        IronLog.ADAPTER_API.verbose("");
        initInterstitialInternal(appKey, userId, true, config, listener);
    }

    @Override
    public void initInterstitial(String appKey, final String userId, final JSONObject config, final InterstitialSmashListener listener) {
        IronLog.ADAPTER_API.verbose("");
        initInterstitialInternal(appKey, userId, false, config, listener);
    }

    private void initInterstitialInternal(String appKey, final String userId, final boolean isBidder, final JSONObject config, final InterstitialSmashListener listener) {
        // validate input
        validateInitParams(config, IronSourceConstants.INTERSTITIAL_AD_UNIT, new ResultListener() {
            @Override
            public void onSuccess() {
                try {
                    final String appId = config.optString(APP_ID);
                    final String zoneId = config.optString(ZONE_ID);
                    final String allZoneIds = config.optString(ALL_ZONE_IDS);

                    AdColonyInterstitialAdListener interstitialListener = new AdColonyInterstitialAdListener(AdColonyAdapter.this, listener, zoneId);
                    mZoneIdToInterstitialListener.put(zoneId, interstitialListener);
                    mZoneIdToInterstitialSmashListener.put(zoneId, listener);

                    // init SDK
                    init(userId, appId, allZoneIds);

                    // init success
                    listener.onInterstitialInitSuccess();

                } catch (Exception ex) {
                    IronLog.INTERNAL.error("exception while trying to init IS " + ex);
                    ex.printStackTrace();
                }
            }

            @Override
            public void onFail(IronSourceError error) {
                listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params", IronSourceConstants.INTERSTITIAL_AD_UNIT));
            }
        });
    }

    @Override
    public void loadInterstitialForBidding(final JSONObject config, final InterstitialSmashListener listener, final String serverData) {
        IronLog.ADAPTER_API.verbose("");
        loadInterstitialInternalForBidding(config, listener, serverData);
    }

    @Override
    public void loadInterstitial(JSONObject config, InterstitialSmashListener listener) {
        IronLog.ADAPTER_API.verbose("");
        loadInterstitialInternal(config, listener);
    }

    private void loadInterstitialInternal(final JSONObject config, final InterstitialSmashListener listener) {
        try {
            final String zoneId = config.optString(ZONE_ID);
            IronLog.ADAPTER_API.verbose("loading interstitial with zone id " + zoneId);
            AdColonyInterstitialAdListener interstitialListener = mZoneIdToInterstitialListener.get(zoneId);
            AdColony.requestInterstitial(zoneId, interstitialListener);
        } catch (Exception ex) {
            IronLog.INTERNAL.error("exception while trying to load IS ad " + ex);
            listener.onInterstitialAdLoadFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    private void loadInterstitialInternalForBidding(final JSONObject config, final InterstitialSmashListener listener, final String serverData) {
        try {
            final String zoneId = config.optString(ZONE_ID);
            IronLog.ADAPTER_API.verbose("loading interstitial with zone id " + zoneId);
            AdColonyAdOptions adOptions = new AdColonyAdOptions().setOption(ADM, serverData);
            AdColonyInterstitialAdListener interstitialListener = mZoneIdToInterstitialListener.get(zoneId);
            AdColony.requestInterstitial(zoneId, interstitialListener, adOptions);
        } catch (Exception ex) {
            IronLog.INTERNAL.error("exception while trying to load IS for bidding ad " + ex);
            listener.onInterstitialAdLoadFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    @Override
    public void showInterstitial(JSONObject config, InterstitialSmashListener listener) {
        try {
            final String zoneId = config.optString(ZONE_ID);
            AdColonyInterstitial interstitialAd = mZoneIdToInterstitialAdObject.get(zoneId);

            if (interstitialAd != null && !interstitialAd.isExpired()) {
                IronLog.ADAPTER_API.verbose("show zoneId =" + zoneId);
                interstitialAd.show();
            } else {
                IronLog.INTERNAL.error("ad is expired");
                listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
            }
        } catch (Exception ex) {
            IronLog.INTERNAL.error("exception while trying to show ad " + ex);
            ex.printStackTrace();
            listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    @Override
    public boolean isInterstitialReady(JSONObject config) {
        try {
            final String zoneId = config.optString(ZONE_ID);
            AdColonyInterstitial interstitialAd = mZoneIdToInterstitialAdObject.get(zoneId);
            return (interstitialAd != null) && !interstitialAd.isExpired();
        } catch (Exception ex) {
            IronLog.INTERNAL.error("exception = " + ex);
            return false;
        }
    }
    //endregion

    //region Banner
    @Override
    public void initBanners(String appKey, final String userId, final JSONObject config, final BannerSmashListener listener) {
        initBannersInternal(userId, config, listener);
    }

    @Override
    public void initBannerForBidding(String appKey, String userId, JSONObject config, BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose("");
        initBannersInternal(userId, config, listener);
    }

    private void initBannersInternal(final String userId, final JSONObject config, final BannerSmashListener listener) {
        validateInitParams(config, IronSourceConstants.BANNER_AD_UNIT, new ResultListener() {
            @Override
            public void onSuccess() {
                try {
                    final String appId = config.optString(APP_ID);
                    final String zoneId = config.optString(ZONE_ID);
                    final String allZoneIds = config.optString(ALL_ZONE_IDS);

                    mZoneIdToBannerSmashListener.put(zoneId, listener);

                    // init SDK
                    init(userId, appId, allZoneIds);

                    // call init success
                    listener.onBannerInitSuccess();
                } catch (Exception ex) {
                    IronLog.INTERNAL.error("exception while trying to init banner " + ex);
                    ex.printStackTrace();
                }
            }

            @Override
            public void onFail(IronSourceError error) {
                listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("Missing params", IronSourceConstants.BANNER_AD_UNIT));
            }
        });
    }

    @Override
    public void loadBanner(final IronSourceBannerLayout banner, final JSONObject config, final BannerSmashListener listener) {
        IronLog.ADAPTER_API.verbose("");
        loadBannerInternal(banner, config, listener, null);
    }

    @Override
    public void loadBannerForBidding(IronSourceBannerLayout banner, JSONObject config, BannerSmashListener listener, String serverData) {
        IronLog.ADAPTER_API.verbose("");
        AdColonyAdOptions adOptions = new AdColonyAdOptions().setOption(ADM, serverData);
        loadBannerInternal(banner, config, listener, adOptions);
    }

    private void loadBannerInternal(IronSourceBannerLayout banner, JSONObject config, BannerSmashListener listener, AdColonyAdOptions adOptions) {
        try {
            final String zoneId = config.optString(ZONE_ID);

            if (TextUtils.isEmpty(zoneId)) {
                IronLog.INTERNAL.error("error - missing param zoneId");
                listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(IronSourceConstants.BANNER_AD_UNIT, getProviderName(), "missing param = " + zoneId));
                return;
            }

            // verify size
            if (!isBannerSizeSupported(banner.getSize())) {
                IronLog.INTERNAL.error("loadBanner - size not supported, size = " + banner.getSize().getDescription());
                listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getProviderName()));
                return;
            }

            // validate banner layout
            if (banner == null || banner.getSize() == null) {
                IronLog.INTERNAL.error("banner layout is null");
                listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(IronSourceConstants.BANNER_AD_UNIT, getProviderName(), "banner layout is null"));
                return;
            }

            // add smash listener
            mZoneIdToBannerSmashListener.put(zoneId, listener);

            // add banner layout
            mZoneIdToBannerLayout.put(zoneId, banner);

            // get size
            AdColonyAdSize bannerSize = getBannerSize(banner.getSize());

            FrameLayout.LayoutParams layoutParams = getBannerLayoutParams(banner.getSize());

            AdColonyBannerAdListener bannerListener = new AdColonyBannerAdListener(AdColonyAdapter.this, listener, zoneId, layoutParams);
            mZoneIdToBannerListener.put(zoneId, bannerListener);

            // request banner from AdColony
            IronLog.ADAPTER_API.verbose("loading banner with zone id " + zoneId);
            AdColony.requestAdView(zoneId, bannerListener, bannerSize, adOptions);
        } catch (Exception ex) {
            IronLog.INTERNAL.error("exception while trying to load banner ad " + ex);
            ex.printStackTrace();
        }
    }

    //region Banner API
    @Override
    public Map<String, Object> getBannerBiddingData(JSONObject config) {
        return getBiddingData();
    }

    @Override
    public void reloadBanner(IronSourceBannerLayout banner, JSONObject config, BannerSmashListener listener) {
        // placement id
        final String zoneId = config.optString(ZONE_ID);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        listener = mZoneIdToBannerSmashListener.get(zoneId);
        if (listener != null) {

            banner = mZoneIdToBannerLayout.get(zoneId);
            if (banner == null || banner.getSize() == null) {
                IronLog.INTERNAL.error("error - missing data banner layout for zoneId = " + zoneId);
                listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(IronSourceConstants.BANNER_AD_UNIT, getProviderName(), "missing param = " + zoneId));
                return;
            }

            // call load
            loadBanner(banner, config, listener);

        } else {
            IronLog.INTERNAL.error("error - missing listener for zoneId = " + zoneId);
        }
    }

    @Override
    public void destroyBanner(final JSONObject config) {
        // placement id
        final String zoneId = config.optString(ZONE_ID);
        IronLog.ADAPTER_API.verbose("zoneId = " + zoneId);

        // remove from maps
        mZoneIdToBannerSmashListener.remove(zoneId);
        mZoneIdToBannerListener.remove(zoneId);
        mZoneIdToBannerLayout.remove(zoneId);
        AdColonyAdView bannerView = mZoneIdToBannerAdView.get(zoneId);

        if (bannerView != null) {
            bannerView.destroy();
            mZoneIdToBannerAdView.remove(zoneId);
        }
    }

    @Override
    public boolean shouldBindBannerViewOnReload() {
        return true;
    }
    //endregion

    //region Helper Methods

    private void validateInitParams(JSONObject config, String adUnit, ResultListener initListener) {

        final String appId = config.optString(APP_ID);
        final String zoneId = config.optString(ZONE_ID);
        final String allZoneIds = config.optString(ALL_ZONE_IDS);

        if (TextUtils.isEmpty(appId)) {
            IronLog.INTERNAL.error("error - missing param = " + APP_ID);
            initListener.onFail(ErrorBuilder.buildInitFailedError("missing param = " + APP_ID, adUnit));
            return;
        }

        if (TextUtils.isEmpty(zoneId)) {
            IronLog.INTERNAL.error("error - missing param = " + ZONE_ID);
            initListener.onFail(ErrorBuilder.buildInitFailedError("missing param = " + ZONE_ID, adUnit));
            return;
        }

        if (TextUtils.isEmpty(allZoneIds)) {
            IronLog.INTERNAL.error("error - missing param = " + ALL_ZONE_IDS);
            initListener.onFail(ErrorBuilder.buildInitFailedError("missing param = " + ALL_ZONE_IDS, adUnit));
            return;
        }

        // send success
        initListener.onSuccess();
    }

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
                return AdapterUtils.isLargeScreen(ContextProvider.getInstance().getCurrentActiveActivity()) ? AdColonyAdSize.LEADERBOARD : AdColonyAdSize.BANNER;
            case "CUSTOM":
                return new AdColonyAdSize(size.getWidth(), size.getHeight());
        }

        return null;
    }

    private FrameLayout.LayoutParams getBannerLayoutParams(ISBannerSize size) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(0, 0);

        Activity activity = ContextProvider.getInstance().getCurrentActiveActivity();

        switch (size.getDescription()) {
            case "BANNER":
            case "LARGE":
                layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 320), AdapterUtils.dpToPixels(activity, 50));
                break;
            case "RECTANGLE":
                layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 300), AdapterUtils.dpToPixels(activity, 250));
                break;
            case "SMART":
                if (AdapterUtils.isLargeScreen(activity)) {
                    layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 728), AdapterUtils.dpToPixels(activity, 90));
                } else {
                    layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, 320), AdapterUtils.dpToPixels(activity, 50));
                }
                break;
            case "CUSTOM":
                layoutParams = new FrameLayout.LayoutParams(AdapterUtils.dpToPixels(activity, size.getWidth()), AdapterUtils.dpToPixels(activity, size.getHeight()));
                break;
        }

        // set gravity
        layoutParams.gravity = Gravity.CENTER;

        return layoutParams;
    }
    //endregion


    private interface ResultListener {
        void onSuccess();
        void onFail(IronSourceError error);
    }
}