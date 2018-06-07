package com.ff.deliveryservice.fragments;

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
import android.widget.ListView;
import android.widget.TextView;

import com.ff.deliveryservice.DBHelper;
import com.ff.deliveryservice.OrderDetailsActivity;
import com.ff.deliveryservice.R;

public class OrderItemsFragment extends ListFragment implements AbsListView.OnScrollListener,AbsListView.OnItemLongClickListener {
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

    public void updateCursor(Cursor cursor) {


        if (((CursorAdapter)getListAdapter()) == null) {

            setListAdapter(new CursorAdapter(getActivity(), cursor, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER) {

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
            });
        } else {
            ((CursorAdapter)getListAdapter()).swapCursor(cursor);
        }
    }

    public void recalculateFooter(Cursor cursor) {

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

        if (getListView().getFooterViewsCount() == 0){

            getListView().addFooterView(itemsFooter);

        }

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Cursor cursor = ((CursorAdapter) this.getListAdapter()).getCursor();
        cursor.moveToPosition(position);
        String itemId =  cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_ID));
        String eid =  cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_EID));
        mFragmentCreatedHandler.onItemClicked(itemId,eid);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_order_items, container,false);
        itemsFooter = inflater.inflate(R.layout.item_row_footer, null ,false);

        return rootView;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //the dividers
        mFragmentCreatedHandler.onFragmentViewCreated(getClass().getCanonicalName());
        getListView().setOnScrollListener(this);
        getListView().setOnItemLongClickListener(this);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = ((CursorAdapter) this.getListAdapter()).getCursor();
        cursor.moveToPosition(position);
        String itemId =  cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_ID));
        mFragmentCreatedHandler.onItemLongClicked(itemId);
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