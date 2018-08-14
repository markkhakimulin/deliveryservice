package com.ff.deliveryservice.di.components;

import com.ff.deliveryservice.di.module.SignInModule;
import com.ff.deliveryservice.modules.signin.SignInActivity;
import com.ff.deliveryservice.mvp.presenter.SignInPresenter;

import dagger.Subcomponent;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */
@Subcomponent(modules = SignInModule.class)
public interface SignInComponent {

    void inject(SignInActivity activity);
    void inject(SignInPresenter presenter);

}
