package com.ff.deliveryservice.mvp.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Mark Khakimulin on 13.08.2018.
 * mark.khakimulin@gmail.com
 */
public class OrderItem extends BaseData{

    private float mPrice;
    private int mQuantity;

    public OrderItem() {
        super();

    }

    public OrderItem(String id, String title,float price,int quantity) {
        super(id, title);
        mPrice = price;
        mQuantity = quantity;
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


}
