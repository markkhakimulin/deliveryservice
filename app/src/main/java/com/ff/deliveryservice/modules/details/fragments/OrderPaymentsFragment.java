package com.ff.deliveryservice.modules.details.fragments;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import ru.atol.drivers10.fptr.IFptr;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.R;

public class OrderPaymentsFragment extends ListFragment implements AbsListView.OnItemLongClickListener {
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

    public void updateCursor(Cursor cursor) {

        setListAdapter(new CursorAdapter(getActivity(), cursor, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER) {

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {

                LayoutInflater inflater = LayoutInflater.from(context);
                View view = inflater.inflate(R.layout.payment_row, parent, false);
                return view;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {

                if (!cursor.isAfterLast()) {


                    String paymentType = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_TYPE));
                    float payment = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_SUM));
                    float discount = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_DISCOUNT));
                    String chequeTypeDescription = cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION));
                    String chequeNumber = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_CHECK_NUMBER));
                    String chequeDate = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_DATE));
                    String chequeSession = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_CHECK_SESSION));
                    ((TextView) view.findViewById(R.id.cheque_row_type_view)).setText(chequeTypeDescription);
                    ((TextView) view.findViewById(R.id.payment_row_type_view)).setText(paymentType);
                    ((TextView) view.findViewById(R.id.payment_row_sum_view)).setText(Float.toString(payment - discount));
                    ((TextView) view.findViewById(R.id.cheque_number)).setText(chequeNumber);
                    ((TextView) view.findViewById(R.id.cheque_date)).setText(chequeDate.substring(0,16));
                    ((TextView) view.findViewById(R.id.cheque_session)).setText(chequeSession);
                    int chequeType = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_CHEQUE_TYPE));
                    if (chequeType == IFptr.LIBFPTR_RT_SELL ) {
                        //((ImageView) view.findViewById(R.id.icon_cheque_type)).setImageResource(R.mipmap.plus);
                        view.setBackground(getResources().getDrawable(R.drawable.payment_sell));
                    }
                    else if (chequeType == IFptr.LIBFPTR_RT_SELL_RETURN ) {
                        //((ImageView) view.findViewById(R.id.icon_cheque_type)).setImageResource(R.mipmap.minus);
                        view.setBackground(getResources().getDrawable(R.drawable.payment_return));
                    }
                }
            }
        });


    }


    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {


        if (position == getListAdapter().getCount()) return false;

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = inflater.inflate(R.layout.payment_edit_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(convertView);
        builder.setCancelable(true);
        builder.setTitle("Изменение суммы");
        final AlertDialog dialog = builder.create();

        Cursor cursor = ((CursorAdapter) this.getListAdapter()).getCursor();
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
                changePaymentValue(payment_id,summ);
                dialog.dismiss();
            }
        });
        dialog.show();

        return true;
    }

    void changePaymentValue(int payment_id,double summ) {
        SQLiteOpenHelper sqLiteOpenHelper = DBHelper.getOpenHelper(getActivity());
        SQLiteDatabase db = sqLiteOpenHelper.getWritableDatabase();


        ContentValues cv = new ContentValues();
        cv.put(DBHelper.CN_ORDER_PAYMENT_SUM, summ);
        db.update(DBHelper.TB_ORDER_PAYMENTS,cv,"_id = ?",new String[]{String.valueOf(payment_id)});
        db.close();

        mFragmentCreatedHandler.onEditPaymentComplete();


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
        if (getListView().getFooterViewsCount() == 0) {
            getListView().addFooterView(mFooter);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_order_payments, container,false);
        mFooter = inflater.inflate(R.layout.payment_row, null ,false);
        mFooter.setClickable(false);
        mFooter.setEnabled(false);
        return rootView;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //запускаем таск обновления для платежей
        mFragmentCreatedHandler.onFragmentViewCreated(getClass().getCanonicalName());
        getListView().setOnItemLongClickListener(this);
    }

}