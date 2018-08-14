package com.ff.deliveryservice.mvp.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.ksoap2.serialization.SoapObject;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import java.io.Serializable;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */
public class BaseData{

    protected String mId;

    protected String mDescription;

    public BaseData(String id,String desctiption) {
        mId = id;
        mDescription = desctiption;
    }

    public String getId() {
        return mId;
    }

    public String getDescription() {
        return mDescription;
    }

}
