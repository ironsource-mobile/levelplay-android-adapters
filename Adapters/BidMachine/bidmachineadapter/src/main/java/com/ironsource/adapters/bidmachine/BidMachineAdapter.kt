package com.ironsource.adapters.bidmachine

import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.bidding.BiddingDataCallback
import com.ironsource.mediationsdk.logger.IronLog
import com.ironsource.mediationsdk.metadata.MetaData
import com.ironsource.mediationsdk.metadata.MetaDataUtils
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.adapters.levelplay.LevelPlayBaseAdapter
import io.bidmachine.BMError
import io.bidmachine.BidMachine
import io.bidmachine.BidMachineInstance
import io.bidmachine.BidTokenCallback
import io.bidmachine.model.BidMachineConfiguration
import io.bidmachine.model.ConsentData
import io.bidmachine.model.MediationInfo
import io.bidmachine.model.Placement

class BidMachineAdapter : LevelPlayBaseAdapter() {

    companion object {
        private const val GitHash: String = BuildConfig.GitHash

        private val initLock = Any()
        private val consentLock = Any()
        private val consentDataBuilder = ConsentData.Builder()

        @Volatile
        internal var bidMachineInstance: BidMachineInstance? = null
            private set

        @JvmStatic
        fun getLoadErrorType(error: BMError): AdapterErrorType {
            return when (error.code) {
                BMError.NO_CONTENT -> AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL
                else -> AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
            }
        }

        internal fun createPlacement(placementId: String?): Placement? {
            return if (placementId.isNullOrEmpty()) null else Placement(placementId)
        }

        @Suppress("ACCIDENTAL_OVERRIDE")
        @JvmStatic
        fun networkAdapterVersion(): String = BidMachineConstants.ADAPTER_VERSION
    }

    // region Adapter Methods

    override fun getAdapterVersion(): String = BidMachineConstants.ADAPTER_VERSION

    override fun getNetworkSDKVersion(): String = BidMachine.VERSION

    override fun isUsingActivityBeforeImpression(adFormat: LevelPlay.AdFormat): Boolean = false

    override fun init(
        adData: AdData,
        context: Context,
        networkInitializationListener: NetworkInitializationListener?
    ) {
        val sourceId = adData.getString(BidMachineConstants.SOURCE_ID_KEY)
        if (sourceId.isNullOrEmpty()) {
            val errorMessage = BidMachineConstants.Logs.MISSING_PARAM.format(BidMachineConstants.SOURCE_ID_KEY)
            IronLog.INTERNAL.error(errorMessage)
            networkInitializationListener?.onInitFailed(BMError.NO_CONTENT, errorMessage)
            return
        }

        synchronized(initLock) {
            if (bidMachineInstance == null) {
                IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.SOURCE_ID.format(sourceId))
                BidMachine.setLoggingEnabled(isAdaptersDebugEnabled())

                val mediationInfo = MediationInfo(
                    BidMachineConstants.MEDIATION_NAME,
                    LevelPlay.getSdkVersion(),
                    BidMachineConstants.ADAPTER_VERSION
                )
                bidMachineInstance = BidMachine.initialize(
                    context.applicationContext,
                    sourceId,
                    BidMachineConfiguration(mediationInfo)
                )
            }
        }

        // SDK 4.0 queues load and token requests until initialization completes.
        networkInitializationListener?.onInitSuccess()
    }

    // endregion

    // region Legal Methods

    override fun setMetaData(key: String?, values: MutableList<String?>?) {
        if (values.isNullOrEmpty()) {
            return
        }

        val value = values[0] ?: return
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.META_DATA.format(key ?: "", value))

        val formattedValue = MetaDataUtils.formatValueForType(value, MetaData.MetaDataValueTypes.META_DATA_VALUE_BOOLEAN)
        if (MetaDataUtils.isValidMetaData(key, BidMachineConstants.META_DATA_COPPA_KEY, formattedValue)) {
            setCOPPAValue(MetaDataUtils.getMetaDataBooleanValue(formattedValue))
        }
    }

    override fun setConsent(consent: Boolean) {
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.CONSENT.format(consent))
        synchronized(consentLock) {
            BidMachine.setConsentData(
                consentDataBuilder
                    .setGdprApplies(true)
                    .setGdprConsent(consent)
                    .build()
            )
        }
    }

    private fun setCOPPAValue(value: Boolean) {
        IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.COPPA.format(value))
        synchronized(consentLock) {
            BidMachine.setConsentData(consentDataBuilder.setCoppa(value).build())
        }
    }

    // endregion

    // region Helper Methods

    internal fun collectBiddingData(
        biddingDataCallback: BiddingDataCallback,
        placementId: String?
    ) {
        val instance = bidMachineInstance
        if (instance == null) {
            IronLog.INTERNAL.error(BidMachineConstants.Logs.TOKEN_ERROR)
            biddingDataCallback.onFailure(BidMachineConstants.Logs.TOKEN_FAILED.format(BidMachineConstants.Logs.TOKEN_ERROR))
            return
        }

        instance.collectToken(listOfNotNull(createPlacement(placementId)), object : BidTokenCallback {
            override fun onCollected(bidToken: String) {
                if (bidToken.isEmpty()) {
                    IronLog.INTERNAL.error(BidMachineConstants.TOKEN_EMPTY)
                    biddingDataCallback.onFailure(BidMachineConstants.Logs.TOKEN_FAILED.format(BidMachineConstants.TOKEN_EMPTY))
                    return
                }

                IronLog.ADAPTER_API.verbose(BidMachineConstants.Logs.TOKEN.format(bidToken))
                biddingDataCallback.onSuccess(mapOf(BidMachineConstants.TOKEN_KEY to bidToken))
            }

            override fun onCollectionFailed(bmError: BMError) {
                IronLog.INTERNAL.error(bmError.message)
                biddingDataCallback.onFailure(BidMachineConstants.Logs.TOKEN_FAILED.format(bmError.message))
            }
        })
    }

    // endregion
}
