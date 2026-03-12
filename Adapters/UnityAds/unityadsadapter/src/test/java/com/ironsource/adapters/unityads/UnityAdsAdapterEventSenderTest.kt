package com.ironsource.adapters.unityads

import com.unity3d.mediation.LevelPlay
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class UnityAdsAdapterEventSenderTest {


    private lateinit var adapter: UnityAdsAdapter

    @Before
    fun setUp() {
        adapter = UnityAdsAdapter("UnityAds")
    }

    @After
    fun tearDown() {
        val field = UnityAdsAdapter::class.java.getDeclaredField("errorReporter")
        field.isAccessible = true
        field.set(null, null)
    }

    @Test
    fun `extractEventSender should extract lambda from config when present`() {
        // Given
        var receivedAdUnit: LevelPlay.AdFormat? = null
        var receivedEventId: Int? = null
        var receivedValue: String? = null
        val eventSender: (LevelPlay.AdFormat?, Int, String) -> Unit = { adUnit, eventId, value ->
            receivedAdUnit = adUnit
            receivedEventId = eventId
            receivedValue = value
        }
        val config = JSONObject().put("eventSender", eventSender)

        // When
        adapter.extractEventSender(config)
        val field = UnityAdsAdapter::class.java.getDeclaredField("errorReporter")
        field.isAccessible = true
        val reporter = field.get(null) as? UnityAdsErrorReporter
        reporter?.reportMissingCallback(LevelPlay.AdFormat.REWARDED, false, true, "test")

        // Then
        assertEquals(LevelPlay.AdFormat.REWARDED, receivedAdUnit)
        assertEquals(UnityAdsAdapter.TROUBLESHOOTING_UADS_MISSING_CALLBACK, receivedEventId)
        assertEquals("test_listenerNull", receivedValue)
    }

    @Test
    fun `extractEventSender should be no-op when key is missing from config`() {
        // Given
        val config = JSONObject().put("someOtherKey", "value")

        // When
        adapter.extractEventSender(config)

        // Then
        val field = UnityAdsAdapter::class.java.getDeclaredField("errorReporter")
        field.isAccessible = true
        assertNull(field.get(null))
    }

    @Test
    fun `extractEventSender should not overwrite existing error reporter`() {
        // Given
        var callCount = 0
        val eventSender1: (LevelPlay.AdFormat?, Int, String) -> Unit = { _, _, _ -> callCount = 1 }
        val eventSender2: (LevelPlay.AdFormat?, Int, String) -> Unit = { _, _, _ -> callCount = 2 }
        val config1 = JSONObject().put("eventSender", eventSender1)
        val config2 = JSONObject().put("eventSender", eventSender2)

        // When
        adapter.extractEventSender(config1)
        adapter.extractEventSender(config2)
        val field = UnityAdsAdapter::class.java.getDeclaredField("errorReporter")
        field.isAccessible = true
        val reporter = field.get(null) as? UnityAdsErrorReporter
        reporter?.reportMissingCallback(LevelPlay.AdFormat.INTERSTITIAL, false, true, "test")

        // Then
        assertEquals(1, callCount)
    }

    @Test
    fun `reportMissingCallback should format bothNull correctly`() {
        // Given
        var receivedValue: String? = null
        val reporter = UnityAdsErrorReporter { _, _, value -> receivedValue = value }

        // When
        reporter.reportMissingCallback(LevelPlay.AdFormat.REWARDED, true, true, "rewarded_onUnityAdsAdLoaded")

        // Then
        assertEquals("rewarded_onUnityAdsAdLoaded_bothNull", receivedValue)
    }

    @Test
    fun `reportMissingCallback should format adapterNull correctly`() {
        // Given
        var receivedValue: String? = null
        val reporter = UnityAdsErrorReporter { _, _, value -> receivedValue = value }

        // When
        reporter.reportMissingCallback(LevelPlay.AdFormat.REWARDED, true, false, "rewarded_onUnityAdsAdLoaded")

        // Then
        assertEquals("rewarded_onUnityAdsAdLoaded_adapterNull", receivedValue)
    }

    @Test
    fun `reportMissingCallback should format listenerNull correctly`() {
        // Given
        var receivedValue: String? = null
        val reporter = UnityAdsErrorReporter { _, _, value -> receivedValue = value }

        // When
        reporter.reportMissingCallback(LevelPlay.AdFormat.REWARDED, false, true, "rewarded_onUnityAdsAdLoaded")

        // Then
        assertEquals("rewarded_onUnityAdsAdLoaded_listenerNull", receivedValue)
    }
}
