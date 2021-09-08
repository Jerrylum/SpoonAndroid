package com.jerryio.spoon.android.listeners;

import com.jerryio.spoon.android.MainActivity;
import com.jerryio.spoon.android.SpoonAndroid;
import com.jerryio.spoon.android.enums.ConnectionStatus;
import com.jerryio.spoon.android.utils.InputHelper;
import com.jerryio.spoon.kernal.event.EventHandler;
import com.jerryio.spoon.kernal.event.server.ClientConnectedEvent;
import com.jerryio.spoon.kernal.event.server.ClientDisconnectedEvent;
import com.jerryio.spoon.kernal.event.server.ServerErrorEvent;
import com.jerryio.spoon.kernal.network.protocol.general.SendTextPacket;
import com.jerryio.spoon.kernal.network.protocol.handshake.EncryptionBeginPacket;
import com.jerryio.spoon.kernal.server.RemoteDevice;
import com.jerryio.spoon.kernal.server.ServerListener;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class AndroidServerListener extends ServerListener {
    @Override
    @EventHandler
    public void handleTextReceived(RemoteDevice device, SendTextPacket packet) throws IOException {
        InputHelper.handle(packet.getMessage());
    }

    @Override
    @EventHandler
    public void handleClientAESkey(RemoteDevice device, EncryptionBeginPacket packet) throws GeneralSecurityException {
        super.handleClientAESkey(device, packet);
        MainActivity.getInstance().updateInterface();
    }

    @Override
    @EventHandler
    public void onClientConnectedEvent(ClientConnectedEvent event) {
        if (!SpoonAndroid.getInstance().isAllowNewConnection())
            event.getDevice().getConnection().getWebSocket().close();

        MainActivity.getInstance().updateInterface();
    }

    @Override
    @EventHandler
    public void onClientDisconnectedEvent(ClientDisconnectedEvent event) {
        MainActivity.getInstance().updateInterface();
    }

    @EventHandler
    public void onError(ServerErrorEvent event) {
        if (!event.isSocketProblem()) {
            SpoonAndroid.getInstance().server = null;
            SpoonAndroid.getInstance().setConnectionStatus(ConnectionStatus.ServerStopped);
        }
    }
}
