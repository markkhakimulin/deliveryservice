package com.ff.deliveryservice;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.atol.drivers.fptr.settings.BluetoothSearchActivity;
import com.atol.drivers.fptr.settings.DeviceSettings;
import com.atol.drivers.fptr.settings.OptionItem;
import com.atol.drivers.fptr.settings.OptionItemArrayAdapter;
import com.atol.drivers.fptr.settings.OptionItemCheckbox;
import com.atol.drivers.fptr.settings.OptionItemDevice;
import com.atol.drivers.fptr.settings.OptionItemNumericEdit;
import com.atol.drivers.fptr.settings.OptionItemSpinner;
import com.atol.drivers.fptr.settings.TCPSettingsActivity;
import com.atol.drivers.fptr.settings.UDPSearchActivity;
import com.atol.drivers.fptr.settings.USBSearchActivity;
import com.atol.drivers.fptr.settings.res;
import com.atol.drivers.fptr.IFptr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created by khakimulin on 28.12.2017.
 */
/**
 *
 Main class for setting trading equipment.
 */
public class FPTRSettingsActivity extends ListActivity {
    public static final String DEVICE_SETTINGS = "ECR_DEVICE_SETTINGS";
    public static final String EXTRA_SHOW_OLD_MODELS = "EXTRA_ECR_SHOW_OLD_MODELS";
    public static final String EXTRA_SHOW_NEW_MODELS = "EXTRA_ECR_SHOW_NP_MODELS";
    private static final int REQUEST_SELECT_DEVICE = 1;
    private List<OptionItem> options = null;
    private OptionItemArrayAdapter optionsAdapter;
    private OptionItemSpinner spinnerPort;
    private DeviceSettings settings = new DeviceSettings();
    private boolean showNewModels = false;
    private boolean showOldModels = true;
    private String deviceName = "";
    private ArrayList<String> mNewDevicesName = new ArrayList<>();
    private ArrayList<String> mNewDevicesAdress = new ArrayList<>();
    private boolean waitingForBluetoothEnabling = false;
    private BluetoothAdapter btAdapter;
    private TextView mTextView;
    private FPTRSettingsActivity mContext;
    private BluetoothReceiver mReceiver;
    private ProgressDialog mProgressDialog;

    public FPTRSettingsActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        this.overridePendingTransition(res.get(this, "fptr_pull_in_from_right", "anim"), res.get(this, "fptr_hold", "anim"));
        this.setContentView(res.get(this, "fptr_settings_layout", "layout"));
        if(savedInstanceState == null) {
            Bundle bundle = this.getIntent().getExtras();
            if(bundle != null) {
                this.settings.fromXML(bundle.getString("ECR_DEVICE_SETTINGS"));
                this.showNewModels = bundle.getBoolean("EXTRA_ECR_SHOW_NP_MODELS", true);
                this.showOldModels = bundle.getBoolean("EXTRA_ECR_SHOW_OLD_MODELS", false);
            }
        } else {
            this.settings.fromXML(savedInstanceState.getString("ECR_DEVICE_SETTINGS"));
            this.showNewModels = savedInstanceState.getBoolean("EXTRA_ECR_SHOW_NP_MODELS", true);
            this.showOldModels = savedInstanceState.getBoolean("EXTRA_ECR_SHOW_OLD_MODELS", false);
        }

        this.createOptionsAdapter();
        this.optionsAdapter = new OptionItemArrayAdapter(this, this.options);
        this.setListAdapter(this.optionsAdapter);

        this.btAdapter = BluetoothAdapter.getDefaultAdapter();



        Button setUpButton = findViewById(R.id.setup_button);
        setUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                OptionItemSpinner opSpinner = (OptionItemSpinner)options.get(0);
                opSpinner.setValues(new String[]{getString(R.string.fptr_settings_default_model)},new String[]{String.valueOf(IFptr.MODEL_ATOL_11F)});
                opSpinner.setValueAsString(settings.get("Model"));

                String[] arrayProtocols = new String[]{getString(R.string.fptr_settings_default_protocol)};
                String[] arrayProtocolNumbers = new String[]{String.valueOf(0)};
                opSpinner = (OptionItemSpinner)options.get(1);
                opSpinner.setValues(arrayProtocols, arrayProtocolNumbers);
                opSpinner.setValueAsString(settings.get("Protocol"));

                String[] arrayPorts = new String[]{getString(R.string.fptr_settings_default_transport)};
                String[] arrayPortValues = new String[]{"BLUETOOTH"};
                opSpinner = (OptionItemSpinner)options.get(2);
                opSpinner.setValues(arrayPorts, arrayPortValues);
                opSpinner.setValueAsString(settings.get("Port"));

                String[] arrayPortsOfd = new String[]{getString(R.string.fptr_settings_default_ports_ofd)};
                String[] arrayPortOfdValues = new String[]{"PROTO"};
                opSpinner = (OptionItemSpinner)options.get(6);
                opSpinner.setValues(arrayPortsOfd, arrayPortOfdValues);
                opSpinner.setValueAsString(settings.get("OfdPort"));

                OptionItemCheckbox opCheckbox =(OptionItemCheckbox)options.get(7);
                opCheckbox.setChecked(true);

                OptionItemDevice ob =  (OptionItemDevice)options.get(3);
                if (ob.getDeviceAddr().equalsIgnoreCase("")
                        || ob.getDeviceName().equalsIgnoreCase("Device #1")
                        || ob.getDeviceName().equalsIgnoreCase("")) {

                    if (btAdapter != null) {
                        if (!btAdapter.isEnabled()) {
                            waitingForBluetoothEnabling = true;
                            Intent enableIntent = new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE");
                            startActivityForResult(enableIntent, 1);
                        } else {
                            mReceiver = new BluetoothReceiver(mContext);
                            registerReceiver(mReceiver, new IntentFilter("android.bluetooth.device.action.FOUND"));
                            registerReceiver(mReceiver, new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_FINISHED"));
                            startSearchDevices();

                        }
                    } else {
                        showError(getString(R.string.fptr_settings_err_bluetooth_detection));
                    }
                } else {
                    mNewDevicesName.add(ob.getDeviceName());
                    mNewDevicesAdress.add(ob.getDeviceAddr());
                    onBackPressed();
                }
            }
        });
    }
    private void startSearchDevices() {
        if(!waitingForBluetoothEnabling) {
            if(btAdapter.isEnabled()) {
                startDiscovery();
            }
        }
    }

    private void startDiscovery() {
        mNewDevicesName.clear();
        mNewDevicesAdress.clear();
        Set<BluetoothDevice> pairedDevices = this.btAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            Iterator var2 = pairedDevices.iterator();

            while(var2.hasNext()) {
                BluetoothDevice device = (BluetoothDevice)var2.next();
                mNewDevicesName.add(device.getName());
                mNewDevicesAdress.add(device.getAddress());
            }

            fillBluetoothData();

        } else {
            showProgressDialog(getString(R.string.fptr_settings_bluetooth_searching));
            if (btAdapter.isDiscovering()) {
                btAdapter.cancelDiscovery();
            }
            btAdapter.startDiscovery();
        }
    }
    private void stopDiscovery() {
        btAdapter.cancelDiscovery();
        this.hideProgressDialog();
    }

    public class BluetoothReceiver extends BroadcastReceiver {
        FPTRSettingsActivity mContext;

        public BluetoothReceiver(FPTRSettingsActivity fptrSettingsActivity) {
            mContext = fptrSettingsActivity;
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if("android.bluetooth.device.action.FOUND".equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if(device.getBondState() != 12) {
                    String name = device.getName();
                    String addr = device.getAddress();
                    if(name == null) {
                        name = "";
                    }

                    boolean duplicate = false;

                    for(int i = 0; i < mNewDevicesAdress.size(); ++i) {
                        String item = (String)mNewDevicesAdress.get(i);
                        String itemAddr = item.substring(item.length() - 17);
                        if(itemAddr.equals(addr)) {
                            duplicate = true;
                            break;
                        }
                    }

                    if(!duplicate) {
                        mNewDevicesName.add(name);
                        mNewDevicesAdress.add(addr);
                    }
                }
            }else if("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(action)) {
                if(!btAdapter.isDiscovering()) {

                    stopDiscovery();
                    fillBluetoothData();
                }
            }
        }
    }
    public void fillBluetoothData() {
        OptionItemDevice opDevice = (OptionItemDevice) options.get(3);

        if (mNewDevicesAdress.size() == 1 && mNewDevicesName.size() == 1) {
            opDevice.setDeviceAddr(mNewDevicesAdress.get(0));
            opDevice.setDeviceName(mNewDevicesName.get(0));

            onBackPressed();

        } else if(mNewDevicesAdress.size() > 1 && mNewDevicesName.size() > 1)  {

            showError(getString(R.string.fptr_settings_err_setup), new Callable() {
                @Override
                public Object call() throws Exception {
                    Intent intent = new Intent(FPTRSettingsActivity.this, BluetoothSearchActivity.class);
                    intent.putExtra("EXTRA_BLUETOOTH_AUTOENABLE", settings.get("AutoEnableBluetooth"));
                    intent.putExtra("EXTRA_BLUETOOTH_AUTODISABLE", settings.get("AutoDisableBluetooth"));
                    intent.putExtra("EXTRA_BLUETOOTH_CONNECTION_TYPE", settings.get("ConnectionType"));
                    startActivityForResult(intent, 1);
                    return null;
                }
            });

        } else {
            showError(getString(R.string.fptr_settings_err_bluetooth_searching), new Callable() {
                @Override
                public Object call() throws Exception {
                    gotoBlueToothSettings();
                    return null;
                }
            });
        }

        try {
            unregisterReceiver(mReceiver);
        } catch (final Exception exception)
        {}

    }

    void gotoBlueToothSettings() {
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        final ComponentName cn = new ComponentName("com.android.settings",
                "com.android.settings.bluetooth.BluetoothSettings");
        intent.setComponent(cn);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity( intent);
    }

    void showError(String message) {
        showError(message, null);
    }

    void showError(String message, @Nullable final Callable okCallback) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage(message).setTitle(getString(R.string.alert_error));
        if (okCallback != null) {
            builder.setPositiveButton(getString(R.string.alert_button_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        okCallback.call();
                    } catch (Exception e) {
                    }

                }
            });
        }
        builder.create().show();
    }

    public void onPause() {
        this.overridePendingTransition(res.get(this, "fptr_hold", "anim"), res.get(this, "fptr_pull_out_to_right", "anim"));
        super.onPause();
    }


    public void onBackPressed() {

        try {
            unregisterReceiver(mReceiver);
        } catch (final Exception exception)
        {}
        Intent intent = new Intent();
        String s = this.getResultData();
        intent.putExtra("ECR_DEVICE_SETTINGS", s);
        this.setResult(-1, intent);

        Toast.makeText(FPTRSettingsActivity.this,getString(R.string.fptr_settings_change_successful),Toast.LENGTH_LONG).show();
        super.onBackPressed();
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("ECR_DEVICE_SETTINGS", this.getResultData());
        super.onSaveInstanceState(savedInstanceState);
    }

    private void createOptionsAdapter() {
        this.options = new ArrayList();
        List<String> models = new LinkedList();
        List<String> modelsNumbers = new LinkedList();
        if(this.showOldModels) {
            models.add("FPrint-02К / ЕНВД");
            models.add("FPrint-03К / ЕНВД");
            models.add("FPrint-88К / ЕНВД");
            models.add("FPrint-5200К / ЕНВД");
            models.add("FPrint-55ПТК / К / ЕНВД");
            models.add("FPrint-11ПТК / К / ЕНВД");
            models.add("FPrint-22ПТК / К / ЕНВД");
            models.add("FPrint-77ПТК / ЕНВД");
            models.add("FPrintPay-01ПТК / ЕНВД");
            models.add("FPrint-30ЕНВД");
            modelsNumbers.add(String.valueOf(IFptr.MODEL_FPRINT_02));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_FPRINT_03));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_FPRINT_88));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_FPRINT_5200));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_FPRINT_55));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_FPRINT_11));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_FPRINT_22));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_FPRINT_77));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_FPRINT_PAY_01));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_FPRINT_30));
        }

        if(this.showNewModels) {
            models.add("АТОЛ 11Ф");
            models.add("АТОЛ 15Ф");
            models.add("АТОЛ 20Ф");
            models.add("АТОЛ 22Ф");
            models.add("АТОЛ 25Ф");
            models.add("АТОЛ 30Ф");
            models.add("АТОЛ 42ФC");
            models.add("АТОЛ 50Ф");
            models.add("АТОЛ 52Ф");
            models.add("АТОЛ 55Ф");
            models.add("АТОЛ 60Ф");
            models.add("АТОЛ 77Ф");
            models.add("АТОЛ 90Ф");
            models.add("АТОЛ 91Ф");
            models.add("ЭВОТОР СТ2Ф");
            models.add("ЭВОТОР СТ3Ф");
            models.add("Казначей ФА");
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_11F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_15F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_20F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_22F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_25F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_30F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_KAZNACHEY_FA));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_50F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_52F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_55F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_60F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_77F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_90F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_20F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_EVOTOR_ST2F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_EVOTOR_ST3F));
            modelsNumbers.add(String.valueOf(IFptr.MODEL_ATOL_42FS));
        }

        OptionItemSpinner opSpinner = new OptionItemSpinner("Model", "Модель ККТ", "");
        opSpinner.setValues((String[])models.toArray(new String[models.size()]), (String[])modelsNumbers.toArray(new String[modelsNumbers.size()]));
        opSpinner.setPrompt("Выберите модель ККТ");
        opSpinner.setValueAsString(this.settings.get("Model"));
        options.add(0,opSpinner);
        String[] arrayProtocols = new String[]{"По умолчанию", "АТОЛ 2", "АТОЛ 3"};
        String[] arrayProtocolNumbers = new String[]{String.valueOf(0), String.valueOf(1), String.valueOf(2)};
        opSpinner = new OptionItemSpinner("Protocol", "Протокол", "");
        opSpinner.setValues(arrayProtocols, arrayProtocolNumbers);
        opSpinner.setPrompt("Выберите протокол ККТ");
        opSpinner.setValueAsString(this.settings.get("Protocol"));
        options.add(1,opSpinner);
        String[] arrayPorts = new String[]{"Bluetooth", "UDP/IP", "TCP/IP", "USB"};
        String[] arrayPortValues = new String[]{"BLUETOOTH", "UDPIP", "TCPIP", "USB"};
        spinnerPort = new OptionItemSpinner("Port", "Порт", "Способ связи");
        spinnerPort.setValues(arrayPorts, arrayPortValues);
        spinnerPort.setPrompt("Выберите порт");
        spinnerPort.setValueAsString(this.settings.get("Port"));
        options.add(2,this.spinnerPort);
        OptionItemDevice opDevice = new OptionItemDevice("MACAddress", "Устройство", "");
        if(settings.get("Port").equals("BLUETOOTH")) {
            opDevice.setDeviceAddr(this.settings.get("MACAddress"));
            opDevice.setDeviceName(this.settings.get("DeviceName"));
        } else if(!this.settings.get("Port").equals("UDPIP") && !this.settings.get("Port").equals("TCPIP")) {
            if(this.settings.get("Port").equals("USB")) {
                try {
                    int vid = Integer.parseInt(this.settings.get("Vid"));
                    int pid = Integer.parseInt(this.settings.get("Pid"));
                    opDevice.setDeviceAddr(String.format("%04X:%04X", new Object[]{Integer.valueOf(vid), Integer.valueOf(pid)}));
                } catch (NumberFormatException var13) {
                    opDevice.setDeviceAddr("");
                }
            }
        } else {
            opDevice.setDeviceAddr(this.settings.get("IPAddress") + ":" + this.settings.get("IPPort"));
            opDevice.setDeviceName(this.settings.get("DeviceName"));
        }

        this.options.add(3,opDevice);
        OptionItemNumericEdit opNumeric = new OptionItemNumericEdit("AccessPassword", "Пароль доступа", "Пароль доступа к ККТ");
        opNumeric.setValueAsString(this.settings.get("AccessPassword"));
        this.options.add(4,opNumeric);
        opNumeric = new OptionItemNumericEdit("UserPassword", "Пароль пользователя", "");
        opNumeric.setValueAsString(this.settings.get("UserPassword"));
        this.options.add(5,opNumeric);
        String[] arrayPortsOfd = new String[]{"Нет", "USB", "EthernetOverTransport"};
        String[] arrayPortOfdValues = new String[]{"NONE", "USB", "PROTO"};
        opSpinner = new OptionItemSpinner("OfdPort", "Связь с ОФД", "Способ связи с ОФД");
        opSpinner.setValues(arrayPortsOfd, arrayPortOfdValues);
        opSpinner.setPrompt("Выберите порт");
        opSpinner.setValueAsString(this.settings.get("OfdPort"));
        this.options.add(6,opSpinner);
        OptionItemCheckbox opCheckbox = new OptionItemCheckbox("UseJournal", "Хранить чеки в БД", "");
        opCheckbox.setChecked(this.settings.get("UseJournal").compareTo("1") == 0);
        this.options.add(7,opCheckbox);
    }

    private String getResultData() {
        if(this.options == null) {
            return null;
        } else {
            for(int i = 0; i < this.options.size(); ++i) {
                OptionItem opt = (OptionItem)this.options.get(i);
                if(opt.getModelType() != OptionItem.itemType.itDevice) {
                    this.settings.set(opt.getName(), opt.getValueAsString());
                    if(opt.getName().equals("Model")) {
                        this.deviceName = ((OptionItemSpinner)opt).getLabel();
                    }
                } else {
                    OptionItemDevice deviceOpt = (OptionItemDevice)opt;
                    String currentPort = this.spinnerPort.getValueAsString();
                    byte var6 = -1;
                    switch(currentPort.hashCode()) {
                        case 84324:
                            if(currentPort.equals("USB")) {
                                var6 = 3;
                            }
                            break;
                        case 79650984:
                            if(currentPort.equals("TCPIP")) {
                                var6 = 2;
                            }
                            break;
                        case 80604296:
                            if(currentPort.equals("UDPIP")) {
                                var6 = 1;
                            }
                            break;
                        case 460509838:
                            if(currentPort.equals("BLUETOOTH")) {
                                var6 = 0;
                            }
                    }

                    String addr;
                    String[] parts;
                    switch(var6) {
                        case 0:
                            this.settings.set("MACAddress", deviceOpt.getDeviceAddr());
                            break;
                        case 1:
                        case 2:
                            addr = deviceOpt.getDeviceAddr();
                            parts = addr.split(":");
                            if(parts.length == 2) {
                                this.settings.set("IPAddress", parts[0]);
                                this.settings.set("IPPort", parts[1]);
                            }
                            break;
                        case 3:
                            addr = deviceOpt.getDeviceAddr();
                            parts = addr.split(":");
                            if(parts.length == 2) {
                                try {
                                    this.settings.set("Vid", String.valueOf(Integer.parseInt(parts[0], 16)));
                                    this.settings.set("Pid", String.valueOf(Integer.parseInt(parts[1], 16)));
                                } catch (Exception var10) {
                                    ;
                                }
                            }
                    }

                    this.settings.set("DeviceName", deviceOpt.getDeviceName());
                }
            }

            return this.settings.toXML();
        }
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        if(l == this.getListView()) {

            switch((options.get(position)).getModelType().ordinal()) {
                case 2:
                    OptionItemCheckbox item = (OptionItemCheckbox)this.options.get(position);
                    CompoundButton btn = (CompoundButton)v.findViewById(res.get(this, "checkBox", "id"));
                    if(btn != null) {
                        btn.setChecked(!item.isChecked());
                    }
                    break;
                case 3:
                    String currentPort = this.spinnerPort.getValueAsString();
                    Intent intent;
                    if(currentPort.equals("BLUETOOTH")) {
                        intent = new Intent(this, BluetoothSearchActivity.class);
                        intent.putExtra("EXTRA_BLUETOOTH_AUTOENABLE", this.settings.get("AutoEnableBluetooth"));
                        intent.putExtra("EXTRA_BLUETOOTH_AUTODISABLE", this.settings.get("AutoDisableBluetooth"));
                        intent.putExtra("EXTRA_BLUETOOTH_CONNECTION_TYPE", this.settings.get("ConnectionType"));
                    } else if(currentPort.equals("UDPIP")) {
                        intent = new Intent(this, UDPSearchActivity.class);
                    } else if(currentPort.equals("USB")) {
                        intent = new Intent(this, USBSearchActivity.class);
                    } else {
                        if(!currentPort.equals("TCPIP")) {
                            return;
                        }

                        intent = new Intent(this, TCPSettingsActivity.class);
                        intent.putExtra("EXTRA_ADDRESS", this.settings.get("IPAddress"));
                        intent.putExtra("EXTRA_PORT", this.settings.get("IPPort"));
                    }
                    this.startActivityForResult(intent, 1);
            }
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        waitingForBluetoothEnabling = false;

        if(requestCode == 1 && resultCode == -1) {
            OptionItemDevice ob = null;
            Iterator var5 = this.options.iterator();

            while(var5.hasNext()) {
                OptionItem i = (OptionItem)var5.next();
                if(i instanceof OptionItemDevice) {
                    ob = (OptionItemDevice)i;
                }
            }


            if (data != null) {
                Bundle bundle = data.getExtras();
                if (this.spinnerPort.getValueAsString().equals("BLUETOOTH")) {
                    ob.setDeviceAddr(bundle.getString("EXTRA_DEVICE_ADDRESS", ob.getDeviceAddr()));
                    ob.setDeviceName(bundle.getString("EXTRA_DEVICE_NAME", ob.getDeviceName()));
                    this.settings.set("AutoEnableBluetooth", bundle.getString("EXTRA_BLUETOOTH_AUTOENABLE"));
                    this.settings.set("AutoDisableBluetooth", bundle.getString("EXTRA_BLUETOOTH_AUTODISABLE"));
                    this.settings.set("ConnectionType", bundle.getString("EXTRA_BLUETOOTH_CONNECTION_TYPE"));

                } else if (this.spinnerPort.getValueAsString().equals("UDPIP")) {
                    ob.setDeviceAddr(bundle.getString("device_address") + ":" + bundle.getString("device_port"));
                    ob.setDeviceName(bundle.getString("device_name"));
                } else if (this.spinnerPort.getValueAsString().equals("USB")) {
                    ob.setDeviceAddr(bundle.getString("device_vid") + ":" + bundle.getString("device_pid"));
                    ob.setDeviceName(bundle.getString("device_name"));
                } else if (this.spinnerPort.getValueAsString().equals("TCPIP")) {
                    ob.setDeviceAddr(bundle.getString("EXTRA_ADDRESS") + ":" + bundle.getString("EXTRA_PORT"));
                    ob.setDeviceName("");
                }

                getResultData();
                optionsAdapter.notifyDataSetChanged();
            }
        }

    }
    public void showProgressDialog(String title) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);

        }
        mProgressDialog.setMessage(title);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }


}
