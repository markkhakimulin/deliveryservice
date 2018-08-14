package com.ff.deliveryservice.modules.details.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.R;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class OrderDetailsFragment extends Fragment {

    public static OrderDetailsFragment fragment;
    public TextView mCode,mId,mCustomer,mDate,mTime,mPhone,mAddress,mComment,mStatus,mRefuseReason;
    public Button cancelButton;
    public OnFragmentHandler mFragmentCreatedHandler;
    private Button mapButton;

    public OrderDetailsFragment() {}

    public void updateCursor(Cursor cursor) {

        if (cursor.moveToFirst()) {
            mCode.setText(cursor.getString(cursor.getColumnIndex(DBHelper.CN_CODE)));
            mId.setText(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID)));
            mCustomer.setText(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_CUSTOMER)));
            mDate.setText(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_DATE)));
            String time = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_TIME));
            if (time != null && !time.isEmpty()) time = time.replaceAll("\n",". ");
            mTime.setText(time);
            mPhone.setText(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_PHONE)));
            mAddress.setText(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_ADDRESS)));
            mComment.setText(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_COMMENT)));
            mStatus.setText(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_STATUS)));
            mRefuseReason.setText(cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_REFUSE)));
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    mFragmentCreatedHandler.onCancelClicked();
                }
            });
            mapButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = mAddress.getText().toString();
                    try {
                        String result = URLEncoder.encode(url, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    Uri uri = Uri.parse("yandexmaps://maps.yandex.ru/?text="+url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    PackageManager packageManager = getActivity().getPackageManager();

                    List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
                    boolean isIntentSafe = activities.size() > 0;
                    if (isIntentSafe) {
                        mFragmentCreatedHandler.showProgressDialog(getString(R.string.order_detail_map_loading));
                        startActivity(intent);
                    }else {
                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=ru.yandex.yandexmaps"));
                        startActivity(intent);
                    }
                }
            });
        }
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static OrderDetailsFragment newInstance(OnFragmentHandler fragmentCreatedHandler) {
        fragment = new OrderDetailsFragment();
        fragment.mFragmentCreatedHandler = fragmentCreatedHandler;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_order_details, container, false);
        mStatus = (TextView) rootView.findViewById(R.id.order_detail_status_view);
        mCode = (TextView) rootView.findViewById(R.id.order_detail_code_view);
        mId = (TextView) rootView.findViewById(R.id.order_detail_id_view);
        mCustomer = (TextView) rootView.findViewById(R.id.order_detail_customer_view);
        mDate = (TextView) rootView.findViewById(R.id.order_detail_date_view);
        mTime = (TextView) rootView.findViewById(R.id.order_detail_time_view);
        mPhone = (TextView) rootView.findViewById(R.id.order_detail_phone_view);
        mAddress = (TextView) rootView.findViewById(R.id.order_detail_address_view);
        mComment = (TextView) rootView.findViewById(R.id.order_detail_comment_view);
        mRefuseReason = (TextView) rootView.findViewById(R.id.order_detail_refuse_reason_view);
        cancelButton = (Button) rootView.findViewById(R.id.order_details_button_cancel);
        mapButton = (Button) rootView.findViewById(R.id.order_details_button_map);

        return rootView;
    }
    @Override
    public void onViewCreated(View view,Bundle savedInstanceState) {
        mFragmentCreatedHandler.onFragmentViewCreated(getClass().getCanonicalName());
    }
}