package com.ff.deliveryservice.mvp.view;

import android.database.Cursor;

import com.ff.deliveryservice.modules.navigation.adapter.OrderAdapter;

import java.util.concurrent.Callable;

/**
 * Created by Mark Khakimulin on 10.08.2018.
 * mark.khakimulin@gmail.com
 */
public interface OrderNavigationView extends FPTRView {

    void onFilter(Cursor cursor);
    void applyFilter();
    void showProgress();
    void hideProgress();
    void onShowCursorDialog(String title, Cursor cursor, final Callable<Void> positiveCallback, final Callable<Void> negativeCallback);
}
