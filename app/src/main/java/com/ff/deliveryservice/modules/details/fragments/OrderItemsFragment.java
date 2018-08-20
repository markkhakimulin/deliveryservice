package com.ff.deliveryservice.modules.details.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ff.deliveryservice.base.BaseFragment;
import com.ff.deliveryservice.modules.details.adapter.DetailsFragmentList;
import com.ff.deliveryservice.modules.details.adapter.ItemsAdapter;
import com.ff.deliveryservice.modules.details.adapter.PaymentsAdapter;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.modules.details.OrderDetailsActivity;
import com.ff.deliveryservice.R;
import com.ff.deliveryservice.mvp.model.OrderItem;
import com.ff.deliveryservice.mvp.view.BaseView;

import java.util.ArrayList;

public class OrderItemsFragment extends BaseFragment implements
        AbsListView.OnScrollListener,
        AbsListView.OnItemLongClickListener,
        AbsListView.OnItemClickListener,
        BaseView {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    public static OrderItemsFragment fragment;
    public View itemsFooter;
    public OnFragmentHandler mFragmentCreatedHandler;
    public int mItemCount = 0;
    public float mDiscount = 0,mCost = 0,mSumm = 0;
    public Context context;


    public OrderItemsFragment() {

    }
    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static OrderItemsFragment newInstance(OnFragmentHandler fragmentCreatedHandler) {
        fragment = new OrderItemsFragment();
        fragment.mFragmentCreatedHandler = fragmentCreatedHandler;
        return fragment;
    }

    @Override
    protected DetailsFragmentList getNewCursorAdapter(ArrayList<?> list) {
        return new ItemsAdapter(getActivity(), (ArrayList<OrderItem>) list);
    }

    public void recalculateFooter(Cursor cursor) {

        if (cursor.isClosed()) return;
        mItemCount = 0;
        mDiscount = mCost = mSumm = 0;
        float levelDeliveryPay = 0;
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            boolean checked = (1 == cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_CHECKED)));
            int count = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COUNT));
            float discount = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_DISCOUNT));
            float cost = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COST));
            levelDeliveryPay = Math.max(cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_LEVEL_DELIVERY_PAY)),levelDeliveryPay);
            int count_check = checked ? count : 0;
            float discount_check = checked ? discount : 0;
            float cost_check = checked ? cost : 0;
            mItemCount += count_check;
            mDiscount += discount_check * count_check;
            mCost += cost_check * count_check;

        }
        mSumm = mCost - mDiscount ;

        ((TextView) itemsFooter.findViewById(R.id.item_row_footer_count_view)).setText(Integer.toString(mItemCount));
        ((TextView) itemsFooter.findViewById(R.id.item_row_footer_cost_view)).setText(Float.toString(mCost));
        ((TextView) itemsFooter.findViewById(R.id.item_row_footer_discount_view)).setText(Float.toString(mDiscount));
        ((TextView) itemsFooter.findViewById(R.id.item_row_footer_topay_view)).setText(Float.toString(mSumm));
        ((TextView) itemsFooter.findViewById(R.id.footer_delivery_info_text)).setText(Float.toString(levelDeliveryPay)+" р. - порог бесплатной доставки");

        if (mListView.getFooterViewsCount() == 0){

            mListView.setFooterDividersEnabled(true);
            //mListView.addFooterView(itemsFooter);

        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        OrderItem item = (OrderItem)adapter.getItem(position);
        mFragmentCreatedHandler.onItemClicked(item.getItemId(),item.getEid());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_order_items, container,false);
        itemsFooter = inflater.inflate(R.layout.item_row_footer, null ,false);
        mListView = rootView.findViewById(android.R.id.list);
        return rootView;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //the dividers
        mFragmentCreatedHandler.onFragmentViewCreated(getClass().getCanonicalName());
        mListView.setOnScrollListener(this);
        mListView.setOnItemLongClickListener(this);
        mListView.setOnItemClickListener(this);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        OrderItem item = (OrderItem)adapter.getItem(position);
        mFragmentCreatedHandler.onItemLongClicked(item.getItemId());
        return true;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        final int lastItem = firstVisibleItem + visibleItemCount;//1 - это футер
        if(lastItem == totalItemCount && lastItem != visibleItemCount){
            ((OrderDetailsActivity ) getActivity()).hideFabButtons();
        } else {
            ((OrderDetailsActivity ) getActivity()).showFabButtons();
        }

    }

}