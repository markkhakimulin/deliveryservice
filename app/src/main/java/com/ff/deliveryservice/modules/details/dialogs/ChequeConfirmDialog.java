package com.ff.deliveryservice.modules.details.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.modules.details.OrderDetailsActivity;
import com.ff.deliveryservice.R;
import com.ff.deliveryservice.modules.details.fragments.OnFragmentHandler;

import java.util.Map;

import javax.inject.Inject;

public class ChequeConfirmDialog extends DialogFragment implements DialogInterface.OnCancelListener{

    public static ChequeConfirmDialog fragment;
    public OnFragmentHandler mFragmentCreatedHandler;
    private Cursor mCursor;
    private Map<Integer,Double> mPaymentTypeCode;
    private int mCheckType;
    private double mSumm = 0,mDiscount = 0;
    private String mNotification = null,orderId;

    @Inject
    DBHelper dbHelper;

    @Inject
    public ChequeConfirmDialog() {}


    public String getChequeDescription() {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String descriprion = "";
        Cursor cursor = db.rawQuery(String.format("select description from check_types where code = ?"),  new String[] {Integer.toString(mCheckType)});
        cursor.moveToNext();
        descriprion += cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION));

        String args = mPaymentTypeCode.keySet().toString().replaceAll("\\[|\\]|\\s", "");
        cursor = db.rawQuery("select description from payment_types where selectable = 1 and code in ("+args+")", null);
        while (cursor.moveToNext()) {
            descriprion += " ; " + cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION));
        }
        return descriprion;
    }

    public static ChequeConfirmDialog newInstance(OnFragmentHandler fragmentCreatedHandler, Cursor cursor, Map<Integer,Double> paymentTypeCode, int checkType) {
        fragment = new ChequeConfirmDialog();
        fragment.mCursor = cursor;
        fragment.mPaymentTypeCode = paymentTypeCode;
        fragment.mCheckType = checkType;
        fragment.mFragmentCreatedHandler = fragmentCreatedHandler;
        return fragment;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.cheque, null);
        ListView items = (ListView) view.findViewById(R.id.items_checked);
        items.setAdapter(new CursorAdapter(getActivity(), mCursor, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER) {

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {

                LayoutInflater inflater = LayoutInflater.from(context);
                View view = inflater.inflate(R.layout.cheque_row, parent, false);
                return view;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {

                if (!cursor.isAfterLast()) {
                    int count = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COUNT));
                    float discount = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_DISCOUNT));
                    float cost = cursor.getFloat(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COST));
                    orderId = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_ID));
                    ((TextView) view.findViewById(R.id.item_row_description_view)).setText(cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION)));
                    ((TextView) view.findViewById(R.id.item_row_discount_view)).setText(Float.toString(discount));
                    ((TextView) view.findViewById(R.id.item_row_cost_view)).setText(Float.toString(cost));
                    ((TextView) view.findViewById(R.id.item_row_count_view)).setText(Integer.toString(count));

                }
            }
        });
        TextView commonSumm = (TextView) view.findViewById(R.id.common_summ);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        commonSumm.setText(commonSumm.getText() +Double.toString(dbHelper.getSumToPay(db,mCheckType,orderId))+getString(R.string.currency));
        TextView commonChequeDescription = (TextView) view.findViewById(R.id.common_payment_description);
        commonChequeDescription.setText(getChequeDescription());

        Spinner notificationSpinner = (Spinner) view.findViewById(R.id.notification);
        notificationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                SQLiteDatabase db = dbHelper.getReadableDatabase();

                //тут записываем текущее состояние счетчиков чеков ккм
                if (position == 1) {
                    Cursor cursor = db.query(DBHelper.TB_ORDERS, new String[]{DBHelper.CN_ORDER_PHONE}, "_id = ?", new String[]{orderId}, null, null, null, null);
                    cursor.moveToNext();
                    String phone = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_PHONE));
                    cursor.close();
                    mNotification = phone;
                } else {
                    mNotification = null;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mNotification = null;
            }
        });

        builder.setCancelable(true);
        builder.setView(view);
        builder.setTitle(getString(R.string.order_details_check_confirm));
        builder.setPositiveButton(getString(R.string.ok_button),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        mFragmentCreatedHandler.onChequeClicked(mPaymentTypeCode,mCheckType,mNotification);
                    }
                }
        );
        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {

        mFragmentCreatedHandler.onCancel();
    }

    private double getSum() {

        double discount = 0;double cost = 0;int count = 0;
        while (mCursor.moveToNext()) {
            boolean checked = (1 == mCursor.getInt(mCursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_CHECKED)));
            if (checked) {
                count = mCursor.getInt(mCursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COUNT));
                discount = mCursor.getDouble(mCursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_DISCOUNT));
                cost = mCursor.getDouble(mCursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COST));
                mSumm += cost * count;
                mDiscount += discount * count;
            }
        }
        return Math.round(mSumm - mDiscount);
    }

}