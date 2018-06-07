package com.ff.deliveryservice;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.util.Date;
/**
 * Created by khakimulin on 05.02.2017.
 */

/**
 * A loading screen showed while auth in process.
 */
public class LoadingActivity extends AppCompatActivity {

    private UserLoginTask mAuthTask = null;
    private View mProgressView;
    private SQLiteOpenHelper sqLiteOpenHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        sqLiteOpenHelper = DBHelper.getOpenHelper(this);

        mProgressView = findViewById(R.id.progress);


        showProgress(true);

        mAuthTask = new UserLoginTask(getIntent());
        mAuthTask.execute((Void) null);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String url = getString(R.string.soap_url);
        private final String method = getString(R.string.soap_method_login);
        private final String namespace = getString(R.string.soap_namespace);
        private final String soap_action = String.format("%s#Obmen:%s",namespace,method);
        private String errorMessage = "";
        private String errorCode = "";
        SoapSerializationEnvelope envelope;

        Intent mIntent;
        Intent mReturnIntent = new Intent();
        Boolean mIncorrectLogin = false,mIncorrectPassword = false;


        UserLoginTask(Intent intent) {
            mIntent = intent;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            return true;


            //disable method
            /*try {

                String id = mIntent.getStringExtra(DBHelper.CN_ID);
                String password = mIntent.getStringExtra("password");

                SoapObject soapObject = new SoapObject(namespace, method);
                soapObject.addProperty("ID",id);
                soapObject.addProperty("Password",password);

                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
                envelope.dotNet = true;
                envelope.implicitTypes = true;
                envelope.setOutputSoapObject(soapObject);

                System.setProperty("http.keepAlive", "false");

                HttpTransportSE androidHttpTransport = new HttpTransportSE(url,20000);
                androidHttpTransport.debug = true;
                androidHttpTransport.call(soap_action, envelope);
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

                SQLiteDatabase db = sqLiteOpenHelper.getWritableDatabase();
                if (db == null) {
                    return;
                }
                ContentValues cv = new ContentValues();

                cv.put("time",  new Date().getTime());
                cv.put(DBHelper.CN_ID,  mIntent.getStringExtra(DBHelper.CN_ID));
                cv.put(DBHelper.CN_DESCRIPTION,  mIntent.getStringExtra(DBHelper.CN_DESCRIPTION));

                db.update(DBHelper.TB_DRIVERS, cv, "_id = ?", new String[] { mIntent.getStringExtra(DBHelper.CN_ID) } );
                db.close();

                Intent intent = new Intent(LoadingActivity.this, OrderNavigationActivity.class);
                intent.fillIn(mIntent,Intent.FILL_IN_DATA);
                startActivity(intent);
            } else {

                mReturnIntent.putExtra("password_is_invalid", mIncorrectPassword);
                mReturnIntent.putExtra("login_is_invalid", mIncorrectLogin);
                mReturnIntent.putExtra("description", errorMessage);
                setResult(RESULT_CANCELED, mReturnIntent);
            }
            finish();
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
