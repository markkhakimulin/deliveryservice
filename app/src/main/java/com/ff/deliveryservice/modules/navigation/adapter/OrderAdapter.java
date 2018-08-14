package com.ff.deliveryservice.modules.navigation.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.TextView;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.mvp.model.DBHelper;

/**
 * Created by Mark Khakimulin on 10.08.2018.
 * mark.khakimulin@gmail.com
 */
public class OrderAdapter extends CursorAdapter {
    LayoutInflater lInflater;
    public OrderAdapter(Context context, Cursor cursor,FilterQueryProvider filterQueryProvider) {
        super(context,cursor,FLAG_REGISTER_CONTENT_OBSERVER);

        lInflater = LayoutInflater.from(context);
        setFilterQueryProvider(filterQueryProvider);
    }


    public void bindView(View view, Context context, Cursor cursor) {


        String dateString = cursor.getString(cursor.getColumnIndex( DBHelper.CN_ORDER_DATE ));
        //((TextView) view.findViewById(R.id.order_item_map)).setText(cursor.getString(cursor.getColumnIndex( DBHelper.CN_ORDER_MAP_ID )));
        ((TextView) view.findViewById(R.id.order_item_title)).setText(cursor.getString(cursor.getColumnIndex( DBHelper.CN_CODE )));
        ((TextView) view.findViewById(R.id.order_item_date)).setText(dateString);
        ((TextView) view.findViewById(R.id.order_item_time)).setText(cursor.getString(cursor.getColumnIndex( DBHelper.CN_ORDER_TIME )));
        ((TextView) view.findViewById(R.id.order_item_customer)).setText(cursor.getString(cursor.getColumnIndex( DBHelper.CN_ORDER_CUSTOMER )));
        ((TextView) view.findViewById(R.id.order_item_status)).setText(cursor.getString(cursor.getColumnIndex( DBHelper.CN_ORDER_STATUS)));
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return  lInflater.inflate(R.layout.item, parent, false);
    }

}