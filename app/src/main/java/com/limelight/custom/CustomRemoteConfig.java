package com.limelight.custom;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvHTTP;

public class CustomRemoteConfig {
    public static final String BRIDGE_METHOD_GET = "GET";
    public static final String BRIDGE_METHOD_POST = "POST";

    private static final String KEY_ENABLED = "custom_remote_enabled";
    private static final String KEY_HOST_NAME = "custom_remote_host_name";
    private static final String KEY_HOST_ADDRESS = "custom_remote_host_address";
    private static final String KEY_HTTP_PORT = "custom_remote_http_port";
    private static final String KEY_HTTPS_PORT = "custom_remote_https_port";
    private static final String KEY_MAC_ADDRESS = "custom_remote_mac_address";
    private static final String KEY_BRIDGE_ENABLED = "custom_remote_bridge_enabled";
    private static final String KEY_BRIDGE_URL = "custom_remote_bridge_url";
    private static final String KEY_BRIDGE_METHOD = "custom_remote_bridge_method";
    private static final String KEY_BRIDGE_TIMEOUT_MS = "custom_remote_bridge_timeout_ms";

    public static final String DEFAULT_HOST_NAME = "Custom Remote PC";
    public static final int DEFAULT_HTTP_PORT = NvHTTP.DEFAULT_HTTP_PORT;
    public static final int DEFAULT_HTTPS_PORT = 47984;
    public static final int DEFAULT_BRIDGE_TIMEOUT_MS = 10000;

    public final boolean enabled;
    public final String hostName;
    public final String hostAddress;
    public final int httpPort;
    public final int httpsPort;
    public final String macAddress;
    public final boolean bridgeEnabled;
    public final String bridgeUrl;
    public final String bridgeMethod;
    public final int bridgeTimeoutMs;

    private CustomRemoteConfig(SharedPreferences prefs) {
        enabled = prefs.getBoolean(KEY_ENABLED, false);
        hostName = getTrimmedString(prefs, KEY_HOST_NAME, DEFAULT_HOST_NAME);
        hostAddress = getTrimmedString(prefs, KEY_HOST_ADDRESS, "");
        httpPort = sanitizePort(prefs.getInt(KEY_HTTP_PORT, DEFAULT_HTTP_PORT), DEFAULT_HTTP_PORT);
        httpsPort = sanitizePort(prefs.getInt(KEY_HTTPS_PORT, DEFAULT_HTTPS_PORT), DEFAULT_HTTPS_PORT);
        macAddress = getTrimmedString(prefs, KEY_MAC_ADDRESS, "");
        bridgeEnabled = prefs.getBoolean(KEY_BRIDGE_ENABLED, false);
        bridgeUrl = getTrimmedString(prefs, KEY_BRIDGE_URL, "");
        bridgeMethod = sanitizeBridgeMethod(prefs.getString(KEY_BRIDGE_METHOD, BRIDGE_METHOD_GET));
        bridgeTimeoutMs = Math.max(1000, prefs.getInt(KEY_BRIDGE_TIMEOUT_MS, DEFAULT_BRIDGE_TIMEOUT_MS));
    }

    public static CustomRemoteConfig read(Context context) {
        return new CustomRemoteConfig(PreferenceManager.getDefaultSharedPreferences(context));
    }

    public static void write(Context context, boolean enabled, String hostName, String hostAddress,
                             int httpPort, int httpsPort, String macAddress, boolean bridgeEnabled,
                             String bridgeUrl, String bridgeMethod, int bridgeTimeoutMs) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putString(KEY_HOST_NAME, trimOrDefault(hostName, DEFAULT_HOST_NAME))
                .putString(KEY_HOST_ADDRESS, trimOrDefault(hostAddress, ""))
                .putInt(KEY_HTTP_PORT, sanitizePort(httpPort, DEFAULT_HTTP_PORT))
                .putInt(KEY_HTTPS_PORT, sanitizePort(httpsPort, DEFAULT_HTTPS_PORT))
                .putString(KEY_MAC_ADDRESS, trimOrDefault(macAddress, ""))
                .putBoolean(KEY_BRIDGE_ENABLED, bridgeEnabled)
                .putString(KEY_BRIDGE_URL, trimOrDefault(bridgeUrl, ""))
                .putString(KEY_BRIDGE_METHOD, sanitizeBridgeMethod(bridgeMethod))
                .putInt(KEY_BRIDGE_TIMEOUT_MS, Math.max(1000, bridgeTimeoutMs))
                .apply();
    }

    public boolean hasDirectHost() {
        return enabled && !hostAddress.isEmpty();
    }

    public boolean hasBridge() {
        return bridgeEnabled && !bridgeUrl.isEmpty();
    }

    public boolean hasMacAddress() {
        return !macAddress.isEmpty();
    }

    public ComputerDetails createComputerDetails() {
        ComputerDetails details = new ComputerDetails();
        details.name = hostName;
        details.manualAddress = new ComputerDetails.AddressTuple(hostAddress, httpPort);
        details.httpsPort = httpsPort;
        if (hasMacAddress()) {
            details.macAddress = macAddress;
        }
        return details;
    }

    private static String getTrimmedString(SharedPreferences prefs, String key, String defaultValue) {
        return trimOrDefault(prefs.getString(key, defaultValue), defaultValue);
    }

    private static String trimOrDefault(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value.trim();
    }

    private static int sanitizePort(int port, int defaultValue) {
        return port > 0 && port <= 65535 ? port : defaultValue;
    }

    private static String sanitizeBridgeMethod(String method) {
        if (BRIDGE_METHOD_POST.equalsIgnoreCase(method)) {
            return BRIDGE_METHOD_POST;
        }
        return BRIDGE_METHOD_GET;
    }
}
