package com.ff.deliveryservice.modules.details.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.CursorAdapter;
import android.widget.TextView;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.mvp.model.DBHelper;

import butterknife.ButterKnife;
import ru.atol.drivers10.fptr.IFptr;

/**
 * Created by Mark Khakimulin on 15.08.2018.
 * mark.khakimulin@gmail.com
 */
public class PaymentsAdapter extends CursorAdapter {


    private Context mContext;

    public PaymentsAdapter(Context context, Cursor c) {

        super(context, c, android.support.v4.widget.CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        mContext = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.payment_row, parent, false);
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
                view.setBackground(mContext.getResources().getDrawable(R.drawable.payment_sell));
            }
            else if (chequeType == IFptr.LIBFPTR_RT_SELL_RETURN ) {
                //((ImageView) view.findViewById(R.id.icon_cheque_type)).setImageResource(R.mipmap.minus);
                view.setBackground(mContext.getResources().getDrawable(R.drawable.payment_return));
            }
        }
    }
}
