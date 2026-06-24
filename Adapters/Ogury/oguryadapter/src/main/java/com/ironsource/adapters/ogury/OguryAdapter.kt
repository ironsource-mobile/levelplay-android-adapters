package com.ironsource.adapters.ogury

import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrors
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.ogury.ad.OguryBidTokenListener
import com.ogury.ad.OguryBidTokenProvider
import com.ogury.ad.OguryLoadErrorCode
import com.ogury.ad.common.OguryMediation
import com.ogury.core.OguryError
import com.ogury.core.OguryLog
import com.ogury.sdk.Ogury
import com.ogury.sdk.OguryChildPrivacyTreatment
import com.ogury.sdk.OguryOnStartListener
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class OguryAdapter : LevelPlayBaseAdapter() {

    companion object {
        private const val GitHash: String = BuildConfig.GitHash

        enum class InitState {
            INIT_STATE_NONE,
            INIT_STATE_IN_PROGRESS,
            INIT_STATE_SUCCESS,
            INIT_STATE_FAILED
        }

        private val wasInitCalled: AtomicBoolean = AtomicBoolean(false)
        private var initState: InitState = InitState.INIT_STATE_NONE
        private val initListeners = CopyOnWriteArrayList<NetworkInitializationListener>()

        internal val mediation = OguryMediation(
            OguryConstants.MEDIATION_NAME,
            LevelPlay.getSdkVersion(),
            OguryConstants.ADAPTER_VERSION
        )

        internal fun getLoadErrorType(errorCode: Int): AdapterErrorType =
            if (errorCode == OguryLoadErrorCode.NO_FILL) {
                AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
            } else {
                AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = OguryConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = Ogury.getSdkVersion()

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        val assetKey = adData.getString(OguryConstants.ASSET_KEY)
        val adUnitId = adData.getString(OguryConstants.AD_UNIT_ID_KEY)

        if (assetKey.isNullOrEmpty()) {
            val errorMessage = OguryConstants.Logs.MISSING_PARAM.format(OguryConstants.ASSET_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (adUnitId.isNullOrEmpty()) {
            val errorMessage = OguryConstants.Logs.MISSING_PARAM.format(OguryConstants.AD_UNIT_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_MISSING_PARAMS, errorMessage)
            return
        }

        if (initState == InitState.INIT_STATE_SUCCESS) {
            networkInitializationListener?.onInitSuccess()
            return
        }

        if (initState == InitState.INIT_STATE_FAILED) {
            IronLog.INTERNAL.error(OguryConstants.Logs.INIT_FAILED)
            networkInitializationListener?.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, OguryConstants.Logs.INIT_FAILED)
            return
        }

        if (initState == InitState.INIT_STATE_NONE || initState == InitState.INIT_STATE_IN_PROGRESS) {
            networkInitializationListener?.let { initListeners.add(it) }
        }

        if (wasInitCalled.compareAndSet(false, true)) {
            initState = InitState.INIT_STATE_IN_PROGRESS

            IronLog.ADAPTER_API.verbose(OguryConstants.Logs.ASSET_KEY.format(assetKey))

            if (isAdaptersDebugEnabled()) {
                OguryLog.enable(OguryLog.Level.DEBUG)
            }

            Ogury.start(context.applicationContext, assetKey, object : OguryOnStartListener {
                override fun onStarted() {
                    onInitializationSuccess()
                }

                override fun onFailed(error: OguryError) {
                    onInitializationFailure(error.code, error.message ?: OguryConstants.Logs.INIT_FAILED)
                }
            })
        }
    }

    private fun onInitializationSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose()

        initState = InitState.INIT_STATE_SUCCESS

        for (listener in initListeners) {
            listener.onInitSuccess()
        }

        initListeners.clear()
    }

    private fun onInitializationFailure(errorCode: Int, errorMessage: String) {
        IronLog.ADAPTER_CALLBACK.error(OguryConstants.Logs.INIT_FAILED_WITH_ERROR.format(errorCode, errorMessage))

        initState = InitState.INIT_STATE_FAILED

        for (listener in initListeners) {
            listener.onInitFailed(AdapterErrors.ADAPTER_ERROR_INTERNAL, errorMessage)
        }

        initListeners.clear()
    }

    // endregion

    // region Legal Methods

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (key.isNullOrEmpty() || values.isNullOrEmpty()) {
            return
        }

        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(OguryConstants.Logs.KEY_VALUE.format(key, value))

        val formattedValue = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)

        if (MetaDataUtils.isValidMetaData(key, OguryConstants.META_DATA_OGURY_COPPA_KEY, formattedValue)) {
            setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
        }
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(OguryConstants.Logs.COPPA.format(value))
        Ogury.applyChildPrivacy(
            if (value) {
                OguryChildPrivacyTreatment.CHILD_UNDER_COPPA_TREATMENT_TRUE
            } else {
                OguryChildPrivacyTreatment.CHILD_UNDER_COPPA_TREATMENT_FALSE
            }
        )
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(context: Context, biddingDataCallback: BiddingDataCallback) {
        OguryBidTokenProvider.getBidToken(context.applicationContext, object : OguryBidTokenListener {
            override fun onBidTokenGenerated(bidToken: String) {
                IronLog.ADAPTER_API.verbose(OguryConstants.Logs.TOKEN.format(bidToken))
                val biddingDataMap: MutableMap<String?, Any?> = HashMap()
                biddingDataMap[OguryConstants.TOKEN_KEY] = bidToken
                biddingDataCallback.onSuccess(biddingDataMap)
            }

            override fun onBidTokenGenerationFailed(error: OguryError) {
                biddingDataCallback.onFailure(OguryConstants.Logs.TOKEN_FAILURE.format(error.code, error.message))
            }
        })
    }

    // endregion
}
