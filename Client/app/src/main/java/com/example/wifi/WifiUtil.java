package com.example.wifi;

import android.annotation.SuppressLint;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.List;

public class WifiUtil {
    private final WifiManager mWifiManager;

    private final WifiInfo mWifiInfo;
    private List<ScanResult> mWifiList;
    private List<WifiConfiguration> mWifiConfigurations;
    WifiLock mWifiLock;
    Context mContext;

    public WifiUtil(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        mWifiInfo = mWifiManager.getConnectionInfo();
    }

    public void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    public boolean isConn() {
        ConnectivityManager connManager = (ConnectivityManager) mContext
                .getSystemService(mContext.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    public int checkState() {
        return mWifiManager.getWifiState();
    }

    public void acquireWifiLock() {
        mWifiLock.acquire();
    }

    public void releaseWifiLock() {
        if (mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    public void createWifiLock() {
        mWifiLock = mWifiManager.createWifiLock("test");
    }

    public List<WifiConfiguration> getConfiguration() {
        return mWifiConfigurations;
    }

    public boolean connetionConfiguration(int index) {
        if (index > mWifiConfigurations.size()) {
            return false;
        }
        return mWifiManager.enableNetwork(mWifiConfigurations.get(index).networkId, true);
    }
    @SuppressLint("MissingPermission")
    public boolean startScan() {
        boolean flag = mWifiManager.startScan();
        mWifiList = mWifiManager.getScanResults();
        return flag;
    }

    public List<ScanResult> getWifiList() {
        return mWifiList;
    }

    public StringBuffer lookUpScan() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mWifiList.size(); i++) {
            sb.append("Index_").append(Integer.valueOf(i + 1).toString()).append(":");
            sb.append((mWifiList.get(i)).toString()).append("\n");
        }
        return sb;
    }

    @SuppressLint("MissingPermission")
    public String getMacAddress() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress();
    }

    public String getBSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID();
    }

    public int getIpAddress() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress();
    }

    public String getSSID() {
        return (mWifiInfo == null) ? "" : mWifiInfo.getSSID().replace("\"", "");
    }

    public int getNetWordId() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
    }

    public String getWifiInfo() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();
    }

    public String getGateway() {
        return mWifiInfo == null ? "NULL"
                : intToIp(mWifiManager.getDhcpInfo().gateway);
    }

    private String intToIp(int paramInt) {
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "."
                + (0xFF & paramInt >> 16) + "." + (0xFF & paramInt >> 24);
    }

    public boolean addNetWork(WifiConfiguration configuration) {
        int wcgId = mWifiManager.addNetwork(configuration);
        return mWifiManager.enableNetwork(wcgId, true);
    }

    public void disConnectionWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();

    }

    public WifiConfiguration createWifiInfo(String SSID, String Password,
                                            int TYPE) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (TYPE == 1) // WIFICIPHER_NOPASS
        {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        if (TYPE == 2) // WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (TYPE == 3) // WIFICIPHER_WPA  WPA2
        {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    public int getType(String capabilities)
    {
        if (capabilities.contains("WPA"))
            return 3;
        else if (capabilities.contains("WEP"))
            return 2;
        else
            return 1;
    }

    private WifiConfiguration IsExsits(String SSID) {
        @SuppressLint("MissingPermission") List<WifiConfiguration> existingConfigs = mWifiManager
                .getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }
}

