package com.limelight.custom;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class NetworkProfileApplier {
    public enum Profile {
        HOME,
        FIVE_G,
        SAVER
    }

    private static final String RESOLUTION_PREF_STRING = "list_resolution";
    private static final String FPS_PREF_STRING = "list_fps";
    private static final String BITRATE_PREF_STRING = "seekbar_bitrate_kbps";
    private static final String VIDEO_FORMAT_PREF_STRING = "video_format";
    private static final String FRAME_PACING_PREF_STRING = "frame_pacing";

    public static void apply(Context context, Profile profile) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        switch (profile) {
            case HOME:
                editor.putString(RESOLUTION_PREF_STRING, "1920x1080")
                        .putString(FPS_PREF_STRING, "60")
                        .putInt(BITRATE_PREF_STRING, 50000)
                        .putString(VIDEO_FORMAT_PREF_STRING, "auto")
                        .putString(FRAME_PACING_PREF_STRING, "balanced");
                break;
            case FIVE_G:
                editor.putString(RESOLUTION_PREF_STRING, "1920x1080")
                        .putString(FPS_PREF_STRING, "60")
                        .putInt(BITRATE_PREF_STRING, 30000)
                        .putString(VIDEO_FORMAT_PREF_STRING, "forceh265")
                        .putString(FRAME_PACING_PREF_STRING, "latency");
                break;
            case SAVER:
                editor.putString(RESOLUTION_PREF_STRING, "1280x720")
                        .putString(FPS_PREF_STRING, "60")
                        .putInt(BITRATE_PREF_STRING, 10000)
                        .putString(VIDEO_FORMAT_PREF_STRING, "forceh265")
                        .putString(FRAME_PACING_PREF_STRING, "latency");
                break;
            default:
                throw new IllegalArgumentException("Unknown profile: " + profile);
        }

        editor.apply();
    }

    public static String getProfileSummary(Profile profile) {
        switch (profile) {
            case HOME:
                return "Casa: 1080p60, 50 Mbps, codec automatico, pacing equilibrado";
            case FIVE_G:
                return "5G: 1080p60, 30 Mbps, HEVC preferido, minima latencia";
            case SAVER:
                return "Ahorro: 720p60, 10 Mbps, HEVC preferido, minima latencia";
            default:
                return "";
        }
    }
}
