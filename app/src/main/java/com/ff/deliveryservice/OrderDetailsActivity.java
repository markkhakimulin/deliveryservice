package com.ff.deliveryservice;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.atol.drivers.fptr.Fptr;
import com.atol.drivers.fptr.IFptr;
import com.ff.deliveryservice.dialogs.ChequeConfirmDialog;
import com.ff.deliveryservice.dialogs.SimpleListChequeTypeDialog;
import com.ff.deliveryservice.dialogs.SimpleListPaymentDialog;
import com.ff.deliveryservice.dialogs.SimpleListReasonDialog;
import com.ff.deliveryservice.fragments.OnFragmentHandler;
import com.ff.deliveryservice.fragments.OrderDetailsFragment;
import com.ff.deliveryservice.fragments.OrderItemsFragment;
import com.ff.deliveryservice.fragments.OrderPaymentsFragment;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
/**
 * Created by khakimulin on 22.02.2017.
 */

/**
 * A general activity to make order and payment for it.
 */

public class OrderDetailsActivity extends FPTRActivity implements OnFragmentHandler {

    private static final int REQUEST_SHOW_SCANNER = 3;

    public static final String FRAGMENT_PAGE_DETAILS   = OrderDetailsFragment.class.getCanonicalName();
    public static final String FRAGMENT_PAGE_ITEMS     = OrderItemsFragment.class.getCanonicalName();
    public static final String FRAGMENT_PAGE_PAYMENTS  = OrderPaymentsFragment.class.getCanonicalName();

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

    private SQLiteOpenHelper sqLiteOpenHelper;
    private FloatingActionButton fab1,fab;
    private Context activityContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);
        activityContext = this;
        Intent intent = getIntent();
        orderId = intent.getStringExtra(DBHelper.CN_ORDER_ID);
        loginId = intent.getStringExtra(DBHelper.CN_ID);
        numberId = intent.getStringExtra(DBHelper.CN_CODE);
        loginDesc = intent.getStringExtra(DBHelper.CN_DESCRIPTION);

        sqLiteOpenHelper = DBHelper.getOpenHelper(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(),this);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    Intent intent = new Intent(view.getContext(), FullScannerResultActivity.class);
                    startActivityForResult(intent,REQUEST_SHOW_SCANNER);
            }
        });
        // Creates a new ImageView
        fab1 = (FloatingActionButton) findViewById(R.id.fab1);
        fab1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                fab1.setOnTouchListener(new View.OnTouchListener() {

                    float _xDelta = 0;
                    float _yDelta = 0;
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {

                        switch (event.getAction()) {


                            case MotionEvent.ACTION_MOVE:

                                if (_xDelta == 0 &&_yDelta == 0 ) {
                                    _xDelta = view.getX() - event.getRawX();
                                    _yDelta = view.getY() - event.getRawY();
                                }

                                view.animate()
                                        .x(event.getRawX() + _xDelta)
                                        .y(event.getRawY() + _yDelta)
                                        .setDuration(0)
                                        .start();
                                break;
                            case MotionEvent.ACTION_UP:

                                fab1.setOnTouchListener(null);
                                break;
                            default:
                                return false;
                        }
                        return true;
                    }
                });
                return false;
            }
        });


        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab1.setEnabled(false);
                runOnUiThread(new OrderDetailsCheckType());

            }
        });

        setTitle(getString(R.string.title_activity_order_detail) +": "+ numberId);
    }

    public void hideFabButtons() {
        fab1.setVisibility(View.INVISIBLE);
        fab.setVisibility(View.INVISIBLE);
    }
    public void showFabButtons() {
        fab1.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);
    }

    public void onCancel() {
        fab1.setEnabled(true);
    }

    @Override
    public void onEditPaymentComplete() {

        runOnUiThread(new OrderItemPaymentCursorUpdate());//update payment list


    }
    @Override
    protected void onResume() {
        super.onResume();
        hideProgressDialog();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode,resultCode,data);

        if (requestCode == REQUEST_SHOW_SCANNER && resultCode == RESULT_OK)
        {
            String barcode = data.getStringExtra("barcode");
            runOnUiThread(new SearchBarcode(barcode, new SearchBarcodeCallback() {
                @Override
                public void processCallback(int result,String reason,String barcode,Context context) {

                    if (result == 0) {
                        Toast.makeText(context, reason +" = "+barcode , Toast.LENGTH_LONG).show();
                    } else
                    {
                        runOnUiThread(new OrderChanged(null));
                        runOnUiThread(new OrderItemListCursorUpdate());//update item list
                    }

                }

                @Override
                public void lockChequeButton() {

                }

                @Override
                public void unlockChequeButton() {

                }
            },this));
        }
    }

    @Override
    public void onFragmentViewCreated(String fragment) {
        if (fragment == FRAGMENT_PAGE_DETAILS) {
            runOnUiThread(new OrderDetailsCursorUpdate());
        } else if (fragment == FRAGMENT_PAGE_ITEMS) {
            runOnUiThread(new OrderItemListCursorUpdate());
        } else if (fragment == FRAGMENT_PAGE_PAYMENTS) {
            runOnUiThread(new OrderItemPaymentCursorUpdate());
        }
    }

    @Override
    public void onItemClicked(String itemId,String eid) {

        runOnUiThread(new UpdateOrderItem(itemId,eid, new SearchBarcodeCallback() {
            @Override
            public void processCallback(int result, String reason, String barcode, Context context) {

                if (result > 0) {
                    runOnUiThread(new OrderChanged(null));
                    runOnUiThread(new OrderItemListCursorUpdate());
                } else {
                    Toast.makeText(context,reason,Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void lockChequeButton() {

            }

            @Override
            public void unlockChequeButton() {

            }
        },this));
    }

    @Override
    public void onItemLongClicked(String itemId) {
        runOnUiThread(new UpdateOrderItemDelivery(itemId, new SearchBarcodeCallback() {
            @Override
            public void processCallback(int result, String reason, String barcode, Context context) {

                if (result > 0) {
                    runOnUiThread(new OrderChanged(null));
                    runOnUiThread(new OrderItemListCursorUpdate());
                } else {
                    Toast.makeText(context,reason,Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void lockChequeButton() {

            }

            @Override
            public void unlockChequeButton() {

            }
        },this));
    }

    @Override
    public void onChequeClicked(Map<Integer,Double> map,final int checkType,String notification) {

        showProgressDialog(getString(R.string.action_process_cheque));

        AddPayment payment = new AddPayment(map,checkType,notification, new SearchBarcodeCallback() {
            @Override
            public void processCallback(int result, String reason, String id, Context context) {
                if (result == 0) {
                    Toast.makeText(context, reason, Toast.LENGTH_SHORT).show();
                } else
                {
                    runOnUiThread(new OrderCompleted(checkType));
                    runOnUiThread(new OrderItemPaymentCursorUpdate());//update payment list
                }
                onCancel();
            }

            @Override
            public void lockChequeButton() {
                fab1.setEnabled(false);
            }

            @Override
            public void unlockChequeButton() {
                onCancel();
            }
        },new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                hideProgressDialog();
                return null;
            }
        });
        payment.execute(this);
    }

    @Override
    public void onCancelClicked() {
        runOnUiThread(new OrderDetailsRefuse(new SearchBarcodeCallback() {
            @Override
            public void processCallback(int result, String reason, String barcode, Context context) {
                if (result == 0) {
                    Toast.makeText(context, reason, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void lockChequeButton() {

            }

            @Override
            public void unlockChequeButton() {

            }
        },this));
    }

    @Override
    public void onReasonChoosed(String reasonId,int canceled) {
        runOnUiThread(new OrderCanceled(reasonId,canceled));
    }

    @Override
    public void onCheckTypeChoosed(int checkType) {
        runOnUiThread(new OrderDetailsPayment(checkType));
    }

    @Override
    public void onPaymentTypeChoosed(Map<Integer,Double> map,int checkType) {
        runOnUiThread(new ConfirmPayment(map,checkType));
    }

    public class ConfirmPayment implements Runnable {
        //private String mPaymentTypeId;
        private Map<Integer,Double> mPaymentTypeCode;
        private int mCheckType;

        ConfirmPayment(Map<Integer,Double> map,int checkType) {
            //mPaymentTypeId = paymentTypeId;
            mPaymentTypeCode = map;
            mCheckType = checkType;
        }

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            String[] args = {orderId};
            Cursor cursor = db.rawQuery(String.format("select oi._id,oi.order_id,oi.item_id,oi.checked,oi.discount,oi.cost,oi.count,i.description from order_items as oi left join items as i on oi.item_id = i._id where oi.order_id = ? and oi.checked = 1 group by oi._id,oi.order_id,oi.item_id,oi.checked,oi.discount,oi.cost,oi.count,i.description"), args);

            showCheckConfirmDialog(cursor,mPaymentTypeCode, mCheckType);
        }
    }

    public class OrderCanceled implements Runnable {
        private String mItemId = "";
        private int mCanceled;

        OrderCanceled(String itemId,int canceled) {
            mItemId = itemId;
            mCanceled = canceled;
        }

        @Override
        public void run() {

            ContentValues cv = new ContentValues();
            cv.put(DBHelper.CN_ORDER_REFUSE_ID, mItemId);
            cv.put(DBHelper.CN_ORDER_CANCELED, mCanceled);

            SQLiteDatabase db = sqLiteOpenHelper.getWritableDatabase();
            if (db == null) {
                return;
            }
            db.update(DBHelper.TB_ORDERS, cv, "_id = ? and driver_id = ?", new String[] { orderId,loginId });
            db.close();

            runOnUiThread(new OrderChanged(null));
            runOnUiThread(new OrderDetailsCursorUpdate());
        }
    }

    public class OrderCompleted implements Runnable {
        private int mCompleted = 0;
        private int mCheckType;

        OrderCompleted(int checkType) {
            mCheckType = checkType;
        }

        @Override
        public void run() {

            ContentValues cv = new ContentValues();

            SQLiteDatabase db = sqLiteOpenHelper.getWritableDatabase();

            if (db == null) {
                return;
            }

            double sumToPay = getSumToPay(db,IFptr.CHEQUE_TYPE_SELL);
            cv.put(DBHelper.CN_ORDER_COMPLETED, sumToPay > 0.0 ? 0 : 1);
            db.update(DBHelper.TB_ORDERS, cv, "_id = ? and driver_id = ?", new String[] { orderId,loginId });
            db.close();

            runOnUiThread(new OrderChanged(null));
        }
    }

    public class UpdateOrderItem implements Runnable {
        private String mItemId = "",mEID = "";
        private SearchBarcodeCallback mCallback;
        private Context mContext;

        UpdateOrderItem(String itemId,String eid, SearchBarcodeCallback callback, Context context) {
            mItemId = itemId;
            mEID = eid;
            mCallback = callback;
            mContext = context;
        }

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (isOrderComplete(db)) {
                db.close();
                if (mCallback != null)
                    mCallback.processCallback(0,getString(R.string.order_detail_order_completed),orderId,mContext);
                return;
            }

            Cursor cursor = db.rawQuery(String.format("select _id,checked from order_items where order_id = ? and item_id = ? and eid = ?"),new String[] {orderId,mItemId,mEID});
            ContentValues cv = new ContentValues();
            if (cursor.moveToNext()) {
                cv.put(DBHelper.CN_ORDER_ITEM_CHECKED, cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_CHECKED)) == 1 ? 0 : 1);
                cursor.close();
            } else
            {
                mCallback.processCallback(0,getString(R.string.order_details_error_item_order_not_found),mItemId,mContext);
                return;
            }
            db.close();

            db = sqLiteOpenHelper.getWritableDatabase();
            if (db == null) {
                return;
            }
            int update = db.update(DBHelper.TB_ORDER_ITEMS, cv, "order_id = ? and item_id = ? and eid = ?", new String[] { orderId,mItemId,mEID });
            cv = new ContentValues();
            cv.put(DBHelper.CN_ORDER_ITEM_CHECKED, DBHelper.isDeliveryCheck(db, orderId)?1:0);
            db.update(DBHelper.TB_ORDER_ITEMS, cv, "item_id = ? and order_id = ?", new String[] {DBHelper.ID_DELIVERY, orderId });
            db.close();
            mCallback.processCallback(update,"",mItemId,mContext);
        }
    }

    public class UpdateOrderItemDelivery implements Runnable {
        private String mItemId = "";
        private SearchBarcodeCallback mCallback;
        private Context mContext;

        UpdateOrderItemDelivery(String itemId, SearchBarcodeCallback callback, Context context) {
            mItemId = itemId;
            mCallback = callback;
            mContext = context;
        }

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (isOrderComplete(db)) {
                db.close();
                if (mCallback != null)
                    mCallback.processCallback(0,getString(R.string.order_detail_order_completed),orderId,mContext);
                return;
            }

            Cursor cursor = db.rawQuery(String.format("select _id,checked from order_items where order_id = ? and item_id = ?"),new String[] {orderId,mItemId});
            ContentValues cv = new ContentValues();
            if (cursor.moveToNext()) {
                cv.put(DBHelper.CN_ORDER_ITEM_CHECKED, cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_CHECKED)) == 1 ? 0 : 1);
                cursor.close();
            } else
            {
                mCallback.processCallback(0,getString(R.string.order_details_error_item_order_not_found),mItemId,mContext);
                return;
            }
            db.close();

            db = sqLiteOpenHelper.getWritableDatabase();
            if (db == null) {
                return;
            }
            int update = db.update(DBHelper.TB_ORDER_ITEMS, cv, "item_id = ? and order_id = ?", new String[] {DBHelper.ID_DELIVERY, orderId });
            db.close();
            mCallback.processCallback(update,"",mItemId,mContext);
        }
    }

    public class SearchBarcode implements Runnable {
        private String mBarcode = "";
        private SearchBarcodeCallback mCallback;
        private Context mContext;

        SearchBarcode(String barcode, SearchBarcodeCallback callback,Context context) {
            mBarcode = barcode;
            mCallback = callback;
            mContext = context;
        }

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (isOrderComplete(db)) {
                db.close();
                if (mCallback != null)
                    mCallback.processCallback(0,getString(R.string.order_detail_order_completed),orderId,mContext);
                return;
            }
            String itemId = "";
            Cursor cursor = db.rawQuery(String.format("select oi._id,oi.checked,oi.item_id, oi.eid from order_items oi join items i on i._id = oi.item_id where oi.order_id = ? and (oi.eid = ? or i.barcode = ?)"),new String[] {orderId,mBarcode,mBarcode});

            ContentValues cv = new ContentValues();
            if (cursor.moveToNext()) {
                cv.put(DBHelper.CN_ID, cursor.getString(0));
                cv.put(DBHelper.CN_ORDER_ITEM_CHECKED, cursor.getInt(1) == 1 ? 0 : 1);
                itemId = cursor.getString(3);

            } else
            {
                if (mCallback != null)
                mCallback.processCallback(0,getString(R.string.order_details_error_item_order_not_found),mBarcode,mContext);
                cursor.close();
                return;
            }
            cursor.close();
            db.close();

            db = sqLiteOpenHelper.getWritableDatabase();
            if (db == null) {
                return;
            }
            int update = db.update(DBHelper.TB_ORDER_ITEMS, cv, "order_id = ? and eid = ?", new String[] { orderId,itemId });
            cv = new ContentValues();
            cv.put(DBHelper.CN_ORDER_ITEM_CHECKED, DBHelper.isDeliveryCheck(db, orderId)?1:0);
            db.update(DBHelper.TB_ORDER_ITEMS, cv, "order_id = ? and item_id = \"1\"", new String[] { orderId });
            db.close();
            if (mCallback != null)
            mCallback.processCallback(update,"",mBarcode,mContext);
        }
    }

    public class OrderChanged implements Runnable {
        private SearchBarcodeCallback mCallback;

        OrderChanged(SearchBarcodeCallback callback) {
            mCallback = callback;
        }

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getWritableDatabase();
            if (db == null) {
                return;
            }

            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String date = dateFormat.format(new Date());
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.CN_ID, orderId);
            cv.put(DBHelper.CN_ORDER_DRIVER_ID, loginId);
            cv.put(DBHelper.CN_CODE, numberId);
            cv.put(DBHelper.CN_ORDER_TIME, date);
            long update = db.replace(DBHelper.TB_ORDERS_CHG,null, cv);
            db.close();
            if (mCallback != null)
            mCallback.processCallback((int) update,"",orderId,null);
        }
    }

    public class OrderDetailsRefuse implements Runnable {

        private SearchBarcodeCallback mCallback;
        private Context mContext;

        OrderDetailsRefuse(SearchBarcodeCallback callback, Context context) {
            mCallback = callback;
            mContext = context;
        }

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (isOrderComplete(db)) {
                db.close();
                if (mCallback != null)
                    mCallback.processCallback(0,getString(R.string.order_detail_order_completed),orderId,mContext);
                return;
            }
            Cursor cursor = db.query(DBHelper.TB_REFUSE_REASONS, null, null, null, null, null, null);
            showRefuseDialog(cursor);
        }
    }

    public class OrderDetailsPayment implements Runnable {

        int mCheckType;

        public OrderDetailsPayment(int checkType) {
            mCheckType = checkType;
        }

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }

            Cursor cursor;
            if (mCheckType == IFptr.CHEQUE_TYPE_SELL) {
                cursor = db.rawQuery(String.format("select * from payment_types  where selectable = 1"), null);

            } else {//исключаем те виды оплаты которых нет в текущей оплате, чтобы они случайно не выбрали другой тип оплаты при возврате
                /*cursor = db.rawQuery(String.format("select * from payment_types as pt " +
                        "inner join (select payment_type_id,sum(" +
                        "case when check_type < 2 then (sum - ifnull(discount,0))  " +
                        "else -1 * (sum - ifnull(discount,0))) end paid_sum from order_payments where order_id = ? group by payment_type_id) as op on pt._id = op.payment_type_id " +
                        "where op.paid_sum > 0"), new String[]{orderId});//and op.paid > 0*/

                cursor = db.rawQuery(String.format("select * from payment_types as pt " +
                        "inner join (select payment_type_id,sum(case when check_type < 2 then (sum - ifnull(discount, 0)) else -1 * (sum - ifnull(discount, 0)) end) as paid_sum " +
                        "from order_payments where order_id = ? " +
                        "group by payment_type_id) as op on pt._id = op.payment_type_id " +
                        "where op.paid_sum > 0"), new String[]{orderId});//and op.paid > 0

            }
            //сразу расчитываем сумму для смешанной оплаты если это чек продажи
            double sumToPay = getSumToPay(db,mCheckType);

            showPaymentTypesDialog(cursor,mCheckType,sumToPay);
        }
    }

    public class OrderDetailsCheckType implements Runnable {

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            Cursor cursor = db.rawQuery("select * from check_types",null);

            showCheckTypeDialog(cursor);
        }
    }

    public Boolean isOrderComplete(SQLiteDatabase db) {

        if (db == null) {
            return null;
        }
        Cursor cursor = db.rawQuery(String.format("select o._id,s.completed from orders as o "+
                " inner join statuses as s on o.status_id = s._id "+
                " where o._id = ? and (o.posted = 1 or o.completed = 1)") ,new String[] {orderId});
        return cursor.getCount() > 0;
    }

    public Boolean isOrderPaid(SQLiteDatabase db) {

        if (db == null) {
            return null;
        }
        Cursor cursor = db.rawQuery(String.format("select op.* from order_payments op where op.order_id = ? and op.paid = 1"),new String[] {orderId});

        if (cursor.getCount() > 0) {

            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }

    public static double getSumToPay(SQLiteDatabase db,int checkType) {

        if (db == null) {
            return 0;
        }
        double sum = 0;
        Cursor cursor = null;
        if (checkType == IFptr.CHEQUE_TYPE_SELL) {//проверяем если уже был платеж то если дозаказали товар
            cursor = db.rawQuery(String.format("select tp.topay - ifnull(op.paid,0) sum " +
                    "from " +
                    "(select sum(count * (cost - discount)) topay,order_id  from order_items " +
                    "where order_id = ? and checked = 1) tp  " +
                    "left join " +
                    "(select sum(p.paid) paid,p.order_id from " +
                    "(select case when check_type = 1 then sum - ifnull(discount,0) else -(sum - ifnull(discount,0)) end paid,order_id from order_payments where order_id = ?) p " +
                    "group by p.order_id) op " +
                    "on tp.order_id = op.order_id ") , new String[]{orderId,orderId});
            //cursor = db.rawQuery("select sum(p.paid) paid,p.order_id from (select case when check_type = 1 then sum - ifnull(discount,0) else -(sum - ifnull(discount,0)) end paid,order_id from order_payments where order_id = ?) p group by p.order_id", new String[]{ orderId});
        }  else if (checkType == IFptr.CHEQUE_TYPE_RETURN) {
            cursor = db.rawQuery(String.format("select tp.topay sum " +
                    "from " +
                    "(select sum(count * (cost - discount)) topay from order_items " +
                    "where order_id = ? and checked = 1 ) as tp"), new String[]{ orderId});
        }


        while (cursor.moveToNext()) {
            sum += cursor.getDouble(cursor.getColumnIndex(DBHelper.CN_ORDER_PAYMENT_SUM));
        }
        cursor.close();
        return sum;
    }

    private static String getDriverDescription(SQLiteDatabase db) {

        String name = "Курьер";
        if (db == null) {
            return name;
        }
        Cursor cursor = db.rawQuery(String.format("select decription from drivers where _id = ? "), new String[]{ loginId});
        while (cursor.moveToNext()) {
            name = cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION));
        }
        cursor.close();
        return name;
    }

    private String getError() {
        int rc = fptr.get_ResultCode();
        if (rc < 0) {
            String rd = fptr.get_ResultDescription(), bpd = null;
            if (rc == -6) {
                bpd = fptr.get_BadParamDescription();
            }
            if (bpd != null) {
                return String.format("[%d] %s (%s)", rc, rd, bpd);
            } else {
                return String.format("[%d] %s", rc, rd);
            }
        }
        return "";
    }

    public class AddPayment extends AsyncTask<Context, String, Integer> {
    //public class AddPayment {
        //private String mPaymentTypeId = "";
        private Map<Integer,Double> mPaymentTypeCode;
        private int mCheckType;
        private SearchBarcodeCallback mCallback;
        private Context mContext;
        private SQLiteDatabase db;
        private String mErrorMessage;
        private BigDecimal mSum = new BigDecimal(0);
        private BigDecimal mDiscount = new BigDecimal(0);
        private double mRemainder,mChange;
        private Snackbar snackbar;
        private Boolean isReturnCheque;
        private Callable<Boolean> mCompleteCallback;
        private String mNotification;
        private int mSession;


        AddPayment(Map<Integer,Double> paymentTypeCode,int checkType,String notification, SearchBarcodeCallback callback,Callable<Boolean> completeCallback) {
            //mPaymentTypeId = paymentTypeId;
            mCallback = callback;
            mPaymentTypeCode = paymentTypeCode;
            mCheckType = checkType;
            isReturnCheque = mCheckType == IFptr.CHEQUE_TYPE_RETURN;
            mCompleteCallback = completeCallback;
            mNotification = notification;


        }
        private void checkError() throws DriverException {
            int rc = fptr.get_ResultCode();
            if (rc < 0) {
                String rd = fptr.get_ResultDescription(), bpd = null;
                if (rc == -6) {
                    bpd = fptr.get_BadParamDescription();
                }
                if (bpd != null) {
                    throw new DriverException(String.format("[%d] %s (%s)", rc, rd, bpd));
                } else {
                    throw new DriverException(String.format("[%d] %s", rc, rd));
                }

            }

            mCallback.unlockChequeButton();
        }

        private void printText(String text, int alignment, int wrap) throws DriverException {
            if (fptr.put_Caption(text) < 0) {
                checkError();
            }
            if (fptr.put_TextWrap(wrap) < 0) {
                checkError();
            }
            if (fptr.put_Alignment(alignment) < 0) {
                checkError();
            }
            if (fptr.PrintString() < 0) {
                checkError();
            }
        }

        private void printText(String text) throws DriverException {
            printText(text, IFptr.ALIGNMENT_CENTER, IFptr.WRAP_LINE);
        }

        private void openCheck(int type) throws DriverException {
            if (fptr.put_Mode(IFptr.MODE_REGISTRATION) < 0) {
                checkError();
            }
            if (fptr.SetMode() < 0) {
                checkError();
            }

            if (fptr.put_CheckType(type) < 0) {
                checkError();
            }
            if (fptr.OpenCheck() < 0) {
                checkError();
            }

        }

        private int closeCheck(int typeClose) throws DriverException {
            if (fptr.put_TypeClose(typeClose) < 0) {
                checkError();
            }
            if (fptr.CloseCheck() < 0) {
                checkError();
            }
            return fptr.get_CheckNumber();
        }

        private void reportZ() throws DriverException {
            if (fptr.put_Mode(IFptr.MODE_REPORT_CLEAR) < 0) {
                checkError();
            }
            if (fptr.SetMode() < 0) {
                checkError();
            }
            if (fptr.put_ReportType(IFptr.REPORT_Z) < 0) {
                checkError();
            }
            if (fptr.Report() < 0) {
                checkError();
            }
        }

        private void registrationFZ54(String name, double price, double quantity,
                                      double positionSum, int taxNumber) throws DriverException {
            if (fptr.put_TaxNumber(taxNumber) < 0) {
                checkError();
            }
            if (fptr.put_PositionSum(positionSum) < 0) {
                checkError();
            }
            if (fptr.put_Quantity(quantity) < 0) {
                checkError();
            }
            if (fptr.put_Price(price) < 0) {
                checkError();
            }
            if (fptr.put_TextWrap(IFptr.WRAP_WORD) < 0) {
                checkError();
            }
            if (fptr.put_Name(name) < 0) {
                checkError();
            }
            if (fptr.put_EnableCheckSumm(false) < 0) {
                checkError();
            }
            if (fptr.Registration() < 0) {
                checkError();
            }

        }


        private void payment(double sum, int type,double remainder,double change) throws DriverException {
            if (fptr.put_Summ(sum) < 0) {
                checkError();
            }
            if (fptr.put_TypeClose(type) < 0) {
                checkError();
            }
            if (fptr.Payment() < 0) {
                checkError();
            }
            change = fptr.get_Change();
            remainder = fptr.get_Remainder();
            publishProgress(String.format("Remainder: %.2f, Change: %.2f", remainder, change));
        }

        private void cashIncome(double sum) throws DriverException {

            publishProgress(getString(R.string.fptr_settings_cancel_cheque));
            if  (fptr.get_Mode() == IFptr.MODE_REGISTRATION && fptr.CancelCheck() < 0) {//проверяем если чек открыт то анулируем его
                checkError();
            }

            if (fptr.put_Mode(IFptr.MODE_REGISTRATION) < 0) {
                checkError();
            }
            if (fptr.SetMode() < 0) {
                checkError();
            }

            if (fptr.put_Summ(sum) < 0) {
                checkError();
            }
            if (fptr.CashIncome() < 0) {
                checkError();
            }
        }

/*        @Override
        protected void execute(Context... params) {

            int result = doInBackground(params);
            onPostExecute(result);
        }*/

        protected Integer doInBackground(Context... params) {

            mContext = params[0];
            mErrorMessage = "";
            mSession = -1;
            //snackbar = Snackbar.make(mContext,R.string.error_unknown, Snackbar.LENGTH_LONG);

            db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                mErrorMessage = "не удалось получить бд для чтения";
                Log.e(OrderDetailsActivity.class.getCanonicalName()+".AddPayment.doInBackground",mErrorMessage);
                return 0;
            }
            mCallback.lockChequeButton();

            double sumToPay = getSumToPay(db,mCheckType);

            if (mCheckType == IFptr.CHEQUE_TYPE_RETURN_BUY) {
                //приводим ошибочный чек к типу чека возврата, для того чтобы ошибочный чек можно было возвращать с услугами (не сработал флаг isReturnCheque)
                mCheckType = IFptr.CHEQUE_TYPE_RETURN;
            }

/*            if (isOrderPaid(db) &&  mCheckType != IFptr.CHEQUE_TYPE_RETURN) {
                db.close();
                mErrorMessage = getString(R.string.order_detail_order_paid);
                return 0;
            }*/

            if (sumToPay <= 0) {
                db.close();
                mErrorMessage = getString(R.string.order_details_error_sum_zero);
                Log.e(OrderDetailsActivity.class.getCanonicalName()+".AddPayment.doInBackground",mErrorMessage);
                return 0;
            }

            db.close();

            if (fptr != null)
            {
                mErrorMessage = "Идет отправка документов в ОФД.'\n'Повторите попытку еще раз.";
                Log.e(OrderDetailsActivity.class.getCanonicalName()+".AddPayment.doInBackground",mErrorMessage);
                return 0;
            }

            fptr = new Fptr();
            fptr.create(mContext);

            try {

                publishProgress(getString(R.string.fptr_settings_loading));
                if (fptr.put_DeviceSettings(getSettings()) < 0) {
                    checkError();
                }
                publishProgress(getString(R.string.fptr_settings_set_connection));
                if (fptr.put_DeviceEnabled(true) < 0) {
                    checkError();
                }
                publishProgress(getString(R.string.fptr_settings_check_connection));
                if (fptr.GetStatus() < 0) {
                    checkError();
                }
                publishProgress(getString(R.string.fptr_settings_cancel_cheque));
                try {
                    if  (fptr.get_Mode() == IFptr.MODE_REGISTRATION && fptr.CancelCheck() < 0) {//проверяем если чек открыт то анулируем его
                        checkError();
                    }
                } catch (DriverException e) {
                    int rc = fptr.get_ResultCode();
                    if (rc != -16 && rc != -3801) {
                        mErrorMessage = e.getMessage();
                        return 0;
                    }
                }
                // Открываем чек продажи, попутно обработав превышение смены
                publishProgress(getString(R.string.fptr_settings_open_cheque));
                try {
                    openCheck(mCheckType);
                } catch (DriverException e) {
                    // Проверка на превышение смены
                    if (fptr.get_ResultCode() == -3822) {
                        reportZ();
                        openCheck(mCheckType);
                    } else {
                        mErrorMessage = e.getMessage();
                        return 0;
                    }
                }

                fptr.put_FiscalPropertyNumber(1021);
                fptr.put_FiscalPropertyPrint(true);//печатать это свойство на чеке
                fptr.put_FiscalPropertyType(IFptr.FISCAL_PROPERTY_TYPE_STRING);
                fptr.put_FiscalPropertyValue(loginDesc);
                fptr.WriteFiscalProperty();

                Boolean isTestFN = isTestFN(fptr);
                if (mNotification != null && !isTestFN) {
                    fptr.put_FiscalPropertyNumber(1008);
                    fptr.put_FiscalPropertyPrint(true);//печатать это свойство на чеке
                    fptr.put_FiscalPropertyType(IFptr.FISCAL_PROPERTY_TYPE_STRING);
                    fptr.put_FiscalPropertyValue(mNotification);
                    fptr.WriteFiscalProperty();
                }

                db = sqLiteOpenHelper.getWritableDatabase();
                if (db == null) {
                    return 0;
                }


                //тут записываем текущее состояние счетчиков чеков ккм

                Cursor cursor = null;
/*                if (isReturnCheque) {
                    cursor = db.rawQuery("select oi.*,i.description from order_items oi " +
                            "left join items as i on oi.item_id = i._id " +
                            "where oi.order_id = ? and oi.checked = 1 and oi.item_id <> ? group by oi._id", new String[]{orderId,"1"});
                } else {*/
                    cursor = db.rawQuery(String.format("select oi.*,i.description from order_items oi left join items as i on oi.item_id = i._id where oi.order_id = ? and oi.checked = 1 group by oi._id"), new String[]{orderId});
                //}

                while (cursor.moveToNext()) {
                    double price = 0, discount = 0;
                    int quantity = 0, nds;
                    String name = cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION));
                    quantity = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COUNT));
                    discount = cursor.getDouble(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_DISCOUNT));
                    price = cursor.getDouble(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_COST));
                    nds = cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_NDS));
                    registrationFZ54(name, price, quantity, (price - discount)*quantity, nds);
                    mSum = mSum.add(new BigDecimal(price).multiply(new BigDecimal(quantity)));
                    mDiscount = mDiscount.add(new BigDecimal(discount).multiply(new BigDecimal(quantity)));
                }

                cursor.close();
                db.close();

                publishProgress(getString(R.string.fptr_settings_payment));
                mRemainder = 0.0;//неоплач остаток
                mChange = 0.0;//сдача


                int lastPaymentcode = 0;
                for (int paymentCode:mPaymentTypeCode.keySet()) {
                    payment(mPaymentTypeCode.get(paymentCode), paymentCode,mRemainder,mChange);
                    lastPaymentcode = paymentCode;
                }
                publishProgress(getString(R.string.fptr_settings_close_cheque));


                int checkNumber = 0;
                mSession  = fptr.get_Session() + 1;
                if (mPaymentTypeCode.size() > 1) {
                    checkNumber = closeCheck(0);//0 можно вставить любое количество, если payment больше 1 этот параметр все равно игнорируется

                } else {
                    checkNumber = closeCheck(lastPaymentcode);//последний и единственный тип оплаты
                }
                return checkNumber;

            } catch (DriverException e) {


                mErrorMessage = e.getMessage();

                Log.e(OrderDetailsActivity.class.getCanonicalName()+".AddPayment.doInbackground",mErrorMessage);

                if (fptr.get_ResultCode() == -3800) {//нет наличности в кассе. делаем внесение

                    publishProgress(getString(R.string.fptr_settings_income_cash));
                    try {
                        cashIncome(mSum.doubleValue() - mDiscount.doubleValue());

                        mErrorMessage = "Было сделано автоматическое внесение денег в кассу." +
                                "Необходимо повторно сделать чек возврата!";
                        return -3800;
                    } catch (DriverException e1) {
                        mErrorMessage = e1.getMessage();
                    }
                }
                return 0;
            }

        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (values == null || values.length == 0) {
                return;
            }

            Toast.makeText(mContext,values[0],500).show();
        }

        private String getDateTime() {
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = new Date();
            return dateFormat.format(date);
        }

        @Override
        protected void onPostExecute(final Integer checkNumber) {

            //onCancelled();
            mCallback.unlockChequeButton();



            //if (fptr != null) fptr.destroy();

            if (checkNumber > 0 || (checkNumber == 0 && (mErrorMessage.isEmpty() || mErrorMessage.equals("")))) {


                try {

                    db = sqLiteOpenHelper.getWritableDatabase();
                    for (int paymentCode:mPaymentTypeCode.keySet()) {

                        Cursor payment = db.query(DBHelper.TB_PAYMENT_TYPES,new String[]{DBHelper.CN_ID},"code = ?",new String[]{String.valueOf(paymentCode)},null,null,null);
                        payment.moveToNext();
                        String paymentID = payment.getString(payment.getColumnIndex(DBHelper.CN_ID));
                        ContentValues cv = new ContentValues();
                        cv.put(DBHelper.CN_ORDER_ID, orderId);
                        cv.put(DBHelper.CN_ORDER_PAYMENT_TYPE_ID, paymentID);
                        cv.put(DBHelper.CN_ORDER_PAYMENT_SUM, mPaymentTypeCode.get(paymentCode));
                        cv.put(DBHelper.CN_ORDER_PAYMENT_DISCOUNT, 0);
                        cv.put(DBHelper.CN_ORDER_PAYMENT_CHECK_NUMBER, checkNumber);
                        cv.put(DBHelper.CN_ORDER_PAYMENT_CHECK_SESSION, mSession);
                        cv.put(DBHelper.CN_ORDER_DATE, getDateTime());
                        cv.put(DBHelper.CN_ORDER_PAYMENT_CHEQUE_TYPE, mCheckType);
                        db.insert(DBHelper.TB_ORDER_PAYMENTS, null, cv);
                    }


                    db.close();
                } catch (Exception e) {


                    SharedPreferences preferences = getSharedPreferences("error_"+orderId, Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("error_decription",e.getMessage());
                    editor.apply();

                    Log.e(OrderDetailsActivity.class.getCanonicalName()+".AddPayment.doInbackground запись платежа в БД",e.getMessage());
                }


                if (mChange > 0) {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mContext);
                    builder.setMessage(Double.toString(mChange)).setTitle(R.string.order_detail_order_change);
                    builder.create().show();
                }
                if (mRemainder > 0) {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mContext);
                    builder.setMessage(Double.toString(mRemainder)).setTitle(R.string.order_detail_order_remainder);
                    builder.create().show();
                }

                if (mCallback != null)
                    mCallback.processCallback(mCheckType, mErrorMessage, orderId, mContext);

                //запускаем таск который считывает количество неотправленных документов в ОФД

                //new GetNotSendedDocuments(mCompleteCallback).execute(mContext);
                loadDataToOFD();

            } else {

                Log.e(OrderDetailsActivity.class.getCanonicalName()+".AddPayment.doInbackground",mErrorMessage);


                if (mCompleteCallback != null) try {
                    mCompleteCallback.call();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(OrderDetailsActivity.class.getCanonicalName()+".AddPayment.onPostExecute",e.getMessage());
                }

                if (checkNumber == -3800) {//было произведено внесение

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mContext);
                    builder.setMessage(mErrorMessage).setTitle("Внесение денег в кассу");
                    builder.create().show();
                    return;
                } else {

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mContext);
                    builder.setMessage(mErrorMessage).setTitle(R.string.order_details_error_build_check);
                    builder.create().show();
                }

                if (mCallback != null) {
                    if (mCallback != null)
                        mCallback.processCallback(checkNumber, mErrorMessage, orderId, mContext);
                }


                if (fptr != null)
                fptr.destroy();
                fptr = null;

            }

        }
    }

    private static class DriverException extends Exception {
        public DriverException(String msg) {
            super(msg);
        }
    }

    public class OrderItemListCursorUpdate implements Runnable {

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            String[] args = {orderId,orderId};
            Cursor cursor = db.rawQuery(String.format(
                    "select oi._id,oi.item_id,oi.checked,oi.discount,oi.cost,oi.count,i.description,os.completed,os.delivery_cost,os.level_delivery_pay, oi.eid from order_items as oi " +
                    "left join items as i on oi.item_id = i._id " +
                    "left join " +
                    "   (select o._id,o.delivery_cost,o.level_delivery_pay,s.completed from orders as o "+
                    "    left join statuses as s on o.status_id = s._id "+
                    //"	 ) as os on oi.order_id = os._id " +
                    //" group by oi._id,oi.item_id,oi.checked,oi.discount,oi.cost,oi.count,i.description,os.completed,os.delivery_cost,os.level_delivery_pay, oi.eid", null);
                    "	 where o._id = ?) as os on oi.order_id = os._id " +
                    "where oi.order_id = ? group by oi._id,oi.item_id,oi.checked,oi.discount,oi.cost,oi.count,i.description,os.completed,os.delivery_cost,os.level_delivery_pay, oi.eid"), args);

            if (!OrderItemsFragment.fragment.isDetached()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    OrderItemsFragment.fragment.updateCursor(cursor);
                    OrderItemsFragment.fragment.recalculateFooter(cursor);
                } else {
                    OrderItemsFragment.fragment.recalculateFooter(cursor);
                    OrderItemsFragment.fragment.updateCursor(cursor);
                }
            }
        }
    }

    public class OrderDetailsCursorUpdate implements Runnable {

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }
            String[] args = {orderId, loginId};
            Cursor cursor = db.rawQuery(String.format("select o.*,s.description as status,s.completed,r.description as refuse from orders as o " +
                    "left join statuses as s on o.status_id = s._id " +
                    "left join refuse_reasons as r on o.refuse_id = r._id " +
                    "where o._id = ? and o.driver_id = ?"), args);
            if (OrderDetailsFragment.fragment != null) {
                OrderDetailsFragment.fragment.updateCursor(cursor);
            }
        }
    }

    public class OrderItemPaymentCursorUpdate implements Runnable {

        @Override
        public void run() {
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                return;
            }

            String[] args = {orderId};
            Cursor cursor = db.rawQuery(String.format(
                    "select op._id,op.check_type,op.check_number,op.session,op.date,op.payment_type_id, " +
                            "case when op.check_type  = 1 then op.sum else -op.sum end sum," +
                            "case when op.check_type  = 1 then ifnull(op.discount,0)  else  ifnull(-op.discount,0) end discount," +
                            "pt.description as payment_type,ct.description from order_payments as op " +
                            "left join payment_types as pt on op.payment_type_id = pt._id " +
                            "left join check_types as ct on op.check_type = ct.code " +
                            "where op.order_id = ? "), args);


            if (OrderPaymentsFragment.fragment == null) return;

            if (!OrderPaymentsFragment.fragment.isDetached()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    OrderPaymentsFragment.fragment.updateCursor(cursor);
                    OrderPaymentsFragment.fragment.recalculateFooter(cursor);
                } else {
                    OrderPaymentsFragment.fragment.recalculateFooter(cursor);
                    OrderPaymentsFragment.fragment.updateCursor(cursor);
                }
            }
        }
    }

    public  void showRefuseDialog(Cursor cursor) {

        FragmentManager fm = getSupportFragmentManager();
        SimpleListReasonDialog.newInstance(this,cursor).show(fm,"fragment_simple_list_reason_dialog");
    }

    public  void showPaymentTypesDialog(Cursor cursor,int checkType,double sumToPay) {

        FragmentManager fm = getSupportFragmentManager();
        SimpleListPaymentDialog.newInstance(this,cursor,checkType,sumToPay).show(fm,"fragment_simple_list_payment_dialog");
    }

    public  void showCheckTypeDialog(Cursor cursor) {

        FragmentManager fm = getSupportFragmentManager();

        SimpleListChequeTypeDialog.newInstance(this,cursor).show(fm,"fragment_simple_list_check_type_dialog");
    }

    public  void showCheckConfirmDialog(Cursor cursor,Map<Integer,Double> paymentTypeCode,int checkType) {

        FragmentManager fm = getSupportFragmentManager();
        ChequeConfirmDialog.newInstance(this,cursor,paymentTypeCode,checkType).show(fm,"fragment_simple_check_confirm_dialog");
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {


        OrderDetailsActivity mContext;
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
                    return getString(R.string.order_details_page1);
                case 1:
                    return getString(R.string.order_details_page2);
                case 2:
                    return getString(R.string.order_details_page3);
            }
            return null;
        }
    }


}
