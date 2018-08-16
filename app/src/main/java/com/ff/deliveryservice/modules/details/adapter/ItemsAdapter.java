package com.ff.deliveryservice.modules.details.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.mvp.model.DBHelper;

/**
 * Created by Mark Khakimulin on 15.08.2018.
 * mark.khakimulin@gmail.com
 */
public class ItemsAdapter extends CursorAdapter {


    public ItemsAdapter(Context context, Cursor c) {
        super(context, c, true);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_row, parent, false);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        if (!cursor.isAfterLast()) {
            boolean checked = (1 == cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_CHECKED)));
            int count = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COUNT));
            float discount = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_DISCOUNT));
            float cost = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COST));
            ((CheckBox) view.findViewById(R.id.item_row_checkbox_view)).setChecked(checked);
            ((TextView) view.findViewById(R.id.item_row_description_view)).setText(cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION)));
            ((TextView) view.findViewById(R.id.item_id)).setText(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_EID)));
            ((TextView) view.findViewById(R.id.item_row_discount_view)).setText(Float.toString(discount));
            ((TextView) view.findViewById(R.id.item_row_cost_view)).setText(Float.toString(cost));
            ((TextView) view.findViewById(R.id.item_row_count_view)).setText(Integer.toString(count));
        }
    }
    @Override
    public Cursor swapCursor(Cursor newCursor) {
        Cursor old = super.swapCursor(newCursor);
        notifyDataSetChanged();
        return old;
    }
    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        notifyDataSetChanged();
    }
}
