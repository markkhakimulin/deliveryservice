package com.ff.deliveryservice.base;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.di.components.ApplicationComponent;
import com.ff.deliveryservice.mvp.view.BaseView;

import java.util.concurrent.Callable;

import butterknife.ButterKnife;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */
public abstract class BaseActivity extends AppCompatActivity implements BaseView{

    private ProgressDialog mProgressDialog;
    //private Dialog mDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());
        ButterKnife.bind(this);
        onViewReady(savedInstanceState, getIntent());
    }

    @CallSuper
    protected void onViewReady(Bundle savedInstanceState, Intent intent) {
        resolveDaggerDependency();
        //To be used by child activities
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    protected abstract void resolveDaggerDependency();

    protected void showBackArrow() {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowHomeEnabled(true);
        }
    }

    public void showProgressDialog(String title,int progress,int max) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(true);
            if (max > 0) {
                mProgressDialog.setProgress(progress);
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

    public void showYesNoMessageDialog(String title, String message,@Nullable final Callable<Void> positiveCallback,@Nullable final Callable negativeCallback) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(title);
        //builder.setCancelable(false);
        if (positiveCallback != null)
        builder.setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {
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
        if (negativeCallback != null)
        builder.setNegativeButton(R.string.alert_button_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    dialog.dismiss();
                    if (negativeCallback != null) {
                        negativeCallback.call();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNeutralButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    public void showYesNoMessageDialog(String message,@Nullable final Callable<Void> positiveCallback) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
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

/*    protected ApplicationComponent getApplicationComponent() {
        return (ApplicationComponent) ((DeliveryServiceApplication) getApplication()).getAppComponent();
    }*/

    protected abstract int getContentView();

}