package com.ff.deliveryservice.di.module;

import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.mvp.presenter.DetailsPresenter;
import com.ff.deliveryservice.mvp.presenter.NavigationPresenter;
import com.ff.deliveryservice.mvp.view.OrderDetailsView;
import com.ff.deliveryservice.mvp.view.OrderNavigationView;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

import static com.ff.deliveryservice.common.Constants.FORMATDATE_1C;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */

@Module
public class DetailsModule {
    private OrderDetailsView mView;
    private String mUserId,mOrederId,mCodeId;

    public DetailsModule(OrderDetailsView view,String orderId,String codeId, String userId) {
        mView = view;
        mUserId = userId;
        mOrederId = orderId;
        mCodeId = codeId;
    }

    @Provides
    OrderDetailsView provideView() {
        return mView;
    }


    @Provides
    DetailsPresenter provideNavigationPresenter() {
        return new DetailsPresenter(mView,mCodeId,mOrederId,mUserId);
    }




}
