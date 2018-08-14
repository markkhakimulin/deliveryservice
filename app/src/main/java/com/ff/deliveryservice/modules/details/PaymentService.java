package com.ff.deliveryservice.modules.details;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.modules.fptr.FPTRService;
import com.ff.deliveryservice.mvp.model.OrderData;

import java.util.HashMap;

import ru.atol.drivers10.fptr.Fptr;
import ru.atol.drivers10.fptr.IFptr;
import ru.atol.drivers10.fptr.settings.SettingsActivity;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class PaymentService extends FPTRService {

    //request
    public static final String ACTION_CHECK_CONNECT_REQUEST = "com.ff.deliveryservice.service.action.PAYMENT_REQUEST";
    //responses
    public static final String ACTION_CHECK_CONNECT_RESPONSE = "com.ff.deliveryservice.service.action.PAYMENT_RESPONSE";

    public PaymentService(OrderData orderData) {
        super("PaymentService");

    }
    @Override
    public void onCreate() {

        super.onCreate();

    }


    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && errorMessage.isEmpty()) {
            final String action = intent.getAction();
            if (ACTION_OFD_REPORT_REQUEST.equals(action)) {
                handleActionPayment();
            }
        }

    }

    private void handleActionPayment(int chequeType,int paymentType,) {

    }

}
