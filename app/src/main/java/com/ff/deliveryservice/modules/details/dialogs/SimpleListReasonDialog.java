package com.ff.deliveryservice.modules.details.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.R;
import com.ff.deliveryservice.modules.details.fragments.OnFragmentHandler;

public class SimpleListReasonDialog extends DialogFragment implements DialogInterface.OnClickListener{

    public static SimpleListReasonDialog fragment;
    public OnFragmentHandler mFragmentCreatedHandler;
    private Cursor mCursor;

    public SimpleListReasonDialog() {}

    public static SimpleListReasonDialog newInstance(OnFragmentHandler fragmentCreatedHandler, Cursor cursor) {
        fragment = new SimpleListReasonDialog();
        fragment.mCursor = cursor;
        fragment.mFragmentCreatedHandler = fragmentCreatedHandler;
        return fragment;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true);
        builder.setTitle(getString(R.string.order_detail_select_refuse_reasons)).setSingleChoiceItems(mCursor,0, DBHelper.CN_DESCRIPTION,this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        dialog.dismiss();
        mCursor.moveToPosition(which);
        String reasonId = mCursor.getString(mCursor.getColumnIndex(DBHelper.CN_ID));
        int canceled = mCursor.getInt(mCursor.getColumnIndex(DBHelper.CN_CANCELED));
        mFragmentCreatedHandler.onReasonChoosed(reasonId,canceled);
    }
}