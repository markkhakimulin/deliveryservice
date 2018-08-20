package com.ff.deliveryservice.modules.details.adapter;

import android.widget.ListAdapter;

import com.ff.deliveryservice.mvp.model.BaseData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mark Khakimulin on 16.08.2018.
 * mark.khakimulin@gmail.com
 */
public interface DetailsFragmentList<E extends BaseData> extends ListAdapter {

    void update(ArrayList<E> list);
}
