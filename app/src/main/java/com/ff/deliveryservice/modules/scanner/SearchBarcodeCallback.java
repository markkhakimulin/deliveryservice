package com.ff.deliveryservice.modules.scanner;

import android.content.Context;

/**
 * Created by khakimulin on 01.03.2017.
 */

/**
 * Process scan zxing response.
 */
public interface SearchBarcodeCallback {
    void processCallback(int result,String reason,String barcode,Context context);
    void lockChequeButton();
    void unlockChequeButton();
}
