package com.limelight.custom;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BridgeClient {
    private static final MediaType EMPTY_BODY_TYPE = MediaType.parse("application/json");

    public static String prepareHost(CustomRemoteConfig config) throws IOException {
        HttpUrl bridgeUrl = validateBridgeUrl(config);
        // TODO: For a production-critical self-hosted bridge, consider certificate pinning
        // (OkHttp CertificatePinner) or a dedicated trust policy to reduce MITM risk.
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(config.bridgeTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(config.bridgeTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(config.bridgeTimeoutMs, TimeUnit.MILLISECONDS)
                .build();

        Request.Builder requestBuilder = new Request.Builder().url(bridgeUrl);
        if (CustomRemoteConfig.BRIDGE_METHOD_POST.equals(config.bridgeMethod)) {
            requestBuilder.post(RequestBody.create("", EMPTY_BODY_TYPE));
        }
        else if (CustomRemoteConfig.BRIDGE_METHOD_GET.equals(config.bridgeMethod)) {
            requestBuilder.get();
        }
        else {
            throw new IOException("Método HTTP no permitido para bridge. Usa GET o POST.");
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Bridge returned HTTP " + response.code());
            }

            String body = response.body() != null ? response.body().string().trim() : "";
            return body.isEmpty() ? "Bridge OK (" + response.code() + ")" : body;
        }
        catch (SocketTimeoutException e) {
            throw new IOException("Timeout al conectar con bridge. Aumenta el timeout o verifica conectividad.", e);
        }
        catch (SSLException e) {
            throw new IOException("Fallo TLS con bridge. Verifica certificado/CA o usa un endpoint HTTPS válido.", e);
        }
    }

    private static HttpUrl validateBridgeUrl(CustomRemoteConfig config) throws IOException {
        if (config.bridgeUrl == null || config.bridgeUrl.trim().isEmpty()) {
            throw new IOException("URL de bridge vacía. Configura una URL HTTPS válida.");
        }

        HttpUrl parsed = HttpUrl.parse(config.bridgeUrl.trim());
        if (parsed == null && !config.bridgeUrl.contains("://")) {
            parsed = HttpUrl.parse("https://" + config.bridgeUrl.trim());
        }
        if (parsed == null) {
            throw new IOException("URL de bridge inválida. Ejemplo válido: https://bridge.example.com/ping");
        }

        if (!"https".equalsIgnoreCase(parsed.scheme())) {
            throw new IOException("TLS requerido: usa https:// en la URL del bridge.");
        }

        String host = parsed.host();
        if (host == null || host.trim().isEmpty()) {
            throw new IOException("URL de bridge inválida: host vacío.");
        }

        if (!config.bridgeAllowUnsafeTarget) {
            InetAddress resolved = InetAddress.getByName(host);
            if (resolved.isLoopbackAddress() || resolved.isLinkLocalAddress()) {
                throw new IOException("Host de bridge no permitido (loopback/link-local). Habilita el ajuste avanzado si necesitas usarlo.");
            }
        }

        return parsed;
    }
}
