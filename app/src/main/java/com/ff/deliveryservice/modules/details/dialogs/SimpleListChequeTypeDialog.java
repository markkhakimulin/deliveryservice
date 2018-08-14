package com.ff.deliveryservice.modules.details.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.ArrayAdapter;

import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.R;
import com.ff.deliveryservice.modules.details.fragments.OnFragmentHandler;

import java.util.ArrayList;
import java.util.List;

public class SimpleListChequeTypeDialog extends DialogFragment implements DialogInterface.OnClickListener,DialogInterface.OnCancelListener{

    public static SimpleListChequeTypeDialog fragment;
    public OnFragmentHandler mFragmentCreatedHandler;
    private Cursor mCursor;

    public SimpleListChequeTypeDialog() {}

    public static SimpleListChequeTypeDialog newInstance(OnFragmentHandler fragmentCreatedHandler, Cursor cursor) {
        fragment = new SimpleListChequeTypeDialog();
        fragment.mCursor = cursor;
        fragment.mFragmentCreatedHandler = fragmentCreatedHandler;
        return fragment;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        List<String> names = new ArrayList<>();
        while (mCursor.moveToNext()) {
            names.add(mCursor.getString(mCursor.getColumnIndex(DBHelper.CN_DESCRIPTION)));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_single_choice,names);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true);
        builder.setTitle(getString(R.string.order_detail_select_check_type)).setSingleChoiceItems(adapter,0,this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        dialog.dismiss();
        mCursor.moveToPosition(which);
        int checkType = mCursor.getInt(mCursor.getColumnIndex(DBHelper.CN_CODE));
        mFragmentCreatedHandler.onCheckTypeChoosed(checkType);
    }
    @Override
    public void onCancel(DialogInterface dialog) {

        mFragmentCreatedHandler.onCancel();
    }
}
