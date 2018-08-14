package com.ff.deliveryservice.di.module;

import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.mvp.presenter.SignInPresenter;
import com.ff.deliveryservice.mvp.view.SignInView;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */

@Module
public class SignInModule {
    private SignInView mView;
    private String mHash;

    public SignInModule(SignInView view,String hash) {
        mView = view;
        mHash = hash;
    }

    @Provides
    SignInView provideView() {
        return mView;
    }

    @Provides
    @Named(Constants.SOAP_METHOD_LOGIN)
    SoapObject provideSoapObjectLogin() {

        SoapObject so = new SoapObject(Constants.SOAP_NAMESPACE, Constants.SOAP_METHOD_LOGIN);
        so.addProperty("hash",mHash);
        return so;
    }

    @Provides
    @Named(Constants.SOAP_METHOD_LOGIN)
    SoapSerializationEnvelope provideSoapSerializationEnvelopeLogin(@Named(Constants.SOAP_METHOD_LOGIN) SoapObject soapObject) {
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
        envelope.dotNet = true;
        envelope.implicitTypes = true;
        envelope.setOutputSoapObject(soapObject);
        return envelope;
    }

    @Provides
    SignInPresenter provideSignInPresenter() {
        return new SignInPresenter(mView);
    }


}
