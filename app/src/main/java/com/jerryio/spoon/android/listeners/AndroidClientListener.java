package com.jerryio.spoon.android.listeners;

import com.jerryio.spoon.android.MainActivity;
import com.jerryio.spoon.android.SpoonAndroid;
import com.jerryio.spoon.android.enums.ConnectionStatus;
import com.jerryio.spoon.android.utils.InputHelper;
import com.jerryio.spoon.kernal.client.ClientListener;
import com.jerryio.spoon.kernal.event.EventHandler;
import com.jerryio.spoon.kernal.event.client.ClientErrorEvent;
import com.jerryio.spoon.kernal.event.client.ConnectionCloseEvent;
import com.jerryio.spoon.kernal.event.client.ConnectionOpenEvent;
import com.jerryio.spoon.kernal.network.protocol.general.SendTextPacket;
import com.jerryio.spoon.kernal.network.protocol.handshake.RequireEncryptionPacket;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class AndroidClientListener extends ClientListener {

    @Override
    @EventHandler
    public void handleTextReceived(SendTextPacket packet) throws IOException {
        InputHelper.handle(packet.getMessage());
    }

    @Override
    @EventHandler
    public void lastMessageReceivedEvent() {
        MainActivity.getInstance().setInputText("");
    }

    @Override
    @EventHandler
    public void handleRequireEncryption(RequireEncryptionPacket packet) throws GeneralSecurityException, IOException {
        super.handleRequireEncryption(packet);
        SpoonAndroid.getInstance().setChannel(MainActivity.getInstance().getInputChannel());
        SpoonAndroid.getInstance().setConnectionStatus(ConnectionStatus.ClientConnected);
    }

    @Override
    @EventHandler
    public void onOpenEvent(ConnectionOpenEvent event) {
        SpoonAndroid.getInstance().setConnectionStatus(ConnectionStatus.ClientHandshake);
    }

    @Override
    @EventHandler
    public void onCloseEvent(ConnectionCloseEvent event) {
        SpoonAndroid.getInstance().setConnectionStatus(ConnectionStatus.ClientDisconnected);
    }

    @EventHandler
    public void onError(ClientErrorEvent event) {
        event.getException().printStackTrace();
    }
}
