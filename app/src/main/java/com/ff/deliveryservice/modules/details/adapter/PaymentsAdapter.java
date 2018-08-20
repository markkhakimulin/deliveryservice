package com.ff.deliveryservice.modules.details.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.CursorAdapter;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.mvp.model.OrderItem;
import com.ff.deliveryservice.mvp.model.OrderPayment;

import java.util.ArrayList;

import butterknife.ButterKnife;
import ru.atol.drivers10.fptr.IFptr;

/**
 * Created by Mark Khakimulin on 15.08.2018.
 * mark.khakimulin@gmail.com
 */
public class PaymentsAdapter extends ArrayAdapter<OrderPayment> implements DetailsFragmentList<OrderPayment>  {


    private Context mContext;

    public PaymentsAdapter(Context context, ArrayList<OrderPayment> payments) {
        super(context, R.layout.payment_row, payments);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            view = layoutInflater.inflate(R.layout.item_row, null);
        }

        OrderPayment item = getItem(position);

        if (item != null) {
            String paymentTypeId = item.getPaymentTypeId();
            float payment = item.getSum();
            float discount = item.getDiscount();
            String chequeTypeDescription = item.getChequeTypeDescription();
            int chequeNumber = item.getChequeNumber();
            String chequeDate = item.getDate();
            int chequeSession = item.getSession();
            String paymentTypeDescription = item.getChequeTypeDescription();
            ((TextView) view.findViewById(R.id.cheque_row_type_view)).setText(chequeTypeDescription);
            ((TextView) view.findViewById(R.id.payment_row_type_view)).setText(paymentTypeDescription);
            ((TextView) view.findViewById(R.id.payment_row_sum_view)).setText(Float.toString(payment - discount));
            ((TextView) view.findViewById(R.id.cheque_number)).setText(chequeNumber);
            ((TextView) view.findViewById(R.id.cheque_date)).setText(chequeDate.substring(0,16));
            ((TextView) view.findViewById(R.id.cheque_session)).setText(chequeSession);
            int chequeType = item.getChequeType();
            if (chequeType == IFptr.LIBFPTR_RT_SELL ) {
                //((ImageView) view.findViewById(R.id.icon_cheque_type)).setImageResource(R.mipmap.plus);
                view.setBackground(mContext.getResources().getDrawable(R.drawable.payment_sell));
            }
            else if (chequeType == IFptr.LIBFPTR_RT_SELL_RETURN ) {
                //((ImageView) view.findViewById(R.id.icon_cheque_type)).setImageResource(R.mipmap.minus);
                view.setBackground(mContext.getResources().getDrawable(R.drawable.payment_return));
            }
        }
        return view;
    }

    @Override
    public void update(ArrayList<OrderPayment> list) {
        clear();
        for (OrderPayment item : list) {
            insert(item,getCount());
        }
        notifyDataSetChanged();
    }
}
