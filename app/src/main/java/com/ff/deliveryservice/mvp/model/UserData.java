package com.ff.deliveryservice.mvp.model;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Mark Khakimulin on 02.07.2018.
 * mark.khakimulin@gmail.com
 */
public class UserData extends BaseData {


    public UserData() {

    }

    public UserData(String id, String title) {
        super(id, title);
    }

    public ContentValues toContentValues() {
        ContentValues cv =  new ContentValues();
        cv.put(DBHelper.CN_DESCRIPTION, getDescription());
        cv.put(DBHelper.CN_ID, getId());
        return cv;
    }

    public String toString() {
        return  getDescription();
    }

    public static final Parcelable.Creator<UserData> CREATOR = new Parcelable.Creator<UserData>() {
        // распаковываем объект из Parcel
        public UserData createFromParcel(Parcel in) {
            return new UserData(in);
        }

        public UserData[] newArray(int size) {
            return new UserData[size];
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

    public UserData(Parcel parcel) {
        mId = parcel.readString();
        mDescription = parcel.readString();
    }
}
