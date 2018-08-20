package com.ff.deliveryservice.mvp.presenter;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.base.BasePresenter;
import com.ff.deliveryservice.modules.details.adapter.ItemsAdapter;
import com.ff.deliveryservice.modules.details.fragments.OrderDetailsFragment;
import com.ff.deliveryservice.modules.details.fragments.OrderItemsFragment;
import com.ff.deliveryservice.modules.details.fragments.OrderPaymentsFragment;
import com.ff.deliveryservice.modules.scanner.SearchBarcodeCallback;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.mvp.model.OrderData;
import com.ff.deliveryservice.mvp.model.OrderItem;
import com.ff.deliveryservice.mvp.model.OrderPayment;
import com.ff.deliveryservice.mvp.view.OrderDetailsView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import ru.atol.drivers10.fptr.IFptr;

/**
 * Created by Mark Khakimulin on 09.07.2018.
 * mark.khakimulin@gmail.com
 */
public class DetailsPresenter extends BasePresenter<OrderDetailsView>
{
    private String mOrderId,mUserId,mCodeId;

    @Inject
    public DBHelper dbHelper;

    @Inject
    public Resources res;


    @Inject
    public DetailsPresenter(OrderDetailsView view, String orderId,String codeId, String userId) {
        mView = view;
        mOrderId = orderId;
        mCodeId = codeId;
        mUserId = userId;
        DeliveryServiceApplication.getDetailsComponent().inject(this);
    }


    public void changePaymentValue(int paymentType,double summ) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.CN_ORDER_PAYMENT_SUM, summ);
        db.update(DBHelper.TB_ORDER_PAYMENTS,cv,"_id = ?",new String[]{String.valueOf(paymentType)});
        db.close();

        getView().onChangePaymentValueComplete();
    }

    @Override
    protected OrderDetailsView getView() {
        return mView;
    }



    private Boolean isOrderCompleted(SQLiteDatabase db) {

        if (db == null) {
            return null;
        }
        Cursor cursor = db.rawQuery("select o._id,s.completed from orders as o "+
                " inner join statuses as s on o.status_id = s._id "+
                " where o._id = ? and (o.posted = 1 or o.completed = 1)",new String[] {mOrderId});
        return cursor.getCount() > 0;
    }


/*
    public Boolean isOrderPaid(SQLiteDatabase db) {

        if (db == null) {
            return null;
        }
        Cursor cursor = db.rawQuery(String.format("select op.* from order_payments op where op.order_id = ? and op.paid = 1"),new String[] {mOrderId});

        if (cursor.getCount() > 0) {

            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }
*/


    public void getRefuseList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (isOrderCompleted(db)) {
            db.close();
            getView().onShowToast(res.getString(R.string.order_detail_order_completed));
            return;
        }
        Cursor cursor = db.query(DBHelper.TB_REFUSE_REASONS, null, null, null, null, null, null);
        getView().onGetRefusesCursor(cursor);
    }

    /*public String getDriverDescription(SQLiteDatabase db) {

        String name = "Курьер";
        if (db == null) {
            return name;
        }
        Cursor cursor = db.rawQuery(String.format("select decription from drivers where _id = ? "), new String[]{mUserId });
        while (cursor.moveToNext()) {
            name = cursor.getString(cursor.getColumnIndex(DBHelper.CN_DESCRIPTION));
        }
        cursor.close();
        return name;
    }*/

    public void getChequeTypeList() {

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db == null) {
            return;
        }
        Cursor cursor = db.rawQuery("select * from check_types",null);

        getView().onGetChequeTypesCursor(cursor);

    }

    /*
     *
     *     ASYNC  CALLS
     *
     */

    //with out guid result needed

    public void setOrderCompleted() {
        new Thread(new OrderCompleted()).start();
    }

    public class OrderCompleted implements Runnable {

        @Override
        public void run() {

            ContentValues cv = new ContentValues();

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            if (db == null) {
                return;
            }

            double sumToPay = dbHelper.getSumToPay(db, IFptr.LIBFPTR_RT_SELL,mOrderId);
            cv.put(DBHelper.CN_ORDER_COMPLETED, sumToPay > 0.0 ? 0 : 1);
            db.update(DBHelper.TB_ORDERS, cv, "_id = ? and driver_id = ?", new String[] { mOrderId,mUserId });
            db.close();

            new OrderChanged().run();//в этом же потоке
        }
    }

    public void setOrderChanged() {
        new Thread(new OrderChanged()).run();
    }

    public class OrderChanged implements Runnable {

        @Override
        public void run() {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if (db == null) {
                return;
            }

            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String date = dateFormat.format(new Date());
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.CN_ID, mOrderId);
            cv.put(DBHelper.CN_ORDER_DRIVER_ID, mUserId);
            cv.put(DBHelper.CN_CODE, mCodeId);
            cv.put(DBHelper.CN_ORDER_TIME, date);
            db.replace(DBHelper.TB_ORDERS_CHG,null, cv);
            db.close();
        }
    }

    public void setOrderCanceled(String refuseId,int cancel) {
        new Thread(new OrderCanceled(refuseId,cancel)).start();
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

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if (db == null) {
                return;
            }
            db.update(DBHelper.TB_ORDERS, cv, "_id = ? and driver_id = ?", new String[] {
                    mOrderId,mUserId
            });
            db.close();

            new OrderChanged().run();//в этом же потоке
            new UpdateOrderDetailsTask().execute();//в этом же потоке
        }
    }

    //with guid result needed

    public void confirmPayment() {
        ConfirmPaymentTask confirmPaymentTask = new ConfirmPaymentTask();
        confirmPaymentTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public class ConfirmPaymentTask  extends AsyncTask<Void, Void, Cursor>  {

        @Override
        protected Cursor doInBackground(Void... voids) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String[] args = {mOrderId};
            return db.rawQuery(String.format("select oi._id,oi.order_id,oi.item_id,oi.checked,oi.discount,oi.cost,oi.count,i.description from order_items as oi left join items as i on oi.item_id = i._id where oi.order_id = ? and oi.checked = 1 group by oi._id,oi.order_id,oi.item_id,oi.checked,oi.discount,oi.cost,oi.count,i.description"), args);
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            super.onPostExecute(cursor);
            getView().onConfirmPayment(cursor);
        }
    }




    public void getOrderPayments(int checkType) {
        GetOrderPaymentTypesTask orderPaymentTypesTask = new GetOrderPaymentTypesTask(checkType);
        orderPaymentTypesTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public class GetOrderPaymentTypesTask  extends AsyncTask<Void, Void, Cursor> {

        int mCheckType;
        double sumToPay;

        public GetOrderPaymentTypesTask(int checkType) {
            mCheckType = checkType;
        }

        @Override
        protected Cursor doInBackground(Void... voids) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor;
            if (mCheckType == IFptr.LIBFPTR_RT_SELL) {
                cursor = db.rawQuery("select * from payment_types  where selectable = 1", null);

            } else {//исключаем те виды оплаты которых нет в текущей оплате, чтобы они случайно не выбрали другой тип оплаты при возврате
                /*cursor = db.rawQuery(String.format("select * from payment_types as pt " +
                        "inner join (select payment_type_id,sum(" +
                        "case when check_type < 2 then (sum - ifnull(discount,0))  " +
                        "else -1 * (sum - ifnull(discount,0))) end paid_sum from order_payments where order_id = ? group by payment_type_id) as op on pt._id = op.payment_type_id " +
                        "where op.paid_sum > 0"), new String[]{orderId});//and op.paid > 0*/

                cursor = db.rawQuery("select * from payment_types as pt " +
                        "inner join (select payment_type_id,sum(case when check_type < 2 then (sum - ifnull(discount, 0)) else -1 * (sum - ifnull(discount, 0)) end) as paid_sum " +
                        "from order_payments where order_id = ? " +
                        "group by payment_type_id) as op on pt._id = op.payment_type_id " +
                        "where op.paid_sum > 0", new String[]{mOrderId});//and op.paid > 0

            }


            //сразу расчитываем сумму для смешанной оплаты если это чек продажи
            sumToPay = dbHelper.getSumToPay(db,mCheckType,mOrderId);
            return  cursor;
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            super.onPostExecute(cursor);

            getView().onGetPaymentsCursor(cursor,sumToPay,mCheckType);
        }
    }


    public void updateOrderPayments() {
        UpdateOrderItemListTask updateOrderItemsTask = new UpdateOrderItemListTask();
        updateOrderItemsTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public class OrderItemPaymentCursorUpdate extends AsyncTask<Void, Void, ArrayList<OrderPayment> >{



        @Override
        protected ArrayList<OrderPayment>  doInBackground(Void... voids) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            String[] args = {mOrderId};
            Cursor cursor = db.rawQuery("select op._id,op.check_type,op.check_number,op.session,op.date,op.payment_type_id, " +
                    "case when op.check_type  = 1 then op.sum else -op.sum end sum," +
                    "case when op.check_type  = 1 then ifnull(op.discount,0)  else  ifnull(-op.discount,0) end discount," +
                    "pt.description as payment_type,ct.description from order_payments as op " +
                    "left join payment_types as pt on op.payment_type_id = pt._id " +
                    "left join check_types as ct on op.check_type = ct.code " +
                    "where op.order_id = ? ", args);


            ArrayList<OrderPayment> list = new ArrayList<>();

            while (cursor.moveToNext()) {
                list.add(new OrderPayment(cursor));
            }
            cursor.close();
            db.close();

            return list;

        }

        @Override
        protected void onPostExecute(ArrayList<OrderPayment>  list) {


            if (!OrderPaymentsFragment.fragment.isDetached()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    OrderPaymentsFragment.fragment.updateCursor(list);
                    OrderPaymentsFragment.fragment.recalculateFooter();
                } else {
                    OrderPaymentsFragment.fragment.recalculateFooter();
                    OrderPaymentsFragment.fragment.updateCursor(list);
                }
            }
        }

    }



    public void updateOrderItems() {
        UpdateOrderItemListTask updateOrderItemsTask = new UpdateOrderItemListTask();
        updateOrderItemsTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public class UpdateOrderItemListTask  extends AsyncTask<Void, Void, ArrayList<OrderItem>> {


        @Override
        protected ArrayList<OrderItem> doInBackground(Void... voids) {

            ArrayList<OrderItem> list = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            if (db == null || !db.isOpen()) {
                return  list;
            }

            if (db.isDbLockedByCurrentThread()) {
                db.setTransactionSuccessful();
                db.yieldIfContendedSafely();
            }

            String[] args = {mOrderId,mOrderId};

            Cursor cursor = db.rawQuery("select oi._id,oi.item_id,oi.checked,oi.discount,oi.cost,oi.count,i.description,os.completed,os.delivery_cost,os.level_delivery_pay, oi.eid from order_items as oi " +
                    "left join items as i on oi.item_id = i._id " +
                    "left join " +
                    "   (select o._id,o.delivery_cost,o.level_delivery_pay,s.completed from orders as o "+
                    "    left join statuses as s on o.status_id = s._id "+
                    //"	 ) as os on oi.order_id = os._id " +
                    //" group by oi._id,oi.item_id,oi.checked,oi.discount,oi.cost,oi.count,i.description,os.completed,os.delivery_cost,os.level_delivery_pay, oi.eid", null);
                    "	 where o._id = ?) as os on oi.order_id = os._id " +
                    "where oi.order_id = ? group by oi._id,oi.item_id,oi.checked,oi.discount,oi.cost,oi.count,i.description,os.completed,os.delivery_cost,os.level_delivery_pay, oi.eid", args);


            while (cursor.moveToNext()) {
                list.add(new OrderItem(cursor));
            }

            cursor.close();
            db.close();
            return  list;

        }

        @Override
        protected void onPostExecute(ArrayList<OrderItem> list) {

            if (OrderItemsFragment.fragment != null && !OrderItemsFragment.fragment.isDetached()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    OrderItemsFragment.fragment.updateCursor(list);
                    //OrderItemsFragment.fragment.recalculateFooter(cursor);
                } else {
                    //OrderItemsFragment.fragment.recalculateFooter(cursor);
                    OrderItemsFragment.fragment.updateCursor(list);
                }
            }

        }
    }


    public void updateOrderDetails() {
        UpdateOrderDetailsTask updateOrderDetailTask = new UpdateOrderDetailsTask();
        updateOrderDetailTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public class UpdateOrderDetailsTask extends AsyncTask<Void, Void, OrderData>  {

        SQLiteDatabase db;

        @Override
        protected OrderData doInBackground(Void... voids) {
            db = dbHelper.getReadableDatabase();

            String[] args = {mOrderId, mUserId};
            Cursor cursor = db.rawQuery("select o.*,s.description as status,s.completed,r.description as refuse from orders as o " +
                    "left join statuses as s on o.status_id = s._id " +
                    "left join refuse_reasons as r on o.refuse_id = r._id " +
                    "where o._id = ? and o.driver_id = ?", args);

            cursor.moveToNext();
            OrderData orderData = new OrderData(cursor);

            cursor.close();
            db.close();

            return  orderData;
        }

        @Override
        protected void onPostExecute(OrderData orderData) {



            if (OrderDetailsFragment.fragment != null && !OrderItemsFragment.fragment.isDetached()) {
                OrderDetailsFragment.fragment.updateCursor(orderData);
            }
        }
    }






    public void updateOrderDeliveryItem(String itemId) {
        UpdateOrderItemDeliveryTask updateOrderItemTask = new UpdateOrderItemDeliveryTask(itemId);
        updateOrderItemTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public class UpdateOrderItemDeliveryTask extends AsyncTask<Void, Void, Boolean> {
        private String mItemId = "";
        private String mReason;

        UpdateOrderItemDeliveryTask(String itemId) {
            mItemId = itemId;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {


            SQLiteDatabase db = dbHelper.getReadableDatabase();
            if (isOrderCompleted(db)) {
                db.close();
                mReason = res.getString(R.string.order_detail_order_completed);
                return false;
            }

            Cursor cursor = db.rawQuery(String.format("select _id,checked from order_items where order_id = ? and item_id = ?"),new String[] {mOrderId,mItemId});
            ContentValues cv = new ContentValues();
            if (cursor.moveToNext()) {
                cv.put(DBHelper.CN_ORDER_ITEM_CHECKED, cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_CHECKED)) == 1 ? 0 : 1);
                cursor.close();
            } else
            {
                mReason = res.getString(R.string.order_details_error_item_order_not_found);
                return false;
            }
            db.close();

            db = dbHelper.getWritableDatabase();
            if (db == null) {
                return false;
            }
            int update = db.update(DBHelper.TB_ORDER_ITEMS, cv, "item_id = ? and order_id = ?", new String[] {DBHelper.ID_DELIVERY, mOrderId });
            db.close();

            return update > 0;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                getView().onOrderItemUpdated();
            } else {
                getView().onShowToast(mReason);
            }
        }
    }




    public void updateOrderItem(String itemId,String eid) {
        UpdateOrderItemTask updateOrderItemTask = new UpdateOrderItemTask(itemId,eid);
        updateOrderItemTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public class UpdateOrderItemTask extends AsyncTask<Void, Void, Boolean> {
        private String mItemId = "",mEID = "";
        private String mReason;

        UpdateOrderItemTask(String itemId,String eid) {
            mItemId = itemId;
            mEID = eid;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {


            SQLiteDatabase db = dbHelper.getReadableDatabase();
            if (isOrderCompleted(db)) {
                db.close();
                mReason = res.getString(R.string.order_detail_order_completed);
                return false;
            }

            Cursor cursor = db.rawQuery(String.format("select _id,checked from order_items where order_id = ? and item_id = ? and eid = ?"),new String[] {mOrderId,mItemId,mEID});
            ContentValues cv = new ContentValues();
            if (cursor.moveToNext()) {
                cv.put(DBHelper.CN_ORDER_ITEM_CHECKED, cursor.getInt(cursor.getColumnIndex(DBHelper.CN_ORDER_ITEM_CHECKED)) == 1 ? 0 : 1);
                cursor.close();
            } else
            {
                mReason = res.getString(R.string.order_details_error_item_order_not_found);
                return false;
            }
            db.close();

            db = dbHelper.getWritableDatabase();
            if (db == null) {
                return false;
            }
            int update = db.update(DBHelper.TB_ORDER_ITEMS, cv, "order_id = ? and item_id = ? and eid = ?", new String[] { mOrderId,mItemId,mEID });
            cv = new ContentValues();
            cv.put(DBHelper.CN_ORDER_ITEM_CHECKED, DBHelper.isDeliveryCheck(db, mOrderId)?1:0);
            db.update(DBHelper.TB_ORDER_ITEMS, cv, "item_id = ? and order_id = ?", new String[] {DBHelper.ID_DELIVERY, mOrderId });
            db.close();
            return update > 0;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                getView().onOrderItemUpdated();
            } else {
                getView().onShowToast(mReason);
            }
        }
    }





    public void updateOrderItemByBarcode(String barcode) {
        SearchBarcodeTask searchBarcodeTask = new SearchBarcodeTask(barcode);
        searchBarcodeTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public class SearchBarcodeTask extends AsyncTask<Void, Void, Boolean>  {
        private String mBarcode = "";
        private String mReason;

        SearchBarcodeTask(String barcode){
            mBarcode = barcode;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            if (isOrderCompleted(db)) {
                db.close();
                mReason = res.getString(R.string.order_detail_order_completed);
                return false;
            }
            String itemId = "";
            Cursor cursor = db.rawQuery("select oi._id,oi.checked,oi.item_id, oi.eid from order_items oi join items i on i._id = oi.item_id where oi.order_id = ? and (oi.eid = ? or i.barcode = ?)",new String[] {mOrderId,mBarcode,mBarcode});

            ContentValues cv = new ContentValues();
            if (cursor.moveToNext()) {
                cv.put(DBHelper.CN_ID, cursor.getString(0));
                cv.put(DBHelper.CN_ORDER_ITEM_CHECKED, cursor.getInt(1) == 1 ? 0 : 1);
                itemId = cursor.getString(3);

            } else
            {
                mReason = res.getString(R.string.order_details_error_item_order_not_found);
                cursor.close();
                db.close();
                return false;
            }

            cursor.close();
            db.close();

            db = dbHelper.getWritableDatabase();
            if (db == null) {
                return false;
            }

            int update = db.update(DBHelper.TB_ORDER_ITEMS, cv, "order_id = ? and eid = ?", new String[] { mOrderId,itemId });
            cv = new ContentValues();
            cv.put(DBHelper.CN_ORDER_ITEM_CHECKED, DBHelper.isDeliveryCheck(db, mOrderId)?1:0);
            db.update(DBHelper.TB_ORDER_ITEMS, cv, "order_id = ? and item_id = \"1\"", new String[] { mOrderId });
            db.close();


            return update > 0;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                getView().onOrderItemUpdated();//обновляем лист
            } else {
                getView().onShowToast(String.format(mReason+" : %s",mBarcode));
            }
        }
    }


}