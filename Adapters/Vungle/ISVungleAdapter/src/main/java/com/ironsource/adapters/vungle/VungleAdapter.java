package com.ironsource.adapters.vungle;

import static com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN;

import android.content.Context;
import android.text.TextUtils;

import com.ironsource.environment.ContextProvider;
import com.ironsource.mediationsdk.AbstractAdapter;
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
import com.vungle.ads.AdConfig;
import com.vungle.ads.BannerAdSize;
import com.vungle.ads.VungleAds;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class VungleAdapter extends AbstractAdapter {

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

    private VungleRewardedAdapter rewardedAdapter;
    private VungleInterstitialAdapter interstitialAdapter;
    private VungleBannerAdapter bannerAdapter;

    private static String mAdOrientation = null;

    //region Adapter Methods
    public static VungleAdapter startAdapter(String providerName) {
        return new VungleAdapter(providerName);
    }

    private VungleAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose("");
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
        return com.vungle.ads.BuildConfig.VERSION_NAME;
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

        VungleInitializer.getInstance().initialize(appId,
                ContextProvider.getInstance().getApplicationContext(),
                new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        listener.onRewardedVideoInitSuccess();
                    }

                    @Override
                    public void onInitializeError(String error) {
                        IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                        listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                    }
                });
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

        loadRewardedVideoInternal(appId, placementId, listener, null);
    }

    public void loadRewardedVideo(final JSONObject config, JSONObject adData, final RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        String appId = config.optString(APP_ID);

        loadRewardedVideoInternal(appId, placementId, listener, null);
    }

    @Override
    public void loadRewardedVideoForBidding(JSONObject config, JSONObject adData, String serverData, final RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        String appId = config.optString(APP_ID);

        IronLog.ADAPTER_API.verbose("placementId = " + placementId + ", appId = " + appId);
        loadRewardedVideoInternal(appId, placementId, listener, serverData);
    }

    private void loadRewardedVideoInternal(String appId, String placementId, RewardedVideoSmashListener listener, String serverData) {
        IronLog.ADAPTER_API.verbose("placementId = " + placementId + ", appId = " + appId);

        VungleInitializer.getInstance().initialize(appId,
                ContextProvider.getInstance().getApplicationContext(),
                new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        AdConfig adConfig = createAdConfig();
                        rewardedAdapter = new VungleRewardedAdapter(placementId, adConfig, listener);
                        rewardedAdapter.loadWithBid(serverData);
                    }

                    @Override
                    public void onInitializeError(String error) {
                        IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                        listener.onRewardedVideoAvailabilityChanged(false);
                    }
                });
    }

    @Override
    public void showRewardedVideo(JSONObject config, final RewardedVideoSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // if we can play
        if (rewardedAdapter != null) {
            // dynamic user id
            if (!TextUtils.isEmpty(getDynamicUserId())) {
                rewardedAdapter.setUserId(getDynamicUserId());
            }

            rewardedAdapter.play();
        } else {
            IronLog.INTERNAL.error("There is no ad available for placementId = " + placementId);
            listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
        }
    }

    @Override
    public boolean isRewardedVideoAvailable(JSONObject config) {
        String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        return rewardedAdapter != null && rewardedAdapter.canPlayAd();
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

        VungleInitializer.getInstance().initialize(appId,
                ContextProvider.getInstance().getApplicationContext(),
                new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        listener.onInterstitialInitSuccess();
                    }

                    @Override
                    public void onInitializeError(String error) {
                        IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                        listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT));
                    }
                });
    }

    @Override
    public void loadInterstitialForBidding(JSONObject config, JSONObject adData, String serverData, InterstitialSmashListener listener) {
        final String placementId = config.optString(PLACEMENT_ID);
        final String appId = config.optString(APP_ID);

        loadInterstitialInternal(appId, placementId, listener, serverData);
    }

    @Override
    public void loadInterstitial(JSONObject config, JSONObject adData, final InterstitialSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        String appId = config.optString(APP_ID);

        loadInterstitialInternal(appId, placementId, listener, null);
    }

    private void loadInterstitialInternal(final String appId, final String placementId, final InterstitialSmashListener listener, String serverData) {
        IronLog.ADAPTER_API.verbose("placementId = " + placementId + ", appId = " + appId);
        VungleInitializer.getInstance().initialize(appId,
                ContextProvider.getInstance().getApplicationContext(),
                new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        AdConfig adConfig = createAdConfig();
                        interstitialAdapter = new VungleInterstitialAdapter(placementId, adConfig, listener);
                        interstitialAdapter.loadWithBid(serverData);
                    }

                    @Override
                    public void onInitializeError(String error) {
                        IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                        listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT));
                    }
                });
    }

    @Override
    public void showInterstitial(JSONObject config, final InterstitialSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);

        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // if we can play
        if (interstitialAdapter != null) {
            interstitialAdapter.play();
        } else {
            IronLog.INTERNAL.error("There is no ad available for placementId = " + placementId);
            listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }
    }

    @Override
    public boolean isInterstitialReady(JSONObject config) {
        return interstitialAdapter != null && interstitialAdapter.canPlayAd();
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

        VungleInitializer.getInstance().initialize(appId,
                ContextProvider.getInstance().getApplicationContext(),
                new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        listener.onBannerInitSuccess();
                    }

                    @Override
                    public void onInitializeError(String error) {
                        IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                        listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.BANNER_AD_UNIT));
                    }
                });
    }

    @Override
    public void loadBannerForBidding(JSONObject config, JSONObject adData, String serverData, IronSourceBannerLayout banner, BannerSmashListener listener) {
        final String placementId = config.optString(PLACEMENT_ID);
        final String appId = config.optString(APP_ID);
        loadBannerInternal(appId, placementId, banner, listener, serverData);
    }

    @Override
    public void loadBanner(JSONObject config, JSONObject adData, final IronSourceBannerLayout banner, final BannerSmashListener listener) {
        String placementId = config.optString(PLACEMENT_ID);
        String appId = config.optString(APP_ID);

        loadBannerInternal(appId, placementId, banner, listener, null);
    }

    private void loadBannerInternal(final String appId, final String placementId, final IronSourceBannerLayout banner,
                                    final BannerSmashListener listener, final String serverData) {
        IronLog.ADAPTER_API.verbose("placementId = " + placementId + ", appId = " + appId);

        // verify size
        ISBannerSize isBannerSize = banner.getSize();
        BannerAdSize loBannerSize = VungleBannerAdapter.getBannerSize(isBannerSize);
        IronLog.ADAPTER_API.verbose("bannerSize = " + loBannerSize);
        if (loBannerSize == null) {
            IronLog.ADAPTER_API.verbose("size not supported, size = " + banner.getSize().getDescription());
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize(getProviderName()));
            return;
        }

        VungleInitializer.getInstance().initialize(appId,
                ContextProvider.getInstance().getApplicationContext(),
                new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        bannerAdapter = new VungleBannerAdapter(placementId, isBannerSize, loBannerSize, listener);
                        bannerAdapter.loadWithBid(serverData);
                    }

                    @Override
                    public void onInitializeError(String error) {
                        IronLog.INTERNAL.verbose("init failed - placementId = " + placementId);
                        listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.BANNER_AD_UNIT));
                    }
                });

    }

    @Override
    public void destroyBanner(JSONObject config) {
        final String placementId = config.optString(PLACEMENT_ID);
        IronLog.ADAPTER_API.verbose("placementId = " + placementId);

        // run on main thread
        postOnUIThread(() -> {
            if (bannerAdapter != null) {
                bannerAdapter.destroy();
                bannerAdapter = null;
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
            if (rewardedAdapter != null) {
                rewardedAdapter.destroy();
                rewardedAdapter = null;
            }
        } else if (adUnit == IronSource.AD_UNIT.INTERSTITIAL) {
            if (interstitialAdapter != null) {
                interstitialAdapter.destroy();
                interstitialAdapter = null;
            }
        } else if (adUnit == IronSource.AD_UNIT.BANNER) {
            if (bannerAdapter != null) {
                bannerAdapter.destroy();
                bannerAdapter = null;
            }
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

        VungleConsent.setGDPRStatus(consent, CONSENT_MESSAGE_VERSION);
    }

    protected void setMetaData(String key, List<String> values) {
        if (values.isEmpty()) {
            return;
        }

        // this is a list of 1 value
        String value = values.get(0);
        IronLog.ADAPTER_API.verbose("key = " + key + ", value = " + value);

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            boolean ccpa = MetaDataUtils.getMetaDataBooleanValue(value);
            VungleConsent.setCCPAValue(ccpa);
        } else if (MetaDataUtils.isValidMetaData(key, ORIENTATION_FLAG, value)) {
            mAdOrientation = value;
        } else {
            String formattedValue = MetaDataUtils.formatValueForType(value, META_DATA_VALUE_BOOLEAN);

            if (MetaDataUtils.isValidMetaData(key, VUNGLE_COPPA_FLAG, formattedValue)) {
                boolean isUserCoppa = MetaDataUtils.getMetaDataBooleanValue(formattedValue);
                VungleConsent.setCOPPAValue(isUserCoppa);
            }
        }
    }
    //endregion

    //region Helpers
    private Map<String, Object> getBiddingData() {
        String bidderToken = VungleAds.getBiddingToken(ContextProvider.getInstance().getApplicationContext());
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
    //endregion

}
