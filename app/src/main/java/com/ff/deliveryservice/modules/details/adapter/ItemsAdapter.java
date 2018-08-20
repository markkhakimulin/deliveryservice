package com.ff.deliveryservice.modules.details.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.mvp.model.BaseData;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.mvp.model.OrderItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark Khakimulin on 15.08.2018.
 * mark.khakimulin@gmail.com
 */
public class ItemsAdapter extends ArrayAdapter<OrderItem> implements DetailsFragmentList<OrderItem> {


    public ItemsAdapter(Context context, ArrayList<OrderItem> items) {
        super(context, R.layout.item_row, items);
    }


    @SuppressLint("DefaultLocale")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            view = layoutInflater.inflate(R.layout.item_row, null);
        }

        OrderItem item = getItem(position);

        if (item != null) {
            boolean checked = item.getChecked() == 1;
            int quantity = item.getQuantity();
            float discount = item.getDiscount();
            float cost = item.getPrice();
            ((CheckBox) view.findViewById(R.id.item_row_checkbox_view)).setChecked(checked);
            ((TextView) view.findViewById(R.id.item_row_description_view)).setText(item.getDescription());
            ((TextView) view.findViewById(R.id.item_id)).setText(item.getEid());
            ((TextView) view.findViewById(R.id.item_row_discount_view)).setText(String.format("%f",discount));
            ((TextView) view.findViewById(R.id.item_row_cost_view)).setText(String.format("%f",cost));
            ((TextView) view.findViewById(R.id.item_row_count_view)).setText(String.format("%d",quantity));
        }

        return view;
    }

    @Override
    public void update(ArrayList<OrderItem> list) {
        clear();
        for (OrderItem item : list) {
            insert(item,getCount());
        }
        notifyDataSetChanged();
    }
}
