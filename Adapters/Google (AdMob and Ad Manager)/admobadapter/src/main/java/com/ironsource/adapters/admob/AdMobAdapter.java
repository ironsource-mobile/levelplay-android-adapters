package com.ironsource.adapters.admob;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerSignalRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement;
import com.google.android.libraries.ads.mobile.sdk.common.AdFormat;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration;
import com.google.android.libraries.ads.mobile.sdk.initialization.AdapterStatus;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialSignalRequest;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeSignalRequest;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedSignalRequest;
import com.google.android.libraries.ads.mobile.sdk.signal.Signal;
import com.google.android.libraries.ads.mobile.sdk.signal.SignalError;
import com.google.android.libraries.ads.mobile.sdk.signal.SignalGenerationCallback;
import com.google.android.libraries.ads.mobile.sdk.signal.SignalRequest;
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration.MaxAdContentRating;
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration.TagForChildDirectedTreatment;
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration.TagForUnderAgeOfConsent;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.ironsource.adapters.admob.banner.AdMobBannerAdapter;
import com.ironsource.adapters.admob.interstitial.AdMobInterstitialAdapter;
import com.ironsource.adapters.admob.nativead.AdMobNativeAdAdapter;
import com.ironsource.adapters.admob.rewardedvideo.AdMobRewardedVideoAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.environment.StringUtils;
import com.ironsource.mediationsdk.AbstractAdapter;
import com.ironsource.mediationsdk.AdapterNetworkData;
import com.ironsource.mediationsdk.INetworkInitCallbackListener;
import com.ironsource.mediationsdk.IntegrationData;
import com.ironsource.mediationsdk.LoadWhileShowSupportState;
import com.ironsource.mediationsdk.bidding.BiddingDataCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.metadata.MetaDataUtils;
import com.unity3d.mediation.LevelPlay;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN;

import androidx.annotation.NonNull;

import java.util.Set;

public class AdMobAdapter extends AbstractAdapter {

    //AdMob requires a request agent name
    private final String REQUEST_AGENT = "unity";
    private final String PLATFORM_NAME = "unity";
    //adapter version
    private static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String GitHash = BuildConfig.GitHash;
    private static final String AD_UNIT_ID = "adUnitId";
    public static final String CREATIVE_ID_KEY = "creativeId";
    private static final String EMPTY_STRING = "";

    // Init configuration flags
    private final String NETWORK_ONLY_INIT = "networkOnlyInit";
    private final String INIT_RESPONSE_REQUIRED = "initResponseRequired";

    // shared variables between instances
    private static Boolean mConsent = null;
    private static Boolean mCCPAValue = null;
    private static TagForChildDirectedTreatment mCoppaValue = null;
    private static TagForUnderAgeOfConsent mEuValue = null;
    private static MaxAdContentRating mRatingValue = null;
    private static String mContentMappingURLValue = null;
    private static Set<String> mNeighboringContentMappingURLValue = new HashSet<>();

    // handle init callback for all adapter instances
    private static final HashSet<INetworkInitCallbackListener> initCallbackListeners = new HashSet<>();

    public static InitState mInitState = InitState.INIT_STATE_NONE;
    private static final AtomicBoolean mWasInitCalled = new AtomicBoolean(false);

    //init state possible values
    public enum InitState {
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
        String ADMOB_CONTENT_MAPPING_KEY = "google_content_mapping";
    }

    // Network Data flags
    private static final String NETWORK_DATA_CONTENT_MAPPING = "ContentMapping";
    private static final String NETWORK_DATA_CONTENT_RATING = "MaxAdContentRating";

    //region Adapter Methods
    public static AdMobAdapter startAdapter(String providerName) {
        return new AdMobAdapter(providerName);
    }

    private AdMobAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose();

        setRewardedVideoAdapter(new AdMobRewardedVideoAdapter(this));
        setInterstitialAdapter(new AdMobInterstitialAdapter(this));
        setBannerAdapter(new AdMobBannerAdapter(this));
        setNativeAdAdapter(new AdMobNativeAdAdapter(this));

        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE;
    }

    // get the network and adapter integration data
    public static IntegrationData getIntegrationData(Context context) {
        return new IntegrationData("AdMob", VERSION);
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
        return MobileAds.getVersion().toString();
    }

    public boolean isUsingActivityBeforeImpression(@NotNull LevelPlay.AdFormat adFormat) {
        return false;
    }
    //endregion

    //region Initializations methods and callbacks
    @SuppressLint("MissingPermission")
    public void initSDK(final JSONObject config) {
        // Get app ID from config - required for Next Gen SDK
        String appId = config.optString("appId", "");
        if (TextUtils.isEmpty(appId)) {
            IronLog.ADAPTER_API.error("appId is missing from config");
            initializationFailure();
            return;
        }

        // add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(AdMobAdapter.this);
        }
        //init sdk will only be called once
        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS;

            IronLog.ADAPTER_API.verbose("appId = " + appId);

            boolean networkOnlyInit = config.optBoolean(NETWORK_ONLY_INIT, true);

            //check if we want to perform the init process with an init callback
            boolean shouldWaitForInitCallback = config.optBoolean(INIT_RESPONSE_REQUIRED, false);

            // Build initialization config
            InitializationConfig.Builder initConfigBuilder = new InitializationConfig.Builder(appId);

            if (networkOnlyInit) {
                IronLog.ADAPTER_API.verbose("disableMediationAdapterInitialization");
                // Limit the AdMob initialization to its network
                initConfigBuilder.disableMediationAdapterInitialization();
            }

            InitializationConfig initConfig = initConfigBuilder.build();

            new Thread(() -> {
                if (shouldWaitForInitCallback) {
                    IronLog.ADAPTER_API.verbose("init and wait for callback");
                    MobileAds.initialize(ContextProvider.getInstance().getApplicationContext(), initConfig, initializationStatus -> {
                        AdapterStatus adMobStatus = initializationStatus.getAdapterStatusMap().get("com.google.android.gms.ads.MobileAds");
                        if (adMobStatus != null) {
                            IronLog.ADAPTER_API.verbose("AdMob initialization state = " + adMobStatus.getInitializationState() + ", description = " + adMobStatus.getDescription());
                        }

                        if (adMobStatus != null && adMobStatus.getInitializationState() == AdapterStatus.InitializationState.COMPLETE) {
                            initializationSuccess();
                        } else {
                            String error = adMobStatus != null ? adMobStatus.getDescription() : "AdMob adapter status not found";
                            IronLog.ADAPTER_API.error("AdMob init failed: " + error);
                            initializationFailure();
                        }
                    });
                } else {
                    IronLog.ADAPTER_API.verbose("init without callback");
                    MobileAds.initialize(ContextProvider.getInstance().getApplicationContext(), initConfig);
                    initializationSuccess();
                }
            }).start();
        }
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

    public InitState getInitState() {
        return mInitState;
    }
    //endregion

    //region legal
    @Override
    protected void setConsent(boolean consent) {
        IronLog.ADAPTER_API.verbose("consent = " + consent);
        mConsent = consent;
    }

    @Override
    protected void setMetaData(String key, List<String> values) {
        if (values.isEmpty()) {
            return;
        }

        if (values.size() > 1 && key.equalsIgnoreCase(AdMobMetaDataFlags.ADMOB_CONTENT_MAPPING_KEY)) {
            // multiple URL
            mNeighboringContentMappingURLValue = new HashSet<>(values);
            IronLog.ADAPTER_API.verbose("key = " + key + ", values = " + values);
            return;
        }

        // this is a list of 1 value.
        String value = values.get(0);
        IronLog.ADAPTER_API.verbose("key = " + key + ", value = " + value);

        if (MetaDataUtils.isValidCCPAMetaData(key, value)) {
            setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value));
        } else {
            setAdMobMetaDataValue(StringUtils.toLowerCase(key), StringUtils.toLowerCase(value));
        }
    }

    @Override
    public void setNetworkData(@NonNull AdapterNetworkData networkData) {
        JSONObject allData = networkData.allData();

        // If the contentMapping key maps to a string
        String networkDataContentMappingString = networkData.dataByKeyIgnoreCase(NETWORK_DATA_CONTENT_MAPPING, String.class);
        if (networkDataContentMappingString != null) {
            processContentMapping(networkDataContentMappingString);
        }

        // If the contentMapping key maps to an array
        JSONArray networkDataContentMappingArray = networkData.dataByKeyIgnoreCase(NETWORK_DATA_CONTENT_MAPPING, JSONArray.class);
        if (networkDataContentMappingArray != null) {
            processContentMapping(networkDataContentMappingArray);
        }

        String networkDataContentRating = networkData.dataByKeyIgnoreCase(NETWORK_DATA_CONTENT_RATING, String.class);
        if (networkDataContentRating != null) {
            processContentRating(networkDataContentRating);
        }
    }

    private void processContentMapping(String value) {
        mContentMappingURLValue = value;
        IronLog.ADAPTER_API.verbose("key = " + NETWORK_DATA_CONTENT_MAPPING + ", contentMappingValue = " + mContentMappingURLValue);
    }

    private void processContentMapping(JSONArray value) {
        mNeighboringContentMappingURLValue.clear();
        for (int i = 0; i < value.length(); i++) {
            mNeighboringContentMappingURLValue.add(value.optString(i));
        }
        IronLog.ADAPTER_API.verbose("key = " + NETWORK_DATA_CONTENT_MAPPING + ", contentMappingValues = " + mNeighboringContentMappingURLValue.toString());
    }

    private void processContentRating(String value) {
        mRatingValue = getAdMobRatingValue(StringUtils.toLowerCase(value));
        IronLog.ADAPTER_API.verbose("key = " + NETWORK_DATA_CONTENT_RATING + ", inputValue = " + value + ", ratingValue = " + mRatingValue);
        setRequestConfiguration();
    }

    private void setCCPAValue(boolean value) {
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
            case AdMobMetaDataFlags.ADMOB_CONTENT_MAPPING_KEY:
                mContentMappingURLValue = value;
                IronLog.ADAPTER_API.verbose("key = " + key + ", contentMappingValue = " + mContentMappingURLValue);
                break;
        }

        setRequestConfiguration();
    }

    private TagForChildDirectedTreatment getAdMobCoppaValue(String value) {
        boolean coppaValue = MetaDataUtils.getMetaDataBooleanValue(value);
        return coppaValue ? TagForChildDirectedTreatment.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
                : TagForChildDirectedTreatment.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
    }

    private TagForUnderAgeOfConsent getAdMobEuValue(String value) {
        boolean euValue = MetaDataUtils.getMetaDataBooleanValue(value);
        return euValue ? TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
                : TagForUnderAgeOfConsent.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
    }

    private MaxAdContentRating getAdMobRatingValue(String value) {
        if (TextUtils.isEmpty(value)) {
            IronLog.INTERNAL.error("The ratingValue is null");
            return null;
        }

        MaxAdContentRating ratingValue = null;
        switch (value) {
            case AdMobMaxContentRating.ADMOB_MAX_AD_CONTENT_RATING_G:
                ratingValue = MaxAdContentRating.MAX_AD_CONTENT_RATING_G;
                break;
            case AdMobMaxContentRating.ADMOB_MAX_AD_CONTENT_RATING_PG:
                ratingValue = MaxAdContentRating.MAX_AD_CONTENT_RATING_PG;
                break;
            case AdMobMaxContentRating.ADMOB_MAX_AD_CONTENT_RATING_T:
                ratingValue = MaxAdContentRating.MAX_AD_CONTENT_RATING_T;
                break;
            case AdMobMaxContentRating.ADMOB_MAX_AD_CONTENT_RATING_MA:
                ratingValue = MaxAdContentRating.MAX_AD_CONTENT_RATING_MA;
                break;
            default:
                IronLog.INTERNAL.error("The ratingValue = " + value + " is undefine");
                break;
        }

        return ratingValue;
    }
    //endregion

    // region Helpers

    public String getAdUnitIdKey() {
        return AdMobAdapter.AD_UNIT_ID;
    }

    public AdRequest createAdRequest(String adUnitId, JSONObject adData) {
        AdRequest.Builder builder = new AdRequest.Builder(adUnitId);
        builder.setRequestAgent(REQUEST_AGENT);

        //handle single content mapping for ad request
        if (!TextUtils.isEmpty(mContentMappingURLValue)) {
            IronLog.ADAPTER_API.verbose("mContentMappingURLValue = " + mContentMappingURLValue);
            builder.setContentUrl(mContentMappingURLValue);
        }

        //handle neighboring content mapping for ad request
        if (!mNeighboringContentMappingURLValue.isEmpty()) {
            IronLog.ADAPTER_API.verbose("mNeighboringContentMappingURLValue = " + mNeighboringContentMappingURLValue);
            builder.setNeighboringContentUrls(mNeighboringContentMappingURLValue);
        }

        setRequestConfiguration();

        builder.setGoogleExtrasBundle(createExtrasBundle(adData));

        return builder.build();
    }

    public BannerAdRequest createBannerAdRequest(String adUnitId, AdSize adSize, JSONObject adData) {
        BannerAdRequest.Builder builder = new BannerAdRequest.Builder(adUnitId, adSize);
        builder.setRequestAgent(REQUEST_AGENT);

        //handle single content mapping for ad request
        if (!TextUtils.isEmpty(mContentMappingURLValue)) {
            IronLog.ADAPTER_API.verbose("mContentMappingURLValue = " + mContentMappingURLValue);
            builder.setContentUrl(mContentMappingURLValue);
        }

        //handle neighboring content mapping for ad request
        if (!mNeighboringContentMappingURLValue.isEmpty()) {
            IronLog.ADAPTER_API.verbose("mNeighboringContentMappingURLValue = " + mNeighboringContentMappingURLValue);
            builder.setNeighboringContentUrls(mNeighboringContentMappingURLValue);
        }

        setRequestConfiguration();

        builder.setGoogleExtrasBundle(createExtrasBundle(adData));

        return builder.build();
    }

    public NativeAdRequest createNativeAdRequest(String adUnitId, AdChoicesPlacement adChoicesPlacement, JSONObject adData) {
        NativeAdRequest.Builder builder = new NativeAdRequest.Builder(adUnitId, Collections.singletonList(NativeAd.NativeAdType.NATIVE));
        builder.setRequestAgent(REQUEST_AGENT);
        builder.setAdChoicesPlacement(adChoicesPlacement);

        //handle single content mapping for ad request
        if (!TextUtils.isEmpty(mContentMappingURLValue)) {
            IronLog.ADAPTER_API.verbose("mContentMappingURLValue = " + mContentMappingURLValue);
            builder.setContentUrl(mContentMappingURLValue);
        }

        //handle neighboring content mapping for ad request
        if (!mNeighboringContentMappingURLValue.isEmpty()) {
            IronLog.ADAPTER_API.verbose("mNeighboringContentMappingURLValue = " + mNeighboringContentMappingURLValue);
            builder.setNeighboringContentUrls(mNeighboringContentMappingURLValue);
        }

        setRequestConfiguration();

        builder.setGoogleExtrasBundle(createExtrasBundle(adData));

        return builder.build();
    }

    private void setRequestConfiguration() {
        RequestConfiguration.Builder requestConfigurationBuilder = new RequestConfiguration.Builder();
        RequestConfiguration requestConfiguration = null;

        if (mCoppaValue != null) {
            requestConfiguration = requestConfigurationBuilder.setTagForChildDirectedTreatment(mCoppaValue).build();
        }

        if (mEuValue != null) {
            requestConfiguration = requestConfigurationBuilder.setTagForUnderAgeOfConsent(mEuValue).build();
        }

        if (mRatingValue != null) {
            requestConfiguration = requestConfigurationBuilder.setMaxAdContentRating(mRatingValue).build();
        }

        if (requestConfiguration != null) {
            MobileAds.setRequestConfiguration(requestConfiguration);
        }
    }

    //check if the error was no fill error
    public static boolean isNoFillError(LoadAdError.ErrorCode errorCode) {
        return errorCode == LoadAdError.ErrorCode.NO_FILL;
    }

    public void collectBiddingData(final BiddingDataCallback biddingDataCallback, AdFormat adFormat, Bundle additionalExtras) {
        if (mInitState == InitState.INIT_STATE_NONE) {
            String error = "returning null as token since init hasn't started";
            IronLog.INTERNAL.verbose(error);
            biddingDataCallback.onFailure(error + " - AdMob");
            return;
        }

        IronLog.ADAPTER_API.verbose(adFormat.toString());

        SignalRequest signalRequest = createSignalRequest(adFormat, additionalExtras);
        if (signalRequest == null) {
            String error = "unsupported ad format for signal generation: " + adFormat;
            IronLog.INTERNAL.error(error);
            biddingDataCallback.onFailure(error + " - AdMob");
            return;
        }

        MobileAds.generateSignal(signalRequest, new SignalGenerationCallback() {
            @Override
            public void onSuccess(@NonNull Signal signal) {
                String returnedToken = signal.getSignalString() != null ? signal.getSignalString() : EMPTY_STRING;
                String sdkVersion = getCoreSDKVersion();
                IronLog.ADAPTER_API.verbose("token = " + returnedToken + ", sdkVersion = " + sdkVersion);
                Map<String, Object> biddingDataMap = new HashMap<>();
                biddingDataMap.put("token", returnedToken);
                biddingDataMap.put("sdkVersion", sdkVersion);
                biddingDataCallback.onSuccess(biddingDataMap);
            }

            @Override
            public void onFailure(@NonNull SignalError error) {
                biddingDataCallback.onFailure("failed to receive token - AdMob " + error.getMessage());
            }
        });
    }

    private SignalRequest createSignalRequest(AdFormat adFormat, Bundle additionalExtras) {
        switch (adFormat) {
            case INTERSTITIAL:
                return new InterstitialSignalRequest.Builder("requester_type_2")
                        .build();
            case REWARDED:
                return new RewardedSignalRequest.Builder("requester_type_2")
                        .build();
            case BANNER:
                return new BannerSignalRequest.Builder("requester_type_2")
                        .setGoogleExtrasBundle(additionalExtras)
                        .build();
            case NATIVE:
                return new NativeSignalRequest.Builder("requester_type_2")
                        .build();
            default:
                return null;
        }
    }

    private Bundle createExtrasBundle(JSONObject adData) {
        Bundle extras = new Bundle();
        extras.putString("platform_name", PLATFORM_NAME);
        boolean hybridMode = false;

        if (adData != null) {
            String requestId = adData.optString("requestId", EMPTY_STRING);
            hybridMode = adData.optBoolean("isHybrid", false);

            if (!requestId.isEmpty()) {
                extras.putString("placement_req_id", requestId);
                IronLog.INTERNAL.verbose("adData requestId = " + requestId + ", isHybrid = " + hybridMode);
            }
        } else {
            IronLog.INTERNAL.verbose("adData is null, using default hybridMode = false");
        }

        extras.putString("is_hybrid_setup", String.valueOf(hybridMode));

        if (mConsent != null || mCCPAValue != null) {
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
        }

        return extras;
    }

    //endregion
}
