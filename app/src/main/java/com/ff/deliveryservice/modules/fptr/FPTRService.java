package com.ff.deliveryservice.modules.fptr;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.mvp.model.ChequeData;
import com.ff.deliveryservice.mvp.model.DBHelper;

import java.util.ArrayList;
import java.util.HashMap;

import javax.inject.Inject;

import ru.atol.drivers10.fptr.Fptr;
import ru.atol.drivers10.fptr.IFptr;
import ru.atol.drivers10.fptr.settings.SettingsActivity;

import static ru.atol.drivers10.fptr.IFptr.LIBFPTR_PARAM_DOCUMENT_PRINTED;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FPTRService extends IntentService {


    @Inject
    protected DBHelper db;

    @Inject
    protected Resources res;

    HashMap<Byte,String> ofd_exchange_status = new HashMap<> ();

    public static final String ACTION_CHECK_CONNECT_REQUEST = "com.ff.deliveryservice.service.action.CHECK_CONNECT_REQUEST";
    public static final String ACTION_CHECK_CONNECT_OFD_REQUEST = "com.ff.deliveryservice.service.action.OFD_REPORT_REQUEST";
    public static final String ACTION_OFD_REPORT_REQUEST = "com.ff.deliveryservice.service.action.OFD_STATE_RESPONSE";
    public static final String ACTION_BATTERY_STATE_REQUEST = "com.ff.deliveryservice.service.action.BATTERY_STATE_REQUEST";
    public static final String ACTION_X_REPORT_REQUEST = "com.ff.deliveryservice.service.action.X_REPORT_REQUEST";
    public static final String ACTION_Z_REPORT_REQUEST = "com.ff.deliveryservice.service.action.Z_REPORT_REQUEST";
    public static final String ACTION_PAYMENT_REQUEST = "com.ff.deliveryservice.service.action.PAYMENT_REQUEST";

    // from service to activity


    public static final String ACTION_CHECK_CONNECT_RESPONSE = "com.ff.deliveryservice.service.action.CHECK_CONNECT_RESPONSE";
    public static final String ACTION_CHECK_CONNECT_OFD_RESPONSE = "com.ff.deliveryservice.service.action.OFD_STATE_RESPONSE";
    public static final String ACTION_OFD_REPORT_RESPONSE = "com.ff.deliveryservice.service.action.OFD_REPORT_RESPONSE";
    public static final String ACTION_BATTERY_STATE_RESPONSE = "com.ff.deliveryservice.service.action.BATTERY_STATE_RESPONSE";
    public static final String ACTION_X_REPORT_RESPONSE = "com.ff.deliveryservice.service.action.X_REPORT_RESPONSE";
    public static final String ACTION_Z_REPORT_RESPONSE = "com.ff.deliveryservice.service.action.Z_REPORT_RESPONSE";
    public static final String ACTION_PAYMENT_RESPONSE = "com.ff.deliveryservice.service.action.PAYMENT_RESPONSE";

    //progress
    public static final String EXTRA_PROGRESS = "com.ff.deliveryservice.service.extra.PROGRESS";
    public static final String EXTRA_MAX  = "com.ff.deliveryservice.service.extra.MAX";
    public static final String EXTRA_MESSAGE = "com.ff.deliveryservice.service.extra.MESSAGE";
    public static final String EXTRA_RESULT = "com.ff.deliveryservice.service.extra.RESULT";
    //battery
    public static final String EXTRA_CHARGE  = "com.ff.deliveryservice.service.extra.CHARGE";
    public static final String EXTRA_VOLTAGE       = "com.ff.deliveryservice.service.extra.VOLTAGE";
    public static final String EXTRA_USE_BATTERY   = "com.ff.deliveryservice.service.extra.USE_BATTERY";
    public static final String EXTRA_IS_CHARGING   = "com.ff.deliveryservice.service.extra.IS_CHARGING";
    public static final String EXTRA_CAN_PRINT       = "com.ff.deliveryservice.service.extra.CAN_PRINT ";
    //ofd
    public static final String EXTRA_EXCHANGE_STATUS        = "com.ff.deliveryservice.service.extra.EXCHANGE_STATUS";
    public static final String EXTRA_UNSENT_COUNT               = "com.ff.deliveryservice.service.extra.UNSENT_COUNT";
    public static final String EXTRA_FIRST_UNSENT_NUMBER      = "com.ff.deliveryservice.service.extra.FIRST_UNSENT_NUMBER ";
    public static final String EXTRA_OFD_MESSAGE_READ      = "com.ff.deliveryservice.service.extra.OFD_MESSAGE_READ";
    public static final String EXTRA_DATETIME                   = "com.ff.deliveryservice.service.extra.DATETIME ";
    //report Z
    public static final String EXTRA_USER_NAME = "com.ff.deliveryservice.service.extra.USER_NAME";
    public static final String EXTRA_USER_INN = "com.ff.deliveryservice.service.extra.USER_INN";

    protected IFptr fptr;
    protected SharedPreferences preferences;
    protected String errorMessage = "";
    byte status1 = 1;
    byte status2 = 2;
    byte status3 = 4;
    byte status4 = 8;
    byte status5 = 16;
    byte status6 = 32;

    @Inject
    public FPTRService() {
        this("FPTRService");

    }

    public FPTRService(String title) {
        super(title);

    }
    @Override
    public void onCreate() {


        fptr = new Fptr(getApplicationContext());

        preferences = getSharedPreferences(Constants.FPTR_PREFERENCES, Context.MODE_PRIVATE);

        ofd_exchange_status.put(status1,"транспортное соединение установлено");
        ofd_exchange_status.put(status2,"есть сообщение для передачи в ОФД");
        ofd_exchange_status.put(status3,"ожидание ответного сообщения (квитанции) от ОФД");
        ofd_exchange_status.put(status4,"есть команда от ОФД");
        ofd_exchange_status.put(status5,"изменились настройки соединения с ОФД");
        ofd_exchange_status.put(status6,"ожидание ответа на команду от ОФД");

        super.onCreate();

        DeliveryServiceApplication.getApplicationComponent().inject(this);


    }

    protected Intent getResponseIntent(String action,int progress,int max,String message,Boolean result) {
        Intent response = new Intent();
        response.setAction(action);
        if (progress > 0 && max > 0) {
            response.putExtra(EXTRA_PROGRESS, progress);
            response.putExtra(EXTRA_MAX, max);
        }
        response.putExtra(EXTRA_MESSAGE,message);
        response.putExtra(EXTRA_RESULT,result);
        return response;
    }
    private Intent getBatteryStateResponseIntent(int progress,int max) {
        Intent response = getResponseIntent(ACTION_BATTERY_STATE_RESPONSE,progress,max,getString(R.string.fptr_settings_ok),true);
        response.putExtra(EXTRA_CHARGE,fptr.getParamInt(IFptr.LIBFPTR_PARAM_BATTERY_CHARGE));
        response.putExtra(EXTRA_VOLTAGE,fptr.getParamDouble(IFptr.LIBFPTR_PARAM_VOLTAGE));
        response.putExtra(EXTRA_USE_BATTERY,fptr.getParamBool(IFptr.LIBFPTR_PARAM_USE_BATTERY));
        response.putExtra(EXTRA_IS_CHARGING,fptr.getParamBool(IFptr.LIBFPTR_PARAM_BATTERY_CHARGING));
        response.putExtra(EXTRA_CAN_PRINT,fptr.getParamBool(IFptr.LIBFPTR_PARAM_CAN_PRINT_WHILE_ON_BATTERY));
        return response;
    }
    private Intent getOFDStateResponseIntent(int progress,int max) {
        Intent response = getResponseIntent(ACTION_CHECK_CONNECT_OFD_RESPONSE,progress,max,getString(R.string.fptr_settings_ok),true);


        String statusDescription = "";

        int status = (int)fptr.getParamInt(IFptr.LIBFPTR_PARAM_OFD_EXCHANGE_STATUS);
        if ((status&status1) >= 0) {
            statusDescription = ofd_exchange_status.get(status1);
        };
        if ((status&status2) > 0) {
            statusDescription = ofd_exchange_status.get(status2);
        };
        if ((status&status3) > 0) {
            statusDescription = ofd_exchange_status.get(status3);
        };
        if ((status&status4) > 0) {
            statusDescription = ofd_exchange_status.get(status4);
        };
        if ((status&status5) > 0) {
            statusDescription = ofd_exchange_status.get(status5);
        };
        if ((status&status6) > 0) {
            statusDescription = ofd_exchange_status.get(status6);
        };

        response.putExtra(EXTRA_EXCHANGE_STATUS,statusDescription);
        response.putExtra(EXTRA_UNSENT_COUNT,fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENTS_COUNT));
        response.putExtra(EXTRA_FIRST_UNSENT_NUMBER,fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENT_NUMBER));
        response.putExtra(EXTRA_DATETIME,fptr.getParamDateTime(IFptr.LIBFPTR_PARAM_DATE_TIME).getTime());
        response.putExtra(EXTRA_OFD_MESSAGE_READ,fptr.getParamBool(IFptr.LIBFPTR_PARAM_OFD_MESSAGE_READ)?"Есть":"Нет");

        return response;
    }

    protected String getSettings() {
        return preferences.getString(SettingsActivity.DEVICE_SETTINGS, getDefaultSettings());
    }

    public static String getDefaultSettings() {
        @SuppressLint("DefaultLocale")
        String settings = String.format("{\"%s\": %d, \"%s\": %d, \"%s\": %d",
                IFptr.LIBFPTR_SETTING_MODEL, IFptr.LIBFPTR_MODEL_ATOL_11F,
                IFptr.LIBFPTR_SETTING_PORT, IFptr.LIBFPTR_PORT_BLUETOOTH,
                IFptr.LIBFPTR_SETTING_OFD_CHANNEL, IFptr.LIBFPTR_OFD_CHANNEL_PROTO);
        return settings;
    }

    protected void checkError() throws DriverException {
        int rc = fptr.errorCode();
        if (rc > 0) {
            String rd = fptr.errorDescription();
            throw new DriverException(String.format("[%d] %s ", rc, rd));
        }
    }

    public static class DriverException extends Exception {
        public DriverException(String msg) {
            super(msg);
        }
    }

    @Override
    public void onDestroy() {

        fptr.destroy();
        fptr = null;
        super.onDestroy();

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && errorMessage.isEmpty()) {
            final String action = intent.getAction();
            if (ACTION_CHECK_CONNECT_REQUEST.equals(action)) {
                handleActionCheckConnect();
            } else if (ACTION_CHECK_CONNECT_OFD_REQUEST.equals(action)) {
                handleActionCheckUnsentDocumentsOFD();
            } else if (ACTION_BATTERY_STATE_REQUEST.equals(action)) {
                handleActionBatteryState();
            } else if (ACTION_X_REPORT_REQUEST.equals(action)) {
                handleActionReportX();
            } else if (ACTION_Z_REPORT_REQUEST.equals(action)) {
                String userName = intent.getStringExtra(EXTRA_USER_NAME);
                String userINN = intent.getStringExtra(EXTRA_USER_INN);
                handleActionReportZ(userName,userINN);
            }else if (ACTION_OFD_REPORT_REQUEST.equals(action)) {
                handleActionReportOFD();
            } else if (ACTION_OFD_REPORT_REQUEST.equals(action)) {
                ChequeData chequeData = intent.getParcelableExtra(ChequeData.class.getCanonicalName());
                handleActionPayment(chequeData);
            }
        }

    }

    private void handleActionPayment(ChequeData chequeData) {

       /* mPaymentTypeCode = chequeData.getPaymentsType();
        mChequeType = chequeData.getChequeType();
        isReturnCheque = mChequeType == IFptr.LIBFPTR_RT_SELL_RETURN;
        mNotification = chequeData.getmNotification();*/

        errorMessage = "";
        try {

            if (fptr == null) {
                throw new DriverException("Не инициализирован драйвер устройства");
            }
            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_RESPONSE,1,4,getString(R.string.fptr_settings_set_settings),true));
            fptr.setSettings(getSettings());

            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_RESPONSE,2,4,getString(R.string.fptr_settings_set_connection),true));
            if (fptr.open() < 0) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_RESPONSE,3,4,getString(R.string.fptr_settings_check_connection),true));
            if (!fptr.isOpened()) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_RESPONSE,4,4,getString(R.string.fptr_settings_ok),true));

        } catch (DriverException e) {
            errorMessage = e.getMessage();
            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_RESPONSE,0,0,errorMessage,false));
        }
    }


    private void handleActionCheckConnect() {

        errorMessage = "";
        try {

            if (fptr == null) {
                throw new DriverException("Не инициализирован драйвер устройства");
            }
            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_RESPONSE,1,4,getString(R.string.fptr_settings_set_settings),true));
            fptr.setSettings(getSettings());

            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_RESPONSE,2,4,getString(R.string.fptr_settings_set_connection),true));
            if (fptr.open() < 0) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_RESPONSE,3,4,getString(R.string.fptr_settings_check_connection),true));
            if (!fptr.isOpened()) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_RESPONSE,4,4,getString(R.string.fptr_settings_ok),true));

        } catch (DriverException e) {
            errorMessage = e.getMessage();
            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_RESPONSE,0,0,errorMessage,false));
        }
    }

    private void handleActionBatteryState() {

        errorMessage = "";
        try {

            if (fptr == null) {
                throw new DriverException("Не инициализирован драйвер устройства");
            }

            sendBroadcast(getResponseIntent(ACTION_BATTERY_STATE_RESPONSE,1,5,getString(R.string.fptr_settings_set_settings),true));
            fptr.setSettings(getSettings());


            sendBroadcast(getResponseIntent(ACTION_BATTERY_STATE_RESPONSE,2,5,getString(R.string.fptr_settings_set_connection),true));
            if (fptr.open() < 0) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_BATTERY_STATE_RESPONSE,3,5,getString(R.string.fptr_settings_check_connection),true));
            if (!fptr.isOpened()) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_BATTERY_STATE_RESPONSE,4,5,getString(R.string.fptr_settings_check_connection),true));

            fptr.setParam(IFptr.LIBFPTR_PARAM_DATA_TYPE, IFptr.LIBFPTR_DT_POWER_SOURCE_STATE);
            fptr.setParam(IFptr.LIBFPTR_PARAM_POWER_SOURCE_TYPE, IFptr.LIBFPTR_PST_BATTERY);
            fptr.queryData();

            sendBroadcast(getBatteryStateResponseIntent(5,5));

        } catch (DriverException e) {
            errorMessage = e.getMessage();
            sendBroadcast(getResponseIntent(ACTION_BATTERY_STATE_RESPONSE,0,0,errorMessage,false));
        }
    }

    private void handleActionCheckUnsentDocumentsOFD() {

        errorMessage = "";
        try {

            if (fptr == null) {
                throw new DriverException("Не инициализирован драйвер устройства");
            }
            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_OFD_RESPONSE,1,5,getString(R.string.fptr_settings_set_settings),true));
            fptr.setSettings(getSettings());

            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_OFD_RESPONSE,2,5,getString(R.string.fptr_settings_set_connection),true));
            if (fptr.open() < 0) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_OFD_RESPONSE,3,5,getString(R.string.fptr_settings_check_connection),true));
            if (!fptr.isOpened()) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_OFD_RESPONSE,4,5,getString(R.string.fptr_settings_check_ofd_state),true));

            fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_OFD_EXCHANGE_STATUS);
            fptr.fnQueryData();

            sendBroadcast(getOFDStateResponseIntent(5,5));

        } catch (DriverException e) {
            errorMessage = e.getMessage();
            sendBroadcast(getResponseIntent(ACTION_CHECK_CONNECT_OFD_RESPONSE,0,0,errorMessage,false));
        }
    }

    private void handleActionReportOFD() {

        errorMessage = "";
        try {

            if (fptr == null) {
                throw new DriverException("Не инициализирован драйвер устройства");
            }
            sendBroadcast(getResponseIntent(ACTION_OFD_REPORT_RESPONSE,1,5,getString(R.string.fptr_settings_set_settings),true));
            fptr.setSettings(getSettings());

            sendBroadcast(getResponseIntent(ACTION_OFD_REPORT_RESPONSE,2,5,getString(R.string.fptr_settings_set_connection),true));
            if (fptr.open() < 0) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_OFD_REPORT_RESPONSE,3,5,getString(R.string.fptr_settings_check_connection),true));
            if (!fptr.isOpened()) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_OFD_REPORT_RESPONSE,4,5,getString(R.string.fptr_settings_report_OFD),true));

            fptr.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_OFD_TEST);
            fptr.report();

            sendBroadcast(getResponseIntent(ACTION_OFD_REPORT_RESPONSE,5,5,getString(R.string.fptr_settings_ok),true));

        } catch (DriverException e) {
            errorMessage = e.getMessage();
            sendBroadcast(getResponseIntent(ACTION_OFD_REPORT_RESPONSE,0,0,errorMessage,false));
        }
    }

    private void handleActionReportX() {

        errorMessage = "";
        try {

            if (fptr == null) {
                throw new DriverException("Не инициализирован драйвер устройства");
            }
            sendBroadcast(getResponseIntent(ACTION_X_REPORT_RESPONSE,1,5,getString(R.string.fptr_settings_set_settings),true));
            fptr.setSettings(getSettings());

            sendBroadcast(getResponseIntent(ACTION_X_REPORT_RESPONSE,2,5,getString(R.string.fptr_settings_set_connection),true));
            if (fptr.open() < 0) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_X_REPORT_RESPONSE,3,5,getString(R.string.fptr_settings_check_connection),true));
            if (!fptr.isOpened()) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_X_REPORT_RESPONSE,4,5,getString(R.string.fptr_settings_set_x_report),true));
            fptr.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_X);
            if (fptr.report()<0) {
                checkError();
            }

            sendBroadcast(getResponseIntent(ACTION_X_REPORT_RESPONSE,5,5,getString(R.string.fptr_settings_ok),true));

        } catch (DriverException e) {
            errorMessage = e.getMessage();
            sendBroadcast(getResponseIntent(ACTION_X_REPORT_RESPONSE,0,0,errorMessage,false));
        }
    }

    private void handleActionReportZ(String userName,String userINN) {

        if (userName == null)

        errorMessage = "";
        try {

            if (fptr == null) {
                throw new DriverException("Не инициализирован драйвер устройства");
            }
            sendBroadcast(getResponseIntent(ACTION_Z_REPORT_RESPONSE,1,7,getString(R.string.fptr_settings_set_settings),true));
            fptr.setSettings(getSettings());

            sendBroadcast(getResponseIntent(ACTION_Z_REPORT_RESPONSE,2,7,getString(R.string.fptr_settings_set_connection),true));
            if (fptr.open() < 0) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_Z_REPORT_RESPONSE,3,7,getString(R.string.fptr_settings_check_connection),true));
            if (!fptr.isOpened()) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_Z_REPORT_RESPONSE,4,7,getString(R.string.fptr_settings_set_operator),true));
            if (!TextUtils.isEmpty(userName))
            fptr.setParam(1021, userName);
            if (!TextUtils.isEmpty(userINN))
            fptr.setParam(1203, userINN);
            if (!TextUtils.isEmpty(userName) || !TextUtils.isEmpty(userINN))
            fptr.operatorLogin();

            sendBroadcast(getResponseIntent(ACTION_Z_REPORT_RESPONSE,5,7,getString(R.string.fptr_settings_set_z_report),true));
            fptr.setParam(IFptr.LIBFPTR_PARAM_REPORT_TYPE, IFptr.LIBFPTR_RT_CLOSE_SHIFT);
            if (fptr.report()<0) {
                checkError();
            }

            sendBroadcast(getResponseIntent(ACTION_Z_REPORT_RESPONSE,6,7,getString(R.string.fptr_settings_check_document_closed),true));

            if (fptr.checkDocumentClosed() < 0) {
                // Не удалось проверить состояние документа. Вывести пользователю текст ошибки, попросить устранить неполадку и повторить запрос
                throw  new DriverException(String.format("Не удалось закрыть смену (Ошибка \"%s\"). Устраните неполадку и повторите.", fptr.errorDescription()));
            }

            if (!fptr.getParamBool(IFptr.LIBFPTR_PARAM_DOCUMENT_PRINTED)) {
                // Можно сразу вызвать метод допечатывания документа, он завершится с ошибкой, если это невозможно
                if (fptr.continuePrint() < 0) {
                    // Если не удалось допечатать документ - показать пользователю ошибку и попробовать еще раз.
                    throw  new DriverException(String.format("Не удалось напечатать документ (Ошибка \"%s\"). Устраните неполадку и повторите.", fptr.errorDescription()));
                }
            }
            sendBroadcast(getResponseIntent(ACTION_Z_REPORT_RESPONSE,7,7,getString(R.string.fptr_settings_ok),true));

        } catch (DriverException e) {
            errorMessage = e.getMessage();
            sendBroadcast(getResponseIntent(ACTION_Z_REPORT_RESPONSE,0,0,errorMessage,false));
        }
    }

}
