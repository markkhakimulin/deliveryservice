package com.ff.deliveryservice.di.module;

import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.mvp.presenter.NavigationPresenter;
import com.ff.deliveryservice.mvp.view.OrderNavigationView;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static com.ff.deliveryservice.common.Constants.FORMATDATE_1C;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */

@Module
public class NavigationModule {
    private OrderNavigationView mView;
    private String mUserId;

    public NavigationModule(OrderNavigationView view, String userId) {
        mView = view;
        mUserId = userId;
    }

    @Provides
    OrderNavigationView provideView() {
        return mView;
    }

    @Provides
    SoapObject provideSoapObjectGetOrderPack() {
        DateFormat dateFormat = new SimpleDateFormat(FORMATDATE_1C,Locale.getDefault());

        SoapObject so = new SoapObject(Constants.SOAP_NAMESPACE, Constants.SOAP_METHOD_ORDER_PACK);
        so.addProperty("LoginID",mUserId);
        so.addProperty("Date",dateFormat.format(new Date()));
        return so;
    }

    @Provides
    @Named(Constants.SOAP_METHOD_ORDER_PACK)
    SoapSerializationEnvelope provideSoapSerializationEnvelopeGetOrderPack(SoapObject soapObject) {
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
        envelope.dotNet = true;
        envelope.implicitTypes = true;
        envelope.setOutputSoapObject(soapObject);
        return envelope;
    }


    @Provides
    @Named(Constants.SOAP_METHOD_PUT_ORDER_PACK)
    SoapSerializationEnvelope provideSoapSerializationEnvelopePutOrderPack() {
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
        envelope.dotNet = true;
        envelope.implicitTypes = true;
        return envelope;
    }


    @Provides
    NavigationPresenter provideNavigationPresenter() {
        return new NavigationPresenter(mView,mUserId);
    }


}
