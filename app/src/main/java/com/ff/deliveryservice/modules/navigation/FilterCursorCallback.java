package com.ff.deliveryservice.modules.navigation;

import android.content.Context;
import android.database.Cursor;

/**
 * Created by Mark Khakimulin on 10.08.2018.
 * mark.khakimulin@gmail.com
 */
public interface FilterCursorCallback {
    void applyCursor(Cursor cursor);
}