package com.jerryio.spoon.android.utils;

import com.jerryio.spoon.android.MainActivity;

public class InputHelper {
    public static void handle(String text) {
        MainActivity.getInstance().setClipboard(text);
    }

}
