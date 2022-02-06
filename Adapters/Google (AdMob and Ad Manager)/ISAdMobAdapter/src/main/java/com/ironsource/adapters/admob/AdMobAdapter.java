package com.ironsource.adapters.admob;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;


import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.ironsource.environment.ContextProvider;
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

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;
import static com.google.android.gms.ads.mediation.MediationAdConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
import static com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN;

/**
 * Created by pnina.r on 4/12/16.
 */
public class AdMobAdapter extends AbstractAdapter implements INetworkInitCallbackListener {

    private static final String CORE_SDK_VERSION = "20.5.0";

    public static int RV_NOT_READY_ERROR_CODE = 101;
    public static int BN_FAILED_TO_RELOAD_ERROR_CODE = 103;
    public static int IS_NOT_READY_ERROR_CODE = 104;

    private static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String GitHash = BuildConfig.GitHash;
    private final String IRONSOURCE_REQUEST_AGENT = "ironSource";

    // Rewarded video
    private ConcurrentHashMap<String, RewardedVideoSmashListener> mAdUnitIdToRewardedVideoListener;
    private CopyOnWriteArraySet<String> mRewardedVideoPlacementsForInitCallbacks;
    public ConcurrentHashMap<String, RewardedAd> mAdIdToRewardedVideoAd;
    public ConcurrentHashMap<String, Boolean> mRewardedVideoAdsAvailability;

    // Interstitial
    private ConcurrentHashMap<String, InterstitialSmashListener> mAdUnitIdToInterstitialListener;
    public ConcurrentHashMap<String, InterstitialAd> mAdIdToInterstitialAd;
    public ConcurrentHashMap<String, Boolean> mInterstitialAdsAvailability;

    // Banner
    private ConcurrentHashMap<String, BannerSmashListener> mAdUnitIdToBannerListener;
    private ConcurrentHashMap<String, AdView> mAdIdToBannerAd;

    private enum InitState {
        INIT_STATE_NONE,
        INIT_STATE_IN_PROGRESS,
        INIT_STATE_SUCCESS,
        INIT_STATE_FAILED
    }

    // Meta data max rating values
    private interface AdMobMaxContentRating {
        String ADMOB_MAX_AD_CONTENT_RATING_G = "max_ad_content_rating_g";
        String ADMOB_MAX_AD_CONTENT_RATING_PG = "max_ad_content_rating_pg";
        String ADMOB_MAX_AD_CONTENT_RATING_T = "max_ad_content_rating_t";
        String ADMOB_MAX_AD_CONTENT_RATING_MA = "max_ad_content_rating_ma";
    }

    // Meta data flags
    private interface AdMobMetaDataFlags {
        String ADMOB_TFCD_KEY = "admob_tfcd";
        String ADMOB_TFUA_KEY = "admob_tfua";
        String ADMOB_MAX_RATING_KEY = "admob_maxcontentrating";
    }

    private final String AD_UNIT_ID = "adUnitId";
    private final String NETWORK_ONLY_INIT = "networkOnlyInit";
    private final String INIT_RESPONSE_REQUIRED = "initResponseRequired";

    // shared variables between instances
    private static Boolean mConsent = null;
    private static Boolean mCCPAValue = null;
    private static Integer mCoppaValue = null;
    private static Integer mEuValue = null;
    private static String mRatingValue = "";

    private static AtomicBoolean mWasInitCalled = new AtomicBoolean(false);
    // handle init callback for all adapter instances
    private static HashSet<INetworkInitCallbackListener> initCallbackListeners = new HashSet<>();
    private static InitState mInitState = InitState.INIT_STATE_NONE;

    //region Adapter Methods
    public static AdMobAdapter startAdapter(String providerName) {

        return new AdMobAdapter(providerName);
    }

    private AdMobAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose("");

        mAdUnitIdToRewardedVideoListener = new ConcurrentHashMap<>();
        mAdIdToRewardedVideoAd = new ConcurrentHashMap<>();
        mRewardedVideoAdsAvailability = new ConcurrentHashMap<>();
        mRewardedVideoPlacementsForInitCallbacks = new CopyOnWriteArraySet<>();

        mAdIdToInterstitialAd = new ConcurrentHashMap<>();
        mInterstitialAdsAvailability = new ConcurrentHashMap<>();
        mAdUnitIdToInterstitialListener = new ConcurrentHashMap<>();

        mAdIdToBannerAd = new ConcurrentHashMap<>();
        mAdUnitIdToBannerListener = new ConcurrentHashMap<>();

        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE;
    }

    public static IntegrationData getIntegrationData(Activity activity) {
        IntegrationData ret = new IntegrationData("AdMob", VERSION);
        ret.activities = new String[]{"com.google.android.gms.ads.AdActivity"};
        return ret;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getCoreSDKVersion() {
        return CORE_SDK_VERSION;
    }
    //endregion

    // All calls to MobileAds must be on the main thread --> run all calls to initSDK in a thread.
    private void initSDK(final JSONObject config) {
        // add self to init delegates only when init not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this);
        }

        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS;

            IronLog.ADAPTER_API.verbose("");
            boolean networkOnlyInit = config.optBoolean(NETWORK_ONLY_INIT, true);

            if (networkOnlyInit) {
                IronLog.ADAPTER_API.verbose("disableMediationAdapterInitialization");
                MobileAds.disableMediationAdapterInitialization(ContextProvider.getInstance().getCurrentActiveActivity());
            }

            boolean shouldWaitForInitCallback = config.optBoolean(INIT_RESPONSE_REQUIRED, false);

            if (shouldWaitForInitCallback) {
                IronLog.ADAPTER_API.verbose("init and wait for callback");

                MobileAds.initialize(ContextProvider.getInstance().getApplicationContext(), new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(@NotNull InitializationStatus initializationStatus) {
                        AdapterStatus status = initializationStatus.getAdapterStatusMap().get("com.google.android.gms.ads.MobileAds");
                        AdapterStatus.State state = status != null ? status.getInitializationState() : null;

                        if (state == AdapterStatus.State.READY) {
                            IronLog.ADAPTER_API.verbose("initializationStatus = READY");
                            initializationSuccess();
                        } else {
                            IronLog.ADAPTER_API.verbose("initializationStatus = NOT READY");
                            initializationFailure();
                        }
                    }
                });
            } else {
                IronLog.ADAPTER_API.verbose("init without callback");
                MobileAds.initialize(ContextProvider.getInstance().getApplicationContext());
                initializationSuccess();
            }
        }
    }

    //region Rewarded Video API

    @Override
    public void initRewardedVideoWithCallback(String appKey, String userId, final JSONObject config, final RewardedVideoSmashListener listener) {

        final String adUnitId = config.optString(AD_UNIT_ID);
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        if (TextUtils.isEmpty(adUnitId)) {
            IronLog.INTERNAL.error("adUnitId is empty");
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("missing credentials - " + AD_UNIT_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }
        mAdUnitIdToRewardedVideoListener.put(adUnitId, listener);
        mRewardedVideoPlacementsForInitCallbacks.add(adUnitId);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {

                if (mInitState == InitState.INIT_STATE_SUCCESS) {
                    listener.onRewardedVideoInitSuccess();
                } else if (mInitState == InitState.INIT_STATE_FAILED) {
                    IronLog.INTERNAL.verbose("init failed - placementName = " + adUnitId);
                    listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Sdk failed to initiate", IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                } else {
                    initSDK(config);
                }
            }
        });
    }

    @Override
    public void initAndLoadRewardedVideo(String appKey, String userId, final JSONObject config, final RewardedVideoSmashListener listener) {

        final String adUnitId = config.optString(AD_UNIT_ID);
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        if (TextUtils.isEmpty(adUnitId)) {
            IronLog.INTERNAL.error("adUnitId is empty");
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }
        mAdUnitIdToRewardedVideoListener.put(adUnitId, listener);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {

                if (mInitState == InitState.INIT_STATE_SUCCESS) {
                    IronLog.INTERNAL.verbose("loadVideo - placementName = " + adUnitId);
                    loadRewardedVideoAdFromAdMob(adUnitId, listener);
                } else if (mInitState == InitState.INIT_STATE_FAILED) {
                    IronLog.INTERNAL.verbose("onRewardedVideoAvailabilityChanged(false) - placementName = " + adUnitId);
                    listener.onRewardedVideoAvailabilityChanged(false);
                } else {
                    initSDK(config);
                }
            }
        });
    }

    @Override
    public void fetchRewardedVideoForAutomaticLoad(final JSONObject config, final RewardedVideoSmashListener listener) {
        IronLog.ADAPTER_API.verbose("");
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                loadRewardedVideoAdFromAdMob(config.optString(AD_UNIT_ID), listener);
            }
        });
    }

    @Override
    public boolean isRewardedVideoAvailable(JSONObject config) {
        String adUnitId = config.optString(AD_UNIT_ID);
        return isRewardedVideoAvailableForAdUnitId(adUnitId);
    }

    private boolean isRewardedVideoAvailableForAdUnitId(String adUnitId) {
        return mRewardedVideoAdsAvailability.containsKey(adUnitId) && mRewardedVideoAdsAvailability.get(adUnitId);
    }

    @Override
    public void showRewardedVideo(final JSONObject config, final RewardedVideoSmashListener listener) {
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                final String adUnitId = config.optString(AD_UNIT_ID);
                RewardedAd rewardedAd = mAdIdToRewardedVideoAd.get(adUnitId);
                IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
                if (rewardedAd != null && isRewardedVideoAvailableForAdUnitId(adUnitId)) {
                    AdMobRewardedVideoAdShowListener adMobRewardedVideoAdShowListener = new AdMobRewardedVideoAdShowListener(AdMobAdapter.this, adUnitId, listener);
                    rewardedAd.setFullScreenContentCallback(adMobRewardedVideoAdShowListener);
                    rewardedAd.show(ContextProvider.getInstance().getCurrentActiveActivity(), adMobRewardedVideoAdShowListener);
                } else {
                    listener.onRewardedVideoAdShowFailed((new IronSourceError(RV_NOT_READY_ERROR_CODE, getProviderName() + "showRewardedVideo rv not ready " + adUnitId)));
                }
                mRewardedVideoAdsAvailability.put(adUnitId, false);
                listener.onRewardedVideoAvailabilityChanged(false);
            }
        });
    }


    // loadRewardedVideoAdFromAdMob is private and is being called from other methods that run on the main thread. No need to create a new UIthread to run loadAd.
    private void loadRewardedVideoAdFromAdMob(final String adUnitId, RewardedVideoSmashListener listener) {
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        mRewardedVideoAdsAvailability.put(adUnitId, false);
        AdRequest adRequest = createAdRequest();
        AdMobRewardedVideoAdLoadListener adMobRewardedVideoAdLoadListener = new AdMobRewardedVideoAdLoadListener(AdMobAdapter.this, adUnitId, listener);
        RewardedAd.load(ContextProvider.getInstance().getApplicationContext(), adUnitId, adRequest, adMobRewardedVideoAdLoadListener);
    }
    //endregion

    //region Interstitial API
    @Override
    public void initInterstitial(String appKey, String userId, final JSONObject config, final InterstitialSmashListener listener) {

        final String adUnitId = config.optString(AD_UNIT_ID);

        if (TextUtils.isEmpty(adUnitId)) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params: 'adUnitId' ", IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                mAdUnitIdToInterstitialListener.put(adUnitId, listener);
                if (mInitState == InitState.INIT_STATE_SUCCESS) {
                    IronLog.INTERNAL.verbose("onInterstitialInitSuccess - placementName = " + adUnitId);
                    listener.onInterstitialInitSuccess();
                } else if (mInitState == InitState.INIT_STATE_FAILED) {
                    IronLog.INTERNAL.verbose("onInterstitialInitFailed - placementName = " + adUnitId);
                    listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("AdMob sdk init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT));
                } else {
                    initSDK(config);
                }
            }
        });
    }


    @Override
    public void loadInterstitial(final JSONObject config, final InterstitialSmashListener listener) {
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                final String adUnitId = config.optString(AD_UNIT_ID);
                IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
                mInterstitialAdsAvailability.put(adUnitId, false);
                AdRequest adRequest = createAdRequest();
                AdMobInterstitialAdLoadListener adMobInterstitialAdLoadListener = new AdMobInterstitialAdLoadListener(AdMobAdapter.this, adUnitId, listener);
                InterstitialAd.load(ContextProvider.getInstance().getApplicationContext(), adUnitId, adRequest, adMobInterstitialAdLoadListener);
            }
        });
    }

    @Override
    public void showInterstitial(final JSONObject config, final InterstitialSmashListener listener) {
        // Show the ad if it's ready. Otherwise toast and restart the game.
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                String adUnitId = config.optString(AD_UNIT_ID);
                InterstitialAd interstitialAd = getInterstitialAd(adUnitId);
                IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
                if (interstitialAd != null && isInterstitialReadyForAdUnitId(adUnitId)) {
                    IronLog.ADAPTER_API.verbose("show");
                    AdMobInterstitialAdShowListener adMobInterstitialAdShowListener = new AdMobInterstitialAdShowListener(AdMobAdapter.this, listener, adUnitId);
                    interstitialAd.setFullScreenContentCallback(adMobInterstitialAdShowListener);
                    interstitialAd.show(ContextProvider.getInstance().getCurrentActiveActivity());
                } else {
                    listener.onInterstitialAdShowFailed((new IronSourceError(IS_NOT_READY_ERROR_CODE, getProviderName() + "showInterstitial is not ready " + adUnitId)));
                }
                mInterstitialAdsAvailability.put(adUnitId, false);


            }
        });
    }

    @Override
    public final boolean isInterstitialReady(final JSONObject config) {
        String adUnitId = config.optString(AD_UNIT_ID);
        return isInterstitialReadyForAdUnitId(adUnitId);
    }

    private boolean isInterstitialReadyForAdUnitId(final String adUnitId) {
        if (mInterstitialAdsAvailability.get(adUnitId) != null) {
            return mInterstitialAdsAvailability.get(adUnitId);
        }
        return false;
    }
    //endregion

    //region Banner API
    @Override
    public void initBanners(String appKey, String userId, final JSONObject config, final BannerSmashListener listener) {

        final String adUnitId = config.optString(AD_UNIT_ID);
        if (TextUtils.isEmpty(adUnitId)) {
            IronSourceError error = ErrorBuilder.buildInitFailedError("AdMobAdapter loadBanner adUnitId is empty", IronSourceConstants.BANNER_AD_UNIT);
            listener.onBannerInitFailed(error);
            return;
        }
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                mAdUnitIdToBannerListener.put(adUnitId, listener);

                if (mInitState == InitState.INIT_STATE_SUCCESS) {
                    IronLog.INTERNAL.verbose("onBannerInitSuccess - placementName = " + adUnitId);
                    listener.onBannerInitSuccess();
                } else if (mInitState == InitState.INIT_STATE_FAILED) {
                    IronLog.INTERNAL.verbose("onBannerInitFailed - placementName = " + adUnitId);
                    listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("AdMob sdk init failed", IronSourceConstants.BANNER_AD_UNIT));
                } else {
                    initSDK(config);
                }
            }
        });
    }

    @Override
    public void loadBanner(final IronSourceBannerLayout banner, final JSONObject config, final BannerSmashListener listener) {

        if (banner == null) {
            IronLog.ADAPTER_API.error("banner == null");
            return;
        }

        final String adUnitId = config.optString(AD_UNIT_ID);

        final AdSize size = getAdSize(banner.getSize(), AdapterUtils.isLargeScreen(banner.getActivity()));
        if (size == null) {
            listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize("AdMob"));
            return;
        }

        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    AdView adView = new AdView(banner.getActivity());
                    adView.setAdSize(size);
                    adView.setAdUnitId(adUnitId);
                    AdMobBannerAdListener adMobBannerAdListener = new AdMobBannerAdListener(AdMobAdapter.this, listener, adUnitId, adView);
                    adView.setAdListener(adMobBannerAdListener);
                    mAdIdToBannerAd.put(adUnitId, adView);

                    AdRequest adRequest = createAdRequest();
                    IronLog.ADAPTER_API.verbose("loadAd");
                    adView.loadAd(adRequest);

                } catch (Exception e) {
                    IronSourceError error = ErrorBuilder.buildLoadFailedError("AdMobAdapter loadBanner exception " + e.getMessage());
                    listener.onBannerAdLoadFailed(error);
                }
            }
        });
    }

    @Override
    public void reloadBanner(final IronSourceBannerLayout banner, final JSONObject config, final BannerSmashListener listener) {
        final String adUnitId = config.optString(AD_UNIT_ID);
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (mAdIdToBannerAd.get(config.optString(AD_UNIT_ID)) != null) {
                    AdRequest adRequest = createAdRequest();
                    AdView banner = mAdIdToBannerAd.get(config.optString(AD_UNIT_ID));
                    if (banner != null) {
                        banner.loadAd(adRequest);

                    } else if (mAdUnitIdToBannerListener.containsKey(adUnitId)) {
                        mAdUnitIdToBannerListener.get(adUnitId).onBannerAdLoadFailed((new IronSourceError(BN_FAILED_TO_RELOAD_ERROR_CODE, getProviderName() + "reloadBanner missing banner " + adUnitId)));
                    }
                }
            }
        });
    }

    @Override
    public void destroyBanner(final JSONObject config) {
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String adUnitId = config.optString(AD_UNIT_ID);
                    IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
                    if (mAdIdToBannerAd.containsKey(adUnitId)) {
                        AdView ad = mAdIdToBannerAd.get(adUnitId);
                        ad.destroy();
                        mAdIdToBannerAd.remove(adUnitId);
                    }

                } catch (Exception e) {
                    IronLog.ADAPTER_API.error("e = " + e);
                }
            }
        });
    }
    //endregion

    // ********** Helpers **********

    //region Helpers
    private AdRequest createAdRequest() {
        AdRequest.Builder builder = new AdRequest.Builder();
        builder.setRequestAgent(IRONSOURCE_REQUEST_AGENT);
        setRequestConfiguration();

        if (mConsent != null || mCCPAValue != null) {
            Bundle extras = new Bundle();
            if (mConsent != null && !mConsent) {
                IronLog.ADAPTER_API.verbose("mConsent = " + mConsent);
                extras.putString("npa", "1");
            }
            if (mCCPAValue != null) {
                IronLog.ADAPTER_API.verbose("mCCPAValue = " + mCCPAValue);
                extras.putInt("rdp", mCCPAValue ? 1 : 0);
            }

            builder.addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter.class, extras);
        }
        return builder.build();
    }

    private void setRequestConfiguration() {
        final RequestConfiguration.Builder requestConfigurationBuilder = MobileAds.getRequestConfiguration().toBuilder();
        RequestConfiguration requestConfiguration = null;
        if (mCoppaValue != null) {
            requestConfiguration = requestConfigurationBuilder.setTagForChildDirectedTreatment(mCoppaValue).build();
        }
        if (mEuValue != null) {
            requestConfiguration = requestConfigurationBuilder.setTagForUnderAgeOfConsent(mEuValue).build();
        }
        if (!TextUtils.isEmpty(mRatingValue)) {
            requestConfiguration = requestConfigurationBuilder.setMaxAdContentRating(mRatingValue).build();
        }
        if (requestConfiguration != null) {
            MobileAds.setRequestConfiguration(requestConfiguration);
        }
    }

    private AdSize getAdSize(ISBannerSize selectedBannerSize, boolean isLargeScreen) {
        AdSize adSize;
        switch (selectedBannerSize.getDescription()) {
            case "BANNER": {
                adSize = AdSize.BANNER;
                break;
            }
            case "LARGE": {
                adSize = AdSize.LARGE_BANNER;
                break;
            }

            case "RECTANGLE": {
                adSize = AdSize.MEDIUM_RECTANGLE;
                break;
            }
            case "SMART": {
                adSize = isLargeScreen ? AdSize.LEADERBOARD : AdSize.BANNER;
                break;
            }
            case "CUSTOM": {
                adSize = new AdSize(selectedBannerSize.getWidth(), selectedBannerSize.getHeight());
                break;
            }

            default: adSize = null;
        }
        //only available from mediation version 7.1.14
        //added this try catch because we want to protect from a crash if the publisher is using an older mediation version and a new admob adapter
        try {
            if (selectedBannerSize.isAdaptive() && adSize != null) {
                AdSize adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ContextProvider.getInstance().getApplicationContext(), adSize.getWidth());
                IronLog.INTERNAL.verbose("original height - " + adSize.getHeight() + " adaptive height - " + adaptiveSize.getHeight());
                return adaptiveSize;
            }
        } catch (NoSuchMethodError e) {
            IronLog.INTERNAL.verbose("adaptive banners are not supported on Ironsource sdk on versions older than 7.1.14");
        }
        return adSize;
    }

    private InterstitialAd getInterstitialAd(String adUnitId) {
        if (TextUtils.isEmpty(adUnitId) || !mAdIdToInterstitialAd.containsKey(adUnitId)) {
            return null;
        }

        return mAdIdToInterstitialAd.get(adUnitId);
    }

    protected void setConsent(boolean consent) {
        IronLog.ADAPTER_API.verbose("consent = " + consent);
        mConsent = consent;
    }


    protected void setMetaData(String key, List<String> values) {
        if (values.isEmpty()) {
            return;
        }

        // this is a list of 1 value.
        String value = values.get(0);
        IronLog.ADAPTER_API.verbose("key = " + key + ", value = " + value);

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value));
        } else {
            setAdMobMetaDataValue(key.toLowerCase(Locale.ENGLISH), value.toLowerCase(Locale.ENGLISH));
        }

    }

    private void setCCPAValue(final boolean value) {
        IronLog.ADAPTER_API.verbose("value = " + value);
        mCCPAValue = value;
    }

    private void setAdMobMetaDataValue(String key, String value) {
        String formattedValue = value;

        if (key == AdMobMetaDataFlags.ADMOB_TFCD_KEY || key == AdMobMetaDataFlags.ADMOB_TFUA_KEY) {
            // These AdMob MetaData keys accept only boolean values
            formattedValue = MetaDataUtils.formatValueForType(value, META_DATA_VALUE_BOOLEAN);

            if (TextUtils.isEmpty(formattedValue)) {
                IronLog.ADAPTER_API.verbose("MetaData value for key " + key + " is invalid " + value);
                return;
            }
        }

        switch (key) {
            case AdMobMetaDataFlags.ADMOB_TFCD_KEY:
                mCoppaValue = getAdMobCoppaValue(formattedValue);
                IronLog.ADAPTER_API.verbose("key = " + key + ", coppaValue = " + mCoppaValue);
                break;
            case AdMobMetaDataFlags.ADMOB_TFUA_KEY:
                mEuValue = getAdMobEuValue(formattedValue);
                IronLog.ADAPTER_API.verbose("key = " + key + ", euValue = " + mEuValue);
                break;
            case AdMobMetaDataFlags.ADMOB_MAX_RATING_KEY:
                mRatingValue = getAdMobRatingValue(formattedValue);
                IronLog.ADAPTER_API.verbose("key = " + key + ", ratingValue = " + mRatingValue);
                break;
        }

        setRequestConfiguration();
    }

    private int getAdMobCoppaValue(String value) {
        boolean val = MetaDataUtils.getMetaDataBooleanValue(value);
        return val ? TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE : TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
    }

    private int getAdMobEuValue(String value) {
        boolean val = MetaDataUtils.getMetaDataBooleanValue(value);
        return val ? TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE : TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
    }

    private String getAdMobRatingValue(String value) {
        if (TextUtils.isEmpty(value)) {
            IronLog.INTERNAL.error("The ratingValue is null");
            return null;
        }

        String val = "";
        switch (value) {
            case AdMobMaxContentRating.ADMOB_MAX_AD_CONTENT_RATING_G:
                val = RequestConfiguration.MAX_AD_CONTENT_RATING_G;
                break;
            case AdMobMaxContentRating.ADMOB_MAX_AD_CONTENT_RATING_PG:
                val = RequestConfiguration.MAX_AD_CONTENT_RATING_PG;
                break;
            case AdMobMaxContentRating.ADMOB_MAX_AD_CONTENT_RATING_T:
                val = RequestConfiguration.MAX_AD_CONTENT_RATING_T;
                break;
            case AdMobMaxContentRating.ADMOB_MAX_AD_CONTENT_RATING_MA:
                val = RequestConfiguration.MAX_AD_CONTENT_RATING_MA;
                break;
            default:
                IronLog.INTERNAL.error("The ratingValue = " + value + " is undefine");
                break;
        }

        return val;
    }

    private void initializationSuccess() {
        mInitState = InitState.INIT_STATE_SUCCESS;

        for (INetworkInitCallbackListener adapter : initCallbackListeners) {
            adapter.onNetworkInitCallbackSuccess();
        }

        initCallbackListeners.clear();
    }

    private void initializationFailure() {
        mInitState = InitState.INIT_STATE_FAILED;

        for (INetworkInitCallbackListener adapter : initCallbackListeners) {
            adapter.onNetworkInitCallbackFailed(null);
        }

        initCallbackListeners.clear();
    }

    @Override
    public void onNetworkInitCallbackSuccess() {
        for (InterstitialSmashListener listener : mAdUnitIdToInterstitialListener.values()) {
            listener.onInterstitialInitSuccess();
        }

        for (BannerSmashListener listener : mAdUnitIdToBannerListener.values()) {
            listener.onBannerInitSuccess();
        }

        for (String placement : mAdUnitIdToRewardedVideoListener.keySet()) {
            RewardedVideoSmashListener listener = mAdUnitIdToRewardedVideoListener.get(placement);
            if (mRewardedVideoPlacementsForInitCallbacks.contains(placement)) {
                listener.onRewardedVideoInitSuccess();
            } else {
                loadRewardedVideoAdFromAdMob(placement, listener);
            }
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        for (InterstitialSmashListener listener : mAdUnitIdToInterstitialListener.values()) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("AdMob sdk init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }

        for (BannerSmashListener listener : mAdUnitIdToBannerListener.values()) {
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("AdMob sdk init failed", IronSourceConstants.BANNER_AD_UNIT));
        }

        for (String placement : mAdUnitIdToRewardedVideoListener.keySet()) {
            RewardedVideoSmashListener listener = mAdUnitIdToRewardedVideoListener.get(placement);
            if (mRewardedVideoPlacementsForInitCallbacks.contains(placement)) {
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("AdMob sdk init failed", IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            } else {
                listener.onRewardedVideoAvailabilityChanged(false);
            }
        }
    }

    @Override
    public void onNetworkInitCallbackLoadSuccess(String placement) {

    }

    protected boolean isNoFillError(int errorCode) {
        return errorCode == AdRequest.ERROR_CODE_NO_FILL || errorCode == AdRequest.ERROR_CODE_MEDIATION_NO_FILL;
    }
    //endregion

    @Override
    public void releaseMemory(IronSource.AD_UNIT adUnit, JSONObject config) {
        IronLog.INTERNAL.verbose("adUnit = " + adUnit);

        if (adUnit == IronSource.AD_UNIT.REWARDED_VIDEO) {
            // release rewarded ads
            for (RewardedAd rewardedVideoAd : mAdIdToRewardedVideoAd.values()) {
                rewardedVideoAd.setFullScreenContentCallback(null);
            }
            mAdIdToRewardedVideoAd.clear();
            mAdUnitIdToRewardedVideoListener.clear();
            mRewardedVideoAdsAvailability.clear();
            mRewardedVideoPlacementsForInitCallbacks.clear();

        } else if (adUnit == IronSource.AD_UNIT.INTERSTITIAL) {
            // release interstitial ads
            for (InterstitialAd interstitialAd : mAdIdToInterstitialAd.values()) {
                interstitialAd.setFullScreenContentCallback(null);
            }
            mAdIdToInterstitialAd.clear();
            mAdUnitIdToInterstitialListener.clear();
            mInterstitialAdsAvailability.clear();

        } else if (adUnit == IronSource.AD_UNIT.BANNER) {
            // release banner ads
            postOnUIThread(new Runnable() {
                @Override
                public void run() {
                    for (AdView adView : mAdIdToBannerAd.values()) {
                        adView.destroy();
                    }
                    mAdIdToBannerAd.clear();
                    mAdUnitIdToBannerListener.clear();
                }
            });
        }
    }
}
