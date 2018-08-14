package com.ff.deliveryservice.modules.login.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

import com.ff.deliveryservice.mvp.model.UserData;

import java.util.List;

/**
 * Created by Mark Khakimulin on 05.07.2018.
 * mark.khakimulin@gmail.com
 */
public class UserAdapter extends ArrayAdapter<UserData> {

    int sPosition = -1;

    public UserAdapter(@NonNull Context context,@NonNull List<UserData> objects) {
        super(context, android.R.layout.simple_dropdown_item_1line,android.R.id.text1, objects);
    }

    public void setSelectedPosition(int position) {
        sPosition = position;
    }

    public int getSelectedPosition() {
        return sPosition;
    }

    public UserData getSelectedData() {
        if (sPosition < 0 && sPosition>= getCount()) return null;
        return getItem(sPosition);
    }

}