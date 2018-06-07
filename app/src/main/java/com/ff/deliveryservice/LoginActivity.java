package com.ff.deliveryservice;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.text.Format;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by khakimulin on 03.02.2017.
 */

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends FPTRActivity {


    private static final int REQUEST_SHOW_LOADING = 2;
    private static final int REQUEST_SHOW_SCANNER = 3;

    private RefreshLoginBackgroundTask mRefreshTask = null;

    // UI references.
    private AutoCompleteTextView mLoginView;
    private View mProgressView;
    private SwipeRefreshLayout mLoginFormView;

    private SQLiteOpenHelper  sqLiteOpenHelper;
    private String mVersionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        //init db
        sqLiteOpenHelper = DBHelper.getOpenHelper(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Set up the login form.
        mLoginView = (AutoCompleteTextView) findViewById(R.id.login);
        String lastLogin = getLastLogin();
        if (lastLogin != ""){
            mLoginView.setText(lastLogin);
            loginDesc = lastLogin;
        }

        ImageButton barcodeButton = (ImageButton) findViewById(R.id.barcodeButton);
        barcodeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, FullScannerResultActivity.class);
                startActivityForResult(intent,REQUEST_SHOW_SCANNER);
            }
        });


        Button mLoginSignInButton = (Button) findViewById(R.id.login_sign_in_button);
        mLoginSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                attemptLogin("",true);
            }
        });
        mLoginSignInButton.requestFocus();

        mLoginFormView = (SwipeRefreshLayout) findViewById(R.id.login_form);
        mLoginFormView.setOnClickListener(new OnClickListener(){
            @Override
           public void onClick(View v) {

                mRefreshTask = null;
                mLoginFormView.setRefreshing(false);
            }
        });
        mLoginFormView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                 initiateRefresh();
            }
        });

        mLoginFormView.setColorSchemeColors(getColorWrapper(this,R.color.holo_blue_light),
                getColorWrapper(this,R.color.holo_green_light),
                getColorWrapper(this,R.color.holo_orange_light),
                getColorWrapper(this,R.color.holo_red_light));
        mProgressView = findViewById(R.id.login_progress);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_about) {

            PackageManager packageManager = getPackageManager();
            String packageName = getPackageName();

            String mVersion = "version : ";
            String mVersionName = "not available";

            try {
                mVersionName = packageManager.getPackageInfo(packageName, 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }


            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setMessage(mVersion+mVersionName+"\nmark.khakimulin@gmail.com").setTitle(getString(R.string.title_about_app));
            builder.create().show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void initiateRefresh()
    {
        mRefreshTask = new RefreshLoginBackgroundTask();
        mRefreshTask.execute((Void) null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode,resultCode,data);

        if (requestCode == REQUEST_SHOW_LOADING  && resultCode == RESULT_CANCELED)
        {

            if (data != null && data.getBooleanExtra("login_is_invalid",false))
            {
                mLoginView.setError(getString(R.string.error_incorrect_login));
                mLoginView.requestFocus();
            } else
            if (data != null) {

                Toast.makeText(this,data.getStringExtra("description"),Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_SHOW_SCANNER && resultCode == RESULT_OK)
        {
            if (data != null && data.getExtras() != null) {
                String barcode = data.getStringExtra("barcode");
                attemptLogin(barcode, false);
            }

        }

    }

    public void attemptLogin(String id,Boolean checkEmptyLogin) {
        // Reset errors.
        mLoginView.setError(null);
        // Store values at the time of the login attempt.
        String login = mLoginView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(login) && checkEmptyLogin) {
            mLoginView.setError(getString(R.string.error_field_required));
            focusView = mLoginView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {


            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            String noDescription = "0";
            if (login.isEmpty()) noDescription = "1";
            String noId = "0";
            if (id.isEmpty()) noId = "1";
            Cursor cur = db.rawQuery(String.format("select * from drivers where (description = ? or ? = '1') and (_id = ? or ? = '1')"), new String[] { login,noDescription,id,noId });
            if (cur.moveToNext()) {

                db.close();

                hideKeyboard(this);

                Intent intent = new Intent(LoginActivity.this, LoadingActivity.class);
                intent.putExtra(DBHelper.CN_ID,cur.getString(cur.getColumnIndex(DBHelper.CN_ID)));
                intent.putExtra(DBHelper.CN_DESCRIPTION,cur.getString(cur.getColumnIndex(DBHelper.CN_DESCRIPTION)));
                intent.putExtra("password","no");//whit out password
                cur.close();
                startActivityForResult(intent,REQUEST_SHOW_LOADING);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.error_found_login_message).setTitle(R.string.error_found_login_title);
                builder.create().show();
            }
        }
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        assert imm != null;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }



//    for different versions
    public static int getColorWrapper(Context context, int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getColor(id);
        } else {
            //noinspection deprecation
            return context.getResources().getColor(id);
        }
    }

    private String  getLastLogin() {
        String login = "";

        SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
        if (db == null) {
            return login;
        }
        Cursor cur = db.query(DBHelper.TB_DRIVERS,new String[]{DBHelper.CN_DESCRIPTION},null,null,null,null,"time DESC","1");
        if (cur.moveToNext()) {

            login = cur.getString(0);

        }
        cur.close();
        db.close();

        return login;
    }

    private List<String>  getLoginList() {
        List<String> loginList = new ArrayList<>();

        SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
        if (db == null) {
            return loginList;
        }
        Cursor cur = db.rawQuery("select description from drivers",null);
        while(cur.moveToNext()) {

            loginList.add(cur.getString(0));

        }
        cur.close();
        db.close();

        return loginList;
    }

    public class RefreshLoginBackgroundTask extends AsyncTask<Void, Void, Boolean> {

        private final String url = getString(R.string.soap_url);
        private final String method = getString(R.string.soap_method_login_list);
        private final String namespace = getString(R.string.soap_namespace);
        private final String soap_action = String.format("%s#Obmen:%s",namespace,method);
        private String errorMessage = "";
        private String errorCode = "";
        SoapSerializationEnvelope envelope;
        List<String> loginList = new ArrayList<>();


        RefreshLoginBackgroundTask() {}

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                SoapObject soapObject = new SoapObject(namespace, method);

                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
                envelope.dotNet = true;
                envelope.implicitTypes = true;
                envelope.setOutputSoapObject(soapObject);

                //System.setProperty("http.keepAlive", "false");

                HttpTransportSE androidHttpTransport = new HttpTransportSE(url,20000);
                androidHttpTransport.debug = true;
                androidHttpTransport.call(soap_action, envelope);
                // Get the SoapResult from the envelope body.
                SoapObject response = (SoapObject) envelope.getResponse();
                try {
                    //writing to database
                    SQLiteDatabase db = sqLiteOpenHelper.getWritableDatabase();
                    if (db == null) {
                        return false;
                    }



                    int count = response.getPropertyCount();
                    if (count > 0) {
                        //clear table
                        db.delete("drivers",null,null);
                    }
                    for (int i = 0; i < count; i++)
                    {
                        SoapObject login = (SoapObject) response.getProperty(i);
                        String name = login.getPropertyAsString("Description");
                        String id = login.getPropertyAsString("ID");
                        ContentValues row = new ContentValues();
                        row.put(DBHelper.CN_DESCRIPTION, name);
                        row.put(DBHelper.CN_ID, id);
                        db.replace(DBHelper.TB_DRIVERS,null, row);
                    }
                    db.close();

                    return true;
                } catch (Exception e) {

                    errorMessage = e.getMessage();
                    return false;
                }//process response

            } catch (Exception  e) {

               SoapFault responseFault = (SoapFault) envelope.bodyIn;

                if (responseFault == null)
                {
                    errorMessage = getString(R.string.error_internet_connection);
                } else
                try {
                    errorCode = responseFault.faultcode;
                    errorMessage = responseFault.faultstring;
                } catch (Exception ef) {
                    errorMessage = getString(R.string.error_unknown);
                }
                return false;
            }//process request
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            onCancelled();

            if (success) {

                mLoginView.setAdapter(new ArrayAdapter<>(LoginActivity.this,android.R.layout.simple_dropdown_item_1line,getLoginList()));
                mLoginView.showDropDown();
                mLoginView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {


                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });

            } else {

                mLoginView.setError(String.format("[%s]: &s",errorCode,errorMessage));
                mLoginView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mRefreshTask = null;
            mLoginFormView.setRefreshing(false);
        }
    }


}

