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


public class AdMobAdapter extends AbstractAdapter implements INetworkInitCallbackListener {


    //AdMob requires a request agent name
    private final String IRONSOURCE_REQUEST_AGENT = "ironSource";

    //adapter version
    private static final String VERSION = BuildConfig.VERSION_NAME;

    private static final String GitHash = BuildConfig.GitHash;
    private final String AD_UNIT_ID = "adUnitId";

    //network version
    private static final String CORE_SDK_VERSION = "20.6.0";

    // Init configuration flags
    private final String NETWORK_ONLY_INIT = "networkOnlyInit";
    private final String INIT_RESPONSE_REQUIRED = "initResponseRequired";

    // shared variables between instances
    private static Boolean mConsent = null;
    private static Boolean mCCPAValue = null;
    private static Integer mCoppaValue = null;
    private static Integer mEuValue = null;
    private static String mRatingValue = "";

    // handle init callback for all adapter instances
    private static HashSet<INetworkInitCallbackListener> initCallbackListeners = new HashSet<>();
    private static InitState mInitState = InitState.INIT_STATE_NONE;
    private static AtomicBoolean mWasInitCalled = new AtomicBoolean(false);


    // Rewarded video collections
    private ConcurrentHashMap<String, RewardedVideoSmashListener> mAdUnitIdToRewardedVideoListener;
    private CopyOnWriteArraySet<String> mRewardedVideoAdUnitIdsForInitCallbacks;
    public ConcurrentHashMap<String, RewardedAd> mAdUnitIdToRewardedVideoAd;
    public ConcurrentHashMap<String, Boolean> mRewardedVideoAdsAvailability; //used to check if an ad is available

    // Interstitial maps
    private ConcurrentHashMap<String, InterstitialSmashListener> mAdUnitIdToInterstitialListener;
    public ConcurrentHashMap<String, InterstitialAd> mAdUnitIdToInterstitialAd;
    public ConcurrentHashMap<String, Boolean> mInterstitialAdsAvailability; //used to check if an ad is available

    // Banner maps
    private ConcurrentHashMap<String, BannerSmashListener> mAdUnitIdToBannerListener;
    private ConcurrentHashMap<String, AdView> mAdUnitIdToBannerAd;


    //init state possible values
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


    //region Adapter Methods
    public static AdMobAdapter startAdapter(String providerName) {
        return new AdMobAdapter(providerName);
    }

    private AdMobAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose("");

        // rewarded video
        mAdUnitIdToRewardedVideoListener = new ConcurrentHashMap<>();
        mAdUnitIdToRewardedVideoAd = new ConcurrentHashMap<>();
        mRewardedVideoAdsAvailability = new ConcurrentHashMap<>();
        mRewardedVideoAdUnitIdsForInitCallbacks = new CopyOnWriteArraySet<>();

        // interstitial
        mAdUnitIdToInterstitialAd = new ConcurrentHashMap<>();
        mInterstitialAdsAvailability = new ConcurrentHashMap<>();
        mAdUnitIdToInterstitialListener = new ConcurrentHashMap<>();

        // banner
        mAdUnitIdToBannerAd = new ConcurrentHashMap<>();
        mAdUnitIdToBannerListener = new ConcurrentHashMap<>();

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE;
    }

    // get the network and adapter integration data
    public static IntegrationData getIntegrationData(Activity activity) {
        IntegrationData ret = new IntegrationData("AdMob", VERSION);
        ret.activities = new String[]{"com.google.android.gms.ads.AdActivity"};
        return ret;
    }

    // get adapter version
    @Override
    public String getVersion() {
        return VERSION;
    }

    //get network sdk version
    @Override
    public String getCoreSDKVersion() {
        return CORE_SDK_VERSION;
    }
    //endregion

    //region Initializations methods and callbacks
    // All calls to MobileAds must be on the main thread --> run all calls to initSDK in a thread.
    private void initSDK(final JSONObject config) {
        // add self to init delegates if init process didn't finish
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this);
        }
        //init sdk will only be called once
        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS;

            IronLog.ADAPTER_API.verbose("");
            boolean networkOnlyInit = config.optBoolean(NETWORK_ONLY_INIT, true);

            if (networkOnlyInit) {
                IronLog.ADAPTER_API.verbose("disableMediationAdapterInitialization");
                // Limit the AdMob initialization to its network
                MobileAds.disableMediationAdapterInitialization(ContextProvider.getInstance().getCurrentActiveActivity());
            }


            //check if we want to perform the init process with an init callback
            boolean shouldWaitForInitCallback = config.optBoolean(INIT_RESPONSE_REQUIRED, false);

            if (shouldWaitForInitCallback) {
                IronLog.ADAPTER_API.verbose("init and wait for callback");

                //init AdMob sdk with callback
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
                //init AdMob sdk without callback
                IronLog.ADAPTER_API.verbose("init without callback");
                MobileAds.initialize(ContextProvider.getInstance().getApplicationContext());
                initializationSuccess();
            }
        }

    }


    @Override
    public void onNetworkInitCallbackSuccess() {
        for (String adUnitId : mAdUnitIdToRewardedVideoListener.keySet()) {
            RewardedVideoSmashListener listener = mAdUnitIdToRewardedVideoListener.get(adUnitId);
            if (mRewardedVideoAdUnitIdsForInitCallbacks.contains(adUnitId)) {
                listener.onRewardedVideoInitSuccess();
            } else {
                loadRewardedVideoAdFromAdMob(adUnitId, listener);
            }
        }

        for (InterstitialSmashListener listener : mAdUnitIdToInterstitialListener.values()) {
            listener.onInterstitialInitSuccess();
        }

        for (BannerSmashListener listener : mAdUnitIdToBannerListener.values()) {
            listener.onBannerInitSuccess();
        }
    }

    @Override
    public void onNetworkInitCallbackFailed(String error) {
        for (String adUnitId : mAdUnitIdToRewardedVideoListener.keySet()) {
            RewardedVideoSmashListener listener = mAdUnitIdToRewardedVideoListener.get(adUnitId);
            if (mRewardedVideoAdUnitIdsForInitCallbacks.contains(adUnitId)) {
                listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            } else {
                listener.onRewardedVideoAvailabilityChanged(false);
            }
        }

        for (InterstitialSmashListener listener : mAdUnitIdToInterstitialListener.values()) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.INTERSTITIAL_AD_UNIT));
        }

        for (BannerSmashListener listener : mAdUnitIdToBannerListener.values()) {
            listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError(error, IronSourceConstants.BANNER_AD_UNIT));
        }
    }

    @Override
    public void onNetworkInitCallbackLoadSuccess(String adUnitId) {

    }

    private void initializationSuccess() {
        mInitState = InitState.INIT_STATE_SUCCESS;

        //iterate over all the adapter instances and report init success
        for (INetworkInitCallbackListener adapter : initCallbackListeners) {
            adapter.onNetworkInitCallbackSuccess();
        }

        initCallbackListeners.clear();
    }

    private void initializationFailure() {
        mInitState = InitState.INIT_STATE_FAILED;
        //iterate over all the adapter instances and report init failed
        for (INetworkInitCallbackListener adapter : initCallbackListeners) {
            adapter.onNetworkInitCallbackFailed("AdMob sdk init failed");
        }

        initCallbackListeners.clear();
    }
    //endregion

    //region Rewarded Video API
    // used for flows when the mediation needs to get a callback for init
    @Override
    public void initRewardedVideoWithCallback(String appKey, String userId,
                                              final JSONObject config, final RewardedVideoSmashListener listener) {

        final String adUnitId = config.optString(AD_UNIT_ID);
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        if (TextUtils.isEmpty(adUnitId)) {
            IronLog.INTERNAL.error("adUnitId is empty");
            listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + AD_UNIT_ID, IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
            return;
        }

        //add to rewarded video listener map
        mAdUnitIdToRewardedVideoListener.put(adUnitId, listener);
        //add to rewarded video init callback map
        mRewardedVideoAdUnitIdsForInitCallbacks.add(adUnitId);
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                // check AdMob sdk init state
                if (mInitState == InitState.INIT_STATE_SUCCESS) {
                    listener.onRewardedVideoInitSuccess();
                } else if (mInitState == InitState.INIT_STATE_FAILED) {
                    IronLog.INTERNAL.verbose("init failed - adUnitId = " + adUnitId);
                    listener.onRewardedVideoInitFailed(ErrorBuilder.buildInitFailedError("AdMob sdk init failed", IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                } else {
                    initSDK(config);
                }
            }
        });

    }

    // used for flows when the mediation doesn't need to get a callback for init
    @Override
    public void initAndLoadRewardedVideo(String appKey, String userId, final JSONObject config, final RewardedVideoSmashListener listener) {

        final String adUnitId = config.optString(AD_UNIT_ID);
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        if (TextUtils.isEmpty(adUnitId)) {
            IronLog.INTERNAL.error("adUnitId is empty");
            listener.onRewardedVideoAvailabilityChanged(false);
            return;
        }

        //add to rewarded video listener map
        mAdUnitIdToRewardedVideoListener.put(adUnitId, listener);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (mInitState == InitState.INIT_STATE_SUCCESS) {
                    IronLog.INTERNAL.verbose("loadVideo - adUnitId = " + adUnitId);
                    loadRewardedVideoAdFromAdMob(adUnitId, listener);
                } else if (mInitState == InitState.INIT_STATE_FAILED) {
                    IronLog.INTERNAL.verbose("onRewardedVideoAvailabilityChanged(false) - adUnitId = " + adUnitId);
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

    private void loadRewardedVideoAdFromAdMob(final String adUnitId, final RewardedVideoSmashListener listener) {
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        // set the rewarded video availability to false before attempting to load
        mRewardedVideoAdsAvailability.put(adUnitId, false);
        AdRequest adRequest = createAdRequest();
        AdMobRewardedVideoAdLoadListener adMobRewardedVideoAdLoadListener = new AdMobRewardedVideoAdLoadListener(AdMobAdapter.this, adUnitId, listener);
        RewardedAd.load(ContextProvider.getInstance().getApplicationContext(), adUnitId, adRequest, adMobRewardedVideoAdLoadListener);
    }

    @Override
    public void showRewardedVideo(final JSONObject config,
                                  final RewardedVideoSmashListener listener) {
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                final String adUnitId = config.optString(AD_UNIT_ID);
                final RewardedAd rewardedAd = mAdUnitIdToRewardedVideoAd.get(adUnitId);
                IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
                if (rewardedAd != null && isRewardedVideoAvailableForAdUnitId(adUnitId)) {

                    AdMobRewardedVideoAdShowListener adMobRewardedVideoAdShowListener = new AdMobRewardedVideoAdShowListener(AdMobAdapter.this, adUnitId, listener);
                    rewardedAd.setFullScreenContentCallback(adMobRewardedVideoAdShowListener);
                    rewardedAd.show(ContextProvider.getInstance().getCurrentActiveActivity(), adMobRewardedVideoAdShowListener);
                } else {
                    listener.onRewardedVideoAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.REWARDED_VIDEO_AD_UNIT));
                }
                //change rewarded video availability to false
                mRewardedVideoAdsAvailability.put(adUnitId, false);
                listener.onRewardedVideoAvailabilityChanged(false);
            }
        });
    }


    @Override
    public boolean isRewardedVideoAvailable(JSONObject config) {
        String adUnitId = config.optString(AD_UNIT_ID);
        return isRewardedVideoAvailableForAdUnitId(adUnitId);
    }

    private boolean isRewardedVideoAvailableForAdUnitId(String adUnitId) {
        if (TextUtils.isEmpty(adUnitId)) {
            return false;
        }
        return mRewardedVideoAdsAvailability.containsKey(adUnitId) && mRewardedVideoAdsAvailability.get(adUnitId);
    }
    //endregion

    //region Interstitial API
    @Override
    public void initInterstitial(String appKey, String userId, final JSONObject config,
                                 final InterstitialSmashListener listener) {

        final String adUnitId = config.optString(AD_UNIT_ID);

        if (TextUtils.isEmpty(adUnitId)) {
            listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("Missing params - " + AD_UNIT_ID, IronSourceConstants.INTERSTITIAL_AD_UNIT));
            return;
        }

        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                //add to interstitial listener map
                mAdUnitIdToInterstitialListener.put(adUnitId, listener);

                //check AdMob sdk init state
                if (mInitState == InitState.INIT_STATE_SUCCESS) {
                    IronLog.INTERNAL.verbose("onInterstitialInitSuccess - adUnitId = " + adUnitId);
                    listener.onInterstitialInitSuccess();
                } else if (mInitState == InitState.INIT_STATE_FAILED) {
                    IronLog.INTERNAL.verbose("onInterstitialInitFailed - adUnitId = " + adUnitId);
                    listener.onInterstitialInitFailed(ErrorBuilder.buildInitFailedError("AdMob sdk init failed", IronSourceConstants.INTERSTITIAL_AD_UNIT));
                } else {
                    initSDK(config);
                }
            }
        });
    }


    @Override
    public void loadInterstitial(final JSONObject config,
                                 final InterstitialSmashListener listener) {
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                final String adUnitId = config.optString(AD_UNIT_ID);
                IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

                // set the interstitial ad availability to false before attempting to load
                mInterstitialAdsAvailability.put(adUnitId, false);
                AdRequest adRequest = createAdRequest();
                AdMobInterstitialAdLoadListener adMobInterstitialAdLoadListener = new AdMobInterstitialAdLoadListener(AdMobAdapter.this, adUnitId, listener);
                InterstitialAd.load(ContextProvider.getInstance().getApplicationContext(), adUnitId, adRequest, adMobInterstitialAdLoadListener);
            }
        });
    }

    @Override
    public void showInterstitial(final JSONObject config,
                                 final InterstitialSmashListener listener) {
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                final String adUnitId = config.optString(AD_UNIT_ID);
                IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
                // Show the ad if it's ready.
                if (!isInterstitialReadyForAdUnitId(adUnitId)) {
                    IronLog.ADAPTER_API.error("Ad not ready to display");
                    listener.onInterstitialAdShowFailed(ErrorBuilder.buildNoAdsToShowError(IronSourceConstants.INTERSTITIAL_AD_UNIT));
                    return;
                }

                InterstitialAd interstitialAd = getInterstitialAd(adUnitId);
                AdMobInterstitialAdShowListener adMobInterstitialAdShowListener = new AdMobInterstitialAdShowListener(AdMobAdapter.this, listener, adUnitId);
                interstitialAd.setFullScreenContentCallback(adMobInterstitialAdShowListener);
                interstitialAd.show(ContextProvider.getInstance().getCurrentActiveActivity());

                //change interstitial availability to false
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
        if (TextUtils.isEmpty(adUnitId)) {
            return false;
        }
        return mInterstitialAdsAvailability.containsKey(adUnitId) && mInterstitialAdsAvailability.get(adUnitId);
    }

    //endregion

    //region Banner API
    @Override
    public void initBanners(String appKey, String userId, final JSONObject config,
                            final BannerSmashListener listener) {

        final String adUnitId = config.optString(AD_UNIT_ID);
        if (TextUtils.isEmpty(adUnitId)) {
            IronSourceError error = ErrorBuilder.buildInitFailedError("Missing params - " + AD_UNIT_ID, IronSourceConstants.BANNER_AD_UNIT);
            listener.onBannerInitFailed(error);
            return;
        }
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);

        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                //add banner to listener map
                mAdUnitIdToBannerListener.put(adUnitId, listener);

                if (mInitState == InitState.INIT_STATE_SUCCESS) {
                    IronLog.INTERNAL.verbose("onBannerInitSuccess - adUnitId = " + adUnitId);
                    listener.onBannerInitSuccess();
                } else if (mInitState == InitState.INIT_STATE_FAILED) {
                    IronLog.INTERNAL.verbose("onBannerInitFailed - adUnitId = " + adUnitId);
                    listener.onBannerInitFailed(ErrorBuilder.buildInitFailedError("AdMob sdk init failed", IronSourceConstants.BANNER_AD_UNIT));
                } else {
                    initSDK(config);
                }
            }
        });
    }

    @Override
    public void loadBanner(final IronSourceBannerLayout banner, final JSONObject config,
                           final BannerSmashListener listener) {

        if (banner == null) {
            IronLog.ADAPTER_API.error("banner is null");
            return;
        }

        final String adUnitId = config.optString(AD_UNIT_ID);
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    //get banner size
                    final AdSize size = getAdSize(banner.getSize(), AdapterUtils.isLargeScreen(banner.getActivity()));
                    if (size == null) {
                        listener.onBannerAdLoadFailed(ErrorBuilder.unsupportedBannerSize("AdMob"));
                        return;
                    }

                    AdView adView = new AdView(banner.getActivity());
                    adView.setAdSize(size);
                    adView.setAdUnitId(adUnitId);
                    AdMobBannerAdListener adMobBannerAdListener = new AdMobBannerAdListener(AdMobAdapter.this, listener, adUnitId, adView);
                    adView.setAdListener(adMobBannerAdListener);

                    //add banner ad to map
                    mAdUnitIdToBannerAd.put(adUnitId, adView);

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
    public void reloadBanner(final IronSourceBannerLayout banner, final JSONObject config,
                             final BannerSmashListener listener) {
        final String adUnitId = config.optString(AD_UNIT_ID);
        IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (mAdUnitIdToBannerAd.get(config.optString(AD_UNIT_ID)) != null) {
                    AdRequest adRequest = createAdRequest();
                    AdView banner = mAdUnitIdToBannerAd.get(config.optString(AD_UNIT_ID));
                    if (banner != null) {
                        banner.loadAd(adRequest);
                    } else if (mAdUnitIdToBannerListener.containsKey(adUnitId)) {
                        String msg = "reloadBanner missing banner " + adUnitId;
                        listener.onBannerAdLoadFailed(ErrorBuilder.buildLoadFailedError(IronSourceConstants.BANNER_AD_UNIT, getProviderName(), msg));
                    }
                }
            }
        });
    }

    // destroy banner ad and clear banner ad map
    @Override
    public void destroyBanner(final JSONObject config) {
        postOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String adUnitId = config.optString(AD_UNIT_ID);
                    IronLog.ADAPTER_API.verbose("adUnitId = " + adUnitId);
                    if (mAdUnitIdToBannerAd.containsKey(adUnitId)) {
                        AdView ad = mAdUnitIdToBannerAd.get(adUnitId);
                        if (ad != null) {
                            ad.destroy();
                        }
                        mAdUnitIdToBannerAd.remove(adUnitId);
                    }

                } catch (Exception e) {
                    IronLog.ADAPTER_API.error("e = " + e);
                }
            }
        });
    }
    //endregion

    // region memory handling
    @Override
    public void releaseMemory(IronSource.AD_UNIT adUnit, JSONObject config) {
        IronLog.INTERNAL.verbose("adUnit = " + adUnit);

        if (adUnit == IronSource.AD_UNIT.REWARDED_VIDEO) {
            // release rewarded ads
            for (RewardedAd rewardedVideoAd : mAdUnitIdToRewardedVideoAd.values()) {
                rewardedVideoAd.setFullScreenContentCallback(null);
            }
            // clear rewarded maps
            mAdUnitIdToRewardedVideoAd.clear();
            mAdUnitIdToRewardedVideoListener.clear();
            mRewardedVideoAdsAvailability.clear();
            mRewardedVideoAdUnitIdsForInitCallbacks.clear();

        } else if (adUnit == IronSource.AD_UNIT.INTERSTITIAL) {
            // release interstitial ads
            for (InterstitialAd interstitialAd : mAdUnitIdToInterstitialAd.values()) {
                interstitialAd.setFullScreenContentCallback(null);
            }
            // clear interstitial maps
            mAdUnitIdToInterstitialAd.clear();
            mAdUnitIdToInterstitialListener.clear();
            mInterstitialAdsAvailability.clear();

        } else if (adUnit == IronSource.AD_UNIT.BANNER) {
            // release banner ads
            postOnUIThread(new Runnable() {
                @Override
                public void run() {
                    for (AdView adView : mAdUnitIdToBannerAd.values()) {
                        adView.destroy();
                    }
                    // clear banner maps
                    mAdUnitIdToBannerAd.clear();
                    mAdUnitIdToBannerListener.clear();
                }
            });
        }
    }
    //endregion

    //region legal
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

        if (AdMobMetaDataFlags.ADMOB_TFCD_KEY.equals(key) || AdMobMetaDataFlags.ADMOB_TFUA_KEY.equals(key)) {
            // AdMob MetaData keys accept only boolean values
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
        boolean coppaValue = MetaDataUtils.getMetaDataBooleanValue(value);
        return coppaValue ? TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE : TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
    }

    private int getAdMobEuValue(String value) {
        boolean euValue = MetaDataUtils.getMetaDataBooleanValue(value);
        return euValue ? TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE : TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
    }

    private String getAdMobRatingValue(String value) {
        if (TextUtils.isEmpty(value)) {
            IronLog.INTERNAL.error("The ratingValue is null");
            return null;
        }

        String ratingValue = "";
        switch (value) {
            case AdMobMaxContentRating.ADMOB_MAX_AD_CONTENT_RATING_G:
                ratingValue = RequestConfiguration.MAX_AD_CONTENT_RATING_G;
                break;
            case AdMobMaxContentRating.ADMOB_MAX_AD_CONTENT_RATING_PG:
                ratingValue = RequestConfiguration.MAX_AD_CONTENT_RATING_PG;
                break;
            case AdMobMaxContentRating.ADMOB_MAX_AD_CONTENT_RATING_T:
                ratingValue = RequestConfiguration.MAX_AD_CONTENT_RATING_T;
                break;
            case AdMobMaxContentRating.ADMOB_MAX_AD_CONTENT_RATING_MA:
                ratingValue = RequestConfiguration.MAX_AD_CONTENT_RATING_MA;
                break;
            default:
                IronLog.INTERNAL.error("The ratingValue = " + value + " is undefine");
                break;
        }

        return ratingValue;
    }


    //endregion

    // region Helpers
    private AdRequest createAdRequest() {
        AdRequest.Builder builder = new AdRequest.Builder();
        builder.setRequestAgent(IRONSOURCE_REQUEST_AGENT);
        setRequestConfiguration();

        if (mConsent != null || mCCPAValue != null) {
            Bundle extras = new Bundle();
            //handle consent for ad request
            if (mConsent != null && !mConsent) {
                IronLog.ADAPTER_API.verbose("mConsent = " + mConsent);
                extras.putString("npa", "1");
            }
            //handle CCPA for ad request
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

            default:
                adSize = null;
        }
        //adaptive banner size is only available from mediation version 7.1.14
        //added this try catch because we want to protect from a crash if the publisher is using an earlier mediation version and a new AdMob adapter
        try {
            if (selectedBannerSize.isAdaptive() && adSize != null) {
                AdSize adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ContextProvider.getInstance().getApplicationContext(), adSize.getWidth());
                IronLog.INTERNAL.verbose("original height - " + adSize.getHeight() + " adaptive height - " + adaptiveSize.getHeight());
                return adaptiveSize;
            }
        } catch (NoSuchMethodError e) {
            IronLog.INTERNAL.verbose("adaptive banners are not supported on Ironsource sdk versions earlier than 7.1.14");
        }
        return adSize;
    }

    private InterstitialAd getInterstitialAd(String adUnitId) {
        if (TextUtils.isEmpty(adUnitId) || !mAdUnitIdToInterstitialAd.containsKey(adUnitId)) {
            return null;
        }

        return mAdUnitIdToInterstitialAd.get(adUnitId);
    }

    //check if the error was no fill error
    protected boolean isNoFillError(int errorCode) {
        return errorCode == AdRequest.ERROR_CODE_NO_FILL || errorCode == AdRequest.ERROR_CODE_MEDIATION_NO_FILL;
    }
    //endregion

}
