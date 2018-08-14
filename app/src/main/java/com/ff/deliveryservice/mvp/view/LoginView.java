package com.ff.deliveryservice.mvp.view;

import com.ff.deliveryservice.mvp.model.UserData;

import java.util.List;

/**
 * Created by Mark Khakimulin on 02.08.2018.
 * mark.khakimulin@gmail.com
 */
public interface LoginView extends FPTRView{

    UserData getSelectedUser();
    String getUserName();
    void showInputError(String description);
    void startLoadingActivity(UserData userData);
    void onUserLoaded(List<UserData> userDataList);
    void showProgress();
    void hideProgress();

}
