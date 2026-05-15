package com.limelight.custom;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.shared.network.StandardWolSender;
import com.limelight.utils.UiHelper;

import java.io.IOException;

public class CustomRemotePanelActivity extends Activity {
    private CheckBox enableDirectHost;
    private EditText hostName;
    private EditText hostAddress;
    private EditText httpPort;
    private EditText httpsPort;
    private EditText macAddress;
    private CheckBox enableBridge;
    private EditText bridgeUrl;
    private RadioGroup bridgeMethod;
    private EditText bridgeTimeout;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_remote_panel);
        UiHelper.notifyNewRootView(this);

        enableDirectHost = findViewById(R.id.customEnableDirectHost);
        hostName = findViewById(R.id.customHostName);
        hostAddress = findViewById(R.id.customHostAddress);
        httpPort = findViewById(R.id.customHttpPort);
        httpsPort = findViewById(R.id.customHttpsPort);
        macAddress = findViewById(R.id.customMacAddress);
        enableBridge = findViewById(R.id.customEnableBridge);
        bridgeUrl = findViewById(R.id.customBridgeUrl);
        bridgeMethod = findViewById(R.id.customBridgeMethod);
        bridgeTimeout = findViewById(R.id.customBridgeTimeout);
        statusText = findViewById(R.id.customStatusText);

        Button saveButton = findViewById(R.id.customSaveButton);
        Button prepareButton = findViewById(R.id.customPrepareButton);
        Button wolButton = findViewById(R.id.customWolButton);
        Button homeProfileButton = findViewById(R.id.customHomeProfileButton);
        Button fiveGProfileButton = findViewById(R.id.customFiveGProfileButton);
        Button saverProfileButton = findViewById(R.id.customSaverProfileButton);

        loadConfig();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveConfig();
                setStatus(getString(R.string.custom_status_saved));
            }
        });

        prepareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveConfig();
                preparePc();
            }
        });

        wolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveConfig();
                sendWakeOnLan();
            }
        });

        homeProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyProfile(NetworkProfileApplier.Profile.HOME);
            }
        });
        fiveGProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyProfile(NetworkProfileApplier.Profile.FIVE_G);
            }
        });
        saverProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyProfile(NetworkProfileApplier.Profile.SAVER);
            }
        });
    }

    private void loadConfig() {
        CustomRemoteConfig config = CustomRemoteConfig.read(this);
        enableDirectHost.setChecked(config.enabled);
        hostName.setText(config.hostName);
        hostAddress.setText(config.hostAddress);
        httpPort.setText(String.valueOf(config.httpPort));
        httpsPort.setText(String.valueOf(config.httpsPort));
        macAddress.setText(config.macAddress);
        enableBridge.setChecked(config.bridgeEnabled);
        bridgeUrl.setText(config.bridgeUrl);
        bridgeTimeout.setText(String.valueOf(config.bridgeTimeoutMs));
        bridgeMethod.check(CustomRemoteConfig.BRIDGE_METHOD_POST.equals(config.bridgeMethod) ?
                R.id.customBridgePost : R.id.customBridgeGet);
    }

    private void saveConfig() {
        CustomRemoteConfig.write(this,
                enableDirectHost.isChecked(),
                hostName.getText().toString(),
                hostAddress.getText().toString(),
                parseInt(httpPort, CustomRemoteConfig.DEFAULT_HTTP_PORT),
                parseInt(httpsPort, CustomRemoteConfig.DEFAULT_HTTPS_PORT),
                macAddress.getText().toString(),
                enableBridge.isChecked(),
                bridgeUrl.getText().toString(),
                bridgeMethod.getCheckedRadioButtonId() == R.id.customBridgePost ?
                        CustomRemoteConfig.BRIDGE_METHOD_POST : CustomRemoteConfig.BRIDGE_METHOD_GET,
                parseInt(bridgeTimeout, CustomRemoteConfig.DEFAULT_BRIDGE_TIMEOUT_MS));
    }

    private void preparePc() {
        final CustomRemoteConfig config = CustomRemoteConfig.read(this);
        if (!config.hasBridge()) {
            setStatus(getString(R.string.custom_status_bridge_not_configured));
            return;
        }

        setStatus(getString(R.string.custom_status_bridge_pending));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String response = BridgeClient.prepareHost(config);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setStatus(getString(R.string.custom_status_bridge_success, response));
                        }
                    });
                } catch (final IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setStatus(getString(R.string.custom_status_bridge_error, e.getMessage()));
                        }
                    });
                }
            }
        }).start();
    }

    private void sendWakeOnLan() {
        final CustomRemoteConfig config = CustomRemoteConfig.read(this);
        if (!config.hasDirectHost() || !config.hasMacAddress()) {
            setStatus(getString(R.string.custom_status_wol_missing));
            return;
        }

        setStatus(getString(R.string.wol_waking_pc));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ComputerDetails details = config.createComputerDetails();
                    StandardWolSender.INSTANCE.sendMagicPacket(details.macAddress, "255.255.255.255", 9);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setStatus(getString(R.string.wol_waking_msg));
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setStatus(getString(R.string.custom_status_wol_error, e.getMessage()));
                        }
                    });
                }
            }
        }).start();
    }

    private void applyProfile(NetworkProfileApplier.Profile profile) {
        NetworkProfileApplier.apply(this, profile);
        String summary = NetworkProfileApplier.getProfileSummary(profile);
        setStatus(summary);
        Toast.makeText(this, summary, Toast.LENGTH_SHORT).show();
    }

    private int parseInt(EditText editText, int defaultValue) {
        try {
            return Integer.parseInt(editText.getText().toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void setStatus(String message) {
        statusText.setText(message);
    }
}
