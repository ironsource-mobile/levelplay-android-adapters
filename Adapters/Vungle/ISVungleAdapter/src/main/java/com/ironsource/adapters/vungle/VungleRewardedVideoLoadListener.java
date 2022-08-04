package com.ironsource.adapters.vungle;

import com.ironsource.mediationsdk.logger.IronLog;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.RewardedVideoSmashListener;
import com.ironsource.mediationsdk.utils.ErrorBuilder;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.error.VungleException;

public class VungleRewardedVideoLoadListener implements LoadAdCallback {

    private RewardedVideoSmashListener mListener;

    VungleRewardedVideoLoadListener(RewardedVideoSmashListener listener) {
        mListener = listener;
    }

    /**
     * Callback used to notify that the advertisement assets have been downloaded and are ready to play.
     * @Params - placementId – The placement identifier for which the advertisement assets have been downloaded.
     */
    @Override
    public void onAdLoad(String placementId) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        mListener.onRewardedVideoAvailabilityChanged(true);
    }

    /**
     * Callback used to notify that an error has occurred while downloading assets. This indicates an unrecoverable error within the SDK, such as lack of network or out of disk space on the device.
     * @Params - placementId – The identifier for the placement for which the error occurred.
     * @Params - exception – exception which will usually be an instance of VungleException when the cause is known.
     */
    @Override
    public void onError(String placementId, VungleException exception) {
        IronLog.ADAPTER_CALLBACK.verbose("placementId = " + placementId + ", exception = " + exception);

        if (mListener == null) {
            IronLog.INTERNAL.verbose("listener is null");
            return;
        }

        IronSourceError error;
        if (exception.getExceptionCode() == VungleException.NO_SERVE) {
            error = new IronSourceError(IronSourceError.ERROR_RV_LOAD_NO_FILL, exception.getLocalizedMessage());
        } else {
            error = ErrorBuilder.buildLoadFailedError(exception.getLocalizedMessage());
        }

        mListener.onRewardedVideoAvailabilityChanged(false);
        mListener.onRewardedVideoLoadFailed(error);
    }
}