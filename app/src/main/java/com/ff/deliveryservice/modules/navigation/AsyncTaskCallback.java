package com.ff.deliveryservice.modules.navigation;

/**
 * Created by Mark Khakimulin on 10.08.2018.
 * mark.khakimulin@gmail.com
 */
public interface AsyncTaskCallback {
    void onPostExecute(Object... params);
    void onCanceled();
}