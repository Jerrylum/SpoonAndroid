package com.jerryio.spoon.android;

import android.content.SharedPreferences;

import com.jerryio.spoon.android.enums.ConnectionMode;
import com.jerryio.spoon.android.enums.ConnectionStatus;
import com.jerryio.spoon.android.listeners.AndroidClientListener;
import com.jerryio.spoon.android.listeners.AndroidServerListener;
import com.jerryio.spoon.android.utils.Util;
import com.jerryio.spoon.kernal.client.ClientDevice;
import com.jerryio.spoon.kernal.network.security.EncryptionManager;
import com.jerryio.spoon.kernal.server.RemoteDevice;
import com.jerryio.spoon.kernal.server.ServerDevice;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.SecretKey;

public class SpoonAndroid {

    private static SpoonAndroid instance; // singleton

    private ConnectionMode mode = ConnectionMode.CLIENT;
    private ConnectionStatus connectionStatus = ConnectionStatus.ClientReadyToConnect;
    private boolean isAllowNewConnection = true;
    private boolean isTrimSpace = true;

    public ServerDevice server;
    public ClientDevice client;

    public static void build() {
        if (instance != null)
            destroy();
        instance = new SpoonAndroid();
    }

    public static void destroy() {
        if (instance == null)
            return;
        instance.doServerStop();
        instance.doClientDisconnect();
//        instance = null; // cause bug
    }

    public SpoonAndroid() {
        doStartConnectionWatchdog();
    }

    public ConnectionMode getMode() {
        return this.mode;
    }

    public void setMode(ConnectionMode mode) {
        this.mode = mode;

        doServerStop();
        doClientDisconnect();

        if (getMode() == ConnectionMode.CLIENT) {
            connectionStatus = ConnectionStatus.ClientReadyToConnect;
        } else {
            connectionStatus = ConnectionStatus.ServerReadyToStart;
        }

        MainActivity.getInstance().setInputAddress(getDefaultAddress());

        MainActivity.getInstance().updateInterface();
    }

    public String[] getProvidedAddress() {
        Set<String> rtn = doGetPreviousAddresses();

        if (getMode() == ConnectionMode.CLIENT) {
            rtn.add("spoon-router1.jerryio.com");
            rtn.add("192.168.0.2");
        } else {
            rtn.add("0.0.0.0:7000");
        }
        return rtn.toArray(new String[0]);
    }

    public String getDefaultAddress() {
        SharedPreferences config = MainActivity.getInstance().getConfig();
        String lastAddress = config.getString("saved-last-address-" + getMode(), null);

        if (lastAddress != null) return lastAddress;

        if (getMode() == ConnectionMode.CLIENT) {
            return "spoon-router1.jerryio.com";
        } else {
            return "0.0.0.0:7000";
        }
    }

    public ConnectionStatus getConnectionStatus() {
        return this.connectionStatus;
    }

    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        boolean isClientRelated = connectionStatus.toString().toLowerCase().contains("client");
        boolean isClient = getMode() == ConnectionMode.CLIENT;

        if (isClientRelated == isClient) // ignore invalid status
            this.connectionStatus = connectionStatus;

        MainActivity.getInstance().updateInterface();
    }

    public String getChannel() {
        if (client != null)
            return client.getChannel();
        return null;
    }

    public void setChannel(String channel) {
        try {
            if (client != null && !client.getChannel().equals(channel)) {
                client.setChannel(channel);
            }
        } catch (Exception e) {
            MainActivity.getInstance().showToast("Failed to set channel");
        }
        MainActivity.getInstance().updateInterface();
    }

    public boolean isAllowNewConnection() {
        return this.isAllowNewConnection;
    }

    public void setIsAllowNewConnection(boolean isAllowNewConnection) {
        this.isAllowNewConnection = isAllowNewConnection;
        MainActivity.getInstance().updateInterface();
    }

    public boolean isTrimSpace() {
        return isTrimSpace;
    }

    public void setIsTrimSpace(boolean isTrimSpace) {
        this.isTrimSpace = isTrimSpace;
    }

    public int getClientsCount() {
        if (server != null)
            return server.getDevices().size();
        return 0;
    }

    public void kickAllClients() {
        try {
            if (server != null) {
                for (RemoteDevice device : server.getDevices()) {
                    device.getConnection().getWebSocket().close();
                }
            }
        } catch (Exception e) {
            MainActivity.getInstance().showToast("Failed to kick clients");
        }
        MainActivity.getInstance().updateInterface();
    }

    public void sendMessage(String msg) {
        try {
            if (server != null)
                server.sendTextMessage(msg);
            else if (client != null)
                client.sendTextMessage(msg);
        } catch (Exception e) {
            MainActivity.getInstance().showToast("Failed to send message");
        }
        MainActivity.getInstance().updateInterface();
    }

    public void executeAction() {
        switch (getConnectionStatus()) {
            case ClientReadyToConnect:
            case ClientConnectionFailed:
            case ClientDisconnected:
                doClientConnect();
                break;
            case ClientConnecting:
            case ClientHandshake:
            case ClientConnected:
                doClientDisconnect();
                break;
            case ServerReadyToStart:
            case ServerStartFailed:
            case ServerStopped:
                doServerStart();
                break;
            case ServerStarting:
            case ServerRunning:
                doServerStop();
                break;
            default:
                break;
        }

        MainActivity.getInstance().updateInterface();
    }

    public byte[] getSecurityCode() {
        RSAPublicKey a = null;
        SecretKey b = null;
        if (client != null) {
            EncryptionManager manager = client.getConnection().getEncryption();
            a = manager.getRSAPublicKey();
            b = manager.getAESSecretKey();
        } else if (server != null) {
            a = (RSAPublicKey) server.getKey().getPublic();

            RemoteDevice target = null;
            for (RemoteDevice device : server.getDevices())
                if (target == null || device.getId() > target.getId())
                    target = device;

            if (target == null)
                return null;
            b = target.getConnection().getEncryption().getAESSecretKey();
        }
        try {
            if (a != null && b != null)
                return Util.getMD5Checksum(a.getEncoded(), b.getEncoded());
        } catch (Exception ignored) {
        }
        return null;
    }

    private Set<String> doGetPreviousAddresses() {
        SharedPreferences config = MainActivity.getInstance().getConfig();
        return config.getStringSet("saved-addresses-" + getMode(), new HashSet<>());
    }

    private void doSaveAddress(String ip) {
        SharedPreferences config = MainActivity.getInstance().getConfig();
        SharedPreferences.Editor editor = config.edit();

        Set<String> data = config.getStringSet("saved-addresses-" + getMode(), new HashSet<>());
        data.add(ip);

        editor.putStringSet("saved-addresses-" + getMode(), data);
        editor.putString("saved-last-address-" + getMode(), ip);
        editor.apply();
    }

    private void doClientConnect() {
        try {
            String rawIP = MainActivity.getInstance().getInputAddress();
            String ip = rawIP;
            if (!ip.contains(":"))
                ip += ":7000";

            ip = "ws://" + ip;

            client = new ClientDevice(new URI(ip), new AndroidClientListener());
            connectionStatus = ConnectionStatus.ClientConnecting;

            doSaveAddress(rawIP);
        } catch (Exception e) {
            doClientDisconnect();
            connectionStatus = ConnectionStatus.ClientConnectionFailed;
        }
    }

    private void doClientDisconnect() {
        try {
            client.getConnection().close();
            connectionStatus = ConnectionStatus.ClientDisconnected;
        } catch (Exception ignored) {
        }
        client = null;
    }

    private void doServerStart() {
        try {
            String ip = MainActivity.getInstance().getInputAddress();
            String address = "0.0.0.0";
            int port = 7000;

            if (ip.contains(":")) {
                String[] s = ip.split(":");
                address = s[0];
                port = Integer.parseInt(s[1]);
            } else if (!ip.contains(".")) {
                port = Integer.parseInt(ip);
            } else {
                address = ip;
            }

            server = new ServerDevice(new InetSocketAddress(address, port), new AndroidServerListener());
            server.setReuseAddr(true);
            server.start();
            connectionStatus = ConnectionStatus.ServerRunning; // XXX: should be ServerStarting
        } catch (Exception e) {
            doServerStop();
            connectionStatus = ConnectionStatus.ServerStartFailed;
        }
    }

    private void doServerStop() {
        try {
            server.stop(1000);
            connectionStatus = ConnectionStatus.ServerStopped;
        } catch (Exception ignored) {
        }
        server = null;
    }

    private void doStartConnectionWatchdog() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SpoonAndroid desktop = SpoonAndroid.getInstance();
                MainActivity activity = MainActivity.getInstance();

                if (desktop.client == null || desktop.getConnectionStatus() == ConnectionStatus.ClientConnecting
                        || desktop.client.getConnection().isConnected())
                    return;

                try {
                    activity.showToast("Watchdog closed connection");
                    desktop.client.getConnection().close();
                } catch (Exception e) {
                    activity.showToast("Watchdog closed connection (error)");
                } finally {
                    desktop.client = null;
                    desktop.setConnectionStatus(ConnectionStatus.ClientDisconnected);
                }
            }
        }, 0, 500);
    }

    public static SpoonAndroid getInstance() {
        return instance;
    }
}
