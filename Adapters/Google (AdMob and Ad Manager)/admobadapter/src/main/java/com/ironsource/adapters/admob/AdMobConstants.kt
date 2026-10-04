package com.ironsource.adapters.admob

object AdMobConstants {

    // Adapter version
    const val ADAPTER_VERSION: String = BuildConfig.VERSION_NAME

    // AdMob requires a request agent name
    const val REQUEST_AGENT = "unity"
    const val PLATFORM_NAME = "unity"
    const val REQUESTER_TYPE = "requester_type_2"

    // Configuration keys
    const val AD_UNIT_ID_KEY = "adUnitId"
    const val APP_ID_KEY = "appId"
    const val CREATIVE_ID_KEY = "creativeId"

    // Init configuration flags
    const val NETWORK_ONLY_INIT_KEY = "networkOnlyInit"
    const val INIT_RESPONSE_REQUIRED_KEY = "initResponseRequired"

    // Ad data keys
    const val REQUEST_ID_KEY = "requestId"
    const val IS_HYBRID_KEY = "isHybrid"

    // Google extras bundle keys
    const val EXTRA_PLATFORM_NAME = "platform_name"
    const val EXTRA_PLACEMENT_REQ_ID = "placement_req_id"
    const val EXTRA_IS_HYBRID_SETUP = "is_hybrid_setup"
    const val EXTRA_NPA = "npa"
    const val EXTRA_RDP = "rdp"
    const val EXTRA_ADAPTIVE_BANNER_WIDTH = "adaptive_banner_w"
    const val EXTRA_ADAPTIVE_BANNER_HEIGHT = "adaptive_banner_h"
    const val NPA_NO_CONSENT_VALUE = "1"

    // Bidding data keys
    const val TOKEN_KEY = "token"
    const val SDK_VERSION_KEY = "sdkVersion"

    // AdMob adapter status key
    const val ADMOB_ADAPTER_CLASS_NAME = "com.google.android.gms.ads.MobileAds"

    // Meta data flags
    const val ADMOB_TFCD_KEY = "admob_tfcd"
    const val ADMOB_TFUA_KEY = "admob_tfua"
    const val ADMOB_MAX_RATING_KEY = "admob_maxcontentrating"
    const val ADMOB_CONTENT_MAPPING_KEY = "google_content_mapping"

    // Meta data max content rating values
    const val ADMOB_MAX_AD_CONTENT_RATING_G = "max_ad_content_rating_g"
    const val ADMOB_MAX_AD_CONTENT_RATING_PG = "max_ad_content_rating_pg"
    const val ADMOB_MAX_AD_CONTENT_RATING_T = "max_ad_content_rating_t"
    const val ADMOB_MAX_AD_CONTENT_RATING_MA = "max_ad_content_rating_ma"

    // Network data flags
    const val NETWORK_DATA_CONTENT_MAPPING = "ContentMapping"
    const val NETWORK_DATA_CONTENT_RATING = "MaxAdContentRating"

    // Banner size descriptions
    const val BANNER_SIZE_BANNER = "BANNER"
    const val BANNER_SIZE_LARGE = "LARGE"
    const val BANNER_SIZE_RECTANGLE = "RECTANGLE"
    const val BANNER_SIZE_SMART = "SMART"
    const val BANNER_SIZE_CUSTOM = "CUSTOM"

    object Logs {
        const val MISSING_PARAM = "Missing param - %s"
        const val APP_ID = "appId = %s"
        const val AD_UNIT_ID = "adUnitId = %s"
        const val CREATIVE_ID = "creativeId = %s"
        const val META_DATA_VALUE = "key = %s, value = %s"
        const val META_DATA_VALUES = "key = %s, values = %s"
        const val CONSENT = "consent = %s"
        const val CCPA_VALUE = "value = %s"
        const val COPPA_VALUE = "key = %s, coppaValue = %s"
        const val EU_VALUE = "key = %s, euValue = %s"
        const val RATING_VALUE = "key = %s, ratingValue = %s"
        const val CONTENT_MAPPING_VALUE = "key = %s, contentMappingValue = %s"
        const val CONTENT_MAPPING_VALUES = "key = %s, contentMappingValues = %s"
        const val INVALID_META_DATA_VALUE = "MetaData value for key %s is invalid %s"
        const val UNDEFINED_RATING_VALUE = "The ratingValue = %s is undefined"
        const val NULL_RATING_VALUE = "The ratingValue is null"

        const val INIT_AND_WAIT = "init and wait for callback"
        const val INIT_WITHOUT_CALLBACK = "init without callback"
        const val DISABLE_MEDIATION_ADAPTER_INIT = "disableMediationAdapterInitialization"
        const val INIT_STATE = "AdMob initialization state = %s, description = %s"
        const val INIT_FAILED = "AdMob init failed - %s"
        const val SDK_INIT_FAILED = "AdMob sdk init failed"
        const val ADAPTER_STATUS_NOT_FOUND = "AdMob adapter status not found"

        const val NETWORK_ADAPTER_IS_NULL = "Network adapter is null"
        const val AD_NOT_READY = "Ad not ready to display"
        const val AD_VIEW_IS_NULL = "adView is null"

        const val TOKEN_INIT_NOT_STARTED = "returning null as token since init hasn't started"
        const val UNSUPPORTED_AD_FORMAT = "unsupported ad format for signal generation - %s"
        const val TOKEN_COLLECTED = "token = %s, sdkVersion = %s"
        const val TOKEN_FAILED = "failed to receive token - %s"

        const val ADAPTIVE_BANNER_SIZE = "adaptive banner width = %s, height = %s"
        const val ADAPTIVE_HEIGHT = "height = %s for width = %s"
        const val BANNER_SIZE_NOT_SUPPORTED = "size not supported, size = %s"
        const val LOAD_WITH_SERVER_DATA = "loading with serverData"
        const val LOAD_AD = "loadAd"
        const val CONTENT_MAPPING_URL = "contentMappingUrl = %s"
        const val NEIGHBORING_CONTENT_MAPPING_URLS = "neighboringContentMappingUrls = %s"

        const val LOAD_EXCEPTION = "load exception - %s"
        const val DESTROY_EXCEPTION = "destroy exception - %s"
        const val FAILED_TO_LOAD = "failed to load, error code = %s, message = %s"
        const val FAILED_TO_SHOW = "failed to show - %s"
    }
}
