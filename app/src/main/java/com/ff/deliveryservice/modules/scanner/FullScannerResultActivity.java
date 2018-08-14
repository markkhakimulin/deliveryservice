package com.ff.deliveryservice.modules.scanner;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import me.dm7.barcodescanner.zxing.sample.FullScannerActivity;
/**
 * Created by khakimulin on 16.03.2017.
 */

/**
 * Scan barcodes via zxing library.
 */
public class FullScannerResultActivity extends FullScannerActivity {

    public static final int PERMISSION_REQUEST = 1;

    @TargetApi(23)
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST);
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Now user should be able to use camera
            } else {
                Toast.makeText(this, "No permissions allowed", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //переопределим чтобы не показывались сообщения
    @Override
    public void showMessageDialog(String message) {

    }

    @Override
    public void processResult(String text,String barcodeType) {

        String result = text;
        if (result != "") {
            String empty = "*"+"strokescribe.com FREE"+"*";
            int indexOf = result.indexOf(empty);
            if (indexOf > 0) {
                result = result.substring(0,indexOf);
            }
            Intent intent = new Intent();
            intent.putExtra("barcode", result);
            setResult(RESULT_OK, intent);
            finish();
        } else
        {
            Toast.makeText(this, "Contents = " + text +
                    ", Format = " + barcodeType, Toast.LENGTH_SHORT).show();
        }
    }


}
