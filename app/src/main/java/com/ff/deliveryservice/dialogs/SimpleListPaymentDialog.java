package com.ff.deliveryservice.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.atol.drivers.fptr.IFptr;
import com.ff.deliveryservice.DBHelper;
import com.ff.deliveryservice.R;
import com.ff.deliveryservice.fragments.OnFragmentHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleListPaymentDialog extends DialogFragment implements DialogInterface.OnCancelListener{

    public static SimpleListPaymentDialog fragment;
    public OnFragmentHandler mFragmentCreatedHandler;
    public Cursor mCursor;
    public int mCheckType;
    public double mSumToPay;
    private LinearLayout mContainerMixPay;
    private EditText mCashView,mCardView;

    public SimpleListPaymentDialog() {}

    public static SimpleListPaymentDialog newInstance(OnFragmentHandler fragmentCreatedHandler, Cursor cursor, int checkType, double sumToPay) {
        fragment = new SimpleListPaymentDialog();
        fragment.mCursor = cursor;
        fragment.mFragmentCreatedHandler = fragmentCreatedHandler;
        fragment.mCheckType = checkType;
        fragment.mSumToPay = sumToPay;
        return fragment;
    }
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = inflater.inflate(R.layout.cheque_type_dialog, null);


        mContainerMixPay = (LinearLayout) convertView.findViewById(R.id.container_mix_pay);
        mCashView = (EditText) convertView.findViewById(R.id.text_cheque_cash);
        mCashView.addTextChangedListener(new TextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {
                if (mCashView.hasFocus()) {
                    String value = s.toString();
                    if (s.length() == 0) value = "0";
                    Double sum = Double.valueOf(value);
                    if (sum < 0) {
                        mCashView.setText("");
                    } else {
                        mCardView.setText(Double.toString(mSumToPay - sum));
                    }
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });
        mCardView = (EditText) convertView.findViewById(R.id.text_cheque_card);
        mCardView.addTextChangedListener(new TextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {


                if (mCardView.hasFocus()) {
                    String value = s.toString();
                    if (s.length() == 0) value = "0";
                    Double sum = Double.valueOf(value);
                    if (sum < 0) {
                        mCardView.setText("");
                    } else {
                        mCashView.setText(Double.toString(mSumToPay - sum));
                    }
                }

            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        ListView view = (ListView) convertView.findViewById(R.id.cheque_type_list);

        List<String> names = new ArrayList<>();
        //Если количество больше одного значит была смешанная оплата.
        //Актуально только если это чек возврата на полную сумму.
        if (mCursor.getCount() > 1 && mCheckType == IFptr.CHEQUE_TYPE_RETURN) {



            mContainerMixPay.setVisibility(View.VISIBLE);
            //добавляем пункт смешанной оплаты просто для вида(чтобы было понятно)
            names.add(getString(R.string.order_detail_mix_pay));
            //распределяем суммы
            //if (!mCursor.isFirst()) mCursor.moveToFirst();

            while (mCursor.moveToNext()) {
                int paymentTypeCode = mCursor.getInt(mCursor.getColumnIndex(DBHelper.CN_PAYMENT_TYPES_CODE));
                if (paymentTypeCode == DBHelper.CASH_PAYMENT_CODE) {
                    mCashView.setText(Float.toString(mCursor.getFloat(mCursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_PAID_SUM))));
                }
                if (paymentTypeCode == DBHelper.CARD_PAYMENT_CODE) {
                    mCardView.setText(Float.toString(mCursor.getFloat(mCursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_PAID_SUM))));
                }
                //mCursor.moveToNext();
            }
            //mCursor.moveToFirst();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),android.R.layout.simple_list_item_single_choice,names);
            view.setAdapter(adapter);

        } else { //тут просто выдаем список выбора для однозначного выбора

            while (mCursor.moveToNext()) {
                names.add(mCursor.getString(mCursor.getColumnIndex(DBHelper.CN_DESCRIPTION)));
            }

            view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    mCursor.moveToPosition(position);
                    int paymentTypeCode = mCursor.getInt(mCursor.getColumnIndex(DBHelper.CN_PAYMENT_TYPES_CODE));
                    if (paymentTypeCode == DBHelper.CASH_PAYMENT_CODE || paymentTypeCode == DBHelper.CARD_PAYMENT_CODE) {

                        dismiss();
                        Map<Integer, Double> map = new HashMap<>();
                        map.put(paymentTypeCode, mSumToPay);
                        mFragmentCreatedHandler.onPaymentTypeChoosed(map, mCheckType);
                    } else {
                        mContainerMixPay.setVisibility(View.VISIBLE);
                        mCashView.setText(Double.toString(mSumToPay));


                    }
                }
            });
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),android.R.layout.simple_list_item_single_choice,names);
            view.setAdapter(adapter);
        }
        view.setItemChecked(0,true);

        Button button = (Button) convertView.findViewById(R.id.button_mix_pay);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<Integer, Double> map = new HashMap<>();

                if (!mCursor.isFirst()) mCursor.moveToFirst();

                double commonSum = 0, cardSum = 0,cashSum = 0;
                while (!mCursor.isAfterLast()) {

                    int paymentTypeCode = mCursor.getInt(mCursor.getColumnIndex(DBHelper.CN_PAYMENT_TYPES_CODE));
                    if (paymentTypeCode == DBHelper.CASH_PAYMENT_CODE  && !mCashView.getText().toString().equals("")) {
                        cashSum = Double.valueOf(mCashView.getText().toString());
                        commonSum+=cashSum;
                        if (cashSum > 0) map.put(paymentTypeCode, cashSum);
                    }
                    if (paymentTypeCode == DBHelper.CARD_PAYMENT_CODE && !mCardView.getText().toString().equals("")) {
                        cardSum = Double.valueOf(mCardView.getText().toString());
                        commonSum+=cardSum;
                        if (cardSum > 0) map.put(paymentTypeCode, cardSum);
                    }
                    mCursor.moveToNext();

                }

                if (commonSum == mSumToPay && cardSum >= 0 && cashSum >= 0) {

                    dismiss();
                    mFragmentCreatedHandler.onPaymentTypeChoosed(map, mCheckType);
                } else {
                    if (cardSum < 0) {
                        mCardView.requestFocus();
                        mCardView.setError(getString(R.string.order_details_error_negative_sum));
                    }
                    if (cashSum < 0)  {
                        mCashView.requestFocus();
                        mCashView.setError(getString(R.string.order_details_error_negative_sum));
                    }

                }

            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(convertView);
        builder.setCancelable(true);
        builder.setTitle(getString(R.string.order_detail_select_check_type));
        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mFragmentCreatedHandler.onCancel();
    }
}