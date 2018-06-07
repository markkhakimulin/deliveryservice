package com.ff.deliveryservice;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.atol.drivers.fptr.Fptr;
import com.atol.drivers.fptr.IFptr;
import com.atol.drivers.fptr.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import static com.ff.deliveryservice.FPTRActivity.fptr;

/**
 * Created by khakimulin on 03.02.2017.
 */
/**
 *
 Main class for working with trading equipment.
 */
public class FPTRActivity extends AppCompatActivity {


    public static final int REQUEST_SHOW_SETTINGS = 4;
    public static final String FPTR_PREFERENCES = "FPTR_PREFERENCES";
    public static final int OFD_OPERATION_PROGRESS = 1;
    public static final int OFD_OPERATION_COMPLETE= 2;
    public static final int OFD_OPERATION_RUN_DIAGNOSTIC= 3;
    public static final int NO_INTERNET_CONNECTION= 4;
    public static final int timerDelay = 10000;
    public static final int timerTick = 5;

    protected SharedPreferences preferences;
    private ProgressDialog mProgressDialog;
    protected static String orderId,
            loginId,numberId,loginDesc;
    protected static IFptr fptr;
    protected static Timer timer;

    protected static CheckOFD  ofdLoader;
    protected ArrayList<String> progressStages;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(FPTR_PREFERENCES, Context.MODE_PRIVATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_SHOW_SETTINGS && resultCode == RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                String settings = data.getExtras().getString(SettingsActivity.DEVICE_SETTINGS);
                setSettings(settings);
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_fptr, menu);
        return true;
    }

    protected void checkError(IFptr fptr) throws DriverException {
        int rc = fptr.get_ResultCode();
        if (rc < 0) {
            String rd = fptr.get_ResultDescription(), bpd = null;
            if (rc == -6) {
                bpd = fptr.get_BadParamDescription();
            }
            if (bpd != null) {
                throw new DriverException(String.format("[%d] %s (%s)", rc, rd, bpd));
            } else {
                throw new DriverException(String.format("[%d] %s ", rc, rd));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {

            Intent intent = new Intent(FPTRActivity.this, FPTRSettingsActivity.class);
            String settings = getSettings();
            if (settings == null) {

                try {
                    settings = getDefaultSettings();
                } catch (Exception e) {
                    Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
                    return false;
                }
            }
            intent.putExtra(SettingsActivity.DEVICE_SETTINGS, settings);
            startActivityForResult(intent,REQUEST_SHOW_SETTINGS);

        }
        if (id == R.id.action_check_unsent_data_ofd) {

            final Context mContext = this;
            showMessageDialog(getString(R.string.msg_fail_ofd_transfer_attention_title), getString(R.string.msg_fail_ofd_transfer_attention), new Callable<Void>() {
                @Override
                public Void call() {
                    showProgressDialog(getString(R.string.action_check_unsent_data_ofd));
                    new GetNotSendedDocuments(new Callable<Boolean>() {
                        @Override
                        public Boolean call() {
                            hideProgressDialog();
                            return null;
                        }
                    }, new Callable<Void>() {
                        @Override
                        public Void call() {
                            loadDataToOFD();
                            return null;
                        }
                    }).execute(mContext);
                    return null;
                }
            });




        }
        if (id == R.id.action_check_settings) {

            @SuppressLint("StaticFieldLeak") AsyncTask<Object, Integer, Boolean> task = new AsyncTask<Object, Integer, Boolean>() {

                String errorMessage = "";


                @Override
                protected Boolean doInBackground(Object... params) {


                    progressStages = new ArrayList<>();
                    progressStages.add(getString(R.string.fptr_settings_loading));
                    progressStages.add(getString(R.string.fptr_settings_set_connection));
                    progressStages.add(getString(R.string.fptr_settings_check_connection));
                    progressStages.add(getString(R.string.fptr_settings_ok));

                    fptr = new Fptr();
                    try {
                        fptr.create(getApplication());
                        publishProgress(0);
                        if (fptr.put_DeviceSettings(getSettings()) < 0) {
                            checkError(fptr);
                        }
                        publishProgress(1);
                        if (fptr.put_DeviceEnabled(true) < 0) {
                            checkError(fptr);
                        }
                        publishProgress(2);
                        if (fptr.GetStatus() < 0) {
                            checkError(fptr);
                        }
                        publishProgress(3);
                    } catch (Exception e) {
                        errorMessage = String.format(Locale.getDefault(),"[%d] %s",fptr.get_ResultCode(),e.getMessage());
                    } finally {
                        fptr.destroy();
                        fptr = null;
                    }
                    return errorMessage.isEmpty();
                }

                @Override
                protected void onProgressUpdate(Integer... values) {
                    showProgressDialog(values[0]);
                }
                @Override
                protected void onPostExecute(Boolean result) {

                    if (!result) {
                        Toast.makeText(getApplication(),errorMessage,Toast.LENGTH_SHORT).show();
                    }
                    hideProgressDialog();
                }
            };
            task.execute(this);
        }

        if (id == R.id.action_x_report) {

            Snackbar.make(findViewById(android.R.id.content),getString(R.string.msg_do_not_turn_off_attention),Snackbar.LENGTH_LONG).show();

            @SuppressLint("StaticFieldLeak")
            AsyncTask<Object, Integer, Boolean> task = new AsyncTask<Object, Integer, Boolean>() {

                String errorMessage = "";
                @Override
                protected Boolean doInBackground(Object... params) {

                    progressStages = new ArrayList<>();
                    progressStages.add(getString(R.string.fptr_settings_loading));
                    progressStages.add(getString(R.string.fptr_settings_set_connection));
                    progressStages.add(getString(R.string.fptr_settings_check_connection));
                    progressStages.add(getString(R.string.fptr_settings_set_x_report));
                    progressStages.add(getString(R.string.fptr_settings_report));

                    return report();
                }

                @Override
                protected void onProgressUpdate(Integer... values) {
                    showProgressDialog(values[0]);
                }
                Boolean report() {

                    fptr = new Fptr();
                    try {
                        fptr.create(getApplication());
                        publishProgress(0);
                        if (fptr.put_DeviceSettings(getSettings()) < 0) {
                            checkError(fptr);
                        }
                        publishProgress(1);
                        if (fptr.put_DeviceEnabled(true) < 0) {
                            checkError(fptr);
                        }
                        publishProgress(2);
                        if (fptr.GetStatus() < 0) {
                            checkError(fptr);
                        }
                        publishProgress(3);
                        if (fptr.put_Mode(IFptr.MODE_REPORT_NO_CLEAR) < 0) {
                            checkError(fptr);
                        }
                        if (fptr.SetMode() < 0) {
                            checkError(fptr);
                        }
                        publishProgress(4);
                        if (fptr.put_ReportType(IFptr.REPORT_X) < 0) {
                            checkError(fptr);
                        }
                        if (fptr.Report() < 0) {
                            checkError(fptr);
                        }
                        return true;

                    } catch (DriverException e) {
                        int rc = fptr.get_ResultCode();
                        if (rc != -16 && rc != -3801) {
                            errorMessage = e.getMessage();
                        }
                        fptr.destroy();
                        fptr = null;
                    }
                    return errorMessage.isEmpty();
                }
                @Override
                protected void onPostExecute(final Boolean result) {

                    hideProgressDialog();
                    if (result) {
                        loadDataToOFD();
                    } else {
                        Toast.makeText(getApplication(),errorMessage,Toast.LENGTH_SHORT).show();
                    }
                }
            };
            task.execute(this);

        }
        if (id == R.id.action_z_report) {

            Snackbar.make(findViewById(android.R.id.content),getString(R.string.msg_do_not_turn_off_attention),Snackbar.LENGTH_LONG).show();

            @SuppressLint("StaticFieldLeak")
            AsyncTask<Object, Integer, Boolean> task = new AsyncTask<Object, Integer, Boolean>() {

                String errorMessage = "";
                @Override
                protected Boolean doInBackground(Object... params) {
                    progressStages = new ArrayList<>();
                    progressStages.add(getString(R.string.fptr_settings_loading));
                    progressStages.add(getString(R.string.fptr_settings_set_connection));
                    progressStages.add(getString(R.string.fptr_settings_check_connection));
                    progressStages.add(getString(R.string.fptr_settings_set_z_report));
                    progressStages.add(getString(R.string.fptr_settings_report));

                    return report();
                }

                @Override
                protected void onProgressUpdate(Integer... values) {
                    showProgressDialog(values[0]);
                }

                Boolean report() {

                    fptr = new Fptr();
                    try {
                        fptr.create(getApplication());
                        publishProgress(0);
                        if (fptr.put_DeviceSettings(getSettings()) < 0) {
                            checkError(fptr);
                        }
                        publishProgress(1);
                        if (fptr.put_DeviceEnabled(true) < 0) {
                            checkError(fptr);
                        }
                        publishProgress(2);
                        if (fptr.GetStatus() < 0) {
                            checkError(fptr);
                        }

                        fptr.put_FiscalPropertyNumber(1021);
                        fptr.put_FiscalPropertyPrint(true);//печатать это свойство на чеке
                        fptr.put_FiscalPropertyType(IFptr.FISCAL_PROPERTY_TYPE_STRING);
                        fptr.put_FiscalPropertyValue(loginDesc);
                        fptr.WriteFiscalProperty();

                        publishProgress(3);
                        if (fptr.put_Mode(IFptr.MODE_REPORT_CLEAR) < 0) {
                            checkError(fptr);
                        }

                        if (fptr.SetMode() < 0) {
                            checkError(fptr);
                        }
                        publishProgress(4);
                        if (fptr.put_ReportType(IFptr.REPORT_Z) < 0) {
                            checkError(fptr);
                        }
                        if (fptr.Report() < 0) {
                            checkError(fptr);
                        }
                        return true;
                    } catch (DriverException e) {
                        int rc = fptr.get_ResultCode();
                        if (rc != -16 && rc != -3801) {
                            errorMessage = e.getMessage();
                        }
                        fptr.destroy();
                        fptr = null;

                    }
                    return errorMessage.isEmpty();
                }

                @Override
                protected void onPostExecute(final Boolean result) {
                    hideProgressDialog();
                    if (result) {
                        loadDataToOFD();
                    } else {
                        Toast.makeText(getApplication(),errorMessage,Toast.LENGTH_SHORT).show();
                    }
                }
            };
            task.execute(this);
        }

        return super.onOptionsItemSelected(item);
    }
    public void showProgressDialog(String title) {
        this.showProgressDialog(title,0,0);
    }
    public void showProgressDialog(int progress) {
        String title = progressStages.get(progress);
        this.showProgressDialog(title,progress+1,progressStages.size());
    }

    public void showProgressDialog(String title,int progress,int max) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(false);
            if (max > 0) {
                mProgressDialog.setProgress(0);
                mProgressDialog.setMax(max);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            } else {
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            }
        }
        if (progress > 0) {
            mProgressDialog.setProgress(progress);
        }
        mProgressDialog.setMessage(title);

        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    public void showMessageDialog(String title, String message,final Callable<Void> positiveCallback) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setTitle(title);
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    dialog.dismiss();

                    if (positiveCallback != null) {
                        positiveCallback.call();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        builder.create().show();
    }

    public void showYesNoMessageDialog(String title, String message, final Callable<Void> positiveCallback, final Callable negativeCallback) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setTitle(title);
        //builder.setCancelable(false);
        builder.setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    dialog.dismiss();
                    positiveCallback.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(R.string.alert_button_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    dialog.dismiss();
                    negativeCallback.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNeutralButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                hideProgressDialog();
            }
        });

        builder.create().show();
    }



    public class GetNotSendedDocuments extends AsyncTask<Context, Integer, Boolean> {

        Context mContext;
        String mErrorMessage = "";
        Callable<Boolean> completeCallback;
        Callable<Void> loadDataToOFDCallback;
        Boolean showdialog = false;
        int count;

        public GetNotSendedDocuments(Callable<Boolean> completeCallback,Callable<Void> loadDataToOFDCallback) {
            //this.fptr = fptr;
            this.completeCallback = completeCallback;
            this.loadDataToOFDCallback = loadDataToOFDCallback;
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            showProgressDialog(values[0]);
        }

        private void checkError() throws DriverException {
            int rc = fptr.get_ResultCode();
            if (rc < 0) {
                String rd = fptr.get_ResultDescription(), bpd = null;
                if (rc == -6) {
                    bpd = fptr.get_BadParamDescription();
                }
                if (bpd != null) {
                    throw new DriverException(String.format("[%d] %s (%s)", rc, rd, bpd));
                } else {
                    throw new DriverException(String.format("[%d] %s", rc, rd));
                }
            }

        }
        @Override
        protected Boolean doInBackground(Context... params) {

            //если драйвер не создан то создаем

            mContext = params[0];

            if (fptr == null) {
                fptr = new Fptr();
                fptr.create(mContext);
                try {
                    if (fptr.put_DeviceSettings(getSettings()) < 0) {
                        checkError();
                    }
                    if (fptr.put_DeviceEnabled(true) < 0) {
                        checkError();
                    }
                    if (fptr.GetStatus() < 0) {
                        checkError();
                    }
                    if (fptr.put_Mode(IFptr.MODE_SELECT) < 0) {
                        checkError();
                    }
                    if (fptr.SetMode() < 0) {
                        checkError();
                    }

                } catch (DriverException e) {
                    int rc = fptr.get_ResultCode();
                    if (rc != -16 && rc != -3801) {
                        mErrorMessage = e.getMessage();
                        return false;
                    }
                }
            }
            //все прошло успешно
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean result) {

            String reason = "";
            String title = getString(R.string.msg_unsent_title);
            //int count = 0;

            if (result) {

                fptr.put_RegisterNumber(getResources().getInteger(R.integer.register_quantity_unsent_ofd));//количество неотпр док-ов в ОФД
                fptr.GetRegister();
                count = fptr.get_Count();
                reason = String.format(Locale.getDefault(),getString(R.string.msg_unsent),count);

                fptr.put_RegisterNumber(getResources().getInteger(R.integer.register_date_unsent_ofd));//дата последнего отпр документа
                fptr.GetRegister();
                reason += String.format(Locale.getDefault(),getString(R.string.msg_from),fptr.get_Date());

                fptr.put_RegisterNumber(getResources().getInteger(R.integer.register_code_errors));//коды ошибок
                fptr.GetRegister();
                if (fptr.get_NetworkError() > 0) {
                    reason += String.format(Locale.getDefault(),getString(R.string.msg_unsent_reason),getString(R.string.msg_unsent_error_network),fptr.get_NetworkError());
                }
                if (fptr.get_OFDError() > 0) {
                    reason += String.format(Locale.getDefault(),getString(R.string.msg_unsent_reason),getString(R.string.msg_unsent_error_ofd),fptr.get_OFDError());
                }
                if (fptr.get_FNError() > 0) {
                    reason += String.format(Locale.getDefault(),getString(R.string.msg_unsent_reason),getString(R.string.msg_unsent_error_fn),fptr.get_FNError());
                }
                showdialog = count >= 0;

                if (showdialog) {

                    showYesNoMessageDialog(title, String.format(getString(R.string.msg_run_diagnostic_agreement), reason),
                        new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                try {


                                    progressStages = new ArrayList<>();
                                    progressStages.add(getString(R.string.msg_progress_0));
                                    progressStages.add(getString(R.string.msg_progress_1));
                                    progressStages.add(getString(R.string.msg_progress_2));
                                    progressStages.add(getString(R.string.msg_progress_3));
                                    progressStages.add(getString(R.string.msg_progress_4));
                                    progressStages.add(getString(R.string.msg_progress_5));
                                    progressStages.add(getString(R.string.msg_progress_6));
                                    progressStages.add(getString(R.string.msg_progress_7));
                                    progressStages.add(getString(R.string.msg_progress_8));
                                    progressStages.add(getString(R.string.msg_progress_9));

                                    fptr.put_RegisterNumber(getResources().getInteger(R.integer.register_version_ffd));
                                    fptr.GetRegister();
                                    int ffdVersion = fptr.get_FfdVersion();
                                    //переходим в режим программирования

                                    int version = 0;

                                    Boolean isTestFN = isTestFN(fptr);


                                    fptr.put_CommandBuffer("9D 91");
                                    if (fptr.RunCommand() == 0) {
                                        String answer = fptr.get_AnswerBuffer();
                                        String build = answer.substring(answer.length() - 5, answer.length()).replace(" ", "");
                                        version = Integer.valueOf(build);
                                    }
                                    if (fptr.put_Mode(IFptr.MODE_PROGRAMMING) < 0) {
                                        checkError();
                                    }
                                    if (fptr.SetMode() < 0) {
                                        checkError();
                                    }
                                    //устанавливаем настройки офд
                                    publishProgress(0);
                                    fptr.put_CaptionPurpose(getResources().getInteger(R.integer.url_ofd_caption_purpose));//Адрес сервера ОФД
                                    fptr.put_Caption(isTestFN? getString(R.string.url_ofd_sbis_test_caption) : ffdVersion > 100? getString(R.string.url_ofd_sbis_caption):getString(R.string.url_ofd_platforma_caption));
                                    if (fptr.SetCaption() < 0) {
                                        checkError();
                                    }
                                    publishProgress(1);
                                    fptr.put_CaptionPurpose(getResources().getInteger(R.integer.dns_ofd_caption_purpose));//DNS ОФД
                                    fptr.put_Caption(getString(R.string.dns_ofd_caption));
                                    if (fptr.SetCaption() < 0) {
                                        checkError();
                                    }
                                    publishProgress(2);
                                    fptr.put_CaptionPurpose(getResources().getInteger(R.integer.url_fns_caption_purpose));//Адрес сайта ФНС
                                    fptr.put_Caption(getString(R.string.url_fns_caption));
                                    if (fptr.SetCaption() < 0) {
                                        checkError();
                                    }
                                    publishProgress(3);
                                    fptr.put_CaptionPurpose(getResources().getInteger(R.integer.url_atol_caption_purpose));//Адрес сервера диагностики
                                    fptr.put_Caption(getString(R.string.url_atol_caption));
                                    if (fptr.SetCaption() < 0) {
                                        checkError();
                                    }
                                    publishProgress(4);
                                    fptr.put_ValuePurpose(getResources().getInteger(R.integer.port_ofd_value_purpose));//Порт сервера ОФД
                                    fptr.put_Value(isTestFN? getResources().getInteger(R.integer.port_ofd_sbis_value) : ffdVersion > 100? getResources().getInteger(R.integer.port_ofd_sbis_value) : getResources().getInteger(R.integer.port_ofd_platforma_value));//19081 тестовый
                                    if (fptr.SetValue() < 0) {
                                        checkError();
                                    }
                                    publishProgress(5);
                                    fptr.put_ValuePurpose(getResources().getInteger(R.integer.network_chanel_value_purpose));//Канал обмена с ОФД
                                    fptr.put_Value(version > 4000 ? getResources().getInteger(R.integer.network_chanel_eot_value) : getResources().getInteger(R.integer.network_chanel_gsm_value));//5-EoT,4-GSM
                                    if (fptr.SetValue() < 0) {
                                        checkError();
                                    }

                                    publishProgress(6);
                                    fptr.put_ValuePurpose(getResources().getInteger(R.integer.port_http_value_purpose));//Порт сервера диагностики
                                    fptr.put_Value(getResources().getInteger(R.integer.port_http_value));
                                    if (fptr.SetValue() < 0) {
                                        checkError();
                                    }
                                    if (version > 4000) {
                                        publishProgress(7);
                                        fptr.put_ValuePurpose(getResources().getInteger(R.integer.port_http_value_purpose));//Интервал посылок диагностических сообщений
                                        fptr.put_Value(getResources().getInteger(R.integer.port_http_value));//в секундах
                                        if (fptr.SetValue() < 0) {
                                            checkError();
                                        }
                                        publishProgress(8);
                                        fptr.put_ValuePurpose(getResources().getInteger(R.integer.port_http_value_purpose));//Интервал ожидания квитации ОФД
                                        fptr.put_Value(getResources().getInteger(R.integer.port_http_value));//в минутах
                                        if (fptr.SetValue() < 0) {
                                            checkError();
                                        }
                                    }
                                    publishProgress(9);
                                    new RunOFDDiagnostic(completeCallback, loadDataToOFDCallback).execute();

                                } catch (DriverException e) {
                                    fptr.destroy();
                                    fptr = null;
                                    completeCallback.call();
                                    mErrorMessage = e.getMessage();
                                    Toast.makeText(mContext, mErrorMessage, Toast.LENGTH_LONG).show();
                                }
                                return null;
                            }
                        }, new Callable() {
                            @Override
                            public Object call() throws Exception {
                                try {
                                    completeCallback.call();
                                    loadDataToOFDCallback.call();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        });

                    if (mErrorMessage.equalsIgnoreCase("")) {
                        Intent notificationIntent = new Intent(mContext, LoginActivity.class);
                        notificationIntent.setAction(Intent.ACTION_MAIN);
                        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext)
                                .setContentTitle(title)
                                .setTicker(getResources().getString(R.string.app_name))
                                .setContentText(reason)
                                .setSmallIcon(R.mipmap.ic_stat_notify)
                                .setContentIntent(pendingIntent).setLights(Color.RED, 1, 1);
                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        assert mNotificationManager != null;
                        mNotificationManager.notify(100, mBuilder.build());

                    }

                }

            } else {
                fptr.destroy();
                fptr = null;
                try {
                    completeCallback.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Toast.makeText(mContext,mErrorMessage,Toast.LENGTH_LONG).show();
                return;
            }

            if (!showdialog) {
                try {
                    completeCallback.call();
                    loadDataToOFDCallback.call();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected Boolean isTestFN(IFptr fptr) {
        fptr.put_RegisterNumber(getResources().getInteger(R.integer.register_fn_number));
        fptr.GetRegister();
        return  fptr.get_SerialNumber().substring(0,4).equalsIgnoreCase("9999");
    }

    protected void setSettings(String settings) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SettingsActivity.DEVICE_SETTINGS, settings);
        editor.apply();
    }

    protected String getSettings() {
        return preferences.getString(SettingsActivity.DEVICE_SETTINGS, getDefaultSettings());
    }

    protected String getDefaultSettings() {
        IFptr fprint = new Fptr();
        fprint.create(this);
        String settings = fprint.get_DeviceSettings();
        fprint.destroy();
        return settings;
    }


    public static class DriverException extends Exception {
        DriverException(String msg) {
            super(msg);
        }
    }


    public  void loadDataToOFD() {


        try {
            if (ofdLoader != null) ofdLoader.cancel();
            ofdLoader = new CheckOFD(getApplication());
        } catch (Exception e) {
            return;
        }

        if (timer != null ) {
            timer.cancel();
        }

        timer = new Timer();
        timer.schedule(ofdLoader,0,timerDelay);//проверяет каждые 20 секунд
    }


    public class CheckOFD extends TimerTask {


        int count = 0;
        boolean hasInternetConnection;

        CheckOFD(Context context) {
            count = 0;
            hasInternetConnection = checkInternetConnection(context);
        }

        @Override
        public void run() {


            if (!hasInternetConnection) {
                Message message = mOFDHandler.obtainMessage(NO_INTERNET_CONNECTION);
                message.sendToTarget();

                cancel();
                return;
            }

            try {
                fptr.ResetMode();
                fptr.put_RegisterNumber(getResources().getInteger(R.integer.register_quantity_unsent_ofd));
                fptr.GetRegister();
                int fnDocuments = fptr.get_Count();

                if (fnDocuments == 0) {
                    Message message = mOFDHandler.obtainMessage(OFD_OPERATION_COMPLETE);
                    message.sendToTarget();

                    cancel();
                    return;
                }

                if (count > timerTick && hasInternetConnection) {
                    Message message = mOFDHandler.obtainMessage(OFD_OPERATION_RUN_DIAGNOSTIC, fnDocuments);
                    message.sendToTarget();

                    cancel();
                    return;
                }

                Message message = mOFDHandler.obtainMessage(OFD_OPERATION_PROGRESS, fnDocuments,count);
                message.sendToTarget();

                count++;


            } catch (Exception e ){

                Message message = mOFDHandler.obtainMessage(OFD_OPERATION_COMPLETE);
                message.sendToTarget();

                cancel();
            }

        }

        @Override
        public boolean cancel() {

            if (super.cancel()) {
                if (fptr != null)
                fptr.destroy();
                fptr = null;
                if (timer !=null)
                timer.cancel();
                return true;
            }
            return false;
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fptr!= null) fptr.destroy();
        fptr = null;
        timer = null;
    }

    //хандлер для асинхронных тасков передачи данных в ОФД
    Handler mOFDHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {

            switch (message.what) {
                case OFD_OPERATION_PROGRESS:
                    showProgressDialog(String.format(Locale.getDefault(),getString(R.string.msg_do_not_turn_off_ofd_transfer),message.arg1,message.arg2,timerTick));
                    break;
                case OFD_OPERATION_COMPLETE:
                    hideProgressDialog();
                    Toast.makeText(getApplication(),getString(R.string.msg_complete_ofd_transfer),Toast.LENGTH_LONG).show();
                    break;
                case OFD_OPERATION_RUN_DIAGNOSTIC:
                    showProgressDialog(getString(R.string.action_check_unsent_data_ofd));
                    new GetNotSendedDocuments(new Callable<Boolean>() {
                        @Override
                        public Boolean call() {
                            hideProgressDialog();
                            return null;
                        }
                    }, new Callable<Void>() {
                        @Override
                        public Void call() {
                            loadDataToOFD();
                            return null;
                        }
                    }).execute(getApplication());
                    break;
                case NO_INTERNET_CONNECTION:
                    hideProgressDialog();
                    showMessageDialog(getString(R.string.msg_fail_ofd_transfer_title),getString(R.string.msg_fail_ofd_transfer),null);
                    break;
            }

        }
    };

    static Boolean checkInternetConnection(Context context) {

        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

class RunOFDDiagnostic extends AsyncTask<Object, Void, Void> {

    private Callable<Boolean> completeCallback;
    private Callable<Void> loadDataToOFDCallback;

    RunOFDDiagnostic(Callable<Boolean> completeCallback,Callable<Void> loadDataToOFDCallback) {

        this.completeCallback = completeCallback;
        this.loadDataToOFDCallback = loadDataToOFDCallback;
    }

    private void checkError() throws FPTRActivity.DriverException {
        int rc = fptr.get_ResultCode();
        if (rc < 0) {
            String rd = fptr.get_ResultDescription(), bpd = null;
            if (rc == -6) {
                bpd = fptr.get_BadParamDescription();
            }
            if (bpd != null) {
                throw new FPTRActivity.DriverException(String.format("[%d] %s (%s)", rc, rd, bpd));
            } else {
                throw new FPTRActivity.DriverException(String.format("[%d] %s", rc, rd));
            }
        }

    }

    @Override
    protected Void doInBackground(Object[] objects) {
        try {

            //переходим в режим отчетов
            if (fptr.ResetMode() < 0) {
                checkError();
            }
            if (fptr.put_ReportType(45) < 0) {
                checkError();
            }

            if (fptr.Report() < 0) {
                checkError();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (completeCallback != null) {
            try {
                completeCallback.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (loadDataToOFDCallback != null) {
            try {
                loadDataToOFDCallback.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}


}
