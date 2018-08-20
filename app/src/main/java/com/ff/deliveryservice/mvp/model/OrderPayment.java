package com.ff.deliveryservice.mvp.model;

import android.database.Cursor;
import android.os.Parcel;

/**
 * Created by Mark Khakimulin on 13.08.2018.
 * mark.khakimulin@gmail.com
 */
public class OrderPayment extends BaseData{

    private float mSum,mDiscount;
    private int mChequeNumber,mSession,mChequeType;
    private String mDate,mChequeTypeDescription,mPaymentTypeDescription,mPaymentTypeId;

    public OrderPayment() {
        super();

    }

    public OrderPayment(Cursor cursor) {
        fill(cursor);
    }

    public static final Creator<OrderPayment> CREATOR = new Creator<OrderPayment>() {
        // распаковываем объект из Parcel
        public OrderPayment createFromParcel(Parcel in) {
            return new OrderPayment(in);
        }

        public OrderPayment[] newArray(int size) {
            return new OrderPayment[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(getId());
        dest.writeString(getDescription());

    }

    private OrderPayment(Parcel parcel) {
        mId = parcel.readString();
        mDescription = parcel.readString();
    }

    public int getChequeType() {
        return mChequeType;
    }

    public int getChequeNumber() {
        return mChequeNumber;
    }

     public int getSession() {
        return mSession;
    }

    public String getDate() {
        return mDate;
    }



    public void fill(Cursor cursor) {
        mId = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID));
        mChequeTypeDescription = cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION));
        mPaymentTypeDescription = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_TYPE));
        mChequeNumber = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_CHECK_NUMBER));
        mSession = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_CHECK_SESSION));
        mDate = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_DATE));
        mPaymentTypeId = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_TYPE_ID));
        mChequeType = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_CHEQUE_TYPE));
        mDiscount = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_DISCOUNT));
        mSum = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_SUM));
    }

    public float getSum() {
        return mSum;
    }

    public float getDiscount() {
        return mDiscount;
    }
    public String getPaymentTypeId() {
        return mPaymentTypeId;
    }

    public String getPaymentTypeDescription() {
        return mPaymentTypeDescription;
    }

    public String getChequeTypeDescription() {
        return mChequeTypeDescription;
    }
}
