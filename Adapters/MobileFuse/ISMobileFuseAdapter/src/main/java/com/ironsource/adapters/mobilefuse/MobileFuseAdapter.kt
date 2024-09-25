package com.ironsource.adapters.mobilefuse

import android.content.Context
import com.ironsource.adapters.mobilefuse.banner.MobileFuseBannerAdapter
import com.ironsource.adapters.mobilefuse.interstitial.MobileFuseInterstitialAdapter
import com.ironsource.adapters.mobilefuse.rewardedvideo.MobileFuseRewardedVideoAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.LoadWhileShowSupportState
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.mobilefuse.sdk.MobileFuse
import com.mobilefuse.sdk.internal.MobileFuseBiddingTokenProvider
import com.mobilefuse.sdk.internal.MobileFuseBiddingTokenRequest
import com.mobilefuse.sdk.internal.TokenGeneratorListener
import com.mobilefuse.sdk.privacy.MobileFusePrivacyPreferences
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class MobileFuseAdapter(providerName: String) : AbstractAdapter(providerName),
  INetworkInitCallbackListener, com.mobilefuse.sdk.SdkInitListener{
  init {
    setRewardedVideoAdapter(MobileFuseRewardedVideoAdapter(this))
    setInterstitialAdapter(MobileFuseInterstitialAdapter(this))
    setBannerAdapter(MobileFuseBannerAdapter(this))

    // The network's capability to load a Rewarded Video ad while another Rewarded Video ad of that network is showing
    mLWSSupportState = LoadWhileShowSupportState.LOAD_WHILE_SHOW_BY_NETWORK
  }
  companion object {

    // Adapter version
    private const val VERSION: String = BuildConfig.VERSION_NAME
    private const val GitHash: String = BuildConfig.GitHash

    // MobileFuse Keys
    private const val NETWORK_NAME: String = "MobileFuse"
    private const val PLACEMENT_ID: String = "placementId"

    // Meta data flags
    private const val DO_NOT_SELL_YES_VALUE: String = "1YY-"
    private const val DO_NOT_SELL_NO_VALUE: String = "1YN-"
    private const val META_DATA_MOBILE_FUSE_COPPA_KEY = "LevelPlay_ChildDirected"

    const val LOG_INIT_FAILED = "MobileFuse sdk init failed"
    private const val TEST_MODE = false

    // Handle init callback for all adapter instances
    private val mWasInitCalled: AtomicBoolean = AtomicBoolean(false)
    private var mInitState: InitState = InitState.INIT_STATE_NONE
    private val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

    // Privacy values
    private var coppaValue: Boolean = false
    private var doNotSellValue: String = "1-"
    private var doNotTrackValue: Boolean = false

    // Init state possible values
    enum class InitState {
      INIT_STATE_NONE,
      INIT_STATE_IN_PROGRESS,
      INIT_STATE_SUCCESS,
      INIT_STATE_FAILED
    }

    @JvmStatic
    fun startAdapter(providerName: String): MobileFuseAdapter {
      return MobileFuseAdapter(providerName)
    }

    @JvmStatic
    fun getIntegrationData(context: Context?): IntegrationData {
      return IntegrationData(NETWORK_NAME, VERSION)
    }

    @JvmStatic
    fun getAdapterSDKVersion(): String = MobileFuse.getSdkVersion()

    fun getPlacementIdKey(): String {
      return PLACEMENT_ID
    }

  }

  //region Adapter Methods

  // Get adapter version
  override fun getVersion(): String {
    return VERSION
  }

  // Get network sdk version
  override fun getCoreSDKVersion(): String {
    return getAdapterSDKVersion()
  }

  override fun isUsingActivityBeforeImpression(adUnit: IronSource.AD_UNIT): Boolean = false

  //endregion

  //region Initializations methods and callbacks

  fun initSdk(config: JSONObject) {

    // Add self to the init listeners only in case the initialization has not finished yet
    if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
      initCallbackListeners.add(this)
    }

    if (mWasInitCalled.compareAndSet(false, true)) {
      mInitState = InitState.INIT_STATE_IN_PROGRESS

      // Init MobileFuse SDK
      MobileFuse.init(this)
    }
  }

  override fun onInitSuccess() {
    initializationSuccess()
  }

  override fun onInitError() {
    initializationFailure()
  }

  private fun initializationSuccess() {
    IronLog.ADAPTER_CALLBACK.verbose()

    mInitState = InitState.INIT_STATE_SUCCESS

    //iterate over all the adapter instances and report init success
    for (adapter: INetworkInitCallbackListener in initCallbackListeners) {
      adapter.onNetworkInitCallbackSuccess()
    }
    initCallbackListeners.clear()
  }

  private fun initializationFailure() {
    IronLog.ADAPTER_CALLBACK.verbose()

    mInitState = InitState.INIT_STATE_FAILED

    //iterate over all the adapter instances and report init failed
    for (adapter: INetworkInitCallbackListener in initCallbackListeners) {
      adapter.onNetworkInitCallbackFailed(LOG_INIT_FAILED)
    }
    initCallbackListeners.clear()
  }

  fun getInitState(): InitState {
    return mInitState
  }

  //endregion

  //region legal

  override fun setMetaData(key: String, values: List<String>) {
    if (values.isEmpty()) {
      return
    }

    // This is a list of 1 value
    val value = values[0]
    IronLog.ADAPTER_API.verbose("key = $key, value = $value")
    val formattedValue: String = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

    when {
      MetaDataUtils.isValidCCPAMetaData(key, value) -> {
        setCCPAValue(MetaDataUtils.getMetaDataBooleanValue(value))
      }
      MetaDataUtils.isValidMetaData(key, META_DATA_MOBILE_FUSE_COPPA_KEY, formattedValue) -> {
        setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
      }
    }
  }

  override fun setConsent(consent: Boolean) {
    IronLog.ADAPTER_API.verbose("consent = $consent")
    doNotTrackValue = !consent // true means user agreed to tracking
  }

  private fun setCCPAValue(doNotSell: Boolean) {
    IronLog.ADAPTER_API.verbose("ccpa = $doNotSell")
    doNotSellValue = if (doNotSell) DO_NOT_SELL_YES_VALUE else DO_NOT_SELL_NO_VALUE
  }

  private fun setCOPPAValue(value: Boolean) {
    IronLog.ADAPTER_API.verbose("isCoppa = $value")
    coppaValue = value
  }

  // region Helpers
  private fun getPrivacyData(): MobileFusePrivacyPreferences {
    val builder: MobileFusePrivacyPreferences.Builder = MobileFusePrivacyPreferences.Builder()

    builder.setUsPrivacyConsentString(doNotSellValue)
    builder.setSubjectToCoppa(coppaValue)
    builder.setDoNotTrack(doNotTrackValue)

    return builder.build()
  }

  fun collectBiddingData(biddingDataCallback: BiddingDataCallback) {
    val tokenRequest = MobileFuseBiddingTokenRequest(getPrivacyData(), TEST_MODE)

      MobileFuseBiddingTokenProvider.getToken(
        tokenRequest,
        ContextProvider.getInstance().currentActiveActivity.applicationContext,
        object : TokenGeneratorListener {

          override fun onTokenGenerated(token: String) {
            val result: MutableMap<String?, Any?> = HashMap()
            result["token"] = token
            biddingDataCallback.onSuccess(result)
          }

          override fun onTokenGenerationFailed(error: String) {
            biddingDataCallback.onFailure(error)
          }
        }
      )
  }
  }

//endregion