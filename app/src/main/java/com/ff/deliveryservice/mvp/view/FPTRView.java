package com.ff.deliveryservice.mvp.view;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.concurrent.Callable;

/**
 * Created by Mark Khakimulin on 03.08.2018.
 * mark.khakimulin@gmail.com
 */
public interface FPTRView extends BaseView {

    void onShowProgressDialog(int min,int max,String message);
    void onHideProgressDialog();

}
