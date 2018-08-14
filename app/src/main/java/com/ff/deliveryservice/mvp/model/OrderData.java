package com.ff.deliveryservice.mvp.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Mark Khakimulin on 13.08.2018.
 * mark.khakimulin@gmail.com
 */
public class OrderData extends BaseData implements Parcelable{



    public OrderData(String id, String title,OrderItem[] items) {
        super(id, title);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {


        dest.w

    }
}
