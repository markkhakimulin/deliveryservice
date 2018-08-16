package com.ff.deliveryservice.mvp.view;

import java.util.concurrent.Callable;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */

public interface BaseView {

    void onShowDialog(String message);
    void onHideDialog();
    void onShowToast(String message);
    void showYesNoMessageDialog(String message,final Callable<Void> positiveCallback);
    void showYesNoMessageDialog(String title, String message,final Callable<Void> positiveCallback, final Callable negativeCallback);
}
