package com.example.server;

import android.app.Activity;
import android.net.wifi.ScanResult;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.altbeacon.beacon.Beacon;

import com.example.wifi.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private static final String HOST = "39.105.200.134";
    private static final int PORT = 5000;
    private Socket socket = null;

    // 客户端更新调用
    public void update(Beacon beacon, List<ScanResult> wifiList) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(HOST, PORT);
                    OutputStream outputStream = socket.getOutputStream();
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("update\n");
                    buffer.append(beacon.getId2().toString());
                    buffer.append(beacon.getId3().toString());
                    buffer.append(beacon.getId1().toString());
                    buffer.append('\n');
                    for (ScanResult scanResult : wifiList) {
                        buffer.append(new Wifi(scanResult.BSSID, scanResult.level));
                        buffer.append('\n');
                    }
                    String message = new String(buffer);
                    outputStream.write(message.getBytes(StandardCharsets.UTF_8));
                    socket.shutdownOutput();

                    outputStream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 客户端查询调用
    public void setBeacon(long startTime, WifiUtil wifiUtil, List<Long> FiNDUseTime) throws IOException {
        List<String> result = new ArrayList<>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean flag = true;
                    long cnt = 0;
                    do {
                        socket = new Socket(HOST, PORT);
                        OutputStream outputStream = socket.getOutputStream();
                        InputStream inputStream = socket.getInputStream();
                        wifiUtil.startScan();
                        List<ScanResult> wifiList = wifiUtil.getWifiList();
                        result.clear();

                        StringBuilder buffer = new StringBuilder();
                        buffer.append("getResult\n");
                        for (ScanResult scanResult : wifiList) {
                            buffer.append(new Wifi(scanResult.BSSID, scanResult.level));
                            buffer.append('\n');
                        }
                        String message = new String(buffer);
                        outputStream.write(message.getBytes(StandardCharsets.UTF_8));

                        socket.shutdownOutput();
                        byte[] bytes = new byte[1024];
                        int len;
                        while ((len = inputStream.read(bytes)) != -1) {
                            result.add(new String(bytes, 0, len, StandardCharsets.UTF_8));
                        }
                        if (result.size() == 0 || result.get(0).length() == 0) continue;
                        if (result.get(0).charAt(0) == '0' && result.get(0).charAt(1) == '0') {
                            flag = false;
                        }
                        inputStream.close();
                        outputStream.close();
                    } while (flag);

                    socket.close();
                    long endTime = System.currentTimeMillis();
                    long useTime = endTime - startTime;
                    FiNDUseTime.add(useTime);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
