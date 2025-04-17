package com.ironsource.adapters.applovin;

import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinErrorCodes;
import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialSmashListener;

import java.lang.ref.WeakReference;

public class AppLovinInterstitialListener implements AppLovinAdLoadListener, AppLovinAdClickListener, AppLovinAdDisplayListener, AppLovinAdVideoPlaybackListener {

    // data
    private final String mZoneId;
    private final InterstitialSmashListener mListener;
    private final WeakReference<AppLovinAdapter> mAdapter;

    public AppLovinInterstitialListener(AppLovinAdapter adapter, InterstitialSmashListener listener, String zoneId) {
        mAdapter = new WeakReference<>(adapter);
        mZoneId = zoneId;
        mListener = listener;
    }

    /**
     * This method is called when a new ad has been received.
     * This method is invoked on the UI thread.
     *
     * @param appLovinAd - Newly received ad. Guaranteed not to be null.
     */
    @Override
    public void adReceived(AppLovinAd appLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        mAdapter.get().addAdToInterstitialAdapter(appLovinAd);
        mAdapter.get().updateInterstitialAvailability(mZoneId, true);
        mListener.onInterstitialAdReady();
    }

    /**
     * This method is called when an ad could not be retrieved from the server.
     * This method is invoked on the UI thread
     * Common error codes are: 204 -- no ad is available 5xx -- internal server error negative number -- internal errors
     *
     * @param errorCode – An error code representing the reason the ad failed to load. Common error codes are defined in {@link AppLovinErrorCodes}.
     */
    @Override
    public void failedToReceiveAd(int errorCode) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId + ", errorCode = " + errorCode);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        if (mAdapter == null || mAdapter.get() == null) {
            IronLog.INTERNAL.verbose("adapter is null");
            return;
        }

        int adapterErrorCode = errorCode == AppLovinErrorCodes.NO_FILL ? IronSourceError.ERROR_IS_LOAD_NO_FILL : errorCode;
        IronSourceError ironSourceError = new IronSourceError(adapterErrorCode, mAdapter.get().getErrorString(errorCode));

        mAdapter.get().disposeInterstitialAd(mZoneId);
        mAdapter.get().updateInterstitialAvailability(mZoneId, false);
        mListener.onInterstitialAdLoadFailed(ironSourceError);
    }

    /**
     * This method is invoked when an ad is displayed inside of the {@link com.applovin.adview.AppLovinAdView} or {@link com.applovin.adview.AppLovinInterstitialAdDialog}.
     * This method is invoked on the main UI thread.
     *
     * @param appLovinAd – Ad that was just displayed. Guaranteed not to be null.
     **/
    @Override
    public void adDisplayed(AppLovinAd appLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }
        mListener.onInterstitialAdShowSucceeded();
        mListener.onInterstitialAdOpened();
    }

    /**
     * Triggered when a video begins playing in a video advertisement.
     * If your app plays other videos or music, please pause them upon receiving this callback.
     *
     * @param ad – Ad in which playback began.
     **/
    @Override
    public void videoPlaybackBegan(AppLovinAd ad) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);
    }

    /**
     * This method is invoked when the ad is clicked.
     * This method is invoked on the main UI thread.
     *
     * @param appLovinAd – Ad that was just clicked. Guaranteed not to be null
     **/
    @Override
    public void adClicked(AppLovinAd appLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onInterstitialAdClicked();
    }

    /**
     * Triggered when a video stops playing in a video advertisement.
     * If your app was playing music when the video began, this is a good opportunity to resume it.
     * If your app was playing video or otherwise requires user interaction, you probably want to use {@link AppLovinAdDisplayListener#adHidden(AppLovinAd)} instead.
     *
     * @param ad – Ad in which playback ended.
     * @param percentViewed – Percent of the video which the user watched.
     * @param fullyWatched – Whether or not the video was watched to, or very near, completion. This parameter is a simply convenience and is computed as (percentViewed >= 95)
     **/
    @Override
    public void videoPlaybackEnded(AppLovinAd ad, double percentViewed, boolean fullyWatched) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);
    }

    /**
     * This method is invoked when an ad is displayed inside of the {@link com.applovin.adview.AppLovinAdView} or {@link com.applovin.adview.AppLovinInterstitialAdDialog}. This occurs when it is explicitly closed (in the case of INTERSTITIALs).
     * This method is invoked on the main UI thread.
     *
     * @param appLovinAd – Ad that was just hidden. Guaranteed not to be null
     **/
    @Override
    public void adHidden(AppLovinAd appLovinAd) {
        IronLog.ADAPTER_CALLBACK.verbose("zoneId = " + mZoneId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mAdapter.get().disposeInterstitialAd(mZoneId);
        mListener.onInterstitialAdClosed();
    }
}
