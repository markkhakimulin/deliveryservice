package com.ff.deliveryservice;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
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
/**
 * Created by khakimulin on 22.02.2017.
 */

/**
 * Navigation between order categories .
 */

public class OrderNavigationActivity extends FPTRActivity
        implements NavigationView.OnNavigationItemSelectedListener,SearchView.OnQueryTextListener,AbsListView.MultiChoiceModeListener{

    private ImageView loginImageView;
    private TextView loginView;
    private TextView loginViewID;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView orderListView;
    private SQLiteOpenHelper sqLiteOpenHelper;
    private RefreshOrderListTask refreshOrderListTask;
    private UploadOrderListTask uploadOrderListTask;
    private String loginId;
    private String loginDesc;
    private OrderAdapter mAdapter;
    private TextView refreshText;


    enum ArchiveType {
        Order,Cheque

    }
    public static final String FORMATDATE_1C = "yyyyMMdd";
    public static final String FORMATDATE_APP = "yyyy-MM-dd";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_navigation);

        Intent intent = getIntent();
        loginDesc = intent.getStringExtra(DBHelper.CN_DESCRIPTION);
        loginId = intent.getStringExtra(DBHelper.CN_ID);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(0);

        View header =  navigationView.getHeaderView(0);

        loginView = (TextView) header.findViewById(R.id.nav_login);
        loginView.setText(loginDesc);

        loginViewID = (TextView) header.findViewById(R.id.nav_login_description);
        loginViewID.setText(loginId);

        orderListView = (ListView) findViewById(R.id.content_order_list);
        orderListView.setSelected(true);
        orderListView.setTextFilterEnabled(true);
        orderListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor cursor = (Cursor) mAdapter.getItem(position);
                if (cursor == null) {
                    Toast.makeText(parent.getContext(),R.string.error_cant_find_order,Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(OrderNavigationActivity.this, OrderDetailsActivity.class);
                intent.putExtra(DBHelper.CN_ORDER_ID, cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID)));
                intent.putExtra(DBHelper.CN_CODE, cursor.getString(cursor.getColumnIndex(DBHelper.CN_CODE)));
                intent.putExtra(DBHelper.CN_ID, loginId);
                intent.putExtra(DBHelper.CN_DESCRIPTION, loginDesc);

                startActivity(intent);

            }
        });
        orderListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        orderListView.setMultiChoiceModeListener(this);
        

        sqLiteOpenHelper = DBHelper.getOpenHelper(this);

        updateOrderListInBackground();

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.content_order_swiper);
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

        SQLiteDatabase db = sqLiteOpenHelper.getWritableDatabase();
        for (int i = 0; i < checked.size();i++) {
            db.delete(DBHelper.TB_ORDERS_CHG,"_id = ?",new String[]{checked.get(i)});
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.CN_ORDER_COMPLETED, 0);
            cv.put(DBHelper.CN_ORDER_POSTED, 0);
            cv.put(DBHelper.CN_ORDER_CANCELED, 0);
            db.update(DBHelper.TB_ORDERS,cv,"_id = ?",new String[]{checked.get(i)});
        }

        db.close();

        Toast.makeText(getApplication(),getResources().getString(R.string.message_orders_cleared)+checked.size(),Toast.LENGTH_SHORT).show();

    }


    private void updateOrderListInBackground()   {
        runOnUiThread(new FilterCursorAllToday(new FilterCursorCallback() {
            @Override
            public void applyCursor(Cursor cursor,Context currentContext) {

                mAdapter = new OrderAdapter(currentContext, cursor);
                orderListView.setAdapter(mAdapter);

                if (cursor.getCount() == 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(currentContext);

                    builder.setTitle(R.string.alert_title_no_orders);
                    builder.setMessage(R.string.alert_message_no_orders);
                    builder.setCancelable(true);
                    builder.setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            swipeRefreshLayout.setRefreshing(true);
                            initiateRefresh();
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton(R.string.alert_button_no, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                }
            }
        },this));
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mAdapter.getFilter().filter(newText);
        return false;
    }


    public class FilterCursorAllToday implements Runnable {
        private FilterCursorCallback mCallback;
        private Context mContext;

        FilterCursorAllToday(FilterCursorCallback callback,Context currentContext) {mCallback = callback;mContext = currentContext;}

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            DateFormat dateFormat = new SimpleDateFormat(FORMATDATE_APP);
            String date = dateFormat.format(new Date());
            String noData = "1";
            String[] args = {date,noData,loginId};

            Cursor cursor = db.rawQuery(String.format("select o.*,s.description as status from orders as o " +
                    "left join statuses as s on o.status_id = s._id " +
                    "left join drivers as d on o.driver_id = d._id  " +
                    "where o.posted = 0 and (o.date = ? or ? = '1') and d._id = ?"),args);
            mCallback.applyCursor(cursor,mContext);
        }
    }

    public class FilterCursorInProgressToday implements Runnable {
        private FilterCursorCallback mCallback;
        private Context mContext;

        FilterCursorInProgressToday(FilterCursorCallback callback,Context currentContext) {mCallback = callback;mContext = currentContext;}

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            DateFormat dateFormat = new SimpleDateFormat(FORMATDATE_APP);
            String date = dateFormat.format(new Date());
            String noData = "1";
            String[] args = {date,noData,loginId};

            Cursor cursor = db.rawQuery(String.format("select o.*,s.description as status from orders as o " +
                    "left join statuses as s on o.status_id = s._id " +
                    "left join drivers as d on o.driver_id = d._id  " +
                    "where o.posted = 0 and o.completed = 0 and o.canceled = 0 and (o.date = ? or ? = '1') and d._id = ?"),args);
            mCallback.applyCursor(cursor,mContext);
        }
    }

    public class FilterCursorCanceledToday implements Runnable {
        private FilterCursorCallback mCallback;
        private Context mContext;

        FilterCursorCanceledToday(FilterCursorCallback callback,Context currentContext) {mCallback = callback;mContext = currentContext;}

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            DateFormat dateFormat = new SimpleDateFormat(FORMATDATE_APP);
            String date = dateFormat.format(new Date());
            String noData = "1";
            String[] args = {date,noData,loginId};

            Cursor cursor = db.rawQuery(String.format("select o.*,s.description as status from orders as o " +
                    "left join statuses as s on o.status_id = s._id " +
                    "left join drivers as d on o.driver_id = d._id  " +
                    "where o.posted = 0 and o.canceled = 1 and (o.date = ? or ? = '1') and d._id = ?"),args);
            mCallback.applyCursor(cursor,mContext);
        }
    }

    public class FilterCursorCompletedToday implements Runnable {
        private FilterCursorCallback mCallback;
        private Context mContext;

        FilterCursorCompletedToday(FilterCursorCallback callback,Context currentContext) {mCallback = callback;mContext = currentContext;}

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            DateFormat dateFormat = new SimpleDateFormat(FORMATDATE_APP);
            String date = dateFormat.format(new Date());
            String noData = "1";
            String[] args = {date,noData,loginId};

            Cursor cursor = db.rawQuery(String.format("select o.*,s.description as status from orders as o " +
                    "left join statuses as s on o.status_id = s._id " +
                    "left join drivers as d on o.driver_id = d._id  " +
                    "where o.posted = 0 and o.completed = 1 and (o.date = ? or ? = '1') and d._id = ?"),args);
            mCallback.applyCursor(cursor,mContext);
        }
    }

    public class FilterCursorArchive implements Runnable {
        private String mStartDate = "";
        private FilterCursorCallback mCallback;
        private Context mContext;
        private ArchiveType mType;

        FilterCursorArchive(ArchiveType type,String startDate,FilterCursorCallback callback,Context currentContext) {
            mType = type;
            mStartDate = startDate;
            mCallback = callback;
            mContext = currentContext;
        }

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            Cursor cursor = null;
            String[] args = {mStartDate, loginId};

            if (mType == ArchiveType.Order) {

                cursor = db.rawQuery(String.format("select o.*,s.description as status from orders as o " +
                        "inner join statuses as s on o.status_id = s._id " +
                        "inner join drivers as d on o.driver_id = d._id  " +
                        "where o.date = ? and d._id = ?"), args);// and o.completed = 1
            } else if (mType == ArchiveType.Cheque) {
                cursor = db.rawQuery(String.format("select distinct o.*,s.description as status from orders as o " +
                        "inner join statuses as s on o.status_id = s._id " +
                        "inner join drivers as d on o.driver_id = d._id  " +
                        "inner join order_payments as op on o._id = op.order_id  " +
                        "where date(op.date) = ? and d._id = ?"), args);// and o.completed = 1
            }
            mCallback.applyCursor(cursor,mContext);
        }
    }

    public class ShowChangesAlert implements Runnable {
        private FilterCursorCallback mCallback;
        private Context mContext;

        ShowChangesAlert(FilterCursorCallback callback,Context currentContext) {mCallback = callback;mContext = currentContext;}

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            Cursor cursor = DBHelper.getUselessOrders(db,loginId);
            mCallback.applyCursor(cursor,mContext);
        }
    }

    public interface AsyncTaskCallback {
        void onPostExecute(Object... params);
        void onCanceled();
    }

    public interface FilterCursorCallback {
        void applyCursor(Cursor cursor,Context currentContext);
    }

    private void initiateRefresh()    {

        swipeRefreshLayout.setRefreshing(true);
        runOnUiThread(new ShowChangesAlert(new FilterCursorCallback() {
            @Override
            public void applyCursor(Cursor cursor, final Context currentContext) {
            if (cursor.getCount() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(currentContext);

                builder.setTitle(R.string.alert_message_confirm_changes);
                builder.setCancelable(false);
                builder.setCursor(cursor,null, DBHelper.CN_CODE);
                builder.setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                    dialog.dismiss();
                    initiateUpload(new AsyncTaskCallback() {
                        @Override
                        public void onPostExecute(Object... params) {


                            if (!(Boolean) params[0]) {
                                Toast.makeText(getApplicationContext(),(String) params[1],Toast.LENGTH_SHORT).show();
                            }

                            refreshOrderListTask = new RefreshOrderListTask();
                            refreshOrderListTask.execute(currentContext);
                        }

                        @Override
                        public void onCanceled() {}
                    });

                    }
                });
                builder.setNegativeButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });

                builder.create().show();
            } else {
                initiateUpload(new AsyncTaskCallback() {
                    @Override
                    public void onPostExecute(Object... params) {

                        if (!(Boolean) params[0]) {
                            Toast.makeText(getApplicationContext(),(String) params[1],Toast.LENGTH_SHORT).show();
                        }
                        refreshOrderListTask = new RefreshOrderListTask();
                        refreshOrderListTask.execute(currentContext);
                    }

                    @Override
                    public void onCanceled() {}
                });

            }
            }
        },this));

    }

    private void initiateUpload(AsyncTaskCallback callback)    {
        swipeRefreshLayout.setRefreshing(true);

        uploadOrderListTask = new UploadOrderListTask(this,callback);
        uploadOrderListTask.execute((Void) null);
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
                updateOrderListInBackground();
                mNavigationMenuItemId = id;
                break;
            case R.id.orders_in_progress:

                setTitle(R.string.navigation_drawer_menu_in_progress);
                mNavigationMenuItemId = id;
                runOnUiThread(new FilterCursorInProgressToday(new FilterCursorCallback() {
                    @Override
                    public void applyCursor(Cursor cursor,Context currentContext) {

                            mAdapter = new OrderAdapter(currentContext, cursor);
                            orderListView.setAdapter(mAdapter);

                    }
                },this));
                break;
            case R.id.orders_canceled:

                setTitle(R.string.navigation_drawer_menu_canceled);
                mNavigationMenuItemId = id;
                runOnUiThread(new FilterCursorCanceledToday(new FilterCursorCallback() {
                    @Override
                    public void applyCursor(Cursor cursor,Context currentContext) {

                            mAdapter = new OrderAdapter(currentContext, cursor);
                            orderListView.setAdapter(mAdapter);

                    }
                },this));
                break;
            case R.id.orders_completed:
                //в обработке
                setTitle(R.string.navigation_drawer_menu_completed);
                mNavigationMenuItemId = id;
                runOnUiThread(new FilterCursorCompletedToday(new FilterCursorCallback() {
                    @Override
                    public void applyCursor(Cursor cursor,Context currentContext) {

                            mAdapter = new OrderAdapter(currentContext, cursor);
                            orderListView.setAdapter(mAdapter);

                    }
                },this));
                break;
            case R.id.orders_archive:
                setTitle(R.string.navigation_drawer_menu_archive);

                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Calendar now = Calendar.getInstance();
                        ArchiveType archiveType = which==0?ArchiveType.Cheque:ArchiveType.Order;
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
    void showPickDateDialog(final ArchiveType archiveType,final Calendar newcalendar,final Boolean startPeriod,final Boolean endPeriod) {

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

    void onDataPick(ArchiveType archiveType,String startDate) {

        runOnUiThread(new FilterCursorArchive(archiveType,startDate, new FilterCursorCallback() {
            @Override
            public void applyCursor(Cursor cursor,Context currentContext) {
                if (mAdapter == null) {
                    mAdapter = new OrderAdapter(currentContext, cursor);
                    orderListView.setAdapter(mAdapter);
                } else {
                    mAdapter.changeCursor(cursor);
                }
            }
        },this));
    }

    private void logOut() {
        Intent intent = new Intent();
        intent.putExtra("password_is_valid", true);
        intent.putExtra("login_is_valid", true);
        intent.putExtra("description", getString(R.string.prompt_login_out));
        setResult(RESULT_CANCELED,intent);
        finish();
    }

    public class OrderAdapter extends CursorAdapter {
        LayoutInflater lInflater;
        SQLiteDatabase db;
        FilterQueryProvider fp;

        OrderAdapter(Context context, Cursor cursor) {
            super(context,cursor,FLAG_REGISTER_CONTENT_OBSERVER);
            lInflater = LayoutInflater.from(context);
            db = sqLiteOpenHelper.getReadableDatabase();
            fp = new FilterQueryProvider() {
                @Override
                public Cursor runQuery(CharSequence constraint) {

                    if (db == null) {
                        return null;
                    }
                    String[] args = {constraint.toString()+"%",loginId};

                    return db.rawQuery(String.format("select o.*,s.description as status from orders as o " +
                            "inner join statuses as s on o.status_id = s._id " +
                            "where o.code like ? and o.driver_id = ?"),args);
                }
            };
            setFilterQueryProvider(fp);
        }


        public void bindView(View view, Context context, Cursor cursor) {


            String dateString = cursor.getString(cursor.getColumnIndex( DBHelper.CN_ORDER_DATE ));
            //((TextView) view.findViewById(R.id.order_item_map)).setText(cursor.getString(cursor.getColumnIndex( DBHelper.CN_ORDER_MAP_ID )));
            ((TextView) view.findViewById(R.id.order_item_title)).setText(cursor.getString(cursor.getColumnIndex( DBHelper.CN_CODE )));
            ((TextView) view.findViewById(R.id.order_item_date)).setText(dateString);
            ((TextView) view.findViewById(R.id.order_item_time)).setText(cursor.getString(cursor.getColumnIndex( DBHelper.CN_ORDER_TIME )));
            ((TextView) view.findViewById(R.id.order_item_customer)).setText(cursor.getString(cursor.getColumnIndex( DBHelper.CN_ORDER_CUSTOMER )));
            ((TextView) view.findViewById(R.id.order_item_status)).setText(cursor.getString(cursor.getColumnIndex( DBHelper.CN_ORDER_STATUS)));
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return  lInflater.inflate(R.layout.item, parent, false);
        }

    }

    public class RefreshOrderListTask extends AsyncTask<Context, Void, Boolean> {

        private final String url = getString(R.string.soap_url);
        private final String method = getString(R.string.soap_method_order_pack);
        private final String namespace = getString(R.string.soap_namespace);
        private final String soap_action = String.format("%s#Obmen:%s",namespace,method);
        private String errorMessage = "";
        private SoapSerializationEnvelope envelope;
        private Context mContext;
        private String mDate;
        private int orderRows;

        RefreshOrderListTask() {
            DateFormat dateFormat = new SimpleDateFormat(FORMATDATE_1C,java.util.Locale.getDefault());
            mDate = dateFormat.format(new Date());
        }

        @Override
        protected Boolean doInBackground(Context... params) {

            mContext  = params[0];
            if (uploadOrderListTask != null) {
                return false;
            }

            try {
                SoapObject soapObject = new SoapObject(namespace, method);

                soapObject.addProperty("LoginID", loginId);
                soapObject.addProperty("Date",mDate);

                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
                envelope.dotNet = true;
                envelope.implicitTypes = true;
                envelope.setOutputSoapObject(soapObject);

                System.setProperty("http.keepAlive", "false");

                HttpTransportSE androidHttpTransport = new HttpTransportSE(url);

                androidHttpTransport.debug = true;
                androidHttpTransport.call(soap_action, envelope);
                // Get the SoapResult from the envelope body.
                SoapObject response = (SoapObject) envelope.getResponse();
                try {

                    SoapPrimitive resultObject = (SoapPrimitive) response.getPrimitiveProperty("Result");
                    Boolean result = Boolean.valueOf(resultObject.toString());
                    if (!result) {
                        errorMessage = response.getPropertyAsString("Description");
                        return false;
                    }
                    //writing to database
                    SQLiteDatabase db = sqLiteOpenHelper.getWritableDatabase();
                    if (db == null) {
                        return false;
                    }

                    //fill payment types
                    SoapObject paymentTypeList = (SoapObject) response.getProperty("PaymentTypeList");
                    int pRows = paymentTypeList.getPropertyCount();
                    for (int i = 0; i < pRows; i++) {
                        SoapObject paymentType = (SoapObject) paymentTypeList.getProperty(i);
                        String ptName = paymentType.getPropertyAsString("Description");
                        String ptId = paymentType.getPropertyAsString("ID");
                        int ptSelectable = Integer.valueOf(paymentType.getPropertyAsString("Selectable"));
                        int ptCode = Integer.valueOf(paymentType.getPropertyAsString("Code"));
                        ContentValues row = new ContentValues();
                        row.put(DBHelper.CN_DESCRIPTION, ptName);
                        row.put(DBHelper.CN_ID, ptId);
                        row.put(DBHelper.CN_SELECTABLE,ptSelectable );
                        row.put(DBHelper.CN_PAYMENT_TYPES_CODE,ptCode );
                        db.replace(DBHelper.TB_PAYMENT_TYPES,null, row);
                    }
                    //fill refuses

                    SoapObject refuseReasonList = (SoapObject) response.getProperty("RefuseReasonList");
                    int refusesRows = refuseReasonList.getPropertyCount();
                    for (int i = 0; i < refusesRows; i++) {
                        SoapObject refuseReason = (SoapObject) refuseReasonList.getProperty(i);
                        String rrName = refuseReason.getPropertyAsString("Description");
                        String rrId = refuseReason.getPropertyAsString("ID");
                        String rrCanceled = refuseReason.getPropertyAsString("Canceled");
                        ContentValues row = new ContentValues();
                        row.put(DBHelper.CN_DESCRIPTION, rrName);
                        row.put(DBHelper.CN_ID, rrId);
                        row.put(DBHelper.CN_CANCELED, rrCanceled);
                        db.replace(DBHelper.TB_REFUSE_REASONS,null, row);
                    }

                    //fill statuses
                    SoapObject statusList = (SoapObject) response.getProperty("StatusList");
                    int statusRows = statusList.getPropertyCount();
                    for (int i = 0; i < statusRows; i++) {
                        SoapObject status = (SoapObject) statusList.getProperty(i);
                        int completed = Integer.valueOf(status.getPropertyAsString("Completed"));
                        String statusName = status.getPropertyAsString("Description");
                        String statusId = status.getPropertyAsString("ID");
                        ContentValues row = new ContentValues();
                        row.put(DBHelper.CN_DESCRIPTION, statusName);
                        row.put(DBHelper.CN_ID, statusId);
                        row.put(DBHelper.CN_STATUSES_COMPLETED, completed);
                        db.replace(DBHelper.TB_STATUSES,null, row);
                    }

                    SoapObject itemList = (SoapObject) response.getProperty("ItemList");
                    int itemRows = itemList.getPropertyCount();
                    for (int i = 0; i < itemRows; i++) {
                        SoapObject item = (SoapObject) itemList.getProperty(i);
                        String itemName = item.getPropertyAsString("Description");
                        String itemId = item.getPropertyAsString("ID");
                        String itemBarcode = item.getPropertyAsString("Barcode");

                        ContentValues row = new ContentValues();
                        row.put(DBHelper.CN_DESCRIPTION, itemName);
                        row.put(DBHelper.CN_ID, itemId);
                        row.put(DBHelper.CN_ITEM_BARCODE, itemBarcode);
                        row.put(DBHelper.CN_ITEM_TYPE, DBHelper.ItemsType.goods.toString());

                        db.replace(DBHelper.TB_ITEMS,null, row);
                    }
                    // Чистим заказы, которые не менялись, все остальные из обмена
                    final Cursor ords = DBHelper.getUselessOrders(db,loginId);
                    while (ords.moveToNext()) {
                        String[] args = {ords.getString(ords.getColumnIndex(DBHelper.CN_ID))};
                        final Cursor ords_c = db.query(DBHelper.TB_ORDERS_CHG, new String[]{DBHelper.CN_ID}, "_id = ?", args, "", "", "");
                        if (!ords_c.moveToNext()){

                            db.delete(DBHelper.TB_ORDERS, "_id = ?", args);
                            db.delete(DBHelper.TB_ORDER_ITEMS,"order_id = ?",args);
                        }
                        ords_c.close();
                    }
                    ords.close();

                    SoapObject orderList = (SoapObject) response.getProperty("OrderList");
                    orderRows = orderList.getPropertyCount();
                    for (int i = 0; i < orderRows; i++) {
                        SoapObject order = (SoapObject) orderList.getProperty(i);
                        String orderId  = order.getPropertyAsString("ID");
                        String[] args = {orderId};
                        //проверяем на завершенность
                        if (isOrderComplete(db,orderId)) continue;

                        //чистим изменения
                        db.delete(DBHelper.TB_ORDERS_CHG,"_id = ?", args);
                        db.delete(DBHelper.TB_ORDERS,"_id = ?", args);

                        String statusId = order.getPropertyAsString("Status_ID");
                        String driverId = order.getPropertyAsString("Login_ID");
                        String refuseId = order.getPropertyAsString("RefuseReason_ID");
                        String paymentId = order.getPropertyAsString("PaymentType_ID");
                        String mapId    = order.getPropertyAsString("Map_ID");
                        String code     = order.getPropertyAsString("Code");
                        String customer = order.getPropertyAsString("Customer");
                        String phone = order.getPropertyAsString("Phone");
                        String comment = order.getPropertyAsString("Comment");
                        String address = order.getPropertyAsString("Address");
                        String time = order.getPropertyAsString("Time");
                        String date = order.getPropertyAsString("Date");
                        DateFormat df = new SimpleDateFormat(FORMATDATE_1C,java.util.Locale.getDefault());
                        Date startDate = df.parse(date);
                        double payment = Double.valueOf(order.getPropertyAsString("Payment"));
                        double delivery_cost = Double.valueOf(order.getPropertyAsString("DeliveryCost"));
                        double levelDeliveryPay = Double.valueOf(order.getPropertyAsString("LevelDeliveryPay"));
                        int canceled = Integer.valueOf(order.getPropertyAsString("Canceled"));
                        int completed = statusId.equalsIgnoreCase("77a59566-15b2-4c0b-9b06-23259066bacc")?1:0;

                        ContentValues orderRow = new ContentValues();
                        orderRow.put(DBHelper.CN_ID, orderId);
                        orderRow.put(DBHelper.CN_ORDER_STATUS_ID, statusId);
                        orderRow.put(DBHelper.CN_ORDER_DRIVER_ID, driverId);
                        orderRow.put(DBHelper.CN_ORDER_REFUSE_ID, refuseId);
                        orderRow.put(DBHelper.CN_ORDER_PAYMENT_TYPE_ID, paymentId);
                        orderRow.put(DBHelper.CN_ORDER_MAP_ID, mapId);
                        orderRow.put(DBHelper.CN_CODE ,code);
                        orderRow.put(DBHelper.CN_ORDER_PHONE, phone);
                        orderRow.put(DBHelper.CN_ORDER_COMMENT, comment);
                        orderRow.put(DBHelper.CN_ORDER_CUSTOMER, customer);
                        orderRow.put(DBHelper.CN_ORDER_ADDRESS, address);
                        orderRow.put(DBHelper.CN_ORDER_TIME, time);
                        orderRow.put(DBHelper.CN_ORDER_DATE, new SimpleDateFormat(FORMATDATE_APP).format(startDate));
                        orderRow.put(DBHelper.CN_ORDER_PAYMENT, payment);
                        orderRow.put(DBHelper.CN_ORDER_DELIVERY_COST, delivery_cost);
                        orderRow.put(DBHelper.CN_ORDER_LEVEL_DELIVERY_PAY, levelDeliveryPay);
                        orderRow.put(DBHelper.CN_ORDER_POSTED, 0);
                        orderRow.put(DBHelper.CN_ORDER_COMPLETED, completed);
                        orderRow.put(DBHelper.CN_ORDER_CANCELED, canceled);
                        db.replace(DBHelper.TB_ORDERS,null, orderRow);
                        SoapObject itemOrderList = (SoapObject) order.getProperty("ItemList");
                        int itemOrderRows = itemOrderList.getPropertyCount();

                        //clear all previous data

                        db.delete(DBHelper.TB_ORDER_ITEMS,"order_id = ?",args);
                        db.delete(DBHelper.TB_ORDER_PAYMENTS,"order_id = ?",args);

                        for (int j = 0; j < itemOrderRows; j++) {
                            SoapObject item = (SoapObject) itemOrderList.getProperty(j);

                            String itemId = item.getPropertyAsString("ID");
                            int itemOrderCount = Integer.valueOf(item.getPropertyAsString("Count"));
                            double itemOrderCost = Double.valueOf(item.getPropertyAsString("Cost"));
                            double itemOrderDiscount = Double.valueOf(item.getPropertyAsString("Discount"));
                            int checked = Integer.valueOf(item.getPropertyAsString("Checked"));
                            int nds = Integer.valueOf(item.getPropertyAsString("NDS"));
                            String eid = item.getPropertyAsString("EID");

                            ContentValues orderItemRow = new ContentValues();
                            orderItemRow.put(DBHelper.CN_ORDER_ID, orderId);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_ID, itemId);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_COUNT, itemOrderCount);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_COST, itemOrderCost);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_DISCOUNT, Math.floor(itemOrderDiscount));
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_CHECKED, checked);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_NDS, nds);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_EID, eid);
                            db.replace(DBHelper.TB_ORDER_ITEMS,null, orderItemRow);
                        }

                        if (delivery_cost > 0) {
                            ContentValues orderItemRow = new ContentValues();
                            orderItemRow.put(DBHelper.CN_ORDER_ID, orderId);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_ID, DBHelper.ID_DELIVERY);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_COUNT, 1);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_COST, delivery_cost);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_DISCOUNT, 0);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_CHECKED, 0);//DBHelper.isDeliveryCheck(db, orderId)?1:0);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_NDS, 3);
                            orderItemRow.put(DBHelper.CN_ORDER_ITEM_EID, "9999999999");
                            db.replace(DBHelper.TB_ORDER_ITEMS,null, orderItemRow);
                        }
                        if (payment > 0) {
                            ContentValues orderPaymentRow = new ContentValues();
                            orderPaymentRow.put(DBHelper.CN_ORDER_ID, orderId);
                            orderPaymentRow.put(DBHelper.CN_ORDER_PAYMENT_TYPE_ID, paymentId);
                            orderPaymentRow.put(DBHelper.CN_ORDER_PAYMENT_SUM, payment);
                            orderPaymentRow.put(DBHelper.CN_ORDER_PAYMENT_PAID, 1);//уже оплачен
                            orderPaymentRow.put(DBHelper.CN_ORDER_PAYMENT_CHEQUE_TYPE, 1);//продажа//2- возврат
                            db.replace(DBHelper.TB_ORDER_PAYMENTS,null, orderPaymentRow);
                        }
                    }

                    db.close();

                } catch (Exception e) {
                    errorMessage = e.getMessage();
                }
            } catch (Exception e) {

                SoapFault responseFault = (SoapFault) envelope.bodyIn;

                if (responseFault == null) {
                    errorMessage = getString(R.string.error_no_internet_connection);
                } else
                    try {
                        errorMessage = responseFault.faultstring;
                    } catch (Exception ef) {
                        errorMessage = getString(R.string.error_unknown);
                    }
            }//process request



            return errorMessage.isEmpty();
        }

        private Boolean isOrderComplete(SQLiteDatabase db,String orderId) {

            if (db == null) {
                return null;
            }
            Cursor cursor = db.rawQuery(String.format("select * from orders o where o._id = ? and o.driver_id = ? and o.completed = 1") ,new String[] {orderId,loginId});
            Boolean complete = cursor.getCount() > 0;
            cursor.close();
            return complete;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            onCancelled();

            if (success) {

                applyNavigationFilter(mNavigationMenuItemId);
                Toast.makeText(mContext,getString(R.string.alert_refresh) +" "+orderRows,Toast.LENGTH_LONG).show();

            } else {

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setMessage(errorMessage).setTitle(R.string.soap_error_order_pack);
                builder.create().show();

            }
        }

        @Override
        protected void onCancelled() {
            refreshOrderListTask = null;
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    public class UploadOrderListTask extends AsyncTask<Void, Integer, Boolean> {

        private final String url = getString(R.string.soap_url);
        private final String method = getString(R.string.soap_method_put_order_pack);
        private final String namespace = getString(R.string.soap_namespace);
        private final String soap_action = String.format("%s#Obmen:%s",namespace,method);
        private String errorMessage = "";
        private String errorCode = "";
        private SoapSerializationEnvelope envelope;
        private Context mContext;
        private String[] ordersResult;
        private AsyncTaskCallback mCallback;

        UploadOrderListTask(Context context,AsyncTaskCallback callback) {
            mContext = context;
            mCallback = callback;

        }
        UploadOrderListTask(Context context) {
            this(context,null);

        }

        @Override
        protected Boolean doInBackground(Void... params) {


            try {

                SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
                if (db == null) {
                    return false;
                }

                SoapObject soapObjectOrderPaymentList = new SoapObject(namespace,"OrderPaymentList");

                Cursor cursorOrderPayment = db.rawQuery(String.format("select op.order_id,op.payment_type_id,op.sum,ifnull(op.date,'ошибка разбора') as date,ct.description check_type from order_payments as op" +
                        " inner join check_types as ct on op.check_type = ct.code " +
                        " inner join orders_chg as och on op.order_id = och._id " +
                        " where och.driver_id = ?"),new String[] {loginId});

                while (cursorOrderPayment.moveToNext()) {

                    SoapObject soapOrderPaymentObject = new SoapObject(namespace,"OrderPayment");
                    soapOrderPaymentObject.addProperty("ID",cursorOrderPayment.getString(cursorOrderPayment.getColumnIndex(DBHelper.CN_ORDER_ID)));
                    soapOrderPaymentObject.addProperty("PaymentType_ID",cursorOrderPayment.getString(cursorOrderPayment.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_TYPE_ID)));
                    soapOrderPaymentObject.addProperty("ChequeType",cursorOrderPayment.getString(cursorOrderPayment.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_CHEQUE_TYPE)));
                    soapOrderPaymentObject.addProperty("Payment",cursorOrderPayment.getString(cursorOrderPayment.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_SUM)));
                    soapOrderPaymentObject.addProperty("Date",cursorOrderPayment.getString(cursorOrderPayment.getColumnIndex(DBHelper.CN_ORDER_DATE)));
                    soapObjectOrderPaymentList.addSoapObject(soapOrderPaymentObject);

                }
                cursorOrderPayment.close();

                Cursor cursor = db.rawQuery(String.format("select o.*,oi.item_id,oi.checked,oi.count,oi.eid,(oi.cost - oi.discount) * oi.count as sum from orders as o " +
                        "inner join orders_chg as oc on o._id = oc._id " +
                        "inner join order_items as oi on o._id = oi.order_id  where o.driver_id = ?"), new String[] {loginId});


                if (cursor.getCount() == 0) {
                    errorMessage = getString(R.string.error_no_data_for_uploading);
                    return false;
                }
                SoapObject requestObject = new SoapObject(namespace, method);

                SoapObject soapObjectList = new SoapObject(namespace,"OrderChangeList");

                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
                envelope.dotNet = true;

                while (cursor.moveToNext()) {

                    SoapObject soapObject = new SoapObject(namespace,"OrderChange");

                    soapObject.addProperty("ID",cursor.getString(cursor.getColumnIndex(DBHelper.CN_ID)));
                    soapObject.addProperty("Status_ID",cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_STATUS_ID)));
                    soapObject.addProperty("Login_ID",cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_DRIVER_ID)));
                    soapObject.addProperty("RefuseReason_ID",cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_REFUSE_ID)));
                    soapObject.addProperty("Code",cursor.getString(cursor.getColumnIndex(DBHelper.CN_CODE)));
                    soapObject.addProperty("Item_ID",cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_ID)));
                    soapObject.addProperty("Count",cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COUNT)));
                    soapObject.addProperty("Map_ID",cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_MAP_ID)));
                    soapObject.addProperty("Completed",cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_COMPLETED)));
                    soapObject.addProperty("Canceled",cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_CANCELED)));
                    soapObject.addProperty("Checked",cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_CHECKED)));
                    soapObject.addProperty("Summa",cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_SUM)));
                    soapObject.addProperty("EID",cursor.getString(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_EID)));

                    soapObjectList.addSoapObject(soapObject);
                }

                cursor.close();
                db.close();

                requestObject.addProperty("OrderList",soapObjectList);
                requestObject.addProperty("OrderPaymentList",soapObjectOrderPaymentList);


                //envelope.implicitTypes = true;
                envelope.setOutputSoapObject(requestObject);


                System.setProperty("http.keepAlive", "false");

                HttpTransportSE androidHttpTransport = new HttpTransportSE(url);
                androidHttpTransport.debug = true;
                androidHttpTransport.call(soap_action, envelope);
                // Get the SoapResult from the envelope body.
                SoapObject response = (SoapObject) envelope.getResponse();
                try {

                    SoapPrimitive resultObject = (SoapPrimitive) response.getProperty("Result");
                    SoapPrimitive resultMessage = (SoapPrimitive) response.getProperty("Name");
                    Boolean result = Boolean.valueOf(resultObject.toString());
                    if (!result )
                    {
                        errorMessage = resultMessage.toString();
                        return false;
                    }
                    ordersResult = resultMessage.toString().split(";");


                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    return false;
                }
            } catch (Exception e) {

                if (e instanceof SQLiteException) {
                    errorMessage = e.getMessage();
                    return false;
                } else if (e instanceof IllegalFormatException)
                {
                    errorMessage = e.getMessage();
                    return false;
                } else if (e instanceof RuntimeException) {
                    errorMessage = e.getMessage();
                    return false;
                }

                SoapFault responseFault = (SoapFault) envelope.bodyIn;

                if (responseFault == null) {
                    errorMessage = getString(R.string.error_no_internet_connection);
                } else
                    try {
                        errorCode = responseFault.faultcode;
                        errorMessage = responseFault.faultstring;
                    } catch (Exception ef) {
                        errorMessage = getString(R.string.error_unknown);
                    }
                return false;
            }//process request


            if (ordersResult.length > 0) {

                SQLiteDatabase db = sqLiteOpenHelper.getWritableDatabase();
                if (db == null) {
                    return false;
                }
                for (String order:ordersResult) {
                    db.delete(DBHelper.TB_ORDERS_CHG,"_id = ?",new String[]{order});
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.CN_ID, order);
                    cv.put(DBHelper.CN_ORDER_DRIVER_ID, loginId);
                    cv.put(DBHelper.CN_ORDER_POSTED, 1);
                    db.update(DBHelper.TB_ORDERS,cv,"_id = ?",new String[]{order});
                }
                db.close();

                return true;
            }
            errorMessage = getString(R.string.order_details_error_upload_orders);
            return false;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            setProgressPercent(progress[0]);
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            onCancelled();

            if (success) {

                applyNavigationFilter(mNavigationMenuItemId);
                Toast.makeText(mContext,getString(R.string.orders_uploaded),Toast.LENGTH_LONG).show();

            } else {

                if (mCallback == null) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage(errorMessage).setTitle(R.string.soap_error_order_pack);
                    builder.create().show();
                }

            }

            if (mCallback != null) mCallback.onPostExecute(success,getString(R.string.soap_error_order_changes)+": "+errorMessage);
        }

        @Override
        protected void onCancelled() {

            uploadOrderListTask = null;
            swipeRefreshLayout.setRefreshing(false);
            if (mCallback != null) mCallback.onCanceled();
        }
    }

    public void setProgressPercent(int progress) {

    }

}

