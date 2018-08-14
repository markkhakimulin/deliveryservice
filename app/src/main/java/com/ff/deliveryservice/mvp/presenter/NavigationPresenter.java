package com.ff.deliveryservice.mvp.presenter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.SparseBooleanArray;
import android.widget.FilterQueryProvider;
import android.widget.Toast;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.base.BasePresenter;
import com.ff.deliveryservice.common.Constants;
import com.ff.deliveryservice.modules.navigation.AsyncTaskCallback;
import com.ff.deliveryservice.modules.navigation.FilterCursorCallback;
import com.ff.deliveryservice.modules.navigation.adapter.OrderAdapter;
import com.ff.deliveryservice.mvp.model.DBHelper;
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
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

import static com.ff.deliveryservice.common.Constants.FORMATDATE_1C;
import static com.ff.deliveryservice.common.Constants.FORMATDATE_APP;

/**
 * Created by Mark Khakimulin on 11.07.2018.
 * mark.khakimulin@gmail.com
 */
public class NavigationPresenter extends BasePresenter<OrderNavigationView> implements FilterCursorCallback {

    private String mUserId;

    @Inject
    protected Resources res;

    @Inject
    @Named(Constants.SOAP_METHOD_PUT_ORDER_PACK)
    protected SoapSerializationEnvelope envelopePut;

    @Inject
    @Named(Constants.SOAP_METHOD_ORDER_PACK)
    protected SoapSerializationEnvelope envelopeGet;

    @Inject
    protected HttpTransportSE androidHttpTransport;

    @Inject
    protected Context mContext;

    @Inject
    protected DBHelper dbHelper;

    private RefreshOrderListTask refreshOrderListTask;
    private UploadOrderListTask uploadOrderListTask;


    public enum ArchiveType {
        Order,Cheque

    }
    @Inject
    public NavigationPresenter(OrderNavigationView view,String userId) {
        super();
        mUserId = userId;
        mView = view;
        DeliveryServiceApplication.getNavComponent().inject(this);
    }

    public FilterQueryProvider filterProvider = new FilterQueryProvider() {

        @Override
        public Cursor runQuery(CharSequence constraint) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            if (db == null) {
                return null;
            }
            String[] args = {constraint.toString()+"%",mUserId};

            return db.rawQuery(String.format("select o.*,s.description as status from orders as o " +
                    "inner join statuses as s on o.status_id = s._id " +
                    "where o.code like ? and o.driver_id = ?"),args);
        }
    };



    public void upload(AsyncTaskCallback callback)    {

        uploadOrderListTask = new UploadOrderListTask(callback);
        uploadOrderListTask.execute();
    }

    public void refresh() {
        new ShowChangesAlert(new FilterCursorCallback() {
            @Override
            public void applyCursor(Cursor cursor) {
                if (cursor.getCount() > 0) {

                    getView().onShowCursorDialog(res.getString(R.string.alert_message_confirm_changes)
                            , cursor
                            , new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {

                            upload(new AsyncTaskCallback() {
                                @Override
                                public void onPostExecute(Object... params) {

                                    if (!(Boolean) params[0]) {
                                        getView().onShowToast((String) params[1]);
                                    }
                                    refreshOrderListTask = new RefreshOrderListTask();
                                    refreshOrderListTask.execute();
                                }

                                @Override
                                public void onCanceled() {}
                            });

                            return null;
                        }
                    }, new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {

                            getView().hideProgress();
                            return null;
                        }
                    });
                } else {
                    upload(new AsyncTaskCallback() {
                        @Override
                        public void onPostExecute(Object... params) {

                            if (!(Boolean) params[0]) {
                                getView().onShowToast((String) params[1]);
                            }
                            refreshOrderListTask = new RefreshOrderListTask();
                            refreshOrderListTask.execute();
                        }

                        @Override
                        public void onCanceled() {}
                    });

                }
            }
        }).execute();
    }


    //call filters

    public void filterAll() {
        new FilterCursorInProgressToday(this).execute();
    }

    public void filterInProgress() {
        new FilterCursorInProgressToday(this).execute();
    }

    public void filterCanceled() {
        new FilterCursorCanceledToday(this).execute();
    }

    public void filterCompleted() {
        new FilterCursorCompletedToday(this).execute();
    }

    public void filterArchive(ArchiveType type,String date) {

        new FilterCursorArchive(type,date,this).execute();
    }

    public void filterAllAndRefresh()   {

        final NavigationPresenter navigationPresenter = this;

        new FilterCursorAllToday(new FilterCursorCallback() {

            @Override
            public void applyCursor(Cursor cursor) {

                getView().hideProgress();
                navigationPresenter.applyCursor(cursor);

                if (cursor.getCount() == 0) {

                    getView().showYesNoMessageDialog(res.getString(R.string.alert_title_no_orders),
                        res.getString(R.string.alert_message_no_orders),
                        new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                getView().showProgress();
                                refresh();
                                return null;
                            }
                        });
                }
            }
        }).execute();
    }

    //applying filters

    @Override
    public void applyCursor(Cursor cursor) {
        getView().onFilter(cursor);
    }

    //async

    private class ShowChangesAlert extends AsyncTask<Object,Object,Cursor>{
        private FilterCursorCallback mCallback;

        ShowChangesAlert(FilterCursorCallback callback) {mCallback = callback;}

        @Override
        protected Cursor doInBackground(Object... params) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            return  DBHelper.getUselessOrders(db,mUserId);

        }

        @Override
        public void onPostExecute(Cursor cursor) {
            mCallback.applyCursor(cursor);
        }

    }

    //filters

    @SuppressLint("StaticFieldLeak")
    private class FilterCursor extends AsyncTask<Object,Object,Cursor> {

        protected FilterCursorCallback mCallback;
        protected String mQuery;

        FilterCursor(FilterCursorCallback callback,String query) {
            mCallback = callback;
            mQuery = query;
        }

        @Override
        protected Cursor doInBackground(Object... params) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            DateFormat dateFormat = new SimpleDateFormat(FORMATDATE_APP);
            String date = dateFormat.format(new Date());
            String noData = "1";
            String[] args = {date,noData,mUserId};

            return db.rawQuery(mQuery,args);
        }

        @Override
        public void onPostExecute(Cursor cursor) {
            mCallback.applyCursor(cursor);
        }

    }

    @SuppressLint("StaticFieldLeak")
    private class FilterCursorInProgressToday extends FilterCursor {
        FilterCursorInProgressToday(FilterCursorCallback callback) {
            super(callback,
                    String.format("select o.*,s.description as status from orders as o " +
                            "left join statuses as s on o.status_id = s._id " +
                            "left join drivers as d on o.driver_id = d._id  " +
                            "where o.posted = 0 and o.completed = 0 and o.canceled = 0 and (o.date = ? or ? = '1') and d._id = ?"));
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class FilterCursorAllToday extends FilterCursor {
        FilterCursorAllToday(FilterCursorCallback callback) {
            super(callback,
                    String.format("select o.*,s.description as status from orders as o " +
                            "left join statuses as s on o.status_id = s._id " +
                            "left join drivers as d on o.driver_id = d._id  " +
                            "where o.posted = 0 and (o.date = ? or ? = '1') and d._id = ?"));
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class FilterCursorCanceledToday extends FilterCursor {
        FilterCursorCanceledToday(FilterCursorCallback callback) {
            super(callback,
                    String.format("select o.*,s.description as status from orders as o " +
                            "left join statuses as s on o.status_id = s._id " +
                            "left join drivers as d on o.driver_id = d._id  " +
                            "where o.posted = 0 and o.canceled = 1 and (o.date = ? or ? = '1') and d._id = ?"));
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class FilterCursorCompletedToday extends FilterCursor {

        FilterCursorCompletedToday(FilterCursorCallback callback) {
            super(callback,String.format("select o.*,s.description as status from orders as o " +
                    "left join statuses as s on o.status_id = s._id " +
                    "left join drivers as d on o.driver_id = d._id  " +
                    "where o.posted = 0 and o.completed = 1 and (o.date = ? or ? = '1') and d._id = ?"));
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class FilterCursorArchive extends FilterCursor {
        private String mStartDate = "";
        private ArchiveType mType;

        FilterCursorArchive(ArchiveType type, String startDate, FilterCursorCallback callback) {
            super(callback,type == ArchiveType.Order?
                    String.format("select o.*,s.description as status from orders as o " +
                            "inner join statuses as s on o.status_id = s._id " +
                            "inner join drivers as d on o.driver_id = d._id  " +
                            "where o.date = ? and d._id = ?"):
                    String.format("select distinct o.*,s.description as status from orders as o " +
                    "inner join statuses as s on o.status_id = s._id " +
                    "inner join drivers as d on o.driver_id = d._id  " +
                    "inner join order_payments as op on o._id = op.order_id  " +
                    "where date(op.date) = ? and d._id = ?"));
            mType = type;
            mStartDate = startDate;
        }

        @Override
        protected Cursor doInBackground(Object... params) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String[] args = {mStartDate, mUserId};
            return db.rawQuery(mQuery, args);
        }
    }

    //rest methods

    @SuppressLint("StaticFieldLeak")
    public class RefreshOrderListTask extends AsyncTask<Context, Void, Boolean> {

        private final String method = Constants.SOAP_METHOD_ORDER_PACK;
        private final String namespace = Constants.SOAP_NAMESPACE;
        private final String soap_action = String.format("%s#Obmen:%s",namespace,method);
        private String errorMessage = "";
        private Context mContext;
        private int orderRows;


        @Override
        protected Boolean doInBackground(Context... params) {

            try {
                System.setProperty("http.keepAlive", "false");

                androidHttpTransport.call(soap_action, envelopeGet);
                // Get the SoapResult from the envelope body.
                SoapObject response = (SoapObject) envelopeGet.getResponse();
                try {

                    SoapPrimitive resultObject = (SoapPrimitive) response.getPrimitiveProperty("Result");
                    Boolean result = Boolean.valueOf(resultObject.toString());
                    if (!result) {
                        errorMessage = response.getPropertyAsString("Description");
                        return false;
                    }
                    //writing to database
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
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
                    final Cursor ords = DBHelper.getUselessOrders(db,mUserId);
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

                SoapFault responseFault = (SoapFault) envelopeGet.bodyIn;

                if (responseFault == null) {
                    errorMessage = res.getString(R.string.error_no_internet_connection);
                } else
                    try {
                        errorMessage = responseFault.faultstring;
                    } catch (Exception ef) {
                        errorMessage = res.getString(R.string.error_unknown);
                    }
            }//process request



            return errorMessage.isEmpty();
        }

        private Boolean isOrderComplete(SQLiteDatabase db,String orderId) {

            if (db == null) {
                return null;
            }
            Cursor cursor = db.rawQuery(String.format("select * from orders o where o._id = ? and o.driver_id = ? and o.completed = 1") ,new String[] {orderId,mUserId});
            Boolean complete = cursor.getCount() > 0;
            cursor.close();
            return complete;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            onCancelled();

            if (success) {

                getView().applyFilter();
                getView().onShowToast(res.getString(R.string.alert_refresh) +" "+orderRows);

            } else {

                getView().showYesNoMessageDialog(res.getString(R.string.soap_error_order_pack),errorMessage,null);

            }
        }

        @Override
        protected void onCancelled() {
            refreshOrderListTask = null;
            getView().hideProgress();
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class UploadOrderListTask extends AsyncTask<Void, Integer, Boolean> {

        private final String method = Constants.SOAP_METHOD_PUT_ORDER_PACK;
        private final String namespace = Constants.SOAP_NAMESPACE;
        private final String soap_action = String.format("%s#Obmen:%s",namespace,method);
        private String errorMessage = "";
        private String[] ordersResult;
        private AsyncTaskCallback mCallback;

        UploadOrderListTask(AsyncTaskCallback callback) {
            mCallback = callback;

        }
        @SuppressLint("DefaultLocale")
        @Override
        protected Boolean doInBackground(Void... params) {

            try {

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (db == null) {
                    return false;
                }

                SoapObject soapObjectOrderPaymentList = new SoapObject(namespace,"OrderPaymentList");

                Cursor cursorOrderPayment = db.rawQuery(String.format("select op.order_id,op.payment_type_id,op.sum,ifnull(op.date,'ошибка разбора') as date,ct.description check_type from order_payments as op" +
                        " inner join check_types as ct on op.check_type = ct.code " +
                        " inner join orders_chg as och on op.order_id = och._id " +
                        " where och.driver_id = ?"),new String[] {mUserId});

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
                        "inner join order_items as oi on o._id = oi.order_id  where o.driver_id = ?"), new String[] {mUserId});


                if (cursor.getCount() == 0) {
                    errorMessage = res.getString(R.string.error_no_data_for_uploading);
                    return false;
                }
                SoapObject requestObject = new SoapObject(namespace, method);
                SoapObject soapObjectList = new SoapObject(namespace,"OrderChangeList");

                envelopePut = new SoapSerializationEnvelope(SoapEnvelope.VER12);

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
                envelopePut.setOutputSoapObject(requestObject);


                System.setProperty("http.keepAlive", "false");

                androidHttpTransport.call(soap_action, envelopePut);
                // Get the SoapResult from the envelope body.
                SoapObject response = (SoapObject) envelopePut.getResponse();
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

                SoapFault responseFault = (SoapFault) envelopePut.bodyIn;

                if (responseFault == null) {
                    errorMessage = res.getString(R.string.error_no_internet_connection);
                } else
                    try {
                        errorMessage = String.format("[%s] : %s",responseFault.faultcode,responseFault.faultstring);
                    } catch (Exception ef) {
                        errorMessage = res.getString(R.string.error_unknown);
                    }
                return false;
            }//process request


            if (ordersResult.length > 0) {

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if (db == null) {
                    return false;
                }
                for (String order:ordersResult) {
                    db.delete(DBHelper.TB_ORDERS_CHG,"_id = ?",new String[]{order});
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.CN_ID, order);
                    cv.put(DBHelper.CN_ORDER_DRIVER_ID, mUserId);
                    cv.put(DBHelper.CN_ORDER_POSTED, 1);
                    db.update(DBHelper.TB_ORDERS,cv,"_id = ?",new String[]{order});
                }
                db.close();

                return true;
            }
            errorMessage = res.getString(R.string.order_details_error_upload_orders);
            return false;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            onCancelled();

            if (success) {

                getView().applyFilter();
                getView().onShowToast(res.getString(R.string.orders_uploaded));

            } else {

                if (mCallback == null) {
                    getView().showYesNoMessageDialog(res.getString(R.string.soap_error_order_pack),errorMessage,null);
                } else {
                    mCallback.onPostExecute(success,res.getString(R.string.soap_error_order_changes)+": "+errorMessage);
                }
            }

        }

        @Override
        protected void onCancelled() {

            uploadOrderListTask = null;
            if (mCallback != null) mCallback.onCanceled();
        }
    }

    //db queries
    public void clearSelectedItems(final ArrayList<String> checked) {

        new Thread(new Runnable() {

            @Override
            public void run() {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                for (int i = 0; i < checked.size();i++) {
                    db.delete(DBHelper.TB_ORDERS_CHG,"_id = ?",new String[]{checked.get(i)});
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.CN_ORDER_COMPLETED, 0);
                    cv.put(DBHelper.CN_ORDER_POSTED, 0);
                    cv.put(DBHelper.CN_ORDER_CANCELED, 0);
                    db.update(DBHelper.TB_ORDERS,cv,"_id = ?",new String[]{checked.get(i)});
                }

                db.close();
            }
        });

    }

}
