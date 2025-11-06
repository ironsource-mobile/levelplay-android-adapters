package com.ironsource.adapters.pubmatic

import android.content.Context
import com.ironsource.adapters.pubmatic.banner.PubMaticBannerAdapter
import com.ironsource.adapters.pubmatic.interstitial.PubMaticInterstitialAdapter
import com.ironsource.adapters.pubmatic.rewardedvideo.PubMaticRewardedVideoAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.pubmatic.sdk.common.OpenWrapSDK
import com.pubmatic.sdk.common.OpenWrapSDKConfig
import com.pubmatic.sdk.common.OpenWrapSDKInitializer
import com.pubmatic.sdk.common.POBAdFormat
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.core.signal.POBBiddingHost
import com.pubmatic.sdk.openwrap.core.signal.POBSignalConfig
import com.pubmatic.sdk.openwrap.core.signal.POBSignalGenerator
import com.unity3d.mediation.LevelPlay
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class PubMaticAdapter(providerName: String) : AbstractAdapter(providerName),
  INetworkInitCallbackListener {

  init {
    setRewardedVideoAdapter(PubMaticRewardedVideoAdapter(this))
    setInterstitialAdapter(PubMaticInterstitialAdapter(this))
    setBannerAdapter(PubMaticBannerAdapter(this))
  }

  companion object {

    // Adapter version
    private const val VERSION: String = BuildConfig.VERSION_NAME
    private const val GitHash: String = BuildConfig.GitHash

    // PubMatic Keys
    const val NETWORK_NAME: String = "PubMatic"
    private const val PUBLISHER_ID_KEY: String = "publisherId"
    private const val PROFILE_ID_KEY: String = "profileId"
    private const val AD_UNIT_ID_KEY: String = "adUnitId"

    // MetaData Flags
    private const val META_DATA_PUBMATIC_COPPA_KEY = "LevelPlay_ChildDirected"

    const val LOG_INIT_FAILED = "$NETWORK_NAME sdk init failed"
    val BiddingHost = POBBiddingHost.UNITYLEVELPLAY

    // Handle init callback for all adapter instances
    private val mWasInitCalled: AtomicBoolean = AtomicBoolean(false)
    private var mInitState: InitState = InitState.INIT_STATE_NONE
    private val initCallbackListeners = HashSet<INetworkInitCallbackListener>()

    // Init state possible values
    enum class InitState {
      INIT_STATE_NONE,
      INIT_STATE_IN_PROGRESS,
      INIT_STATE_SUCCESS,
      INIT_STATE_FAILED
    }

    @JvmStatic
    fun startAdapter(providerName: String): PubMaticAdapter {
      return PubMaticAdapter(providerName)
    }

    @JvmStatic
    fun getIntegrationData(context: Context?): IntegrationData {
      return IntegrationData(NETWORK_NAME, VERSION)
    }

    @JvmStatic
    fun getAdapterSDKVersion(): String {
      return OpenWrapSDK.getVersion()
    }

    fun getPublisherIdKey(): String {
      return PUBLISHER_ID_KEY
    }

    fun getProfileIdKey(): String {
      return PROFILE_ID_KEY
    }

    fun getAdUnitIdKey(): String {
      return AD_UNIT_ID_KEY
    }

    fun getLoadErrorAndCheckNoFill(error: POBError, noFillError: Int): IronSourceError {
      return when (error.errorCode) {
        POBError.NO_ADS_AVAILABLE -> IronSourceError(
          noFillError,
          error.errorMessage
        )
        else -> IronSourceError(error.errorCode, error.errorMessage)
      }
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

  override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean {
    return false
  }

  //endregion

  //region Initializations methods and callbacks

  fun initSdk(config: JSONObject) {

    // Add self to the init listeners only in case the initialization has not finished yet
    if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
      initCallbackListeners.add(this)
    }

    if (mWasInitCalled.compareAndSet(false, true)) {
      mInitState = InitState.INIT_STATE_IN_PROGRESS
      val publisherIdKey = getPublisherIdKey()
      val publisherId = config.optString(publisherIdKey)

      val profileIdKey = getProfileIdKey()
      val profileId = config.optString(profileIdKey)

      IronLog.ADAPTER_API.verbose("publisherId = $publisherId, profileId = $profileId")

      // set log level
      if (isAdaptersDebugEnabled) {
        OpenWrapSDK.setLogLevel(OpenWrapSDK.LogLevel.Debug)
      }

      val context = ContextProvider.getInstance().applicationContext

      val sdkConfig: OpenWrapSDKConfig = OpenWrapSDKConfig.Builder(publisherId, listOf(profileId.toInt())).build()

      // Init PubMatic SDK
      OpenWrapSDK.initialize(context, sdkConfig, object: OpenWrapSDKInitializer.Listener {
        override fun onSuccess() {
          IronLog.ADAPTER_API.verbose("Initialization Success")
          initializationSuccess()
        }

        override fun onFailure(error: POBError) {
          IronLog.ADAPTER_API.verbose("Initialization Failure")
          initializationFailure(error)
        }
      })
    }
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

  private fun initializationFailure(error: POBError) {
    val initFailedError = "$LOG_INIT_FAILED error =  ${error.errorMessage} code = ${error.errorCode}"
    IronLog.ADAPTER_CALLBACK.verbose(initFailedError)

    mInitState = InitState.INIT_STATE_FAILED

    //iterate over all the adapter instances and report init failed
    for (adapter: INetworkInitCallbackListener in initCallbackListeners) {
      adapter.onNetworkInitCallbackFailed(initFailedError)
    }
    initCallbackListeners.clear()
  }

  fun getInitState(): InitState {
    return mInitState
  }

  //end region

  //region legal

  override fun setMetaData(key: String, values: List<String>) {
    if (values.isEmpty()) {
      return
    }

    // This is a list of 1 value
    val value = values[0]
    IronLog.ADAPTER_API.verbose("key = $key, value = $value")
    val formattedValue: String = MetaDataUtils.formatValueForType(
      value,
      MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN
    )

    when {
      MetaDataUtils.isValidMetaData(key, META_DATA_PUBMATIC_COPPA_KEY, formattedValue) -> {
        setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
      }
    }
  }

  private fun setCOPPAValue(value: Boolean) {
    IronLog.ADAPTER_API.verbose("isCoppa = $value")
    OpenWrapSDK.setCoppa(value)
  }

  //endregion

  //region Helpers

  fun collectBiddingData(biddingDataCallback: BiddingDataCallback, adFormat: POBAdFormat) {
  if (mInitState != InitState.INIT_STATE_SUCCESS) {
    val error = "returning null as token since init isn't completed"
    IronLog.INTERNAL.verbose(error)
    biddingDataCallback.onFailure("$error - $NETWORK_NAME")
    return
  }

  val signalConfig: POBSignalConfig = POBSignalConfig.Builder(adFormat)
    .build()

  val context = ContextProvider.getInstance().applicationContext
  val signal: String = POBSignalGenerator.generateSignal(context, BiddingHost, signalConfig)
  IronLog.ADAPTER_API.verbose("token = $signal")
  val ret: MutableMap<String?, Any?> = HashMap()
  ret["token"] = signal
  biddingDataCallback.onSuccess(ret)
  }

  //endregion

}