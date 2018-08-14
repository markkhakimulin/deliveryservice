package com.ff.deliveryservice.base;


import com.ff.deliveryservice.mvp.view.BaseView;

import javax.inject.Inject;

/**
 * Created by Mark Khakimulin on 02.07.2018.
 * mark.khakimulin@gmail.com
 */
public class BasePresenter<V extends BaseView>  {

    @Inject
    protected V mView;

    protected V getView() {
        return mView;
    }
}
