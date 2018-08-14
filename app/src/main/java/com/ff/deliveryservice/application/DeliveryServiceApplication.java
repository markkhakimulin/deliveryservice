package com.ff.deliveryservice.application;

import android.app.Application;
import android.content.Context;

import com.ff.deliveryservice.di.components.ApplicationComponent;
import com.ff.deliveryservice.di.components.DaggerApplicationComponent;
import com.ff.deliveryservice.di.components.LoginComponent;
import com.ff.deliveryservice.di.components.NavigationComponent;
import com.ff.deliveryservice.di.components.SignInComponent;
import com.ff.deliveryservice.di.module.ApplicationModule;
import com.ff.deliveryservice.di.module.LoginModule;
import com.ff.deliveryservice.di.module.NavigationModule;
import com.ff.deliveryservice.di.module.SignInModule;
import com.ff.deliveryservice.mvp.view.FPTRView;
import com.ff.deliveryservice.mvp.view.LoginView;
import com.ff.deliveryservice.mvp.view.OrderNavigationView;
import com.ff.deliveryservice.mvp.view.SignInView;

/**
 * Created by Mark Khakimulin on 12.07.2018.
 * mark.khakimulin@gmail.com
 */
public class DeliveryServiceApplication extends Application {

    private static ApplicationComponent appComponent;
    private static LoginComponent loginComponent;
    private static SignInComponent siComponent;
    private static NavigationComponent navComponent;

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();

        context = this;
    }
    public static ApplicationComponent getApplicationComponent() {
        return appComponent;
    }

    public static LoginComponent initLoginComponent(LoginView view) {

        loginComponent =  appComponent.plus(new LoginModule(view));
        return loginComponent;
    }
    public static LoginComponent getLoginComponent() {
        return loginComponent;
    }
    public static void destroyLoginComponent() {
        loginComponent = null;
    }

    public static SignInComponent initSignInComponent(SignInView view, String hash) {

        siComponent =  appComponent.plus(new SignInModule(view,hash));
        return siComponent;
    }
    public static SignInComponent getSignInComponent() {
        return siComponent;
    }
    public static void destroySignInComponent() {
        siComponent = null;
    }

    public static NavigationComponent initNavigationComponent(OrderNavigationView view, String userId) {

        navComponent =  appComponent.plus(new NavigationModule(view,userId));
        return navComponent;
    }

    public static NavigationComponent getNavComponent() {
        return navComponent;
    }

    public static void destroyNavigationComponent() {
        navComponent = null;
    }

    public static Context getContext() {
        return context;
    }
}