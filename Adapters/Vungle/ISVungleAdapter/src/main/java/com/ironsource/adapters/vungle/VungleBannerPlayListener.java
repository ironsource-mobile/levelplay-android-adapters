package com.ironsource.adapters.vungle;

import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.sdk.BannerSmashListener;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.error.VungleException;

public class VungleBannerPlayListener implements PlayAdCallback {
    private BannerSmashListener mListener;

    VungleBannerPlayListener(BannerSmashListener listener) {
        mListener = listener;
    }

    /**
     * Called when the ad is first rendered, please use this callback to track views.
     * @Params - placementId – The Placement Id of the advertisement shown
     */
    @Override
    public void onAdViewed(String placementId) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdShown();
    }

    /**
     * Callback for an advertisement tapped. Sent when the user has tapped on an ad.
     * @Params - placementId – The Placement ID of the advertisement that tapped
     */
    @Override
    public void onAdClick(String placementId) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdClicked();
    }

    /**
     * Callback when the user has left the app.
     * @Params - placementId – The Placement ID of the advertisement that tapped
     */
    @Override
    public void onAdLeftApplication(String placementId) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onBannerAdLeftApplication();

    }

    /**
     * Callback for an error that has occurred while playing an advertisement. If this is called, the error was unrecoverable by the SDK and error handling should happen at the application layer. If this callback is triggered, onAdStart(String) and onAdEnd(String, boolean, boolean) will not be called. This indicates that the advertisement has finished.
     * @Params - placementId – The identifier for the advertisement placement for which the error occurred.
     * @Params - exception – The exception that prevented the advertisement from playing.
     */
    @Override
    public void onError(String placementId, VungleException exception) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId + ", exception = " + exception);
    }

    /**
     * Called when the ad is just acquired and its creative ID can be queried. The first callback to be notified
     * @Params - creativeId – The Creative Id of the advertisement
     */
    @Override
    public void creativeId(String creativeId) { }

    /**
     * Called when the Vungle SDK has successfully launched the advertisement and an advertisement will begin playing momentarily.
     * @Params - placementId – The Placement ID of the advertisement being played.
     */
    @Override
    public void onAdStart(String placementId) { }

    /**
     * Callback for an advertisement ending. The Vungle SDK has finished playing the advertisement and the user has closed the advertisement.
     * Deprecated - Replaced by onAdEnd(String) and onAdClick(String) and onAdRewarded(String)
     * @Params - placementId – The Placement ID of the advertisement that ended.
     * @Params - completed – Flag that indicates whether or not the user watched the advertisement to completion.
     * @Params - isCTAClicked – Flag that indicates whether or not the user clicked the advertisement.
     */
    @Override
    public void onAdEnd(String placementId, boolean completed, boolean isCTAClicked) { }

    /**
     * Callback for an advertisement ending. The Vungle SDK has finished playing the advertisement and the user has closed the advertisement.
     * @Params - placementId – The Placement ID of the advertisement that ended.
     */
    @Override
    public void onAdEnd(String placementId) { }

    /**
     * Callback for the user has watched the advertisement to completion. The Vungle SDK has finished playing the advertisement and the user has closed the advertisement.
     * @Params - placementId – The Placement ID of the advertisement that ended.
     */
    @Override
    public void onAdRewarded(String placementId) { }
}
