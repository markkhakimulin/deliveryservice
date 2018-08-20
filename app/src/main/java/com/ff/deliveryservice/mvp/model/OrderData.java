package com.ff.deliveryservice.mvp.model;

import android.database.Cursor;
import android.os.Parcel;

/**
 * Created by Mark Khakimulin on 13.08.2018.
 * mark.khakimulin@gmail.com
 */
public class OrderData extends BaseData{

    private String mCode,mCustomer,mDate,mTime,mPhone,mAddress,mComment,mStatus,mRefuseReason;


    public OrderData() {
        super();

    }

    public OrderData(Cursor cursor) {
        fill(cursor);
    }

    public static final Creator<OrderData> CREATOR = new Creator<OrderData>() {
        // распаковываем объект из Parcel
        public OrderData createFromParcel(Parcel in) {
            return new OrderData(in);
        }

        public OrderData[] newArray(int size) {
            return new OrderData[size];
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

    private OrderData(Parcel parcel) {
        mId = parcel.readString();
        mDescription = parcel.readString();
    }

    public void fill(Cursor cursor) {

        mId = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID));
        mCode = cursor.getString(cursor.getColumnIndex(DBHelper.CN_CODE));
        mCustomer = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_CUSTOMER));
        mDate = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_DATE));
        mTime = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_TIME));
        if (mTime != null && !mTime.isEmpty()) mTime = mTime.replaceAll("\n",". ");
        mPhone = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_PHONE));
        mAddress = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_ADDRESS));
        mComment = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_COMMENT));
        mStatus = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_STATUS));
        mRefuseReason = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_REFUSE));

    }


    public String getCode() {
        return mCode;
    }

    public String getCustomer() {
        return mCustomer;
    }

    public String getDate() {
        return mDate;
    }
    public String getTime() {
        return this.mTime;
    }

    public String getPhone() {
        return mPhone;
    }

    public String getAddress() {
        return mAddress;
    }

    public String getComment() {
        return mComment;
    }

    public String getStatus() {
        return mStatus;
    }

    public String getRefuseReason() {
        return mRefuseReason;
    }
}
