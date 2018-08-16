package com.ff.deliveryservice.di.components;

import com.ff.deliveryservice.di.module.DetailsModule;
import com.ff.deliveryservice.di.module.LoginModule;
import com.ff.deliveryservice.di.module.NavigationModule;
import com.ff.deliveryservice.di.module.SignInModule;
import com.ff.deliveryservice.modules.fptr.FPTRActivity;
import com.ff.deliveryservice.modules.fptr.FPTRService;
import com.ff.deliveryservice.modules.splash.MainActivity;
import com.ff.deliveryservice.di.module.ApplicationModule;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */
@Singleton
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent {

    //HttpTransportSE exposeHttpTransportSE();

    /*@Named(Constants.SOAP_METHOD_PUT_ORDER_PACK)
    SoapSerializationEnvelope exposeSoapSerializationEnvelopePutOrderPack();
    @Named(Constants.SOAP_METHOD_ORDER_PACK)
    SoapSerializationEnvelope exposeSoapSerializationEnvelopeOrderPack();

    @Named(Constants.SOAP_METHOD_PUT_ORDER_PACK)
    SoapObject exposeSoapObjectPutOrderPack();
    @Named(Constants.SOAP_METHOD_ORDER_PACK)
    SoapObject exposeSoapObjectOrderPack();*/

    //Resources exposeResources();
    //SharedPreferences exposeSharedPrefs();
    //DBHelper exposeDBHelper();
    FPTRService exposeFptrService();

    void inject(MainActivity mainActivity);
    void inject(FPTRActivity activity);
    void inject(FPTRService service);

    LoginComponent plus(LoginModule listModule);
    SignInComponent plus(SignInModule listModule);
    NavigationComponent plus(NavigationModule listModule);
    DetailsComponent plus(DetailsModule detailsModule);




}
