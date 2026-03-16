package com.ironsource.adapters.voodoo

import android.content.Context
import com.ironsource.adapters.voodoo.interstitial.VoodooInterstitialAdapter
import com.ironsource.adapters.voodoo.rewardedvideo.VoodooRewardedVideoAdapter
import com.ironsource.environment.ContextProvider
import com.ironsource.mediationsdk.AbstractAdapter
import com.ironsource.mediationsdk.INetworkInitCallbackListener
import com.ironsource.mediationsdk.IntegrationData
import com.ironsource.mediationsdk.LoadWhileShowSupportState
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.logger.IronSourceError
import com.unity3d.mediation.LevelPlay
import io.adn.sdk.publisher.AdnAdError
import io.adn.sdk.publisher.AdnAdPlacement
import io.adn.sdk.publisher.AdnBidTokenCallback
import io.adn.sdk.publisher.AdnInitializationCallback
import io.adn.sdk.publisher.AdnInitializationStatus
import io.adn.sdk.publisher.AdnMediationType
import io.adn.sdk.publisher.AdnSdk
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class VoodooAdapter(providerName: String) : AbstractAdapter(providerName), INetworkInitCallbackListener {

    init {
        setRewardedVideoAdapter(VoodooRewardedVideoAdapter(this))
        setInterstitialAdapter(VoodooInterstitialAdapter(this))
    }

    companion object {

        // Adapter version
        private const val VERSION: String = BuildConfig.VERSION_NAME
        private const val GitHash: String = BuildConfig.GitHash

        // Voodoo keys
        const val NETWORK_NAME: String = "Voodoo"
        private const val PLACEMENT_ID_KEY: String = "placementId"

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
        fun startAdapter(providerName: String): VoodooAdapter {
            return VoodooAdapter(providerName)
        }

        @JvmStatic
        fun getIntegrationData(context: Context?): IntegrationData {
            return IntegrationData(NETWORK_NAME, VERSION)
        }

        @JvmStatic
        fun getAdapterSDKVersion(): String {
            return AdnSdk.getVersion()
        }

        fun getPlacementIdKey(): String {
            return PLACEMENT_ID_KEY
        }

        fun getLoadAdError(error: AdnAdError, noFillError: Int): IronSourceError {
            return when {
                (AdnAdError.NoFill == error) -> IronSourceError(
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
            
            val placementId = config.optString(getPlacementIdKey())

            IronLog.ADAPTER_API.verbose("placementId = $placementId")

            // Set log level
            AdnSdk.setVerbose(isAdaptersDebugEnabled)

            // Init Voodoo SDK
            AdnSdk.setMediationType(AdnMediationType.IRONSOURCE)
            AdnSdk.initialize(
                ContextProvider.getInstance().currentActiveActivity.applicationContext,
                object : AdnInitializationCallback {
                    override fun onCompletion(status: AdnInitializationStatus) {
                        when (status) {
                            AdnInitializationStatus.Success -> {
                                IronLog.ADAPTER_API.verbose("Initialization Success")
                                initializationSuccess()
                            }
                            AdnInitializationStatus.Failure -> {
                                IronLog.ADAPTER_API.verbose("Initialization Failure")
                                initializationFailure()
                            }
                            else -> {}
                        }
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

    private fun initializationFailure() {
        IronLog.ADAPTER_CALLBACK.verbose(LOG_INIT_FAILED)
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

    // region Helpers

    fun collectBiddingData(biddingDataCallback: BiddingDataCallback, adnAdPlacement: AdnAdPlacement) {
        if (mInitState == InitState.INIT_STATE_NONE) {
            val error = "returning null as token since init hasn't started"
            IronLog.INTERNAL.verbose(error)
            biddingDataCallback.onFailure("$error - $NETWORK_NAME")
            return
        }

        AdnSdk.getBidToken(adnAdPlacement, object : AdnBidTokenCallback {
            override fun onComplete(response: String) {
                val sdkVersion = coreSDKVersion
                IronLog.ADAPTER_API.verbose("token = $response, sdkVersion = $sdkVersion")
                val biddingDataMap: MutableMap<String, Any> = HashMap()
                biddingDataMap["token"] = response
                biddingDataMap["sdkVersion"] = sdkVersion
                biddingDataCallback.onSuccess(biddingDataMap)
            }
        })
    }

    //endregion
}
