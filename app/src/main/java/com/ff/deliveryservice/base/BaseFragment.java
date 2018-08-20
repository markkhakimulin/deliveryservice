package com.ff.deliveryservice.base;

import android.database.Cursor;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.ff.deliveryservice.modules.details.adapter.DetailsFragmentList;
import com.ff.deliveryservice.mvp.model.BaseData;
import com.ff.deliveryservice.mvp.model.OrderItem;
import com.ff.deliveryservice.mvp.view.BaseView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Created by Mark Khakimulin on 15.08.2018.
 * mark.khakimulin@gmail.com
 */
public abstract class BaseFragment extends Fragment implements BaseView {


    protected ListView mListView;
    protected DetailsFragmentList adapter;

    public BaseFragment() {

    }

    public void updateCursor(ArrayList<?> list) {

        if (mListView.getAdapter() == null) {
            adapter = getNewCursorAdapter(list);
            mListView.setAdapter(adapter);
        } else {
            if (adapter != null) {
                adapter.update(list);
            }

        }
    }

    protected abstract DetailsFragmentList getNewCursorAdapter(ArrayList<?>  list);


    @Override
    public void onShowDialog(String message) {

    }

    @Override
    public void onHideDialog() {

    }

    @Override
    public void onShowToast(String message) {

    }

    @Override
    public void showYesNoMessageDialog(String message, Callable<Void> positiveCallback) {

    }

    @Override
    public void showYesNoMessageDialog(String title, String message, Callable<Void> positiveCallback, Callable negativeCallback) {

    }
}
