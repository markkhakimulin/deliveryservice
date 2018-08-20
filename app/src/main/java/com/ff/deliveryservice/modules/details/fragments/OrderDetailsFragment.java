package com.ff.deliveryservice.modules.details.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.base.BaseFragment;
import com.ff.deliveryservice.modules.details.adapter.DetailsFragmentList;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.R;
import com.ff.deliveryservice.mvp.model.OrderData;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OrderDetailsFragment extends BaseFragment {

    public static OrderDetailsFragment fragment;
    @BindView(R.id.order_detail_code_view)
    public TextView mCode;
    @BindView(R.id.order_detail_id_view)
    public TextView mId;
    @BindView(R.id.order_detail_customer_view)
    public TextView mCustomer;
    @BindView(R.id.order_detail_date_view)
    public TextView mDate;
    @BindView(R.id.order_detail_time_view)
    public TextView mTime;
    @BindView(R.id.order_detail_phone_view)
    public TextView mPhone;
    @BindView(R.id.order_detail_address_view)
    public TextView mAddress;
    @BindView(R.id.order_detail_comment_view)
    public TextView mComment;
    @BindView(R.id.order_detail_status_view)
    public TextView mStatus;
    @BindView(R.id.order_detail_refuse_reason_view)
    public TextView mRefuseReason;
    @BindView(R.id.order_details_button_cancel)
    public Button cancelButton;
    @BindView(R.id.order_details_button_map)
    public Button mapButton;


    public OnFragmentHandler mFragmentCreatedHandler;

    public OrderDetailsFragment() {}

    @Override
    protected DetailsFragmentList getNewCursorAdapter(ArrayList<?> list) {
        return null;
    }


    public void updateCursor(OrderData data) {

            mCode.setText(data.getCode());
            mId.setText(data.getId());
            mCustomer.setText(data.getCustomer());
            mDate.setText(data.getDate());
            mTime.setText(data.getTime());
            mPhone.setText(data.getPhone());
            mAddress.setText(data.getAddress());
            mComment.setText(data.getComment());
            mStatus.setText(data.getStatus());
            mRefuseReason.setText(data.getRefuseReason());
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

                        onShowDialog(getString(R.string.order_detail_map_loading));
                        //mFragmentCreatedHandler.showProgressDialog(getString(R.string.order_detail_map_loading));
                        startActivity(intent);
                    }else {
                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=ru.yandex.yandexmaps"));
                        startActivity(intent);
                    }
                }
            });
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
        ButterKnife.bind(this, rootView);
        return rootView;
    }
    @Override
    public void onViewCreated(View view,Bundle savedInstanceState) {
        mFragmentCreatedHandler.onFragmentViewCreated(getClass().getCanonicalName());
    }
}