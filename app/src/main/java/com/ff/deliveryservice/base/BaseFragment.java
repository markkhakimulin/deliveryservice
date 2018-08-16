package com.ff.deliveryservice.base;

import android.database.Cursor;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.ff.deliveryservice.mvp.view.BaseView;

import java.util.concurrent.Callable;

/**
 * Created by Mark Khakimulin on 15.08.2018.
 * mark.khakimulin@gmail.com
 */
public abstract class BaseFragment extends Fragment implements BaseView {


    protected ListView mListView;
    protected CursorAdapter adapter;
    public BaseFragment() {

    }

    public void updateCursor(Cursor cursor) {

        if (mListView.getAdapter() == null) {
            adapter = getNewCursorAdapter(cursor);
            mListView.setAdapter(getNewCursorAdapter(cursor));
        } else {
            if (adapter != null) {
                adapter.changeCursor(cursor);
                adapter.notifyDataSetChanged();
            }
            //((CursorAdapter)mListView.getAdapter()).swapCursor(cursor);
            //((CursorAdapter)mListView.getAdapter()).notifyDataSetChanged();
        }

    }

    protected abstract CursorAdapter getNewCursorAdapter(Cursor cursor);


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
