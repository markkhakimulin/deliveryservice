package com.ff.deliveryservice.di.components;

import com.ff.deliveryservice.di.module.LoginModule;
import com.ff.deliveryservice.modules.login.LoginActivity;
import com.ff.deliveryservice.mvp.presenter.LoginPresenter;

import dagger.Subcomponent;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */
@Subcomponent(modules = LoginModule.class)
public interface LoginComponent{

    void inject(LoginActivity activity);
    void inject(LoginPresenter presenter);




}
