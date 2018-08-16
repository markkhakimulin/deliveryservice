package com.ff.deliveryservice.modules.fptr;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.base.BaseActivity;
import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.mvp.model.ChequeData;
import com.ff.deliveryservice.mvp.view.FPTRView;

import java.util.Date;
import java.util.Timer;

import javax.inject.Inject;

import butterknife.BindView;
import ru.atol.drivers10.fptr.settings.SettingsActivity;

import static com.ff.deliveryservice.modules.fptr.FPTRService.EXTRA_DATETIME;
import static com.ff.deliveryservice.modules.fptr.FPTRService.EXTRA_EXCHANGE_STATUS;
import static com.ff.deliveryservice.modules.fptr.FPTRService.EXTRA_FIRST_UNSENT_NUMBER;
import static com.ff.deliveryservice.modules.fptr.FPTRService.EXTRA_MAX;
import static com.ff.deliveryservice.modules.fptr.FPTRService.EXTRA_MESSAGE;
import static com.ff.deliveryservice.modules.fptr.FPTRService.EXTRA_OFD_MESSAGE_READ;
import static com.ff.deliveryservice.modules.fptr.FPTRService.EXTRA_PROGRESS;
import static com.ff.deliveryservice.modules.fptr.FPTRService.EXTRA_RESULT;
import static com.ff.deliveryservice.modules.fptr.FPTRService.EXTRA_UNSENT_COUNT;
import static com.ff.deliveryservice.modules.fptr.FPTRService.EXTRA_USER_NAME;

/**
 * Created by khakimulin on 03.02.2017.
 */
/**
 *
 Main class for working with trading equipment.
 */
public abstract class FPTRActivity extends BaseActivity implements FPTRView {


    public static final int REQUEST_SHOW_SETTINGS = 4;

    public static final int OFD_OPERATION_PROGRESS = 1;
    public static final int OFD_OPERATION_COMPLETE= 2;
    public static final int OFD_OPERATION_RUN_DIAGNOSTIC= 3;
    public static final int NO_INTERNET_CONNECTION= 4;
    public static final int timerDelay = 10000;
    public static final int timerTick = 5;
    @Inject
    protected SharedPreferences preferences;

    private ProgressDialog mProgressDialog;
    protected String orderId,loginId,numberId,loginDesc;
    protected static Timer timer;
    private FPTRServiceReceiver fptrServiceReceiver;
    @BindView(android.R.id.content)
    View contentView;

    //@Inject
    //protected FPTRPresenter<FPTRView> presenter;


    @Override
    protected void onViewReady(Bundle savedInstanceState, Intent intent) {
        super.onViewReady(savedInstanceState,intent);
        //preferences = getSharedPreferences(Constants.FPTR_PREFERENCES, Context.MODE_PRIVATE);
    }

    @Override
    protected void resolveDaggerDependency(){
        DeliveryServiceApplication.getApplicationComponent().inject(this);
    }

    @Override
    protected int getContentView() {
        return 0;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_SHOW_SETTINGS && resultCode == RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                String settings = data.getExtras().getString(SettingsActivity.DEVICE_SETTINGS);
                preferences.edit().putString(SettingsActivity.DEVICE_SETTINGS,settings).apply();
                Snackbar.make(contentView,getString(R.string.fptr_settings_change_successful),Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_fptr, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {


            case R.id.action_settings:
                openSettings();
                break;
            case R.id.action_check_settings:
                checkSettings();
                break;
            case R.id.action_check_unsent_data_ofd:
                checkStateOFD();
                break;
            case R.id.action_ofd_report:
                reportOFD();
                break;
            case R.id.action_x_report:
                reportX();
                break;
            case R.id.action_z_report:
                reportZ();
        }
        return super.onOptionsItemSelected(item);
    }

    private void openSettings() {
        Intent intent = new Intent(FPTRActivity.this,SettingsActivity.class);
        intent.putExtra(SettingsActivity.DEVICE_SETTINGS,
                preferences.getString(SettingsActivity.DEVICE_SETTINGS,
                        FPTRService.getDefaultSettings()));
        startActivityForResult(intent,REQUEST_SHOW_SETTINGS);
    }

    void checkSettings() {
        startActionCheckConnect();
    }
    void checkStateOFD() {
        startActionOFDState();
    }
    void reportOFD() {
        startActionOFDReport();
    }
    void reportX() {
        Snackbar.make(contentView,getString(R.string.msg_do_not_turn_off_attention),Snackbar.LENGTH_LONG).show();
        startActionReportX();
    }
    void reportZ() {

        if (preferences.getString(Constants.SP_USER_NAME,"").isEmpty()) {
            showYesNoMessageDialog("Ошибка Z отчета","Сначала выберите пользователя!",null,null);
            return;
        }
        String userName = preferences.getString(Constants.SP_USER_NAME,"");
        Snackbar.make(contentView,getString(R.string.msg_do_not_turn_off_attention),Snackbar.LENGTH_LONG).show();
        startActionReportZ(userName);
    }

    @Override
    public void onShowProgressDialog(int min, int max, String message) {
        this.showProgressDialog(message,min,max);
    }

    @Override
    public void onHideProgressDialog() {
        super.hideProgressDialog();
    }

    @Override
    public void onShowDialog(String message) {

    }

    @Override
    public void onHideDialog() {

    }


    @Override
    public void onShowToast(String message) {
        Snackbar.make(contentView,message,Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fptrServiceReceiver != null) {
            try {
                unregisterReceiver(fptrServiceReceiver);
            } catch (IllegalArgumentException ex) { }
        }
    }
    //protected abstract FPTRPresenter getParentPresenter();

    public void registerReceiver(String[] actions) {

        if (fptrServiceReceiver != null) fptrServiceReceiver.abortBroadcast();

        fptrServiceReceiver = new FPTRServiceReceiver();
        IntentFilter intentFilter = new IntentFilter();
        for (String action : actions) {
            intentFilter.addAction(action);
        }
        registerReceiver(fptrServiceReceiver, intentFilter);
    }

    ////////Start actions////////////

    public void startActionCheckConnect() {

        registerReceiver(new String[]{FPTRService.ACTION_CHECK_CONNECT_RESPONSE});
        startService(getStartIntent(FPTRService.ACTION_CHECK_CONNECT_REQUEST));
    }

    public void startActionOFDReport() {

        registerReceiver(new String[]{FPTRService.ACTION_OFD_REPORT_RESPONSE});
        startService(getStartIntent(FPTRService.ACTION_OFD_REPORT_REQUEST));
    }
    public void startActionOFDState() {

        registerReceiver(new String[]{FPTRService.ACTION_CHECK_CONNECT_OFD_RESPONSE});
        startService(getStartIntent(FPTRService.ACTION_CHECK_CONNECT_OFD_REQUEST));
    }

    public void startActionBatteryState() {

        registerReceiver(new String[]{FPTRService.ACTION_BATTERY_STATE_RESPONSE});
        startService(getStartIntent(FPTRService.ACTION_BATTERY_STATE_REQUEST));
    }

    public void startActionReportX() {

        registerReceiver(new String[]{FPTRService.ACTION_X_REPORT_RESPONSE});
        startService(getStartIntent(FPTRService.ACTION_X_REPORT_REQUEST));
    }

    public void startActionReportZ(String userName) {

        Intent intent = getStartIntent(FPTRService.ACTION_Z_REPORT_REQUEST);
        intent.putExtra(EXTRA_USER_NAME,userName);

        registerReceiver(new String[]{FPTRService.ACTION_Z_REPORT_RESPONSE});
        startService(intent);
    }

    public void startActionPayment(ChequeData chequeData) {

        Intent intent = getStartIntent(FPTRService.ACTION_PAYMENT_REQUEST);
        intent.putExtra(ChequeData.class.getCanonicalName(),chequeData);

        registerReceiver(new String[]{FPTRService.ACTION_PAYMENT_RESPONSE});
        startService(intent);
    }

   /* public  boolean isServiceRunning() {
        return Utils.isServiceRunning(mContext,FPTRService.class);
    }*/

    /*
     * стартует интент сервис. после того как он выполнится сервис сам грохнется
     * Если этого не случилось то можно его убить принудитетельно методом stopService()
     */
    protected Intent getStartIntent(String action){

        Intent intent = new Intent(getApplicationContext(),FPTRService.class);
        intent.setAction(action);
        return intent;

    }

    protected void stopService(){

        Intent intent = new Intent(getApplicationContext(),FPTRService.class);
        stopService(intent);
    }

    private class FPTRServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //если пришла ошибка от сервиста то показываем ее пользюку
            if (!intent.getBooleanExtra(EXTRA_RESULT,false)) {
                context.unregisterReceiver(fptrServiceReceiver);
                onHideProgressDialog();
                onShowToast(intent.getStringExtra(EXTRA_MESSAGE));
                return;
            }

            //завершение обработчика на сервисе
            if (intent.getIntExtra(EXTRA_PROGRESS,1) == intent.getIntExtra(EXTRA_MAX,0)) {

                context.unregisterReceiver(fptrServiceReceiver);

                if (action.equals(FPTRService.ACTION_CHECK_CONNECT_OFD_RESPONSE)){


                    String date = intent.getIntExtra(EXTRA_DATETIME,0) <= 0
                            ? "Нет данных":String.format("%tD",new Date(intent.getIntExtra(EXTRA_DATETIME,0)));

                    @SuppressLint("DefaultLocale")
                    String message = String.format(
                            "Статус информационного обмена : \"%s\"\n" +
                            "Количество неотправленных документов : %d\n" +
                            "Номер первого неотправленного документа : %d\n" +
                            "Дата и время первого неотправленного документа : %s\n" +
                            "Cообщения для ОФД : %s",
                            intent.getStringExtra(EXTRA_EXCHANGE_STATUS),
                            intent.getIntExtra(EXTRA_UNSENT_COUNT,0),
                            intent.getIntExtra(EXTRA_FIRST_UNSENT_NUMBER,0),
                            date,
                            intent.getStringExtra(EXTRA_OFD_MESSAGE_READ));
                    showYesNoMessageDialog("Статус информационного обмена",message,null,null);

                }
                if(action.equals(FPTRService.ACTION_BATTERY_STATE_RESPONSE)) {
                    //String string_from_service = intent.getStringExtra(MyIntentService.KEY_STRING_FROM_SERVICE);
                    //textViewMsgReceived.setText(String.valueOf(string_from_service));

                }

                onHideProgressDialog();
                onShowToast(intent.getStringExtra(EXTRA_MESSAGE));
                return;
            }

            onShowProgressDialog(
                    intent.getIntExtra(EXTRA_PROGRESS,1),
                    intent.getIntExtra(EXTRA_MAX,0),
                    intent.getStringExtra(EXTRA_MESSAGE));

        }
    }




}
