package com.ironsource.adapters.unityads

import com.unity3d.mediation.LevelPlay
import com.ironsource.adapters.unityads.UnityAdsAdapterConstants.TROUBLESHOOTING_UADS_MISSING_CALLBACK

internal class UnityAdsErrorReporter(private val eventSender: (LevelPlay.AdFormat?, Int, String) -> Unit) {

    fun reportMissingCallback(adFormat: LevelPlay.AdFormat?, adapterNull: Boolean, listenerNull: Boolean, errorDetails: String) {
        val nullInfo = when {
            adapterNull && listenerNull -> "bothNull"
            adapterNull -> "adapterNull"
            listenerNull -> "listenerNull"
            else -> ""
        }
        eventSender.invoke(adFormat, TROUBLESHOOTING_UADS_MISSING_CALLBACK, "${errorDetails}_$nullInfo")
    }
}
