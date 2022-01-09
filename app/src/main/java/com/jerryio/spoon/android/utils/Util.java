package com.jerryio.spoon.android.utils;

import android.os.AsyncTask;

import com.jerryio.spoon.android.MainActivity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static byte[] getMD5Checksum(byte[] a, byte[] b) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(a);
        md.update(b);
        return md.digest();
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String trimSpace(String origin) {
        StringBuilder builder = new StringBuilder();

        int p = 0;
        int q = 0;
        char c;
        char[] array = origin.toCharArray();

        for (int i = 0; i < array.length; i++) {
            c = array[i];
            p = i - 1 >= 0 ? array[i - 1] : 0;
            q = i + 1 < array.length ? array[i + 1] : 0;

            if (p > 255 && q > 255 && c == ' ') continue; // don't trim space between english words

            builder.append(c);
        }

        return builder.toString();
    }

    public static String getMyIP() {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("google.com", 80));
            String rtn = socket.getLocalAddress().getHostAddress();
            socket.close();
            return rtn;
        } catch (IOException e) {
            return "(error)";
        }
    }

    public static class UpdateServerIPTask extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... data) {
            MainActivity.getInstance().setServerIpTextView(getMyIP());
            return null;
        }

    }
}