package io.norselibs.heimdal;

/**
 * Project-level Heimdal configuration. Set once at application startup.
 *
 * <pre>
 * // In app startup, before any forms are built:
 * HeimdallConfig.setDefaultUpdateTrigger(UpdateTrigger.BLUR); // for slower backends
 * </pre>
 */
public class HeimdallConfig {

    private static UpdateTrigger defaultUpdateTrigger = UpdateTrigger.CHANGE;

    public static void setDefaultUpdateTrigger(UpdateTrigger trigger) {
        defaultUpdateTrigger = trigger;
    }

    static UpdateTrigger defaultUpdateTrigger() {
        return defaultUpdateTrigger;
    }

    private HeimdallConfig() {}
}
