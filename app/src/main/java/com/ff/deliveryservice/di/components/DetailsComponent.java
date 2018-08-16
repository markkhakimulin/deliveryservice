package com.ff.deliveryservice.di.components;

import com.ff.deliveryservice.di.module.DetailsModule;
import com.ff.deliveryservice.modules.details.OrderDetailsActivity;
import com.ff.deliveryservice.modules.details.dialogs.ChequeConfirmDialog;
import com.ff.deliveryservice.modules.details.fragments.OrderDetailsFragment;
import com.ff.deliveryservice.modules.details.fragments.OrderPaymentsFragment;
import com.ff.deliveryservice.mvp.presenter.DetailsPresenter;

import dagger.Subcomponent;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */
@Subcomponent(modules = DetailsModule.class)
public interface DetailsComponent {


    ChequeConfirmDialog exposeChequeConfirmDialog();

    void inject(DetailsPresenter presenter);
    void inject(OrderDetailsActivity activity);
    //void inject(OrderPaymentsFragment orderPaymentsFragment);
    //void inject(OrderDetailsFragment orderDetailsFragment);
}
