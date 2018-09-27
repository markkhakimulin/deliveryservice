package com.ff.deliveryservice.mvp.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;

/**
 * Created by Mark Khakimulin on 13.08.2018.
 * mark.khakimulin@gmail.com
 */
public class ChequeData implements Parcelable{


    private String mOrderId,mNotification,mContact;

    public void setChequeType(int mChequeType) {
        this.mChequeType = mChequeType;
    }

    private int mChequeType;
    private Map<Integer,Double> mPaymentsType;

    public String getCashier() {
        return mCashier;
    }

    public void setCashier(String mCashier) {
        this.mCashier = mCashier;
    }

    private String mCashier;

    public String getOrderId() {
        return mOrderId;
    }

    public String getContacts() {
        return mContact;
    }
    public String getmNotification() {
        return mContact;
    }

    public int getChequeType() {
        return mChequeType;
    }

    public Map<Integer, Double> getPaymentsType() {
        return mPaymentsType;
    }


    public ChequeData() {
        super();
    }
    public ChequeData(String orderId, String phoneOrEmail, int checkType, Map<Integer,Double> paymentTypes,String notification) {

        mOrderId = orderId;
        mContact = phoneOrEmail;
        mChequeType = checkType;
        mPaymentsType = paymentTypes;
        mNotification = notification;

    }

    public static final Parcelable.Creator<ChequeData> CREATOR = new Parcelable.Creator<ChequeData>() {
        // распаковываем объект из Parcel
        public ChequeData createFromParcel(Parcel in) {
            return new ChequeData(in);
        }

        public ChequeData[] newArray(int size) {
            return new ChequeData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(mOrderId);
        dest.writeString(mContact);
        dest.writeString(mNotification);
        dest.writeInt(mChequeType);
        dest.writeString(mCashier);
        dest.writeInt(mPaymentsType.size());
        for(Map.Entry<Integer,Double> entry : mPaymentsType.entrySet()){
            dest.writeInt(entry.getKey());
            dest.writeDouble(entry.getValue());
        }
    }


    private ChequeData(Parcel parcel) {
        mOrderId = parcel.readString();
        mContact = parcel.readString();
        mNotification = parcel.readString();
        mChequeType = parcel.readInt();
        mCashier = parcel.readString();
        int size = parcel.readInt();
        for(int i = 0; i < size; i++){
            Integer key = parcel.readInt();
            Double value = parcel.readDouble();
            mPaymentsType.put(key,value);
        }
    }


}
