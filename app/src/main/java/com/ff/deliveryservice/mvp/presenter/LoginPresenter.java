package com.ff.deliveryservice.mvp.presenter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.base.BasePresenter;
import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.mvp.model.UserData;
import com.ff.deliveryservice.mvp.view.LoginView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.xmlpull.v1.XmlSerializer;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.XMLFormatter;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;


/**
 * Created by Mark Khakimulin on 03.07.2018.
 * mark.khakimulin@gmail.com
 */
public class LoginPresenter extends BasePresenter<LoginView> {

    @Inject
    Resources res;

    @Inject
    DBHelper dbHelper;

    @Inject
    HttpTransportSE httpTransportSE;

    @Inject
    @Named(Constants.SOAP_METHOD_LOGIN_LIST)
    SoapSerializationEnvelope serializationEnvelope;

    @Inject
    public LoginPresenter(LoginView loginView) {
        //super(DeliveryServiceApplication.getContext(),loginView);
        mView = loginView;
        DeliveryServiceApplication.getLoginComponent().inject(this);
    }


   /* @Override
    protected LoginView getView() {
        return (LoginView)mView;
    }*/

    public void onSignInClicked(){
        UserData userData = getView().getSelectedUser();
        if (userData == null) {

            if (getView().getUserName().isEmpty()) {

                getView().showInputError(res.getString(R.string.error_incorrect_login));

            } else {
                checkUserName(getView().getUserName(), new GetUserCallback() {
                    @Override
                    public void onLoad(List<UserData> users) {

                        if (users.size() > 0) {

                             getView().startLoadingActivity(users.get(0));
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        getView().onShowToast(errorMessage);
                    }
                });
            }
        } else {
            getView().startLoadingActivity(userData);
        }
    }

    public void onBarcodeScanned(String barcode){
        getView().onShowDialog(res.getString(R.string.msg_progress_0));
        checkUserBarcode(barcode, new GetUserCallback() {
            @Override
            public void onLoad(List<UserData> users) {
                getView().onHideDialog();
                if (users.size() > 0) {
                    getView().startLoadingActivity(users.get(0));
                }
            }

            @Override
            public void onError(String errorMessage) {
                getView().onHideDialog();
                getView().onShowToast(errorMessage);
            }
        });
    }

    public void onLoginTextCleared(){
        getView().hideProgress();
        getUsers(new GetUserCallback() {
            @Override
            public void onLoad(List<UserData> users) {
                getView().onUserLoaded(users);
            }

            @Override
            public void onError(String errorMessage) {
                getView().onShowToast(errorMessage);
            }
        });
    }

    public void onFormClicked(){
        stopRefresh();
        getView().hideProgress();
    }

    public void onFormRefreshed(){
        getView().showProgress();
        refreshUsers(new GetUserCallback() {
            @Override
            public void onLoad(List<UserData> users) {
                getView().hideProgress();
                getView().onUserLoaded(users);
            }

            @Override
            public void onError(String errorMessage) {
                getView().hideProgress();
                getView().onShowToast(errorMessage);
            }
        });

    }


    private RefreshUsersTask refreshUsersTask;


    public interface GetUserCallback {
        void onLoad(List<UserData> users);
        void onError(String errorMessage);
    }

    public void refreshUsers(GetUserCallback callback) {
        refreshUsersTask = new RefreshUsersTask(callback);
        refreshUsersTask.execute();
    }

    public Boolean isRefreshing() {
        return refreshUsersTask != null && refreshUsersTask.getStatus() != AsyncTask.Status.FINISHED;
    };

    public void stopRefresh() {
        if (isRefreshing()) {
            refreshUsersTask.cancel(true);
        }
    };

    public void getUsers(GetUserCallback callback) {
        GetUsersTask getUsersTask = new GetUsersTask(callback);
        getUsersTask.execute();
    }
    public void getLastUser(GetUserCallback callback) {
        GetLastUserTask getLastUserTask = new GetLastUserTask(callback);
        getLastUserTask.execute();
    }
    public void checkUserBarcode(String barcode,GetUserCallback callback) {
        AttemptLoginViaBarcodeTask checkUserBarcode = new AttemptLoginViaBarcodeTask(barcode,callback);
        checkUserBarcode.execute();
    }
    public void checkUserName(String userName,GetUserCallback callback) {
        AttemptLoginViaNameTask checkUserBarcode = new AttemptLoginViaNameTask(userName,callback);
        checkUserBarcode.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class RefreshUsersTask extends AsyncTask<Void, Void, Boolean> {

        private final String method = Constants.SOAP_METHOD_LOGIN_LIST;
        private final String namespace = Constants.SOAP_NAMESPACE;
        private final String soap_action = String.format("%s#Obmen:%s",namespace,method);
        private String errorMessage = "";
        private int errorId = -1;
        private List<UserData> loginList;
        private GetUserCallback callback;

        RefreshUsersTask(GetUserCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            errorMessage = "";
            errorId = -1;

            try {
                httpTransportSE.call(soap_action, serializationEnvelope);
                // Get the SoapResult from the envelope body.
                SoapObject response = (SoapObject) serializationEnvelope.getResponse();
                try {
                    //writing to database
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    if (db == null) {
                        errorId = R.string.db_error_open_for_writing;
                        return false;
                    }

                    int count = response.getPropertyCount();
                    if (count > 0) {
                        //clear whole table
                        db.delete(DBHelper.TB_DRIVERS,null,null);
                    }

                    loginList= new LinkedList<>();

                    for (int i = 0; i < count; i++)
                    {

                        SoapObject so = (SoapObject) response.getProperty(i);
                        UserData user = new UserData(so.getPropertyAsString(Constants.ID),so.getPropertyAsString(Constants.DESCRIPTION));

                        db.replace(DBHelper.TB_DRIVERS,null, user.toContentValues());
                        loginList.add(user);
                    }
                    db.close();

                    return true;
                } catch (Exception e) {

                    errorMessage = e.getMessage();
                    return false;
                }//process response

            } catch (Exception  e) {

                SoapFault responseFault = (SoapFault) serializationEnvelope.bodyIn;

                if (responseFault == null)
                {
                    errorId = R.string.error_internet_connection;
                } else
                    try {
                        errorMessage = responseFault.faultstring;
                    } catch (Exception ef) {
                        errorId = R.string.error_unknown;
                        errorMessage = ef.getMessage();
                    }
                return false;
            }//process request
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {
                callback.onLoad(loginList);
            } else {
                callback.onError(String.format("%s: %s",res.getString(errorId),errorMessage));
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetLastUserTask extends AsyncTask<Void, Void, Boolean> {

        private String errorMessage = "";
        private int errorId = -1;
        private GetUserCallback callback;
        private List<UserData> loginList;

        GetLastUserTask(GetUserCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            errorMessage = "";
            errorId = -1;

            try {
                //reading from database
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (db == null) {
                    errorId = R.string.db_error_open_for_reading;
                    return false;
                }

                loginList= new LinkedList<>();
                Cursor cursor = db.query(DBHelper.TB_DRIVERS,null,null,null,null,null,"time DESC","1");
                while (cursor.moveToNext()) {
                    UserData user = new UserData(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID)),
                                                 cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION)));
                    loginList.add(user);
                }
                cursor.close();
                db.close();

                return true;
            } catch (Exception e) {

                errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {
                callback.onLoad(loginList);
            } else {
                callback.onError(String.format("%s: %s",res.getString(errorId),errorMessage));
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetUsersTask extends AsyncTask<Void, Void, Boolean> {

        private String errorMessage = "";
        private int errorId = -1;
        private GetUserCallback callback;
        private List<UserData> loginList;

        GetUsersTask(GetUserCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            errorMessage = "";
            errorId = -1;

            try {
                //reading from database
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (db == null) {
                    errorId = R.string.db_error_open_for_reading;
                    return false;
                }

                loginList= new LinkedList<>();
                Cursor cursor = db.query(DBHelper.TB_DRIVERS, null, null, null, null, null, null);
                while (cursor.moveToNext()) {
                    UserData user = new UserData(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID)),
                            cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION)));
                    loginList.add(user);
                }
                cursor.close();
                db.close();

                return true;
            } catch (Exception e) {

                errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {
                callback.onLoad(loginList);
            } else {
                callback.onError(String.format("%s: %s",res.getString(errorId),errorMessage));
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class AttemptLoginViaBarcodeTask extends AsyncTask<Void, Void, Boolean> {

        private String errorMessage = "",barcode;
        private int errorId = -1;
        private GetUserCallback callback;
        private List<UserData> loginList;

        AttemptLoginViaBarcodeTask(String barcode,GetUserCallback callback) {
            this.barcode = barcode;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            errorMessage = "";
            errorId = -1;

            try {
                //reading from database
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (db == null) {
                    errorId = R.string.db_error_open_for_reading;
                    return false;
                }

                loginList= new LinkedList<>();

                Cursor cursor = db.rawQuery(String.format("select * from drivers where barcode = ?"), new String[] { barcode});
                if (cursor.moveToNext()) {

                    UserData user = new UserData(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID)),cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION)));
                    loginList.add(user);

                    cursor.close();
                    db.close();
                } else {
                    errorId = R.string.error_found_login_message;
                }

                return true;
            } catch (Exception e) {

                errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {
                callback.onLoad(loginList);
            } else {
                callback.onError(String.format("%s: %s",res.getString(errorId),errorMessage));
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class AttemptLoginViaNameTask extends AsyncTask<Void, Void, Boolean> {

        private String errorMessage = "",userName;
        private int errorId = -1;
        private GetUserCallback callback;
        private List<UserData> loginList;

        AttemptLoginViaNameTask(String userName,GetUserCallback callback) {
            this.userName = userName;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            errorMessage = "";
            errorId = -1;

            try {
                //reading from database
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (db == null) {
                    errorId = R.string.db_error_open_for_reading;
                    return false;
                }

                loginList= new LinkedList<>();

                Cursor cursor = db.rawQuery(String.format("select * from drivers where description  = ?"), new String[] { userName});
                if (cursor.moveToNext()) {

                    UserData user = new UserData(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID)),cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION)));
                    loginList.add(user);

                    cursor.close();
                    db.close();
                } else {
                    errorId = R.string.error_found_login_message;
                }

                return true;
            } catch (Exception e) {

                errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {
                callback.onLoad(loginList);
            } else {
                callback.onError(String.format("%s: %s",res.getString(errorId),errorMessage));
            }
        }
    }

    /*public void onDestroy() {
        super.onDestroy();
    }*/




}
