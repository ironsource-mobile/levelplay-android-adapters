package com.ironsource.adapters.vungle;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ironsource.mediationsdk.logger.IronLog;
import com.vungle.ads.InitializationListener;
import com.vungle.ads.Plugin;
import com.vungle.ads.Plugin.WrapperFramework;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleException;
import com.vungle.ads.VungleSettings;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class VungleInitializer implements InitializationListener {

    private static final VungleInitializer instance = new VungleInitializer();
    private final AtomicBoolean isInitializing = new AtomicBoolean(false);
    private final ArrayList<VungleInitializationListener> initListeners;

    @NonNull
    public static VungleInitializer getInstance() {
        return instance;
    }

    private VungleInitializer() {
        initListeners = new ArrayList<>();
        Plugin.addWrapperInfo(
                WrapperFramework.ironsource,
                com.ironsource.adapters.vungle.BuildConfig.VERSION_NAME.replace('.', '_'));
    }

    public void initialize(
            final @NonNull String appId,
            final @NonNull Context context,
            @NonNull VungleInitializationListener listener) {

        if (VungleAds.isInitialized()) {
            listener.onInitializeSuccess();
            return;
        }

        if (isInitializing.getAndSet(true)) {
            initListeners.add(listener);
            return;
        }

        IronLog.ADAPTER_API.verbose("appId = " + appId);

        VungleAds.init(context, appId, this, new VungleSettings());
        initListeners.add(listener);
    }

    @Override
    public void onSuccess() {
        IronLog.ADAPTER_CALLBACK.verbose("Succeeded to initialize SDK");

        for (VungleInitializationListener listener : initListeners) {
            listener.onInitializeSuccess();
        }

        initListeners.clear();
        isInitializing.set(false);
    }

    @Override
    public void onError(@NonNull VungleException exception) {
        IronLog.ADAPTER_CALLBACK.verbose(
                "Failed to initialize SDK - " + exception.getLocalizedMessage());

        for (VungleInitializationListener listener : initListeners) {
            listener.onInitializeError(exception.getLocalizedMessage());
        }

        initListeners.clear();
        isInitializing.set(false);
    }

    public interface VungleInitializationListener {

        void onInitializeSuccess();

        void onInitializeError(String error);
    }
}
