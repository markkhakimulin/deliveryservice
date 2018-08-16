package com.ff.deliveryservice.modules.details.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.modules.details.OrderDetailsActivity;
import com.ff.deliveryservice.modules.details.fragments.OrderDetailsFragment;
import com.ff.deliveryservice.modules.details.fragments.OrderItemsFragment;
import com.ff.deliveryservice.modules.details.fragments.OrderPaymentsFragment;

import javax.inject.Inject;

/**
 * Created by Mark Khakimulin on 15.08.2018.
 * mark.khakimulin@gmail.com
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

/*    @Inject
    Context context;

    @Inject
    Resources res;*/

    private OrderDetailsActivity mContext;

    public SectionsPagerAdapter(FragmentManager fm, OrderDetailsActivity context) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {

        Fragment fragment = null;

        switch (position) {
            case 0:
                fragment = OrderDetailsFragment.newInstance(mContext);
                break;
            case 1:
                fragment = OrderItemsFragment.newInstance(mContext);
                break;
            case  2:
                fragment = OrderPaymentsFragment.newInstance(mContext);
        }
        return fragment;
    }

    @Override
    public int getCount() {
        // Show 3 total pages.
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return mContext.getString(R.string.order_details_page1);
            case 1:
                return mContext.getString(R.string.order_details_page2);
            case 2:
                return mContext.getString(R.string.order_details_page3);
        }
        return null;
    }
}