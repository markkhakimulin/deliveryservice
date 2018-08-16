package com.ff.deliveryservice.modules.details;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;

import com.ff.deliveryservice.R;
import com.ff.deliveryservice.application.DeliveryServiceApplication;
import com.ff.deliveryservice.modules.details.adapter.SectionsPagerAdapter;
import com.ff.deliveryservice.modules.details.dialogs.ChequeConfirmDialog;
import com.ff.deliveryservice.modules.details.dialogs.SimpleListChequeTypeDialog;
import com.ff.deliveryservice.modules.details.dialogs.SimpleListPaymentDialog;
import com.ff.deliveryservice.modules.details.dialogs.SimpleListReasonDialog;
import com.ff.deliveryservice.modules.details.fragments.OnFragmentHandler;
import com.ff.deliveryservice.modules.details.fragments.OrderDetailsFragment;
import com.ff.deliveryservice.modules.details.fragments.OrderItemsFragment;
import com.ff.deliveryservice.modules.details.fragments.OrderPaymentsFragment;
import com.ff.deliveryservice.modules.fptr.FPTRActivity;
import com.ff.deliveryservice.modules.fptr.FPTRService;
import com.ff.deliveryservice.modules.scanner.FullScannerResultActivity;
import com.ff.deliveryservice.mvp.model.ChequeData;
import com.ff.deliveryservice.mvp.model.DBHelper;
import com.ff.deliveryservice.mvp.presenter.DetailsPresenter;
import com.ff.deliveryservice.mvp.view.OrderDetailsView;

import java.util.Map;

import javax.inject.Inject;
/**
 * Created by khakimulin on 22.02.2017.
 */

/**
 * A general activity to make order and payment for it.
 */

public class OrderDetailsActivity extends FPTRActivity implements OnFragmentHandler,OrderDetailsView {

    private static final int REQUEST_SHOW_SCANNER = 3;

    public static final String FRAGMENT_PAGE_DETAILS   = OrderDetailsFragment.class.getCanonicalName();
    public static final String FRAGMENT_PAGE_ITEMS     = OrderItemsFragment.class.getCanonicalName();
    public static final String FRAGMENT_PAGE_PAYMENTS  = OrderPaymentsFragment.class.getCanonicalName();

    //private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;
    private FloatingActionButton fab1,fab;
    private Map<Integer,Double> mPaymentTypes;
    private int mCheckType;



    @Inject
    DetailsPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        orderId = getIntent().getStringExtra(DBHelper.CN_ORDER_ID);
        loginId = getIntent().getStringExtra(DBHelper.CN_ID);
        numberId = getIntent().getStringExtra(DBHelper.CN_CODE);
        loginDesc = getIntent().getStringExtra(DBHelper.CN_DESCRIPTION);

        super.onCreate(savedInstanceState);

    }



    @Override
    protected void onViewReady(Bundle savedInstanceState, Intent intent) {
        super.onViewReady(savedInstanceState,intent);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this);

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

        //тач листенер для того чтобы можно было перетаскивать кнопку
        fab1.setOnLongClickListener(new View.OnLongClickListener() {
            @SuppressLint("ClickableViewAccessibility")
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
                presenter.getChequeTypeList();

            }
        });

        setTitle(getString(R.string.title_activity_order_detail) +": "+ numberId);
    }

    @Override
    protected void resolveDaggerDependency(){
        super.resolveDaggerDependency();
        DeliveryServiceApplication.initDetailsComponent(this,loginId,numberId,orderId).inject(this);
    }
    @Override
    protected int getContentView() {
        return R.layout.activity_order_details;
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
            presenter.updateOrderItemByBarcode(barcode);
        }
    }

    @Override
    public void onFragmentViewCreated(String fragment) {
        if (fragment == FRAGMENT_PAGE_DETAILS) {
            presenter.updateOrderDetails();
        } else if (fragment == FRAGMENT_PAGE_ITEMS) {
            presenter.updateOrderItems();
        } else if (fragment == FRAGMENT_PAGE_PAYMENTS) {
            presenter.updateOrderPayments();
        }
    }

    //itmes fragment

    @Override
    public void onItemClicked(String itemId,String eid) {
        presenter.updateOrderItem(itemId,eid);
    }

    @Override
    public void onItemLongClicked(String itemId) {
        presenter.updateOrderDeliveryItem(itemId);
    }

    @Override
    public void onOrderItemUpdated() {

        presenter.setOrderChanged();
        presenter.updateOrderItems();
    }

    //details fragment

    @Override
    public void onCancelClicked() {
        presenter.getRefuseList();
    }

    public void onGetRefusesCursor(Cursor cursor) {
        showRefuseDialog(cursor);
    }

    public void showRefuseDialog(Cursor cursor) {
        FragmentManager fm = getSupportFragmentManager();
        SimpleListReasonDialog.newInstance(this,cursor).show(fm,"fragment_simple_list_reason_dialog");
    }

    @Override
    public void onReasonChoosed(String reasonId,int canceled) {
        presenter.setOrderCanceled(reasonId,canceled);
    }


    //payment fragment

    @Override
    public void onChangePaymentValue(int paymentType,double summ) {
        presenter.changePaymentValue(paymentType,summ);
    }

    @Override
    public void onChangePaymentValueComplete() {
        presenter.setOrderChanged();
        presenter.updateOrderPayments();
    }


    //cheque types dialog

    public void onGetChequeTypesCursor(Cursor cursor) {
        showChequeTypesDialog(cursor);
    }

    public void showChequeTypesDialog(Cursor cursor) {

        FragmentManager fm = getSupportFragmentManager();

        SimpleListChequeTypeDialog.newInstance(this,cursor).show(fm,"fragment_simple_list_check_type_dialog");
    }

    @Override
    public void onCheckTypeChoosed(int checkType) {
        presenter.getOrderPayments(checkType);
    }

    //payment dialog

    @Override
    public void onGetPaymentsCursor(Cursor cursor, double sumToPay, int checkType) {
        showPaymentTypesDialog(cursor,checkType,sumToPay);
    }

    public void showPaymentTypesDialog(Cursor cursor,int checkType,double sumToPay) {

        FragmentManager fm = getSupportFragmentManager();
        SimpleListPaymentDialog.newInstance(this,cursor,checkType,sumToPay).show(fm,"fragment_simple_list_payment_dialog");
    }

    @Override
    public void onPaymentTypeChoosed(Map<Integer,Double> map,int checkType) {

        mPaymentTypes = map;
        mCheckType = checkType;
        presenter.confirmPayment();

    }

    //cheque confirm dialog

    public void onConfirmPayment(Cursor cursor) {
        showChequeDialog(cursor,mPaymentTypes,mCheckType);
    }
    public void showChequeDialog(Cursor cursor,Map<Integer,Double> paymentTypeCode,int checkType) {

        FragmentManager fm = getSupportFragmentManager();
        ChequeConfirmDialog.newInstance(this,cursor,paymentTypeCode,checkType).show(fm,"fragment_simple_check_confirm_dialog");
    }

    //CALL PAYMENT SERVICE
    @Override
    public void onChequeClicked(Map<Integer,Double> map,final int checkType,String notification) {

        ChequeData chequeData = new ChequeData();
        

        startActionPayment(chequeData);
    }


   /* public class AddPayment extends AsyncTask<Context, Integer, Integer> {
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
        }

        private void cashIncome(double sum) throws DriverException {

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

        protected Integer doInBackground(Context... params) {

            mContext = params[0];
            mErrorMessage = "";
            mSession = -1;

            progressStages = new ArrayList<>();
            progressStages.add(getString(R.string.fptr_settings_loading));
            progressStages.add(getString(R.string.fptr_settings_set_connection));
            progressStages.add(getString(R.string.fptr_settings_check_connection));
            progressStages.add(getString(R.string.fptr_settings_cancel_cheque));
            progressStages.add(getString(R.string.fptr_settings_open_cheque));
            progressStages.add(getString(R.string.fptr_settings_payment));
            progressStages.add(getString(R.string.fptr_settings_close_cheque));


            db = sqLiteOpenHelper.getReadableDatabase();
            if (db == null) {
                mErrorMessage = getString(R.string.db_error_open_for_reading);
                Log.e(OrderDetailsActivity.class.getCanonicalName()+".AddPayment.doInBackground",mErrorMessage);
                return 0;
            }
            mCallback.lockChequeButton();

            double sumToPay = getSumToPay(db,mCheckType);

            if (mCheckType == IFptr.CHEQUE_TYPE_RETURN_BUY) {
                //приводим ошибочный чек к типу чека возврата, для того чтобы ошибочный чек можно было возвращать с услугами (не сработал флаг isReturnCheque)
                mCheckType = IFptr.CHEQUE_TYPE_RETURN;
            }

*//*            if (isOrderPaid(db) &&  mCheckType != IFptr.CHEQUE_TYPE_RETURN) {
                db.close();
                mErrorMessage = getString(R.string.order_detail_order_paid);
                return 0;
            }*//*

            if (sumToPay <= 0) {
                db.close();
                mErrorMessage = getString(R.string.order_details_error_sum_zero);
                Log.e(OrderDetailsActivity.class.getCanonicalName()+".AddPayment.doInBackground",mErrorMessage);
                return 0;
            }

            db.close();

            if (fptr != null)
            {
                mErrorMessage = getString(R.string.order_detail_ofd_send_process);
                Log.e(OrderDetailsActivity.class.getCanonicalName()+".AddPayment.doInBackground",mErrorMessage);
                return 0;
            }

            fptr = new Fptr();
            fptr.create(mContext);

            try {

                publishProgress(0);
                if (fptr.put_DeviceSettings(getSettings()) < 0) {
                    checkError();
                }
                publishProgress(1);
                if (fptr.put_DeviceEnabled(true) < 0) {
                    checkError();
                }
                publishProgress(2);
                if (fptr.GetStatus() < 0) {
                    checkError();
                }
                publishProgress(3);
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
                publishProgress(4);
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
*//*                if (isReturnCheque) {
                    cursor = db.rawQuery("select oi.*,i.description from order_items oi " +
                            "left join items as i on oi.item_id = i._id " +
                            "where oi.order_id = ? and oi.checked = 1 and oi.item_id <> ? group by oi._id", new String[]{orderId,"1"});
                } else {*//*
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

                publishProgress(5);
                mRemainder = 0.0;//неоплач остаток
                mChange = 0.0;//сдача


                int lastPaymentcode = 0;
                for (int paymentCode:mPaymentTypeCode.keySet()) {
                    payment(mPaymentTypeCode.get(paymentCode), paymentCode,mRemainder,mChange);
                    lastPaymentcode = paymentCode;
                }
                publishProgress(6);


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
                    progressStages.add(getString(R.string.fptr_settings_income_cash));
                    publishProgress(7);
                    try {
                        cashIncome(mSum.doubleValue() - mDiscount.doubleValue());

                        mErrorMessage = getString(R.string.order_detail_cash_income_document);
                        return -3800;
                    } catch (DriverException e1) {
                        mErrorMessage = e1.getMessage();
                    }
                }
                return 0;
            }

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values == null || values.length == 0) {
                return;
            }

            showProgressDialog(values[0]);
            //Toast.makeText(mContext,values[0],500).show();
        }

        private String getDateTime() {
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = new Date();
            return dateFormat.format(date);
        }

        @Override
        protected void onPostExecute(final Integer checkNumber) {

            mCallback.unlockChequeButton();

            hideProgressDialog();

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
    }*/





}
