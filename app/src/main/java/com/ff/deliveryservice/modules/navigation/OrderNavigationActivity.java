package com.ff.deliveryservice.modules.navigation;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.modules.fptr.FPTRActivity;
import com.ff.deliveryservice.modules.login.LoginActivity;
import com.ff.deliveryservice.modules.navigation.adapter.OrderAdapter;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.mvp.presenter.NavigationPresenter;
import com.ff.deliveryservice.mvp.view.OrderNavigationView;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import butterknife.BindView;

import static com.ff.deliveryservice.common.Constants.FORMATDATE_APP;
/**
 * Created by khakimulin on 22.02.2017.
 */

/**
 * Navigation between order categories .
 */

public class OrderNavigationActivity extends FPTRActivity implements
        OrderNavigationView,
        NavigationView.OnNavigationItemSelectedListener,
        SearchView.OnQueryTextListener,
        AbsListView.MultiChoiceModeListener{

    TextView loginView;
    TextView loginViewID;

    @BindView(R.id.content_order_swiper)
    public SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.content_order_list)
    public ListView orderListView;

    @BindView(R.id.toolbar)
    public Toolbar toolbar;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @Inject
    NavigationPresenter presenter;

    public OrderAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        loginId = getIntent().getStringExtra(DBHelper.CN_ID);
        loginDesc= getIntent().getStringExtra(DBHelper.CN_DESCRIPTION);

        super.onCreate(savedInstanceState);

    }
    @Override
    protected void onViewReady(Bundle savedInstanceState, Intent intent) {
        super.onViewReady(savedInstanceState,intent);

        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(0);

        View header =  navigationView.getHeaderView(0);

        loginView =  header.findViewById(R.id.nav_login);
        loginView.setText(loginDesc);

        loginViewID =  header.findViewById(R.id.nav_login_description);
        loginViewID.setText(loginId);

        orderListView.setSelected(true);
        orderListView.setTextFilterEnabled(true);
        orderListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor cursor = (Cursor) orderListView.getAdapter().getItem(position);
                if (cursor == null) {
                    onShowToast(getString(R.string.error_cant_find_order));
                    return;
                }
                /*Intent intent = new Intent(OrderNavigationActivity.this, OrderDetailsActivity.class);
                intent.putExtra(DBHelper.CN_ORDER_ID, cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID)));
                intent.putExtra(DBHelper.CN_CODE, cursor.getString(cursor.getColumnIndex(DBHelper.CN_CODE)));
                intent.putExtra(DBHelper.CN_ID, loginId);
                intent.putExtra(DBHelper.CN_DESCRIPTION, loginDesc);

                startActivity(intent);*/

            }
        });
        orderListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        orderListView.setMultiChoiceModeListener(this);

        showProgress();
        presenter.filterAllAndRefresh();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                initiateRefresh();
            }
        });

        swipeRefreshLayout.setColorSchemeColors(
                LoginActivity.getColorWrapper(this,R.color.holo_blue_light),
                LoginActivity.getColorWrapper(this,R.color.holo_green_light),
                LoginActivity.getColorWrapper(this,R.color.holo_orange_light),
                LoginActivity.getColorWrapper(this,R.color.holo_red_light));

    }
    @Override
    protected void resolveDaggerDependency() {
        DeliveryServiceApplication.initNavigationComponent(this,loginId).inject(this);
    }
    @Override
    protected int getContentView() {
        return R.layout.activity_order_navigation;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

        mode.setTitle(String.valueOf(orderListView.getCheckedItemCount()));
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.order_navigation_action_mode, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                clearSelectedItems();
                mode.finish(); // Action picked, so close the CAB
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }


    private void clearSelectedItems() {

        SparseBooleanArray sparseBooleanArray = orderListView.getCheckedItemPositions();
        ArrayList<String> checked = new ArrayList<>();
        for (int i = 0; i < orderListView.getCount(); i++) {
            Cursor cursor = (Cursor) orderListView.getItemAtPosition(i);
            String name = cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID));
            if (sparseBooleanArray.get(i)) {
                checked.add(name);
            }
        }
        presenter.clearSelectedItems(checked);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        ((OrderAdapter)orderListView.getAdapter()).getFilter().filter(newText);
        return false;
    }

    private void initiateRefresh()    {
        showProgress();
        presenter.refresh();
    }


    @Override
    public void onFilter(Cursor cursor) {

        if (orderListView.getAdapter() == null) {
            mAdapter = new OrderAdapter(this, cursor,presenter.filterProvider);
            orderListView.setAdapter(mAdapter);
        } else {

            mAdapter.changeCursor(cursor);
            mAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void applyFilter() {
        applyNavigationFilter(mNavigationMenuItemId);
    }

    @Override
    public void showProgress() {
        swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void hideProgress() {
        swipeRefreshLayout.setRefreshing(false);
    }

    public void onShowCursorDialog(String title, Cursor cursor, final Callable<Void> positiveCallback, final Callable<Void> negativeCallback) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(title);
        builder.setCancelable(false);
        builder.setCursor(cursor,null, DBHelper.CN_CODE);
        builder.setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                if (positiveCallback != null) {
                    try {
                        positiveCallback.call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        builder.setNegativeButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                if (negativeCallback != null) {
                    try {
                        negativeCallback.call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        builder.create().show();
    }

    private void initiateUpload(AsyncTaskCallback callback){
        showProgress();
        presenter.upload(callback);
    }
    private void initiateUpload()    {

        initiateUpload(null);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.order_navigation, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        if (searchView != null) {
            searchView.setOnQueryTextListener(this);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
        if (id == R.id.action_log_out) {
            logOut();
        }

        return super.onOptionsItemSelected(item);
    }

    private int mNavigationMenuItemId = R.id.orders_all;

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        return applyNavigationFilter(item.getItemId());
    }
    private boolean applyNavigationFilter(int id) {
        switch (id) {
            case R.id.orders_all:

                setTitle(R.string.navigation_drawer_menu_all);
                mNavigationMenuItemId = id;
                presenter.filterAll();
                break;
            case R.id.orders_in_progress:

                setTitle(R.string.navigation_drawer_menu_in_progress);
                mNavigationMenuItemId = id;
                presenter.filterInProgress();
                break;
            case R.id.orders_canceled:

                setTitle(R.string.navigation_drawer_menu_canceled);
                mNavigationMenuItemId = id;
                presenter.filterCanceled();
                break;
            case R.id.orders_completed:
                //в обработке
                setTitle(R.string.navigation_drawer_menu_completed);
                mNavigationMenuItemId = id;
                presenter.filterCompleted();
                break;
            case R.id.orders_archive:
                setTitle(R.string.navigation_drawer_menu_archive);
                mNavigationMenuItemId = id;
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Calendar now = Calendar.getInstance();
                        NavigationPresenter.ArchiveType archiveType = which==0? NavigationPresenter.ArchiveType.Cheque: NavigationPresenter.ArchiveType.Order;
                        showPickDateDialog(archiveType,now, true, false);
                    }
                };

                AlertDialog.Builder
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.alert_message_confirm_changes);
                    builder.setSingleChoiceItems(new String[] {getString(R.string.navigation_archive_dialog_date_cheque),getString(R.string.navigation_archive_dialog_date_order)},0, onClickListener);
                    builder.setNegativeButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                    builder.create().show();
                break;
            case R.id.upload:
                setTitle(R.string.title_activity_order_navigation);
                initiateUpload();
                break;
            case R.id.refresh_orders:
                setTitle(R.string.title_activity_order_navigation);
                initiateRefresh();
                break;
            case R.id.settings_order:
                setTitle(R.string.title_activity_order_navigation);
                break;
            case R.id.log_out:
                setTitle(R.string.title_activity_order_navigation);
                logOut();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    void showPickDateDialog(final NavigationPresenter.ArchiveType archiveType, final Calendar newcalendar, final Boolean startPeriod, final Boolean endPeriod) {

        final SimpleDateFormat sdf = new SimpleDateFormat(FORMATDATE_APP);

        DatePickerDialog.OnDateSetListener datePickerListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);//возвращает порядковый номер месца начиная с 0, т.е. к примеру для июня будет 5 а не 6
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                onDataPick(archiveType,sdf.format(calendar.getTime()));

            }
        };
        DatePickerDialog tpd = new DatePickerDialog(this,datePickerListener,newcalendar.get(Calendar.YEAR),newcalendar.get(Calendar.MONTH),newcalendar.get(Calendar.DAY_OF_MONTH));
        tpd.show();
    }

    void onDataPick(NavigationPresenter.ArchiveType archiveType, String startDate) {
        presenter.filterArchive(archiveType, startDate);
    }

    private void logOut() {
        Intent intent = new Intent();
        intent.putExtra("password_is_valid", true);
        intent.putExtra("login_is_valid", true);
        intent.putExtra("description", getString(R.string.prompt_login_out));
        setResult(RESULT_CANCELED,intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DeliveryServiceApplication.destroyNavigationComponent();
    }
}

