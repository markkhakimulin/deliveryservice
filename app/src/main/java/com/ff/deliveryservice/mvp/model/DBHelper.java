package com.ff.deliveryservice.mvp.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ff.deliveryservice.R;

import javax.inject.Inject;

import ru.atol.drivers10.fptr.IFptr;

/**
 * Created by khakimulin on 16.02.2017.
 */


public class DBHelper extends SQLiteOpenHelper {

    public static enum ItemsType {
        service,
        goods
    }


    public static final int VERSION = 7;
    public static final int CASH_PAYMENT_CODE = 0;
    public static final int CARD_PAYMENT_CODE = 1;
    public static final int MIX_PAYMENT_CODE = 9;

    public static final String ID_DELIVERY      = "525bc640-82dc-11e1-8341-000c2911d4ec";

    public static final String DATABASE_NAME    = "DeliveryServiceFF.db";
    public static final String TB_DRIVERS       = "drivers";
    public static final String TB_ITEMS         = "items";
    public static final String TB_STATUSES      = "statuses";
    public static final String TB_ORDERS        = "orders";
    public static final String TB_ORDERS_CHG    = "orders_chg";
    public static final String TB_ORDER_ITEMS   = "order_items";
    public static final String TB_ORDER_PAYMENTS= "order_payments";
    public static final String TB_REFUSE_REASONS= "refuse_reasons";
    public static final String TB_PAYMENT_TYPES = "payment_types";
    public static final String TB_CHECK_TYPES   = "check_types";

    public static final String CN_ID = "_id";
    public static final String CN_PASSWORD = "password";
    public static final String CN_DESCRIPTION = "description";
    public static final String CN_SELECTABLE = "selectable";
    public static final String CN_CODE = "code";
    public static final String CN_CANCELED = "canceled";
    public static final String CN_HASH = "hash";

    public static final String CN_ITEM_TYPE = "item_type";
    public static final String CN_ITEM_BARCODE = "barcode";

    public static final String CN_ORDER_ID = "order_id";
    public static final String CN_ORDER_STATUS = "status";
    public static final String CN_ORDER_REFUSE = "refuse";
    public static final String CN_ORDER_PHONE = "phone";
    public static final String CN_ORDER_EMAIL = "email";
    public static final String CN_ORDER_ADDRESS = "address";
    public static final String CN_ORDER_COMMENT = "comment";
    public static final String CN_ORDER_CUSTOMER = "customer";
    public static final String CN_ORDER_DATE = "date";
    public static final String CN_ORDER_TIME = "time";
    public static final String CN_ORDER_REFUSE_ID = "refuse_id";
    public static final String CN_ORDER_STATUS_ID = "status_id";
    public static final String CN_ORDER_DRIVER_ID = "driver_id";
    public static final String CN_ORDER_MAP_ID = "map_id";
    public static final String CN_ORDER_POSTED = "posted";
    public static final String CN_ORDER_COMPLETED= "completed";
    public static final String CN_ORDER_CANCELED= "canceled";
    public static final String CN_ORDER_DELIVERY_COST= "delivery_cost";
    public static final String CN_ORDER_LEVEL_DELIVERY_PAY= "level_delivery_pay";


    public static final String CN_ORDER_ITEM_CHECKED = "checked";
    public static final String CN_ORDER_ITEM_COUNT = "count";
    public static final String CN_ORDER_ITEM_COST = "cost";
    public static final String CN_ORDER_ITEM_DISCOUNT = "discount";
    public static final String CN_ORDER_ITEM_ID = "item_id";
    public static final String CN_ORDER_ITEM_NDS = "nds";
    public static final String CN_ORDER_ITEM_EID = "eid";

    public static final String CN_ORDER_PAYMENT = "payment";
    public static final String CN_ORDER_PAYMENT_SUM = "sum";
    public static final String CN_ORDER_PAYMENT_DISCOUNT = "discount";
    public static final String CN_ORDER_PAYMENT_CHECK_NUMBER = "check_number";
    public static final String CN_ORDER_PAYMENT_CHECK_SESSION = "session";
    public static final String CN_ORDER_PAYMENT_TYPE = "payment_type";
    public static final String CN_ORDER_PAYMENT_TYPE_ID = "payment_type_id";
    public static final String CN_ORDER_PAYMENT_PAID = "paid";
    public static final String CN_ORDER_PAYMENT_CHEQUE_TYPE = "check_type";
    public static final String CN_ORDER_PAYMENT_PAID_SUM = "paid_sum";



    public static final String CN_STATUSES_COMPLETED = "completed";
    public static final String CN_PAYMENT_TYPES_CODE= "code";

    public static final String CREATE_TABLE_DRIVERS         = "create table drivers (_id text primary key, description text,hash text,barcode text, time integer)";
    public static final String CREATE_TABLE_ITEMS           = "create table items (_id text, description text,checked int,item_type text, barcode text primary key)";
    public static final String CREATE_TABLE_STATUSES        = "create table statuses (_id text primary key, description text,completed int)";
    public static final String CREATE_TABLE_PAYMENT_TYPES   = "create table payment_types (_id text primary key, description text,selectable int,code int)";
    public static final String CREATE_TABLE_REFUSE_REASONS  = "create table refuse_reasons (_id text primary key, description text,canceled int)";
    public static final String CREATE_TABLE_ORDERS          = "create table orders (_id text primary key,driver_id text not null,status_id text ,map_id text,refuse_id text,payment_type_id text,code text , customer text,phone text,address text,date text,time text,comment text,issue_card int,posted int,completed int,canceled int,payment real,delivery_cost real, level_delivery_pay real)";
    public static final String CREATE_TABLE_ORDERS_CHG      = "create table orders_chg (_id text primary key,driver_id text not null,code text ,time text)";
    public static final String CREATE_TABLE_ORDER_ITEMS     = "create table order_items (_id integer primary key autoincrement,order_id text not null,item_id text not null ,description text,checked int,count int, cost real, discount real,nds int,eid text)";
    public static final String CREATE_TABLE_ORDER_PAYMENTS  = "create table order_payments (_id integer primary key autoincrement,order_id text not null,payment_type_id text,sum real,discount real,check_number int,check_type int,paid int,completed int,date text,session int)";
    public static final String CREATE_TABLE_CHECK_TYPES     = "create table check_types (_id text primary key, description text,code int)";
    public static final String CREATE_TABLE_KKM_SETTINGS    = "create table settings (kkm_id text primary key, cheque_number int)";


    public static final String CREATE_INDEX_ORDER_ITEMS_IDX     = "create index order_items_idx on order_items(order_id,eid)";
    public static final String CREATE_INDEX_ORDER_PAYMENTS_IDX  = "create index order_payments_idx on order_payments(order_id)";
    public static final String CREATE_INDEX_ITEMS_IDX           = "create index items_idx on items(barcode)";

    private Context context;

    @Inject
    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CHECK_TYPES);
        db.execSQL(CREATE_TABLE_DRIVERS);
        db.execSQL(CREATE_TABLE_ITEMS);
        db.execSQL(CREATE_INDEX_ITEMS_IDX);
        db.execSQL(CREATE_TABLE_PAYMENT_TYPES);
        db.execSQL(CREATE_TABLE_STATUSES);
        db.execSQL(CREATE_TABLE_REFUSE_REASONS);
        db.execSQL(CREATE_TABLE_ORDERS);
        db.execSQL(CREATE_TABLE_ORDER_ITEMS);
        db.execSQL(CREATE_INDEX_ORDER_ITEMS_IDX);
        db.execSQL(CREATE_TABLE_ORDER_PAYMENTS);
        db.execSQL(CREATE_INDEX_ORDER_PAYMENTS_IDX);
        db.execSQL(CREATE_TABLE_ORDERS_CHG);

        //insert
        ContentValues checkTypes =new ContentValues();
        checkTypes.put(CN_CODE, IFptr.LIBFPTR_RT_SELL );
        checkTypes.put(CN_DESCRIPTION,context.getString(R.string.order_details_check_type_sell));
        db.insert(TB_CHECK_TYPES,"",checkTypes);
        checkTypes.put(CN_CODE, IFptr.LIBFPTR_RT_SELL_RETURN );
        checkTypes.put(CN_DESCRIPTION,context.getString(R.string.order_details_check_type_return));
        db.insert(TB_CHECK_TYPES,"",checkTypes);

        checkTypes.put(CN_ID, "0");
        checkTypes.put(CN_CODE, MIX_PAYMENT_CODE);
        checkTypes.put(CN_DESCRIPTION,"cмешанная");
        checkTypes.put(CN_SELECTABLE,1);
        db.insert(TB_PAYMENT_TYPES,"",checkTypes);


        //доставка добавляется как номенклатура а цена ей назначется из заказа когда сам заказ загружается//
        //если поле delivery cost > 0 тогода я добавляю услугу как номенклатуру в заказ
        //insert
        ContentValues items =new ContentValues();
        items.put(CN_ID, ID_DELIVERY);
        items.put(CN_DESCRIPTION,"Услуга доставки");
        items.put(CN_ITEM_BARCODE, "1234567890128");
        items.put(CN_ORDER_ITEM_CHECKED, 0);
        items.put(CN_ITEM_TYPE, ItemsType.service.toString());
        db.insert(TB_ITEMS,"",items);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion <= 4) {
            ContentValues paymentTypes =new ContentValues();
            paymentTypes.put(CN_ID, "0");
            paymentTypes.put(CN_CODE, MIX_PAYMENT_CODE);
            paymentTypes.put(CN_DESCRIPTION,"cмешанная");
            paymentTypes.put(CN_SELECTABLE,1);
            db.insert(TB_PAYMENT_TYPES,"",paymentTypes);
        }

        if (oldVersion <= 5) {
            db.execSQL("alter table order_payments add date text");
            db.execSQL("alter table order_payments add session int");
        }

        if (oldVersion <= 6) {

            db.execSQL("alter table items add item_type text");

            ContentValues items =new ContentValues();
            items.put(CN_ID, ID_DELIVERY);
            items.put(CN_DESCRIPTION,"Услуга доставки");
            items.put(CN_ITEM_BARCODE, "1234567890128");
            items.put(CN_ORDER_ITEM_CHECKED, 0);
            items.put(CN_ITEM_TYPE, ItemsType.service.toString());
            db.update(TB_ITEMS,items,"_id = ?",new String[]{"1"});

            ContentValues order_items =new ContentValues();
            order_items.put(CN_ORDER_ITEM_ID, ID_DELIVERY);

            db.update(TB_ORDER_ITEMS,order_items,"item_id = ?",new String[]{"1"});

        }
    }

    public static boolean isDeliveryCheck(SQLiteDatabase db, String orderId){
        final Cursor cursor = db.rawQuery("select coalesce(sum(checked * (count * cost - discount)),0) as orderSum, level_delivery_pay  from orders, order_items where orders._id=order_id AND item_id <> ? and order_id = ?  group by level_delivery_pay", new String[]{ID_DELIVERY,orderId});
        if (cursor.moveToNext()) {
            float orderSum = cursor.getFloat(0);
            float levelDeliveryPay = cursor.getFloat(1);
            return levelDeliveryPay > orderSum;
        }
        return true;
    }

    public static boolean hasPayment(SQLiteDatabase db,String orderId) {

        Boolean hasPayment = false;
        Cursor cursor = db.rawQuery("select * from order_payments where order_id = ?" , new String[]{orderId});

        hasPayment = cursor.getCount() > 0;
        cursor.close();
        return hasPayment;
    }

    //which hasn't payment
    public static Cursor getUselessOrders(SQLiteDatabase db,String driver_id) {

        return db.rawQuery("select distinct o._id,o.code from orders o " +
                "left join order_payments op on o._id = op.order_id " +
                "left join orders_chg och on o._id = och._id " +
                "where o.driver_id = ? and o.posted = 0 and op._id is null and och._id is null", new String[]{driver_id});
    }


}
