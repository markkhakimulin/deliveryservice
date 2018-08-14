package com.ff.deliveryservice.modules.details.fragments;

import java.util.Map;

public interface OnFragmentHandler {
    void onFragmentViewCreated(String tag);
    void onItemClicked(String itemId, String eid);
    void onItemLongClicked(String itemId);
    void onChequeClicked(Map<Integer, Double> map, int checkType, String notification);
    void onCancelClicked();
    void onReasonChoosed(String reasonId, int canceled);
    void onCheckTypeChoosed(int checkType);
    void onPaymentTypeChoosed(Map<Integer, Double> map, int checkType);
    void onCancel();
    void onEditPaymentComplete();
    void showProgressDialog(String title);
    void hideProgressDialog();
}