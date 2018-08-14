package com.ff.deliveryservice.modules.splash;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.modules.login.LoginActivity;

/**
 * Created by Mark Khakimulin on 04.07.2018.
 * mark.khakimulin@gmail.com
 */
public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DeliveryServiceApplication.getApplicationComponent().inject(this);

        LoginActivity.startLoginActivity(this);

        finish();
    }
}
