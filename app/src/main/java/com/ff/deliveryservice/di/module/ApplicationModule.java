package com.ff.deliveryservice.di.module;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.mvp.model.DBHelper;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Mark Khakimulin on 12.07.2018.
 * mark.khakimulin@gmail.com
 */
@Module
public class ApplicationModule {

    private Application mApplication;

    public ApplicationModule(Application mApplication) {
        this.mApplication = mApplication;
    }


    @Singleton
    @Provides
    HttpTransportSE provideHttpTransportSE() {
        return new HttpTransportSE(Constants.SOAP_URL,30000);//30 seconds
    }

    @Provides
    @Singleton
    Application provideApplication() {
        return mApplication;
    }

    @Singleton
    @Provides
    public Resources provideResources() {
        return mApplication.getResources();
    }


    @Singleton
    @Provides
    Context provideContext() {
        return mApplication;
    }

    @Singleton
    @Provides
    DBHelper provideDBHelper() {
        return new DBHelper(mApplication);
    }

    @Singleton
    @Provides
    SharedPreferences provideSharedPreferences() {
        return mApplication.getSharedPreferences(Constants.FPTR_PREFERENCES, Context.MODE_PRIVATE);
    }

   /* @Provides
    FPTRPresenter provideFPTRPresenter() {
        return new FPTRPresenter<FPTRView>(mApplication);
    }*/

}