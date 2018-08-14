package com.ff.deliveryservice.di.components;

import com.ff.deliveryservice.di.module.NavigationModule;
import com.ff.deliveryservice.di.module.SignInModule;
import com.ff.deliveryservice.modules.navigation.OrderNavigationActivity;
import com.ff.deliveryservice.modules.navigation.adapter.OrderAdapter;
import com.ff.deliveryservice.modules.signin.SignInActivity;
import com.ff.deliveryservice.mvp.presenter.LoginPresenter;
import com.ff.deliveryservice.mvp.presenter.NavigationPresenter;
import com.ff.deliveryservice.mvp.presenter.SignInPresenter;

import dagger.Subcomponent;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */
@Subcomponent(modules = NavigationModule.class)
public interface NavigationComponent {

    void inject(NavigationPresenter presenter);
    void inject(OrderNavigationActivity navigationActivity);
}
