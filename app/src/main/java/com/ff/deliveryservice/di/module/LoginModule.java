package com.ff.deliveryservice.di.module;

import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.di.components.ApplicationComponent;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.mvp.presenter.LoginPresenter;
import com.ff.deliveryservice.mvp.view.LoginView;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */

@Module
public class LoginModule {
    private LoginView mView;

    public LoginModule(LoginView view) {
        mView = view;
    }

    @Provides
    LoginView provideView() {
        return mView;
    }

    @Provides
    @Named(Constants.SOAP_METHOD_LOGIN_LIST)
    SoapObject provideSoapObjectLoginList() {
        return new SoapObject(Constants.SOAP_NAMESPACE, Constants.SOAP_METHOD_LOGIN_LIST);
    }

    @Provides
    @Named(Constants.SOAP_METHOD_LOGIN_LIST)
    SoapSerializationEnvelope provideSoapSerializationEnvelopeLoginList(@Named(Constants.SOAP_METHOD_LOGIN_LIST) SoapObject soapObject) {
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
        envelope.dotNet = true;
        envelope.implicitTypes = true;
        envelope.skipNullProperties = true;
        envelope.setOutputSoapObject(soapObject);
        return envelope;
    }

    @Provides
    LoginPresenter provideLoginPresenter() {
        return new LoginPresenter(mView);
    }


}
