package com.limelight.custom;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BridgeClient {
    private static final MediaType EMPTY_BODY_TYPE = MediaType.parse("application/json");

    public static String prepareHost(CustomRemoteConfig config) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(config.bridgeTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(config.bridgeTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(config.bridgeTimeoutMs, TimeUnit.MILLISECONDS)
                .build();

        Request.Builder requestBuilder = new Request.Builder().url(config.bridgeUrl);
        if (CustomRemoteConfig.BRIDGE_METHOD_POST.equals(config.bridgeMethod)) {
            requestBuilder.post(RequestBody.create("", EMPTY_BODY_TYPE));
        }
        else {
            requestBuilder.get();
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Bridge returned HTTP " + response.code());
            }

            String body = response.body() != null ? response.body().string().trim() : "";
            return body.isEmpty() ? "Bridge OK (" + response.code() + ")" : body;
        }
    }
}
