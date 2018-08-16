package com.ff.deliveryservice.mvp.view;

import android.database.Cursor;

import java.util.Map;

/**
 * Created by Mark Khakimulin on 13.08.2018.
 * mark.khakimulin@gmail.com
 */
public interface OrderDetailsView extends FPTRView{

    void onOrderItemUpdated();
    void onGetChequeTypesCursor(Cursor cursor);
    void onGetRefusesCursor(Cursor cursor);
    void onGetPaymentsCursor(Cursor cursor,double sumToPay,int checkType);
    void onConfirmPayment(Cursor cursor);
    void onChangePaymentValueComplete();
}
