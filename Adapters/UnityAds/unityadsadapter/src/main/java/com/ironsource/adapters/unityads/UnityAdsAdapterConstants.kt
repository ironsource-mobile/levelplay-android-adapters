package com.ironsource.adapters.unityads

object UnityAdsAdapterConstants {
  // UnityAds Mediation MetaData
  internal const val MEDIATION_NAME = "ironSource"
  internal const val ADAPTER_VERSION_KEY = "adapter_version"

  // UnityAds keys
  internal const val GAME_ID = "sourceId"
  internal const val PLACEMENT_ID = "zoneId"
  internal const val MEDIATION_AD_UNIT = "adUnit"
  internal const val BANNER_SIZE = "bannerSize"

  // Adapter version
  internal const val VERSION = BuildConfig.VERSION_NAME
  internal const val GitHash = BuildConfig.GitHash

  // Meta data flags
  internal const val CONSENT_GDPR = "gdpr.consent"
  internal const val CONSENT_CCPA = "privacy.consent"
  internal const val UNITYADS_COPPA = "user.nonBehavioral"
  internal const val UNITYADS_METADATA_COPPA_KEY = "unityads_coppa"
  internal const val GAME_DESIGNATION = "mode"
  internal const val MIXED_AUDIENCE = "mixed"
  internal const val UADS_INIT_BLOB = "uads_init_blob"
  // Troubleshooting event ID
  internal const val TROUBLESHOOTING_UADS_MISSING_CALLBACK = 80600
  internal const val UADS_TRAITS = "traits"
  internal const val UADS_TRAITS_ENABLE_NEW_API = "newApiEnabled"
  internal const val UADS_AD_DATA_AD_UNIT_ID = "adUnitId"

  // Feature flag key to disable the network's capability to load a Rewarded Video ad
  // while another Rewarded Video ad of that network is showing
  internal const val LWS_SUPPORT_STATE = "isSupportedLWS"
}