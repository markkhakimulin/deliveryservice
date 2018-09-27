package com.ff.deliveryservice.modules.fptr;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.common.Utils;
import com.ff.deliveryservice.mvp.model.ChequeData;
import com.ff.deliveryservice.mvp.model.DBHelper;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

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
public class FPTRService extends IntentService {


    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected Resources res;

    @Inject
    protected Context context;

    HashMap<Byte,String> ofd_exchange_status = new HashMap<> ();

    public static final String ACTION_CHECK_CONNECT_REQUEST = "com.ff.deliveryservice.service.action.request.CHECK_CONNECT";
    public static final String ACTION_OFD_STATE_REQUEST = "com.ff.deliveryservice.service.action.request.OFD_REPORT";
    public static final String ACTION_OFD_REPORT_REQUEST = "com.ff.deliveryservice.service.action.request.OFD_STATE_RESPONSE";
    public static final String ACTION_BATTERY_STATE_REQUEST = "com.ff.deliveryservice.service.action.request.BATTERY_STATE";
    public static final String ACTION_X_REPORT_REQUEST = "com.ff.deliveryservice.service.action.request.X_REPORT";
    public static final String ACTION_Z_REPORT_REQUEST = "com.ff.deliveryservice.service.action.request.Z_REPORT";
    public static final String ACTION_PAYMENT_REQUEST = "com.ff.deliveryservice.service.action.request.PAYMENT";
    public static final String ACTION_OFD_TRANSPORT_REQUEST = "com.ff.deliveryservice.service.action.request.TRANSPORT_OFD";
    public static final String ACTION_CHECK_DOCUMENT_CLOSED_REQUEST = "com.ff.deliveryservice.service.action.request.CHECK_DOCUMENT_CLOSED";

    // from service to activity


    public static final String ACTION_CHECK_CONNECT_RESPONSE = "com.ff.deliveryservice.service.action.response.CHECK_CONNECT";
    public static final String ACTION_OFD_STATE_RESPONSE = "com.ff.deliveryservice.service.action.response.OFD_STATE";
    public static final String ACTION_OFD_REPORT_RESPONSE = "com.ff.deliveryservice.service.action.response.OFD_REPORT";
    public static final String ACTION_BATTERY_STATE_RESPONSE = "com.ff.deliveryservice.service.response.action.BATTERY_STATE";
    public static final String ACTION_X_REPORT_RESPONSE = "com.ff.deliveryservice.service.action.response.X_REPORT";
    public static final String ACTION_Z_REPORT_RESPONSE = "com.ff.deliveryservice.service.action.response.Z_REPORT";
    public static final String ACTION_PAYMENT_RESPONSE = "com.ff.deliveryservice.service.action.response.PAYMENT";
    public static final String ACTION_CHECK_DOCUMENT_CLOSED_RESPONSE = "com.ff.deliveryservice.service.action.response.CHECK_DOCUMENT_CLOSED";

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
    protected OFDTransportLoader ofdLoader;
    protected Timer timer;

    byte status1 = 1;
    byte status2 = 2;
    byte status3 = 4;
    byte status4 = 8;
    byte status5 = 16;
    byte status6 = 32;

    public static final int timerDelay = 10000;
    public static final int timerRepeat = 5;

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
        response.putExtra(EXTRA_USE_BATTERY,fptr.getParamBool(IFptr.LIBFPTR_PARAM_USE_BATTERY)?"Да":"Нет");
        response.putExtra(EXTRA_IS_CHARGING,fptr.getParamBool(IFptr.LIBFPTR_PARAM_BATTERY_CHARGING)?"Да":"Нет");
        response.putExtra(EXTRA_CAN_PRINT,fptr.getParamBool(IFptr.LIBFPTR_PARAM_CAN_PRINT_WHILE_ON_BATTERY)?"Да":"Нет");
        return response;
    }

    private Intent getOFDStateResponseIntent(int progress,int max) {
        return getOFDStateResponseIntent(progress,max,getString(R.string.fptr_settings_ok));
    }

    private Intent getOFDStateResponseIntent(int progress,int max,String message) {
        Intent response = getResponseIntent(ACTION_OFD_STATE_RESPONSE,progress,max,message,true);


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
            } else if (ACTION_OFD_STATE_REQUEST.equals(action)) {
                handleActionCheckUnsentDocumentsOFD();
            } else if (ACTION_BATTERY_STATE_REQUEST.equals(action)) {
                handleActionBatteryState();
            } else if (ACTION_X_REPORT_REQUEST.equals(action)) {
                handleActionReportX();
            } else if (ACTION_Z_REPORT_REQUEST.equals(action)) {
                String userName = intent.getStringExtra(EXTRA_USER_NAME);
                String userINN = intent.getStringExtra(EXTRA_USER_INN);
                handleActionReportZ(userName,userINN);
            } else if (ACTION_OFD_REPORT_REQUEST.equals(action)) {
                handleActionReportOFD();
            } else if (ACTION_PAYMENT_REQUEST.equals(action)) {
                ChequeData chequeData = intent.getParcelableExtra(ChequeData.class.getCanonicalName());
                handleActionPayment(chequeData);
            } else if (ACTION_OFD_TRANSPORT_REQUEST.equals(action)) {
                handleActionTransportOFD();
            }

        }

    }

    /*
    * запускает таймер который просто поддерживает соединение с драйвером тем самым давая ему время
    * передать данные через соединение
    */
    private void handleActionTransportOFD() {

        try {

            if (fptr == null) {
                throw new DriverException("Не инициализирован драйвер устройства");
            }
            sendBroadcast(getResponseIntent(ACTION_OFD_STATE_RESPONSE,1,5,getString(R.string.fptr_settings_set_settings),true));
            fptr.setSettings(getSettings());

            sendBroadcast(getResponseIntent(ACTION_OFD_STATE_RESPONSE,2,5,getString(R.string.fptr_settings_set_connection),true));
            if (fptr.open() < 0) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_OFD_STATE_RESPONSE,3,5,getString(R.string.fptr_settings_check_connection),true));
            if (!fptr.isOpened()) {
                checkError();
            }


        } catch (Exception e) {
            sendBroadcast(getResponseIntent(ACTION_OFD_STATE_RESPONSE,0,0,e.getMessage(),false));
            return;
        }

        if (ofdLoader != null) {
            ofdLoader.cancel();
        }

        ofdLoader = new OFDTransportLoader();

        if (timer != null ) {
            timer.cancel();
        }

        if (Utils.checkInternetConnection(context)) {

            timer = new Timer();
            timer.schedule(ofdLoader, 0, timerDelay);//проверяет каждые 10 секунд
        } else {
            sendBroadcast(getResponseIntent(ACTION_OFD_STATE_RESPONSE,0,0,getString(R.string.error_no_internet_connection),false));
        }
    }


    public class OFDTransportLoader extends TimerTask {

        int repeat = 1;

        @Override
        public void run() {

            try {

                fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_OFD_EXCHANGE_STATUS);
                fptr.fnQueryData();

                Intent result = getOFDStateResponseIntent(4,5,String.format(Locale.getDefault(),"%s : %d / %d",getString(R.string.msg_do_not_turn_off_ofd_transfer),repeat,timerRepeat));
                sendBroadcast(result);

                if (repeat > timerRepeat || result.getIntExtra(EXTRA_UNSENT_COUNT,0) == 0) {

                    sendBroadcast(getOFDStateResponseIntent(5, 5));
                    cancel();
                    return;
                }
                repeat++;

            } catch (Exception e ){

                sendBroadcast(getResponseIntent(ACTION_OFD_STATE_RESPONSE,0,0,e.getMessage(),false));
                cancel();
            }

        }

        @Override
        public boolean cancel() {

            if (super.cancel()) {
                if (timer !=null)
                    timer.cancel();
                return true;
            }
            return false;
        }

    }

    private void handleActionPayment(ChequeData chequeData) {


       int maxProgress = 12;

        errorMessage = "";
        try {

            if (fptr == null) {
                throw new DriverException("Не инициализирован драйвер устройства");
            }
            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,1,maxProgress,getString(R.string.fptr_settings_set_settings),true));
            fptr.setSettings(getSettings());

            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,2,maxProgress,getString(R.string.fptr_settings_set_connection),true));
            if (fptr.open() < 0) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,3,maxProgress,getString(R.string.fptr_settings_check_connection),true));
            if (!fptr.isOpened()) {
                checkError();
            }

            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,4,maxProgress,"Отмена незакрытого чека",true));
            if (!fptr.getParamBool(IFptr.LIBFPTR_PARAM_DOCUMENT_CLOSED)) {
                // Документ не закрылся. Требуется его отменить (если это чек) и сформировать заново
                if (fptr.cancelReceipt()<0) {
                    checkError();
                }
            }

            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,5,maxProgress,"Регистрация кассира",true));
            // Регистрация кассира
            fptr.setParam(1021, chequeData.getCashier());
            if (fptr.operatorLogin() < 0) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,6,maxProgress,"Регистрация контактов",true));
            // Открытие электронного чека (с передачей телефона получателя)
            fptr.setParam(IFptr.LIBFPTR_PARAM_RECEIPT_TYPE, chequeData.getChequeType());
            fptr.setParam(1008, chequeData.getContacts());
            try {
                if (fptr.openReceipt() < 0) {
                    checkError();
                }
            }catch (DriverException e) {
                // Проверка на превышение смены

                if (fptr.errorCode() == IFptr.LIBFPTR_ERROR_SHIFT_EXPIRED ) {
                    handleActionReportZ(chequeData.getCashier(),"");
                    if (fptr.openReceipt() < 0) {
                        checkError();
                    }
                } else {
                    errorMessage = e.getMessage();
                    sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,0,maxProgress,errorMessage,false));
                    return;
                }
            }


            SQLiteDatabase db = dbHelper.getReadableDatabase();

            double sumToPay = dbHelper.getSumToPay(db,chequeData.getChequeType(),chequeData.getOrderId());
            if (sumToPay <= 0) {
                db.close();
                errorMessage = getString(R.string.order_details_error_sum_zero);
                sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,0,maxProgress,errorMessage,false));
                return;
            }
            db.close();

            db = dbHelper.getWritableDatabase();


            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,7,maxProgress,"Регистрация позиций чека",true));

            Cursor cursor = db.rawQuery(String.format("select oi.*,i.description from order_items oi left join items as i on oi.item_id = i._id where oi.order_id = ? and oi.checked = 1 group by oi._id"), new String[]{chequeData.getOrderId()});
            BigDecimal sum = null,discountSum = null;
            while (cursor.moveToNext()) {
                double price = 0, discount = 0;
                int quantity = 0, nds;
                String name = cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION));
                quantity = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COUNT));
                discount = cursor.getDouble(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_DISCOUNT));
                price = cursor.getDouble(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COST));
                nds = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_NDS));
                registration(name, price, quantity, (price - discount)*quantity, nds);
            }
            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,8,maxProgress,"Регистрация итога",true));
            // Регистрация итога (отрасываем копейки)
            fptr.setParam(IFptr.LIBFPTR_PARAM_SUM, sumToPay);
            fptr.receiptTotal();

            // Оплата
            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,9,maxProgress,"Регистрация оплаты",true));
            for (int paymentCode:chequeData.getPaymentsType().keySet()) {

                fptr.setParam(IFptr.LIBFPTR_PARAM_PAYMENT_TYPE, paymentCode);
                fptr.setParam(IFptr.LIBFPTR_PARAM_PAYMENT_SUM, chequeData.getPaymentsType().get(paymentCode));
                fptr.payment();
            }

            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,10,maxProgress,"Закрытие чека",true));
            // Закрытие чека
            fptr.closeReceipt();

            while (fptr.checkDocumentClosed() < 0) {
                // Не удалось проверить состояние документа. Вывести пользователю текст ошибки, попросить устранить неполадку и повторить запрос
                errorMessage =  fptr.errorDescription();
                continue;
            }

            if (!fptr.getParamBool(IFptr.LIBFPTR_PARAM_DOCUMENT_CLOSED)) {
                // Документ не закрылся. Требуется его отменить (если это чек) и сформировать заново
                fptr.cancelReceipt();
                throw new DriverException(String.format("%s :(%s)", "Документ не закрылся. Требуется сформировать чек заново",errorMessage));
            }

            if (!fptr.getParamBool(IFptr.LIBFPTR_PARAM_DOCUMENT_PRINTED)) {
                // Можно сразу вызвать метод допечатывания документа, он завершится с ошибкой, если это невозможно
                while (fptr.continuePrint() < 0) {
                    // Если не удалось допечатать документ - показать пользователю ошибку и попробовать еще раз.
                    errorMessage = String.format("Не удалось напечатать документ (Ошибка \"%s\"). Устраните неполадку и повторите.", fptr.errorDescription());
                    sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,0,0,errorMessage,true));
                    continue;
                }
            }
            // Запрос информации о закрытом чеке

            fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_LAST_DOCUMENT);
            fptr.fnQueryData();
            final String fiscalSign = fptr.getParamString(IFptr.LIBFPTR_PARAM_FISCAL_SIGN);
            final long documentNumber = fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENT_NUMBER);

            @SuppressLint("DefaultLocale")
            String result = String.format("ФПД: %s\nФД: %d",fiscalSign,documentNumber);

            sendBroadcast(getResponseIntent(ACTION_CHECK_DOCUMENT_CLOSED_RESPONSE,0,0,result,true));

            //записываем данные о платеже

            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,11,maxProgress,"Запись информации о платеже",true));

            db = dbHelper.getWritableDatabase();
            for (int paymentCode:chequeData.getPaymentsType().keySet()) {

                Cursor payment = db.query(DBHelper.TB_PAYMENT_TYPES,new String[]{DBHelper.CN_ID},"code = ?",new String[]{String.valueOf(paymentCode)},null,null,null);
                payment.moveToNext();
                String paymentID = payment.getString(payment.getColumnIndex(DBHelper.CN_ID));
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.CN_ORDER_ID, chequeData.getOrderId());
                cv.put(DBHelper.CN_ORDER_PAYMENT_TYPE_ID, paymentID);
                cv.put(DBHelper.CN_ORDER_PAYMENT_SUM, chequeData.getPaymentsType().get(paymentCode));
                cv.put(DBHelper.CN_ORDER_PAYMENT_DISCOUNT, 0);
                cv.put(DBHelper.CN_ORDER_PAYMENT_CHECK_NUMBER, documentNumber);
                cv.put(DBHelper.CN_ORDER_PAYMENT_CHECK_SESSION, fiscalSign);
                cv.put(DBHelper.CN_ORDER_DATE, getDateTime());
                cv.put(DBHelper.CN_ORDER_PAYMENT_CHEQUE_TYPE, chequeData.getChequeType());
                db.insert(DBHelper.TB_ORDER_PAYMENTS, null, cv);
            }

            db.close();

            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,12,maxProgress,getString(R.string.fptr_settings_ok),true));

            // Запрос информации о состоянии переданных данных в ОФД

            fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_OFD_EXCHANGE_STATUS);
            fptr.fnQueryData();
            final long unsentCount = fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENTS_COUNT);
            final long unsentFirstNumber = fptr.getParamInt(IFptr.LIBFPTR_PARAM_DOCUMENT_NUMBER);
            @SuppressLint("SimpleDateFormat")
            DateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            final String unsentDateTime = df.format(fptr.getParamDateTime(IFptr.LIBFPTR_PARAM_DATE_TIME));

            @SuppressLint("DefaultLocale")
            String resultOFD = String.format("Статус обмена с ОФД: %d неотправленно, первый: №%d (%s)",
                    unsentCount,
                    unsentFirstNumber,
                    unsentDateTime);

            sendBroadcast(getResponseIntent(ACTION_OFD_STATE_RESPONSE,0,0,resultOFD,true));

        } catch (DriverException e) {
            errorMessage = e.getMessage();
            sendBroadcast(getResponseIntent(ACTION_PAYMENT_RESPONSE,0,0,errorMessage,false));
        }
    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    private void registration(String name, double price, int quantity, double sum, int nds) {

        fptr.setParam(IFptr.LIBFPTR_PARAM_COMMODITY_NAME, name);
        fptr.setParam(IFptr.LIBFPTR_PARAM_PRICE, price);
        fptr.setParam(IFptr.LIBFPTR_PARAM_DISCOUNT_SUM, sum);
        fptr.setParam(IFptr.LIBFPTR_PARAM_QUANTITY, quantity);
        fptr.setParam(IFptr.LIBFPTR_PARAM_TAX_TYPE, nds);
        fptr.registration();
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
            sendBroadcast(getResponseIntent(ACTION_BATTERY_STATE_RESPONSE,4,5,getString(R.string.fptr_settings_ok),true));

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
            sendBroadcast(getResponseIntent(ACTION_OFD_STATE_RESPONSE,1,5,getString(R.string.fptr_settings_set_settings),true));
            fptr.setSettings(getSettings());

            sendBroadcast(getResponseIntent(ACTION_OFD_STATE_RESPONSE,2,5,getString(R.string.fptr_settings_set_connection),true));
            if (fptr.open() < 0) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_OFD_STATE_RESPONSE,3,5,getString(R.string.fptr_settings_check_connection),true));
            if (!fptr.isOpened()) {
                checkError();
            }
            sendBroadcast(getResponseIntent(ACTION_OFD_STATE_RESPONSE,4,5,getString(R.string.fptr_settings_check_ofd_state),true));

            fptr.setParam(IFptr.LIBFPTR_PARAM_FN_DATA_TYPE, IFptr.LIBFPTR_FNDT_OFD_EXCHANGE_STATUS);
            fptr.fnQueryData();

            sendBroadcast(getOFDStateResponseIntent(5,5));

        } catch (DriverException e) {
            errorMessage = e.getMessage();
            sendBroadcast(getResponseIntent(ACTION_OFD_STATE_RESPONSE,0,0,errorMessage,false));
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
