package com.example.laundrykita;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.epson.epos2.printer.Printer;
import com.epson.epos2.Epos2Exception;
import com.epson.epos2.Log;
import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ReceiveListener {
    public static Printer  mPrinter = null;
    public static EditText mEditTarget = null;
    private static final int REQUEST_PERMISSION = 100;
    private static final int DISCONNECT_INTERVAL = 500;//millseconds
    private Context mContext = null;

    DB_Controller controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//will hide the title
        getSupportActionBar().hide(); //hide the title bar
        setContentView(R.layout.activity_main);

        controller = new DB_Controller(this,"",null,1);
        requestRuntimePermission();
        mContext = this;

        Button button1 =(Button)findViewById(R.id.btnDiscoverPrn);

        TextInputEditText namaToko = (TextInputEditText)findViewById(R.id.namaToko);
        TextInputEditText namaCust = (TextInputEditText)findViewById(R.id.namaCust);
        AutoCompleteTextView newMenu = (AutoCompleteTextView)findViewById(R.id.spinnerMenu);
        TextInputEditText newExt = (TextInputEditText) findViewById(R.id.spinnerMenu2);

        // Initializing a new String Array
        String[] fruits = new String[] {
                "1","2","3","4","5","6","7","8","9","10"
        };

        String[] extra = new String[]{
                "Normal","Extra Sugar","Less Sugar","Ice","Hot"
        };

        // Create a List from String Array elements
        List<String> fruits_list = new ArrayList<String>(Arrays.asList(fruits));
        List<String> extra_list = new ArrayList<String>(Arrays.asList(extra));
        // Create an ArrayAdapter from List
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_dropdown_item_1line, fruits_list);

        ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<String>
                (this, android.R.layout.simple_dropdown_item_1line, extra_list);
        // DataBind ListView with items from ArrayAdapter

        newMenu.setAdapter(arrayAdapter);
        //newExt.setAdapter(arrayAdapter2);
        /*addmenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fruits_list.add(namaMenu.getText().toString());
                arrayAdapter.notifyDataSetChanged();
                try {
                    controller.insert_menu(namaMenu.getText().toString());
                }catch (SQLiteException e){
                    Toast.makeText(MainActivity.this, "ALREADY EXIST", Toast.LENGTH_SHORT).show();
                }
                namaMenu.setText("");
            }
        });*/

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = null;
                intent = new Intent(MainActivity.this,DiscoveryActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        Button buttonprn = (Button)findViewById(R.id.btnPrn);
        buttonprn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (printReceipt()){
                    arrayAdapter.add(newMenu.getText().toString());
                    //arrayAdapter2.add(newExt.getText().toString());
                    arrayAdapter.notifyDataSetChanged();
                    //arrayAdapter2.notifyDataSetChanged();
                    namaCust.setText("");
                    namaToko.setText("");
                    newExt.setText("");
                    newMenu.setText("");
                    controller.delete_menu();
                }
            }
        });
        initializeObject();
    }
    @Override
    protected void onDestroy() {

        finalizeObject();

        super.onDestroy();
    }
    private boolean printReceipt() {
        if (!initializeObject()) {
            return false;
        }
        if (!createCouponData()) {
            finalizeObject();
            return false;
        }
        if (!printData()) {
            finalizeObject();
            return false;
        }
        return true;
    }
    private boolean connectPrinter() {
        boolean isBeginTransaction = false;
        if (mPrinter == null) {
            return false;
        }
        try {
            mPrinter.connect(mEditTarget.getText().toString(), Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            //Toast.makeText(getApplicationContext(),"6"+e.getMessage(),Toast.LENGTH_LONG).show();
            ShowMsg.showException(e, "connect", mContext);
            return false;
        }
        try {
            mPrinter.beginTransaction();
            isBeginTransaction = true;
        }
        catch (Exception e) {
            //Toast.makeText(getApplicationContext(),"7"+e.getMessage(),Toast.LENGTH_LONG).show();
        }
        if (isBeginTransaction == false) {
            try {
                mPrinter.disconnect();
            }
            catch (Epos2Exception e) {
                // Do nothing
                return false;
            }
        }
        return true;
    }
    private boolean printData() {
        if (mPrinter == null) {
            return false;
        }
        if (!connectPrinter()) {
            return false;
        }
        PrinterStatusInfo status = mPrinter.getStatus();
        dispPrinterWarnings(status);
        if (!isPrintable(status)) {
            //Toast.makeText(getApplicationContext(),"3"+makeErrorMessage(status),Toast.LENGTH_LONG).show();
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
            return false;
        }
        try {
            mPrinter.sendData(Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            //Toast.makeText(getApplicationContext(),"4"+e.getMessage(),Toast.LENGTH_LONG).show();
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
            return false;
        }
        return true;
    }
    private boolean initializeObject() {
        try {
            mPrinter = new Printer(Printer.TM_T82,Printer.MODEL_ANK,mContext);
        }
        catch (Exception e) {
            ShowMsg.showException(e, "Printer", mContext);
            //Toast.makeText(getApplicationContext(),"2"+e.getMessage(),Toast.LENGTH_LONG).show();
            return false;
        }
        mPrinter.setReceiveEventListener(this);
        return true;
    }
    private void finalizeObject() {
        if (mPrinter == null) {
            return;
        }
        mPrinter.clearCommandBuffer();
        mPrinter.setReceiveEventListener(null);
        mPrinter = null;
    }
    private boolean createCouponData() {
        mContext = this;
        String method = "";
        StringBuilder textData = new StringBuilder();
        mEditTarget = (EditText)findViewById(R.id.edtTarget);
        TextInputEditText toko = (TextInputEditText) findViewById(R.id.namaToko);
        TextInputEditText cust = (TextInputEditText) findViewById(R.id.namaCust);
        AutoCompleteTextView spinMenu = (AutoCompleteTextView)findViewById(R.id.spinnerMenu);
        TextInputEditText spinMenu2 = (TextInputEditText) findViewById(R.id.spinnerMenu2);

        String nmToko = toko.getText().toString();
        String nmCust = cust.getText().toString();
        String singleMenu = String.valueOf(spinMenu.getText());
        String xtra = String.valueOf(spinMenu2.getText());
        String greeting = null;
        Date dt = new Date();
        int hours = dt.getHours();
        int min = dt.getMinutes();
        if(hours>1 && hours<=11){
            greeting = "Selamat Pagi";
        } else if(hours>=11 && hours<=14){
            greeting = "Selamat Siang";
        } else if(hours>=14 && hours<=18){
            greeting = "Selamat Sore";
        } else if(hours>=18 && hours<=24){
            greeting = "Selamat Malam";
        }
        Toast.makeText(this, greeting, Toast.LENGTH_SHORT).show();
        byte[] cmd= { 0x1B, 0x65, 0x10};

        if (mPrinter == null) {
            return false;
        }

        try {
            mPrinter.addCommand(cmd);
            textData.append("   "+nmToko+"\n");
            textData.append("   Hello "+nmCust+", "+greeting+"\n");
            textData.append("   Jumlah Pakaian Anda : "+singleMenu+"\n");
            textData.append("   Total Biaya Laundry : "+xtra+"\n");
            textData.append("   Terimakasih :)\n");
            method = "addText";
            mPrinter.addText(textData.toString());
            method = "addCut";
            mPrinter.addCut(Printer.CUT_FEED);
        }
        catch (Exception e) {
            mPrinter.clearCommandBuffer();
            ShowMsg.showException(e, method, mContext);
            return false;
        }

        return true;
    }
    protected void onActivityResult(int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && resultCode == RESULT_OK) {
            String target = data.getStringExtra(getString(R.string.title_target));
            if (target != null) {
                EditText mEdtTarget = (EditText) findViewById(R.id.edtTarget);
                mEdtTarget.setText(target);
            }
        }
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode != REQUEST_PERMISSION || grantResults.length == 0) {
            return;
        }

        List<String> requestPermissions = new ArrayList<>();

        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                requestPermissions.add(permissions[i]);
            }
            if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)
                    && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                requestPermissions.add(permissions[i]);
            }

            // If your app targets Android 9 or lower, you can declare ACCESS_COARSE_LOCATION instead.
//            if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)
//                    && grantResults[i] == PackageManager.PERMISSION_DENIED) {
//                requestPermissions.add(permissions[i]);
//            }
        }

        if (!requestPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, requestPermissions.toArray(new String[requestPermissions.size()]), REQUEST_PERMISSION);
        }
    }
    private boolean isPrintable(PrinterStatusInfo status) {
        if (status == null) {
            return false;
        }
        if (status.getConnection() == Printer.FALSE) {
            return false;
        }
        else if (status.getOnline() == Printer.FALSE) {
            return false;
        }
        else {
            ;//print available
        }
        return true;
    }
    private void dispPrinterWarnings(PrinterStatusInfo status) {
        String warningsMsg = "";
        if (status == null) {
            return;
        }

        if (status.getPaper() == Printer.PAPER_NEAR_END) {
            warningsMsg += getString(R.string.handlingmsg_warn_receipt_near_end);
        }

        if (status.getBatteryLevel() == Printer.BATTERY_LEVEL_1) {
            warningsMsg += getString(R.string.handlingmsg_warn_battery_near_end);
        }
        //Toast.makeText(getApplicationContext(),"5"+warningsMsg,Toast.LENGTH_LONG).show();
    }
    private void disconnectPrinter() {
        if (mPrinter == null) {
            return;
        }
        try {
            mPrinter.endTransaction();
        }
        catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                }
            });
        }

        try {
            mPrinter.disconnect();
        }
        catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                }
            });
        }
        finalizeObject();
    }
    private void updateButtonState(boolean state) {
        Button btnLabel = (Button)findViewById(R.id.btnPrn);
        btnLabel.setEnabled(state);
    }
    @Override
    public void onPtrReceive(Printer printer, int i, PrinterStatusInfo status, String s) {
        runOnUiThread(new Runnable() {
            @Override
            public synchronized void run() {
                //Toast.makeText(getApplicationContext(),"9"+e.getMessage(),Toast.LENGTH_LONG).show();
                dispPrinterWarnings(status);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        disconnectPrinter();
                    }
                }).start();
            }
        });
    }
    private void requestRuntimePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        int permissionStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionLocationCoarse= ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionLocationFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        List<String> requestPermissions = new ArrayList<>();

        if (permissionStorage == PackageManager.PERMISSION_DENIED) {
            requestPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionLocationFine == PackageManager.PERMISSION_DENIED) {
            requestPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
//        if (permissionLocationCoarse == PackageManager.PERMISSION_DENIED) {
//            requestPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
//        }

        if (!requestPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, requestPermissions.toArray(new String[requestPermissions.size()]), REQUEST_PERMISSION);
        }
    }
}