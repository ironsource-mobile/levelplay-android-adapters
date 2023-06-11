package com.ironsource.adapters.facebook;

import static com.ironsource.mediationsdk.metadata.MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.BidderTokenProvider;
import com.facebook.ads.CacheFlag;
import com.ironsource.adapters.facebook.banner.FacebookBannerAdapter;
import com.ironsource.adapters.facebook.interstitial.FacebookInterstitialAdapter;
import com.ironsource.adapters.facebook.nativead.FacebookNativeAdAdapter;
import com.ironsource.adapters.facebook.rewardedvideo.FacebookRewardedVideoAdapter;
import com.ironsource.environment.ContextProvider;
import com.ironsource.environment.StringUtils;
import com.ironsource.mediationsdk.AbstractAdapter;
import com.ironsource.mediationsdk.INetworkInitCallbackListener;
import com.ironsource.mediationsdk.IntegrationData;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.LoadWhileShowSupportState;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.metadata.MetaDataUtils;
import com.ironsource.mediationsdk.utils.IronSourceUtils;


import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FacebookAdapter extends AbstractAdapter implements AudienceNetworkAds.InitListener {

    // Meta mediation service name
    private static final String MEDIATION_NAME = "ironSource";

    // Adapter version
    private static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String GitHash = BuildConfig.GitHash;

    // Meta network keys
    protected final String PLACEMENT_ID = "placementId";
    protected final String ALL_PLACEMENT_IDS = "placementIds";
    protected final static String META_NETWORK_NAME = "Facebook";

    // MetaData flags
    protected final String FACEBOOK_INTERSTITIAL_CACHE_FLAG = "facebook_is_cacheflag";
    protected final String META_INTERSTITIAL_CACHE_FLAG = "meta_is_cacheflag";
    protected final String META_MIXED_AUDIENCE = "meta_mixed_audience";
    protected static EnumSet<CacheFlag> mInterstitialFacebookCacheFlags = EnumSet.allOf(CacheFlag.class); // collected cache flags


    // Init state possible values
    public enum InitState {
        INIT_STATE_NONE,
        INIT_STATE_IN_PROGRESS,
        INIT_STATE_SUCCESS,
        INIT_STATE_FAILED
    }


    // Handle init callback for all adapter instances
    protected static HashSet<INetworkInitCallbackListener> initCallbackListeners = new HashSet<>();
    protected static InitState mInitState = InitState.INIT_STATE_NONE;
    protected static AtomicBoolean mWasInitCalled = new AtomicBoolean(false);

    //region Adapter Methods

    public static FacebookAdapter startAdapter(String providerName) {
        return new FacebookAdapter(providerName);
    }

    private FacebookAdapter(String providerName) {
        super(providerName);
        IronLog.INTERNAL.verbose();
        setRewardedVideoAdapter(new FacebookRewardedVideoAdapter(this));
        setInterstitialAdapter(new FacebookInterstitialAdapter(this));
        setBannerAdapter(new FacebookBannerAdapter(this));
        setNativeAdAdapter(new FacebookNativeAdAdapter(this));
        // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
        mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_INSTANCE;
    }

    // Get the network and adapter integration data
    public static IntegrationData getIntegrationData(Context context) {
        return new IntegrationData(META_NETWORK_NAME, VERSION);
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
        return com.facebook.ads.BuildConfig.VERSION_NAME;
    }

    public boolean isUsingActivityBeforeImpression(@NotNull IronSource.AD_UNIT adUnit) {
        return false;
    }

    //endregion

    //region Initializations methods and callbacks
    public void initSDK(String allPlacementIds) {
        // add self to the init listeners only in case the initialization has not finished yet
        if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
            initCallbackListeners.add(this);
        }

        // init SDK should be called only once
        if (mWasInitCalled.compareAndSet(false, true)) {
            final List<String> allPlacementIdsArray = Arrays.asList(allPlacementIds.split(","));
            IronLog.ADAPTER_API.verbose("Initialize Meta with placement ids = " + allPlacementIdsArray.toString());
            AudienceNetworkAds.buildInitSettings(ContextProvider.getInstance().getApplicationContext())
                    .withInitListener(this)
                    .withMediationService(getMediationServiceInfo())
                    .withPlacementIds(allPlacementIdsArray)
                    .initialize();
        }
    }

    @Override
    public void onInitialized(AudienceNetworkAds.InitResult result) {
        IronLog.ADAPTER_CALLBACK.verbose("init SDK is completed with status: " + result.isSuccess() + ", " + result.getMessage());

        if (result.isSuccess()) {
            mInitState = InitState.INIT_STATE_SUCCESS;

            for (INetworkInitCallbackListener adapter : initCallbackListeners) {
                adapter.onNetworkInitCallbackSuccess();
            }

        } else {
            mInitState = InitState.INIT_STATE_FAILED;

            for (INetworkInitCallbackListener adapter : initCallbackListeners) {
                adapter.onNetworkInitCallbackFailed(result.getMessage());
            }
        }

        initCallbackListeners.clear();
    }

    public InitState getInitState() {
        return mInitState;
    }
    //endregion

    //region legal
    @Override
    protected void setMetaData(String key, List<String> values) {
        if (values.isEmpty()) {
            return;
        }

        switch (StringUtils.toLowerCase(key)) {
            case FACEBOOK_INTERSTITIAL_CACHE_FLAG:
            case META_INTERSTITIAL_CACHE_FLAG:
                IronLog.ADAPTER_API.verbose("key = " + key + ", values = " + values);
                mInterstitialFacebookCacheFlags.clear();

                try {
                    for (String value : values) {
                        CacheFlag flag = getFacebookCacheFlag(value);
                        IronLog.ADAPTER_API.verbose("flag for value " + value + " is " + flag.name());
                        mInterstitialFacebookCacheFlags.add(flag);
                    }
                } catch (Exception e) {
                    IronLog.INTERNAL.error("flag is unknown or all, set all as default");
                    mInterstitialFacebookCacheFlags = getFacebookAllCacheFlags();
                }
                break;

            case META_MIXED_AUDIENCE:
                // this is a list of 1 value
                String value = values.get(0);
                IronLog.ADAPTER_API.verbose("key = " + key + ", value = " + value);

                String formattedValue = MetaDataUtils.formatValueForType(value, META_DATA_VALUE_BOOLEAN);
                if (isValidMixedAudienceMetaData(formattedValue)) {
                    setMixedAudience(MetaDataUtils.getMetaDataBooleanValue(formattedValue));
                }
                break;
        }
    }

    private CacheFlag getFacebookCacheFlag(String value) {
        IronLog.ADAPTER_API.verbose("value = " + value);
        return CacheFlag.valueOf(StringUtils.toUpperCase(value));
    }

    private EnumSet<CacheFlag> getFacebookAllCacheFlags() {
        IronLog.ADAPTER_API.verbose();
        return EnumSet.allOf(CacheFlag.class);
    }

    private void setMixedAudience(boolean isMixedAudience) {
        IronLog.ADAPTER_API.verbose("isMixedAudience = " + isMixedAudience);
        AdSettings.setMixedAudience(isMixedAudience);
    }

    private boolean isValidMixedAudienceMetaData(String value) {
        return !TextUtils.isEmpty(value);
    }

    //endregion

    //region Helpers


    private String getMediationServiceInfo() {
        String mediationServiceInfo = String.format("%s_%s:%s", MEDIATION_NAME, IronSourceUtils.getSDKVersion(), VERSION);
        IronLog.INTERNAL.verbose("mediationServiceInfo = " + mediationServiceInfo);
        return mediationServiceInfo;
    }

    //check if the error was no fill error
    public static boolean isNoFillError(AdError adError) {
        return adError.getErrorCode() == AdError.NO_FILL_ERROR_CODE;
    }

    public Map<String, Object> getBiddingData() {
        if (mInitState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.verbose("returning null as token since init failed");
            return null;
        }

        String bidderToken = BidderTokenProvider.getBidderToken(ContextProvider.getInstance().getApplicationContext());
        String returnedToken = (!TextUtils.isEmpty(bidderToken)) ? bidderToken : "";
        IronLog.ADAPTER_API.verbose("token = " + returnedToken);
        Map<String, Object> ret = new HashMap<>();
        ret.put("token", returnedToken);
        return ret;
    }

    public EnumSet<CacheFlag> getCacheFlags() {
        return mInterstitialFacebookCacheFlags;
    }

    public String getPlacementIdKey() {
        return PLACEMENT_ID;
    }

    @NonNull
    public String getAllPlacementIdsKey() {
        return ALL_PLACEMENT_IDS;
    }
    //endregion

}
