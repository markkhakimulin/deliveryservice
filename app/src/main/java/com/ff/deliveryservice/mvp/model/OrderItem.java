package com.ff.deliveryservice.mvp.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Mark Khakimulin on 13.08.2018.
 * mark.khakimulin@gmail.com
 */
public class OrderItem extends BaseData{

    private float mPrice,mDiscount,mPriceDelivery,mPaymentDelivery;
    private int mQuantity,mChecked,mCompleted;
    private String mOrderId,mItemId,mEid;

    public OrderItem() {
        super();

    }

    public OrderItem(Cursor cursor) {
        fill(cursor);
    }

    public OrderItem(String id, String title,float price,float discount ,int quantity) {
        this(id, title,price,discount,quantity,0);
    }

    public OrderItem(String id, String title,float price,float discount ,int quantity,int checked) {
        super(id, title);
        mPrice = price;
        mDiscount = discount;
        mQuantity = quantity;
        mChecked = checked;
    }

    public float getPrice() {
        return mPrice;
    }

    public int getQuantity() {
        return mQuantity;
    }

    public static final Parcelable.Creator<OrderItem> CREATOR = new Parcelable.Creator<OrderItem>() {
        // распаковываем объект из Parcel
        public OrderItem createFromParcel(Parcel in) {
            return new OrderItem(in);
        }

        public OrderItem[] newArray(int size) {
            return new OrderItem[size];
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
        dest.writeInt(getQuantity());
        dest.writeFloat(getPrice());

    }

    private OrderItem(Parcel parcel) {
        mId = parcel.readString();
        mDescription = parcel.readString();
        mQuantity = parcel.readInt();
        mPrice = parcel.readFloat();
    }


    public int getChecked() {
        return mChecked;
    }

    public void setChecked(int mChecked) {
        this.mChecked = mChecked;
    }

    public float getDiscount() {
        return mDiscount;
    }

/*     public String getOrderId() {
        return mOrderId;
    }*/

    public String getItemId() {
        return mItemId;
    }

    public String getEid() {
        return mEid;
    }

    public int getCompleted() {
        return mCompleted;
    }

    public float getPriceDelivery() {
        return mPriceDelivery;
    }

    public float getPaymentDelivery() {
        return mPaymentDelivery;
    }


    public void fill(Cursor cursor) {
        mId = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID));
        //mOrderId = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_ID));
        mItemId = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_ID));
        mChecked = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_CHECKED));
        mDiscount = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_DISCOUNT));
        mPrice = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COST));
        mQuantity = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COUNT));
        mDescription = cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION));
        mCompleted = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_COMPLETED));
        mPriceDelivery = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_DELIVERY_COST));
        mPaymentDelivery = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_LEVEL_DELIVERY_PAY));
        mEid = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_EID));

    }



}
