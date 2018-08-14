package com.ff.deliveryservice.common;

/**
 * Created by Mark Khakimulin on 02.07.2018.
 * mark.khakimulin@gmail.com
 */
public class Constants {

    //Soap links and methods
    public final static String SOAP_URL = "http://delivery.finn-flare.ru/ws/delivery_ff.1cws";
    public final static String SOAP_NAMESPACE = "http://www.rarus.ru/supersynh";
    public final static String SOAP_METHOD_LOGIN_LIST = "GetLoginList";
    public final static String SOAP_METHOD_LOGIN = "Login";
    public final static String SOAP_METHOD_ORDER_PACK = "GetOrderPack";
    public final static String SOAP_METHOD_PUT_ORDER_PACK = "PutOrderChanges";

    public static final String FORMATDATE_1C = "yyyyMMdd";
    public static final String FORMATDATE_APP = "yyyy-MM-dd";


    //server fields
    public final static String ID = "ID";
    public final static String DESCRIPTION = "Description";
    public final static String PASSWORD = "Password";


    //shared_preference
    public static final String FPTR_PREFERENCES    = "com.ff.deliveryservice.service.preference.FPTR";
    public static final String SP_USER_NAME   = "com.ff.deliveryservice.service.preference.username";
    public static final String SP_USER_ID   = "com.ff.deliveryservice.service.preference.userid";

    //soap
}
