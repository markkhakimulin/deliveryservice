package com.ff.deliveryservice.modules.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.modules.fptr.FPTRActivity;
import com.ff.deliveryservice.modules.login.adapter.UserAdapter;
import com.ff.deliveryservice.modules.scanner.FullScannerResultActivity;
import com.ff.deliveryservice.modules.signin.SignInActivity;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.mvp.model.UserData;
import com.ff.deliveryservice.mvp.presenter.LoginPresenter;
import com.ff.deliveryservice.mvp.view.LoginView;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;

import static com.ff.deliveryservice.common.Constants.SP_USER_ID;
import static com.ff.deliveryservice.common.Constants.SP_USER_NAME;


/**
 * Created by khakimulin on 03.02.2017.
 */

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends FPTRActivity implements LoginView {


    private static final int REQUEST_SHOW_LOADING = 2;
    private static final int REQUEST_SHOW_SCANNER = 3;


    // UI references.
    @BindView(R.id.login)
    AutoCompleteTextView mLoginView;

    @BindView(R.id.barcodeButton)
    ImageButton mBarcodeButton;

    @BindView(R.id.login_progress)
    View mProgressView;

    @BindView(R.id.login_form)
    SwipeRefreshLayout mLoginFormView;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.login_sign_in_button)
    Button mSignInButton;

    @Inject
    protected LoginPresenter presenter;

/*    @Inject
    public SharedPreferences preferences;*/

    @Override
    protected void onViewReady(Bundle savedInstanceState, Intent intent) {

        super.onViewReady(savedInstanceState, intent);

        setSupportActionBar(mToolbar);

        //если очищаем поле ввода то принудительно показываем список все курьеров
        mLoginView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (TextUtils.isEmpty(((AutoCompleteTextView)v).getText())) {
                    presenter.onLoginTextCleared();
                    return true;
                }
                //тут надо добавить проверку если стерли имя то обнулять выбранный елемент из списка в адаптере
                return false;
            }
        });

        mBarcodeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(LoginActivity.this, FullScannerResultActivity.class);
                startActivityForResult(intent,REQUEST_SHOW_SCANNER);
            }
        });

        mSignInButton.requestFocus();
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onSignInClicked();
            }
        });

        mLoginFormView.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                presenter.onFormClicked();
            }
        });

        mLoginFormView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                mLoginView.setError(null);
                presenter.onFormRefreshed();
            }
        });

        mLoginFormView.setColorSchemeColors(getColorWrapper(this,R.color.holo_blue_light),
                getColorWrapper(this,R.color.holo_green_light),
                getColorWrapper(this,R.color.holo_orange_light),
                getColorWrapper(this,R.color.holo_red_light));

        if (!preferences.getString(SP_USER_NAME,"").isEmpty()) {
            mLoginView.setText(preferences.getString(SP_USER_NAME,""));
        }

    }

    @Override
    protected void resolveDaggerDependency() {
        DeliveryServiceApplication.initLoginComponent(this).inject(this);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_login;
    }

    public void setLastUser(UserData lastUser) {
        mLoginView.setError(null);
        mLoginView.setText(lastUser.toString());
    }

    public void onUserLoaded(List<UserData> users) {

        hideKeyboard(this);

        final UserAdapter userAdapter = new UserAdapter(this,users);
        mLoginView.setError(null);
        mLoginView.setAdapter(userAdapter);
        mLoginView.showDropDown();
        mLoginView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                userAdapter.setSelectedPosition(position);

                preferences = getSharedPreferences(Constants.FPTR_PREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(SP_USER_NAME,userAdapter.getSelectedData().getDescription());
                editor.putString(SP_USER_ID,userAdapter.getSelectedData().getId());
                editor.apply();
            }
        });
    }

    public UserData getSelectedUser() {
        return mLoginView.getAdapter() == null?null:((UserAdapter)mLoginView.getAdapter()).getSelectedData();
    }
    public String getUserName() {
        return mLoginView.getText().toString();
    }


    public void showProgress() {
        mLoginFormView.setRefreshing(true);
    }

    public void hideProgress() {
        mLoginFormView.setRefreshing(false);
    }

    public void showInputError(String errorMessage) {
        mLoginView.setError(String.format(Locale.getDefault(),errorMessage));
        mLoginView.requestFocus();
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
                presenter.onBarcodeScanned(barcode);
            }

        }

    }

    public static void startLoginActivity(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }

    public void startLoadingActivity(UserData userData) {
        Intent intent = new Intent(LoginActivity.this, SignInActivity.class);
        intent.putExtra(DBHelper.CN_ID,userData.getId());
        intent.putExtra(DBHelper.CN_DESCRIPTION,userData.getDescription());
        intent.putExtra("password","no");//whit out password

        hideKeyboard(this);

        startActivityForResult(intent,REQUEST_SHOW_LOADING);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DeliveryServiceApplication.destroyLoginComponent();
    }

   /* @Override
    protected FPTRPresenter getParentPresenter() {
        return presenter;
    }*/

    @Override
    public void onShowDialog(String message) {
        showYesNoMessageDialog(message,null);
    }

    @Override
    public void onHideDialog() {
        //hideDialog();
    }

    @Override
    public void onShowToast(String message) {
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
    }

   /* @Override
    public void onShowProgressDialog(int min, int max, String message) {
        su
    }*/

/*    @Override
    public void onHideProgressDialog() {

    }*/
}

