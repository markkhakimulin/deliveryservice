package com.ff.deliveryservice.modules.details.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.base.BaseFragment;
import com.ff.deliveryservice.modules.details.adapter.PaymentsAdapter;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.R;
import com.ff.deliveryservice.mvp.presenter.DetailsPresenter;
import com.ff.deliveryservice.mvp.view.BaseView;

import javax.inject.Inject;

public class OrderPaymentsFragment extends BaseFragment implements AbsListView.OnItemLongClickListener, BaseView {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */

    public static OrderPaymentsFragment fragment;
    public View mFooter;
    public OnFragmentHandler mFragmentCreatedHandler;
    public float mSumm = 0,mDiscount = 0;
    //private Cursor mCursor;

    public OrderPaymentsFragment() {

    }
    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static OrderPaymentsFragment newInstance(OnFragmentHandler fragmentCreatedHandler) {
        fragment = new OrderPaymentsFragment();
        fragment.mFragmentCreatedHandler = fragmentCreatedHandler;
        return fragment;
    }


    protected CursorAdapter getNewCursorAdapter(Cursor cursor) {
        return ((CursorAdapter) new PaymentsAdapter(getActivity(),cursor));
    }


    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {


        if (position == mListView.getAdapter().getCount()) return false;

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = inflater.inflate(R.layout.payment_edit_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(convertView);
        builder.setCancelable(true);
        builder.setTitle("Изменение суммы");
        final AlertDialog dialog = builder.create();

        Cursor cursor = ((CursorAdapter) this.mListView.getAdapter()).getCursor();
        cursor.moveToPosition(position);
        final int payment_id = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ID));
        double payment_summ = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_SUM));
        final EditText editSumm = convertView.findViewById(R.id.edit_summ);
        editSumm.setText(String.valueOf(Math.abs(payment_summ)));
        Button applyButton = convertView.findViewById(R.id.button_apply);
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double summ = 0;
                if (editSumm.getText().toString() != "") summ = Double.valueOf(editSumm.getText().toString());

                mFragmentCreatedHandler.onChangePaymentValue(payment_id,summ);

                dialog.dismiss();
            }
        });
        dialog.show();

        return true;
    }

    public void recalculateFooter(Cursor cursor) {

        mSumm = 0;
        mDiscount = 0;
        while (cursor.moveToNext()) {
            float cost = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_SUM));
            float disc = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_DISCOUNT));
            mSumm += cost;
            mDiscount += disc;
        }
        ((TextView) mFooter.findViewById(R.id.payment_row_sum_view)).setText(Float.toString(mSumm - mDiscount));
        if (mListView.getFooterViewsCount() == 0) {
            mListView.addFooterView(mFooter);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_order_payments, container,false);
        mFooter = inflater.inflate(R.layout.payment_row, null ,false);
        mFooter.setClickable(false);
        mFooter.setEnabled(false);

        mListView = rootView.findViewById(android.R.id.list);
        return rootView;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //запускаем таск обновления для платежей
        mFragmentCreatedHandler.onFragmentViewCreated(getClass().getCanonicalName());
        mListView.setOnItemLongClickListener(this);
    }

}