package com.ff.deliveryservice.modules.signin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.base.BaseActivity;
//import com.ff.deliveryservice.modules.navigation.OrderNavigationActivity;
import com.ff.deliveryservice.modules.navigation.OrderNavigationActivity;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.mvp.presenter.SignInPresenter;
import com.ff.deliveryservice.mvp.view.BaseView;
import com.ff.deliveryservice.mvp.view.SignInView;
import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;

import butterknife.BindView;
/**
 * Created by khakimulin on 05.02.2017.
 */

/**
 * A loading screen showed while auth in process.
 */
public class SignInActivity extends BaseActivity implements SignInView {

    @BindView(R.id.progress)
    public View mProgressView;

    @Inject
    SignInPresenter presenter;

   /* @Inject
    Resources res;*/

    @Override
    protected int getContentView() {
        return R.layout.activity_loading;
    }
    @Override
    protected void resolveDaggerDependency() {

        String id = getIntent().getStringExtra(DBHelper.CN_ID);
        String password = getIntent().getStringExtra(DBHelper.CN_PASSWORD);

        String hash = id;
        try {
            hash = hash(id,password);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        DeliveryServiceApplication.initSignInComponent(this,hash).inject(this);

        if (!presenter.inProcess()) {
            onShowDialog("Попытка входа");
            presenter.signIn(getIntent(), new SignInPresenter.SignInCallback() {
                @Override
                public void onSignIn(Boolean success, String session) {

                    showProgress(false);

                    Intent intent = new Intent(SignInActivity.this, OrderNavigationActivity.class);
                    intent.fillIn(getIntent(), Intent.FILL_IN_DATA);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(int errorId, String errorMessage) {

                    Intent intent = new Intent();
                    intent.putExtra("error_id", errorId);
                    intent.putExtra("error_description", errorMessage);
                    setResult(RESULT_CANCELED, intent);
                    finish();
                }
            });
        }
    }

    public String hash(String id, String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        HashFunction hf = Hashing.sha256();
        HashCode hc = hf.newHasher()
                .putString(id, Charsets.UTF_8)
                .putString(password, Charsets.UTF_8)
                .hash();
        return hc.toString();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {

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
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onShowDialog(String message) {
        showProgress(true);
    }

    @Override
    public void onHideDialog() {
        showProgress(false);
    }

    @Override
    public void onShowToast(String message) {

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        DeliveryServiceApplication.destroySignInComponent();
    }
}
