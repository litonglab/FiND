package com.example.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.server.Server;
import com.example.wifidemo.R;
import com.example.wifi.WifiUtil;
import com.example.wifi.myWiFiInfo;
import com.github.mikephil.charting.charts.BarChart;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.github.mikephil.charting.data.*;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {
    private WifiUtil wifiUtil;
    ArrayAdapter<String> adapter = null;

    // 全局变量设置
    private List<myWiFiInfo> myWiFiInfos = new ArrayList<>();
    private WifiManager wifiManager;
    private final List<String> wifiId = new ArrayList<>();
    private List<ScanResult> wifiList = new ArrayList<>();
    private final ArrayList<String> bledata_compare = new ArrayList<>();
    private final ArrayList<String> bledata = new ArrayList<>();
    private final List<Long> useTime = new ArrayList<>();
    private ScanSettings lowLantencySetting = null;
    private ScanSettings balencedSetting = null;
    private BluetoothLeScanner scanner = null;
    private final Server server = new Server();
    private BeaconManager beaconManager;
    private static final String TAG = "MainActivity";
    private TextView textView = null;
    private String beaconId = "", mode = "";
    private final String UUID = "00000000-0000-0000-0000-000000000000";
    long startTime;
    int scanCount = 10;
    private BluetoothAdapter bluetoothAdapter = null;

    private final Handler handler = new Handler();
    BarChart chart;

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint({"MissingPermission", "SetTextI18n"})
        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            super.onScanResult(callbackType, result);
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
    private static final String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    // 初始化设置
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        wifiUtil = new WifiUtil(this);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        lowLantencySetting = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        balencedSetting = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        scanner = bluetoothAdapter.getBluetoothLeScanner();

        requestLocationPermissions();
        verifyStoragePermissions(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, this.bledata);
        textView = (TextView) findViewById(R.id.textView1);
        textView.setText("");
        Spinner spinner = findViewById(R.id.spinner);
        String[] options = {"FiND", "Balanced", "Low Latency"};
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mode = options[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        initChart();
        initBeacon();
        Objects.requireNonNull(getSupportActionBar()).hide();
        transparentStatusBar();
    }

    private void initChart() {
        chart = findViewById(R.id.chart);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);
        chart.getDescription().setEnabled(false);
        chart.setMaxVisibleValueCount(60);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
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
        beaconManager.bind(this);
    }

    // beacon扫描更新
    @Override
    public void onBeaconServiceConnect() {
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
                                return arg1.getRssi() - arg0.getRssi();
                            }
                        });
                        int distance = -1;

                        Beacon nearBeacon = beacons.get(0);
                        if (!nearBeacon.getId1().equals("50765cb7-d9ea-4e21-99a4-fa879613a492")) { // 目标beacon
                            beaconId = nearBeacon.getId1().toString();
                            int Rssi = nearBeacon.getRssi();
                            Log.i(TAG, "didRangeBeaconsInRegion: " + beacons.toString());

                            wifiList.clear();
                            boolean flag = wifiUtil.startScan();
                            wifiList = wifiUtil.getWifiList();
                            server.update(nearBeacon, wifiList);
                            for (ScanResult item : wifiList) {
                                if (!wifiId.contains(item.BSSID)) wifiId.add(item.BSSID);
                            }
                        }
                    }
                }
            }
        });
        beaconManager.startRangingBeacons(new Region("all-region-beacon", null, null, null));
    }

    // 利用FiND进行加速扫描
    public void FiNDScan(View view) throws IOException {
        startTime = System.currentTimeMillis();
        server.setBeacon(startTime, wifiUtil, useTime);
    }

    @SuppressLint("MissingPermission")
    public void scan(View view) throws IOException {
        if (mode.equals("FiND")) {
            for (int i = 0; i < scanCount; i ++ ) {
                FiNDScan(view);
            }
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
            startTime = System.currentTimeMillis();
            ScanSettings settings;
            if (mode.equals("Balanced")) {
                settings = balencedSetting;
            } else {
                settings = lowLantencySetting;
            }
            for (int i = 0; i < scanCount; i ++ ) {
                delayedScan(settings, 10000L * i);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void delayedScan(ScanSettings settings, long time) {
        startTime = System.currentTimeMillis();
        handler.postDelayed(() -> scanner.startScan(null, settings, scanCallback), time);
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice() {

    }
    public void visualization(View view) {
        Collections.sort(useTime);
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 1; i < useTime.size(); i++) {
            entries.add(new BarEntry(i, useTime.get(i)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Time");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLUE);

        BarData data = new BarData(dataSet);
        chart.setData(data);
        chart.invalidate();
    }

    public void clear(View view) {
        useTime.clear();
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

