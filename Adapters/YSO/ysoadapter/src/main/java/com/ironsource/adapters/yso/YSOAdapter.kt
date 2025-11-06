package com.ironsource.adapters.yso

import android.app.Application
import android.content.Context
import com.ironsource.adapters.yso.interstitial.YSOInterstitialAdapter
import com.ironsource.adapters.yso.rewardedvideo.YSORewardedVideoAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.unity3d.mediation.LevelPlay
import com.ysocorp.ysonetwork.YsoNetwork
import com.ysocorp.ysonetwork.enums.YNEnumActionError
import java.util.concurrent.atomic.AtomicBoolean

class YSOAdapter (providerName: String) : AbstractAdapter(providerName),
  INetworkInitCallbackListener {

  init {
    setRewardedVideoAdapter(YSORewardedVideoAdapter(this))
    setInterstitialAdapter(YSOInterstitialAdapter(this))
  }

  companion object {

    // Adapter version
    private const val VERSION: String = BuildConfig.VERSION_NAME
    private const val GitHash: String = BuildConfig.GitHash

    // YSO Keys
    private const val PLACEMENT_KEY = "placementKey"
    const val NETWORK_NAME: String = "YSO"

    const val LOG_INIT_FAILED = "$NETWORK_NAME sdk init failed"

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
    fun startAdapter(providerName: String): YSOAdapter {
      return YSOAdapter(providerName)
    }

    @JvmStatic
    fun getIntegrationData(context: Context?): IntegrationData {
      return IntegrationData(NETWORK_NAME, VERSION)
    }

    @JvmStatic
    fun getAdapterSDKVersion(): String {
      return YsoNetwork.getSdkVersion()
    }

    fun getPlacementKeyId(): String {
      return PLACEMENT_KEY
    }

    fun getLoadError(error: YNEnumActionError): IronSourceError {
      return IronSourceError(error.ordinal, error.name)
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

  fun initSdk(placementKey: String) {

    // Add self to the init listeners only in case the initialization has not finished yet
    if (mInitState == InitState.INIT_STATE_NONE || mInitState == InitState.INIT_STATE_IN_PROGRESS) {
      initCallbackListeners.add(this)
    }

    if (mWasInitCalled.compareAndSet(false, true)) {
      mInitState = InitState.INIT_STATE_IN_PROGRESS
      IronLog.ADAPTER_API.verbose("placementKey: $placementKey")

      // Init YSO SDK
      try {
        val context = ContextProvider.getInstance().applicationContext
        YsoNetwork.initialize(context as Application)
        if (YsoNetwork.isInitialize()){
          IronLog.ADAPTER_API.verbose("Initialization Success")
          initializationSuccess()
        } else {
          IronLog.ADAPTER_API.verbose("Initialization Failure")
          initializationFailure()
        }
      }
      catch (e: Exception) {
        IronLog.INTERNAL.error("YSO Network initialization failed with exception: $e")
        initializationFailure()
      }
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

  //region Helpers
  fun collectBiddingData(biddingDataCallback: BiddingDataCallback) {
    if (mInitState != InitState.INIT_STATE_SUCCESS) {
      val error = "returning null as token since init isn't completed"
      IronLog.INTERNAL.verbose(error)
      biddingDataCallback.onFailure("$error - $NETWORK_NAME")
      return
    }
    val ret: MutableMap<String?, Any?> = HashMap()
    val token = YsoNetwork.getSignal()
    if (token.isNullOrEmpty()) {
      val error = "failed to receive token - returned null/empty token"
      IronLog.INTERNAL.verbose(error)
      biddingDataCallback.onFailure("$error - $NETWORK_NAME")
      return
    }
    val sdkVersion = coreSDKVersion
    IronLog.ADAPTER_API.verbose("token = $token, sdkVersion = $sdkVersion")
    ret["sdkVersion"] = sdkVersion
    ret["token"] = token
    biddingDataCallback.onSuccess(ret)
  }

  //endregion

}