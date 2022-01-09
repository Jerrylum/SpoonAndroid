package com.jerryio.spoon.android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jerryio.spoon.android.enums.ConnectionMode;
import com.jerryio.spoon.android.enums.ConnectionStatus;
import com.jerryio.spoon.android.utils.Util;
import com.jerryio.spoon.android.views.InstantAutoComplete;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak") // XXX: IGNORE: set to *null* in *onDestroy* method
    private static MainActivity instance; // singleton

    private InstantAutoComplete inputAddress;
    private EditText inputChannel;
    private EditText inputMessage;
    private Switch switchMode;
    private Button btnAction;
    private TextView tvServerIP;
    private ConstraintLayout layoutServerControl;
    private ConstraintLayout layoutClientControl;
    private Button btnAcceptConn;
    private Button btnKickClients;
    private LinearLayout layoutSecurityPanel;
    private TextView tvSecurityCode;

    private String[] cacheProvidedAddress;

    private Handler handler;
    private ClipboardManager clipboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;


        inputAddress = findViewById(R.id.inputAddress);
        inputChannel = findViewById(R.id.inputChannel);
        inputMessage = findViewById(R.id.inputMessage);
        switchMode = findViewById(R.id.switchMode);
        btnAction = findViewById(R.id.btnAction);
        tvServerIP = findViewById(R.id.tvServerIP);
        layoutServerControl = findViewById(R.id.layoutServerControl);
        layoutClientControl = findViewById(R.id.layoutClientControl);
        btnAcceptConn = findViewById(R.id.btnAcceptConn);
        btnKickClients = findViewById(R.id.btnKickClients);
        layoutSecurityPanel = findViewById(R.id.layoutSecurityPanel);
        tvSecurityCode = findViewById(R.id.tvSecurityCode);

        inputChannel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                SpoonAndroid.getInstance().setChannel(s.toString());
            }
        });

        handler = new Handler(Looper.getMainLooper());
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        SpoonAndroid.build();
        updateInterface();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SpoonAndroid.destroy();
//        instance = null;
    }

    public static MainActivity getInstance() {
        return instance;
    }

    public void updateInterface() {
        handler.post(this::doUpdateInterface);
    }

    private void doUpdateInterface() {
        SpoonAndroid desktop = SpoonAndroid.getInstance();
        boolean isClient = desktop.getMode() == ConnectionMode.CLIENT;

        if (isClient)
            setServerIpTextView("");
        else
            new Util.UpdateServerIPTask().execute();

        switchMode.setText(isClient ? "Client Mode" : "Server Mode");
        switchMode.setChecked(isClient);

        String[] newProvidedAddress = desktop.getProvidedAddress();
        if (!Arrays.equals(cacheProvidedAddress, newProvidedAddress)) {
            cacheProvidedAddress = newProvidedAddress;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, newProvidedAddress);
            inputAddress.setAdapter(adapter);
        }

        String actionBtnText = "Unknown";
        switch (desktop.getConnectionStatus()) {
            case ClientReadyToConnect:
                actionBtnText = "Connect";
                break;
            case ClientConnecting:
                actionBtnText = "Connecting";
                break;
            case ClientConnectionFailed:
                actionBtnText = "Failed";
                break;
            case ClientHandshake:
                actionBtnText = "Handshake";
                break;
            case ClientConnected:
                actionBtnText = "Ready"; // different from SpoonDesktop
                break;
            case ClientDisconnected:
                actionBtnText = "Disconnected";
                break;
            case ServerReadyToStart:
                actionBtnText = "Start";
                break;
            case ServerStarting:
                actionBtnText = "Starting";
                break;
            case ServerStartFailed:
                actionBtnText = "Failed";
                break;
            case ServerRunning:
                actionBtnText = "Running";
                break;
            case ServerStopped:
                actionBtnText = "Stopped";
                break;
            default:
                break;
        }
        btnAction.setText(actionBtnText);

        String channelReal = desktop.getChannel();
        if (channelReal != null && !channelReal.equals("") && !channelReal.equals(getInputChannel()))
            inputChannel.setText(channelReal);

        layoutServerControl.setVisibility(isClient ? View.GONE : View.VISIBLE);
        layoutClientControl.setVisibility(isClient ? View.VISIBLE : View.GONE);

        btnAcceptConn.setText(desktop.isAllowNewConnection() ? "Accept Conn" : "Locked");
        btnKickClients.setText(MessageFormat.format("Kick {0} Client{1}", desktop.getClientsCount(), desktop.getClientsCount() > 1 ? "s" : ""));

        byte[] codeInbytes = desktop.getSecurityCode();
        if (codeInbytes == null) {
            layoutSecurityPanel.setVisibility(View.GONE);
        } else {
            String security_code = Util.bytesToHex(codeInbytes);

            StringBuilder display = new StringBuilder();

            for (int i = 0; i < security_code.length(); i++) {
                if (i != 0) {
                    if (i % 16 == 0)
                        display.append('\n');
                    else if (i % 4 == 0)
                        display.append(' ');
                }

                display.append(security_code.charAt(i));

            }

            layoutSecurityPanel.setVisibility(View.VISIBLE);
            tvSecurityCode.setText(display.toString());
        }
    }

    ////////////////////////////////////////////////
    //////////////////////EVENT/////////////////////
    ////////////////////////////////////////////////

    public void onSelectAddress(View view) {
        setInputAddress("");
        inputAddress.showDropDown();
    }

    public void onModeSwitch(View view) {
        ConnectionMode mode = ((Switch) view).isChecked() ? ConnectionMode.CLIENT : ConnectionMode.SERVER;
        SpoonAndroid.getInstance().setMode(mode);
    }

    public void onRandomChannel(View view) {
        Random random = new Random();
        setInputChannel(MessageFormat.format("{0}{1}{2}{3}{4}{5}",
                random.nextInt(10),
                random.nextInt(10),
                random.nextInt(10),
                random.nextInt(10),
                random.nextInt(10),
                random.nextInt(10)));
    }

    public void onExecuteAction(View view) {
        SpoonAndroid.getInstance().executeAction();
    }

    public void onAllowConnect(View view) {
        SpoonAndroid.getInstance().setIsAllowNewConnection(!SpoonAndroid.getInstance().isAllowNewConnection());
    }

    public void onKickClient(View view) {
        SpoonAndroid.getInstance().kickAllClients();
    }

    public void onTrimSpace(View view) {
        SpoonAndroid.getInstance().setIsTrimSpace(((CheckBox)view).isChecked());
    }

    public void onInsertSymbol(View view) {
        inputMessage.getText().insert(inputMessage.getSelectionStart(), ((Button)view).getText());
    }

    public void onSend(View view) {
        doSendUserInput();
    }

    ////////////////////////////////////////////////
    ///////////////////////API//////////////////////
    ////////////////////////////////////////////////

    public String getInputAddress() {
        return inputAddress.getText().toString();
    }

    public String getInputChannel() {
        return inputChannel.getText().toString();
    }

    public void setInputText(String msg) {
        handler.post(() -> {
            inputMessage.setText(msg);
        });
    }

    public void setInputAddress(String ip) {
        handler.post(() -> {
            inputAddress.setText(ip);
        });
    }

    public void setInputChannel(String channel) {
        handler.post(() -> {
            inputChannel.setText(channel);
        });
    }

    public void setServerIpTextView(String ip) {
        handler.post(() -> {
            SpoonAndroid desktop = SpoonAndroid.getInstance();
            boolean isClient = desktop.getMode() == ConnectionMode.CLIENT;

            tvServerIP.setText(isClient ? "" : ip);
        });
    }

    public SharedPreferences getConfig() {
        return getPreferences(0);
    }

    public void setClipboard(String msg) {
        ClipData clip = ClipData.newPlainText("Copied Text", msg);
        clipboard.setPrimaryClip(clip);
    }

    public void showToast(String msg) {
        handler.post(() -> {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });
    }

    private void doSendUserInput() {
        SpoonAndroid desktop = SpoonAndroid.getInstance();
        String raw = inputMessage.getText().toString();

        if (raw.isEmpty())
            return;

        if (SpoonAndroid.getInstance().isTrimSpace())
            raw = Util.trimSpace(raw);

        desktop.sendMessage(raw);

        if (desktop.getConnectionStatus() == ConnectionStatus.ServerRunning && desktop.getClientsCount() != 0) {
            setInputText("");
        }

        inputMessage.requestFocus();
    }
}