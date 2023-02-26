package com.example.activity;

import static android.bluetooth.BluetoothAdapter.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.server.Server;
import com.example.wifidemo.R;
import com.example.wifi.WifiUtil;
import com.example.wifi.myWiFiInfo;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {
    private WifiUtil wifiUtil;
    private WifiManager wifiManager;
    ArrayAdapter<String> adapter = null;

    // 全局变量设置
    private List<myWiFiInfo> myWiFiInfos = new ArrayList<>();
    private List<String> wifiId = new ArrayList<>();
    private List<ScanResult> wifiList = new ArrayList<>();
    private ArrayList<String> bledata_compare = new ArrayList<>();;
    private ArrayList<String> bledata = new ArrayList<>();;
    private List<Long> useTime = new ArrayList<>();
    private ScanSettings lowLantencySetting = null;
    private ScanSettings balencedSetting = null;
    private BluetoothLeScanner scanner = null;
    private Server server = new Server();
    private BeaconManager beaconManager;

    private static final String TAG = "MainActivity";
    private TextView textView = null;
    private String beaconId = "";
    long startTime;

    private BluetoothAdapter bluetoothAdapter = getDefaultAdapter();

    // 蓝牙扫描成功回调函数
    private final ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            super.onScanResult(callbackType, result);
            System.out.println(result.getDevice().getAddress());
            if (result.getDevice().getAddress().equals("F4:9A:53:1D:08:A1")) {
                long endTime = System.currentTimeMillis();
                long time = endTime - startTime;
                useTime.add(time);
                textView.setText("蓝牙扫描已收集" + Integer.toString(useTime.size()) + "条信息\n" +
                        "本次扫描时间： " + Long.toString(useTime.get(useTime.size() - 1)) + "ms\n");
                scanner.stopScan(scanCallback);
            }
        }

        @Override
        public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    // beacon相关常量设置
    private static final long DEFAULT_FOREGROUND_SCAN_PERIOD = 1000L;
    private static final long DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD = 1000L;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    public static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    // 初始化设置
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiUtil = new WifiUtil(this);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        lowLantencySetting = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        balencedSetting = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
        scanner = bluetoothAdapter.getBluetoothLeScanner();

        requestLocationPermissions();
        verifyStoragePermissions(this);
        adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, this.bledata);
        textView = (TextView)findViewById(R.id.textView);
        textView.setText("");
        initBeacon();
        getSupportActionBar().hide();
        transparentStatusBar();
    }

    // 权限设置
    private void requestLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Android M Permission check

            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);

                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void verifyStoragePermissions(Activity activity) {
        try {
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // beacon初始化
    private void initBeacon() {
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.setForegroundBetweenScanPeriod(DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD);
        beaconManager.setForegroundScanPeriod(DEFAULT_FOREGROUND_SCAN_PERIOD);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));
        beaconManager.bind( this);
    }


    // beacon扫描更新
    @Override
    public void onBeaconServiceConnect() {
//        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);// 调用getSystemService()方法来获取LocationManager对象
//        String provider = LocationManager.NETWORK_PROVIDER;
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        Location location = locationManager.getLastKnownLocation(provider);// 调用getLastKnownLocation()方法获取当前的位置信息
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
                Log.d(TAG, "收集了" + collection.size() + "条数据");
                if (collection.size() > 0) {
                    List<Beacon> beacons = new ArrayList<>();
                    for (Beacon beacon : collection) {
                            beacons.add(beacon);
                    }
                    if (beacons.size() > 0) {
                        Collections.sort(beacons, new Comparator<Beacon>() {
                            public int compare(Beacon arg0, Beacon arg1) {
                                return arg1.getRssi()-arg0.getRssi();
                            }
                        });
                        int distance = -1;

                        Beacon nearBeacon = beacons.get(0);
                        if (!nearBeacon.getId1().equals("50765cb7-d9ea-4e21-99a4-fa879613a492")) { // 目标beacon
                            beaconId = nearBeacon.getId1().toString();
                            int Rssi = nearBeacon.getRssi();
                            Log.i(TAG, "didRangeBeaconsInRegion: "+ beacons.toString());

                            wifiList.clear();
                            boolean flag = wifiUtil.startScan();
                            wifiList = wifiUtil.getWifiList();
                            server.update(nearBeacon, wifiList);
//                            double longitude = 0, latitude = 0;
//                            if (location != null) {
//                                longitude = location.getLongitude();
//                                latitude = location.getLatitude();
//                            }
//
//                            myWiFiInfo temp = new myWiFiInfo(longitude, latitude, Build.MODEL, wifiList, wifiId, beaconId, distance);
//                            myWiFiInfos.add(temp);
//                            @SuppressLint("SimpleDateFormat")
//                            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                            String timeStamp = df.format(new Date());
//                            Log.i(TAG, "Collecting a message at " + timeStamp + " on" +
//                                    " longitude: " + Double.toString(longitude) + " latitude: " + Double.toString(latitude));

                            for(ScanResult item : wifiList){
                                if(!wifiId.contains(item.BSSID)) wifiId.add(item.BSSID);
                            }
                        }
                    }
                }
            }
        });
        beaconManager.startRangingBeacons(new Region("all-region-beacon", null, null, null));
    }

    // 平衡模式蓝牙扫描
    public void balencedScan(View view) throws InterruptedException {
        if(!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
        }
        startTime = System.currentTimeMillis();
        scanner.startScan(null, balencedSetting, scanCallback);
    }

    // 低时延模式蓝牙扫描
    public void lowLatencyScan(View view) throws InterruptedException {
        if(!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
        }
        startTime = System.currentTimeMillis();
        scanner.startScan(null, lowLantencySetting, scanCallback);
    }

    // 利用FiND进行加速扫描
    public void FiNDScan(View view) throws IOException, InterruptedException {
        startTime = System.currentTimeMillis();
        server.setBeacon(startTime, wifiUtil, useTime);
    }

    // 保存收集的数据
    public void Save(View view) throws IOException {
        Collections.sort(useTime);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(openFileOutput("data.csv", Context.MODE_APPEND)));
        int len = useTime.size();
        for (int i = 0; i < len; i ++ ) {
            out.write(Long.toString(useTime.get(i)));
            out.newLine();
        }
        textView.setText("保存文件成功！");
        useTime.clear();
        out.close();
    }

    // 析构函数
    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    // 界面的创建
    private void transparentStatusBar() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

}

