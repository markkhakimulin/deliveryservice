package com.ff.deliveryservice.mvp.presenter;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.base.BasePresenter;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.mvp.view.BaseView;
import com.ff.deliveryservice.mvp.view.LoginView;
import com.ff.deliveryservice.mvp.view.SignInView;
import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by Mark Khakimulin on 09.07.2018.
 * mark.khakimulin@gmail.com
 */
public class SignInPresenter extends BasePresenter<SignInView>
{
    private SignInTask mAuthTask;

    @Inject
    public DBHelper dbHelper;

    @Inject
    public Resources res;

    @Inject
    public HttpTransportSE httpTransportSE;

    @Inject
    @Named(Constants.SOAP_METHOD_LOGIN)
    public SoapSerializationEnvelope serializationEnvelope;

    @Inject
    public SignInPresenter(SignInView view) {
        mView = view;
        DeliveryServiceApplication.getSignInComponent().inject(this);
    }

    @Override
    protected SignInView getView() {
        return (SignInView)mView;
    }

    public interface SignInCallback {
        void onSignIn(Boolean success,String session);
        void onError(int errorId, String errorMessage);
    }

    public void signIn(Intent intent,SignInCallback callback) {
        mAuthTask = new SignInTask(intent,callback);
        mAuthTask.execute();
    }

    public Boolean inProcess() {
        return mAuthTask != null && mAuthTask.getStatus() != AsyncTask.Status.FINISHED;
    };


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    @SuppressLint("StaticFieldLeak")
    public class SignInTask extends AsyncTask<Void, Void, Boolean> {

        private final String method = Constants.SOAP_METHOD_LOGIN;
        private final String namespace = Constants.SOAP_NAMESPACE;
        private final String soap_action = String.format("%s#Obmen:%s",namespace,method);
        private String errorMessage = "";
        private String errorCode = "";
        SoapSerializationEnvelope envelope;
        Intent mIntent;
        SignInCallback callback;

        SignInTask(Intent intent,SignInCallback callback) {
            mIntent = intent;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            return true;

            /*try {

                System.setProperty("http.keepAlive", "false");

                httpTransportSE.call(soap_action, serializationEnvelope);
                // Get the SoapResult from the envelope body.
                SoapObject response = (SoapObject) envelope.getResponse();
                try {
                    //writing to database
                    SoapPrimitive resultObject = (SoapPrimitive) response.getProperty("Result");
                    SoapPrimitive resultMessage = (SoapPrimitive) response.getProperty("Name");
                    Boolean result = Boolean.valueOf(resultObject.toString());
                    if (!result )
                    {
                        errorMessage = resultMessage.toString();
                        return false;
                    }

                    return true;
                } catch (Exception e) {

                    errorMessage = e.getMessage();
                    return false;
                }//process response

            } catch (Exception  e) {

                SoapFault responseFault = (SoapFault) envelope.bodyIn;

                if (responseFault == null)
                {
                    errorMessage = "Нет соединения с интернетом или ошиибка на сервере.";
                } else
                    try {
                        errorCode = responseFault.faultcode;
                        errorMessage = responseFault.faultstring;
                    } catch (Exception ef) {
                        errorMessage = "Неизвестная ошибка.";
                    }
                return false;
            }//process request*/
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            onCancelled();

            if (success) {

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if (db == null) {
                    return;
                }
                ContentValues cv = new ContentValues();

                cv.put("time",  new Date().getTime());
                cv.put(DBHelper.CN_ID,  mIntent.getStringExtra(DBHelper.CN_ID));
                cv.put(DBHelper.CN_DESCRIPTION,  mIntent.getStringExtra(DBHelper.CN_DESCRIPTION));

                db.update(DBHelper.TB_DRIVERS, cv, "_id = ?", new String[] { mIntent.getStringExtra(DBHelper.CN_ID) } );
                db.close();

                callback.onSignIn(true,"");

            } else {

                callback.onError(R.string.error_incorrect_password,errorMessage);
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            callback.onSignIn(false,null);
        }



    }


}