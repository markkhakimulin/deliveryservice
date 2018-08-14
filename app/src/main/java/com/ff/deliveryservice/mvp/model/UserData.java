package com.ff.deliveryservice.mvp.model;

import android.content.ContentValues;

/**
 * Created by Mark Khakimulin on 02.07.2018.
 * mark.khakimulin@gmail.com
 */
public class UserData extends BaseData {


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
}
