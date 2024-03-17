package com.ironsource.adapters.admob;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.query.QueryInfo;
import com.google.android.gms.ads.query.QueryInfoGenerationCallback;
import com.ironsource.adapters.admob.banner.AdMobBannerAdapter;
import com.ironsource.adapters.admob.interstitial.AdMobInterstitialAdapter;
import com.ironsource.adapters.admob.nativead.AdMobNativeAdAdapter;
import com.ironsource.adapters.admob.rewardedvideo.AdMobRewardedVideoAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.environment.StringUtils;
import com.ironsource.mediationsdk.AbstractAdapter;
import com.ironsource.mediationsdk.INetworkInitCallbackListener;
import com.ironsource.mediationsdk.IntegrationData;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.LoadWhileShowSupportState;
import com.ironsource.mediationsdk.bidding.BiddingDataCallback;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.metadata.MetaDataUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE;
import static com.google.android.gms.ads.mediation.MediationAdConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;
import static com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN;

import androidx.annotation.NonNull;

public class AdMobAdapter extends AbstractAdapter {

    //AdMob requires a request agent name
    private final String REQUEST_AGENT = "unity";
    private final String PLATFORM_NAME = "unity";
    //adapter version
    private static final String VERSION = BuildConfig.VERSION_NAME;

    private static final String GitHash = BuildConfig.GitHash;
    private static final String AD_UNIT_ID = "adUnitId";
    private static final String EMPTY_STRING = "";

    // Init configuration flags
    private final String NETWORK_ONLY_INIT = "networkOnlyInit";
    private final String INIT_RESPONSE_REQUIRED = "initResponseRequired";

    // shared variables between instances
    private static Boolean mConsent = null;
    private static Boolean mCCPAValue = null;
    private static Integer mCoppaValue = null;
    private static Integer mEuValue = null;
    private static String mRatingValue = "";
    private static String mContentMappingURLValue = "";
    private static List<String> mNeighboringContentMappingURLValue = new ArrayList<>();

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

    public boolean isUsingActivityBeforeImpression(@NotNull IronSource.AD_UNIT adUnit) {
        return false;
    }
    //endregion

    //region Initializations methods and callbacks
    public void initSDK(final JSONObject config) {
        // add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(AdMobAdapter.this);
        }
        //init sdk will only be called once
        if (mWasInitCalled.compareAndSet(false, true)) {
            mInitState = InitState.INIT_STATE_IN_PROGRESS;

            IronLog.ADAPTER_API.verbose();
            boolean networkOnlyInit = config.optBoolean(NETWORK_ONLY_INIT, true);

            if (networkOnlyInit) {
                IronLog.ADAPTER_API.verbose("disableMediationAdapterInitialization");
                // Limit the AdMob initialization to its network
                MobileAds.disableMediationAdapterInitialization(ContextProvider.getInstance().getApplicationContext());
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

        if (values.size() > 1 && key.equalsIgnoreCase(AdMobMetaDataFlags.ADMOB_CONTENT_MAPPING_KEY)){
            // multiple URL
            mNeighboringContentMappingURLValue = values;
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
            case AdMobMetaDataFlags.ADMOB_CONTENT_MAPPING_KEY:
                mContentMappingURLValue = value;
                IronLog.ADAPTER_API.verbose("key = " + key + ", contentMappingValue = " + mContentMappingURLValue);
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

    public String getAdUnitIdKey() {
        return AdMobAdapter.AD_UNIT_ID;
    }

    public AdRequest createAdRequest(final JSONObject adData, final String serverData) {

        AdRequest.Builder builder = new AdRequest.Builder();
        builder.setRequestAgent(REQUEST_AGENT);

        if (serverData != null) {
            // add server data to bidder instance
            builder.setAdString(serverData);
        }

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

        setRequestConfiguration();

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

        //handle single content mapping for ad request
        if(!TextUtils.isEmpty(mContentMappingURLValue)){
            IronLog.ADAPTER_API.verbose("mContentMappingURLValue = " + mContentMappingURLValue);
            builder.setContentUrl(mContentMappingURLValue);
        }

        //handle neighboring content mapping for ad request
        if(!mNeighboringContentMappingURLValue.isEmpty()){
            IronLog.ADAPTER_API.verbose("mNeighboringContentMappingURLValue = " + mNeighboringContentMappingURLValue);
            builder.setNeighboringContentUrls(mNeighboringContentMappingURLValue);
        }

        builder.addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter.class, extras);
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

    //check if the error was no fill error
    public static boolean isNoFillError(int errorCode) {
        return errorCode == AdRequest.ERROR_CODE_NO_FILL || errorCode == AdRequest.ERROR_CODE_MEDIATION_NO_FILL;
    }

    public void collectBiddingData(final BiddingDataCallback biddingDataCallback, AdFormat adFormat, Bundle additionalExtras) {
        if (mInitState == InitState.INIT_STATE_NONE) {
            String error = "returning null as token since init hasn't started";
            IronLog.INTERNAL.verbose(error);
            biddingDataCallback.onFailure(error + " - AdMob");
            return;
        }

        Bundle extras = new Bundle();
        extras.putString("query_info_type", "requester_type_2");
        if (additionalExtras != null) {
            extras.putAll(additionalExtras);
        }

        IronLog.ADAPTER_API.verbose(adFormat.toString());
        AdRequest request =
                new AdRequest.Builder()
                        .setRequestAgent(REQUEST_AGENT)
                        .addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter.class, extras)
                        .build();
        QueryInfo.generate(ContextProvider.getInstance().getApplicationContext(), adFormat, request,
                new QueryInfoGenerationCallback() {
                    // Called when query info generation succeeds
                    @Override
                    public void onSuccess(@NonNull final QueryInfo queryInfo) {
                        String returnedToken = (!TextUtils.isEmpty(queryInfo.getQuery())) ? queryInfo.getQuery() : EMPTY_STRING;
                        String sdkVersion = getCoreSDKVersion();
                        IronLog.ADAPTER_API.verbose("token = " + returnedToken + ", sdkVersion = " + sdkVersion);
                        Map<String, Object> biddingDataMap = new HashMap<>();
                        biddingDataMap.put("token", returnedToken);
                        biddingDataMap.put("sdkVersion", sdkVersion);
                        biddingDataCallback.onSuccess(biddingDataMap);
                    }

                    // Called when query info generation fails
                    @Override
                    public void onFailure(@NonNull String error) {
                        biddingDataCallback.onFailure("failed to receive token - AdMob " + error);
                    }
                });

    }


    //endregion
}
