import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Fingerprint {
    private String beaconId;
    private List<Wifi> wifiList = new ArrayList<Wifi>();
    public Fingerprint(String beacon, List<Wifi> list){
        beaconId = beacon;
        for(int i = 0; i < list.size(); i ++ ){
            Wifi wifi = list.get(i);
            wifiList.add(wifi);
        }
    }

    public String getBeaconId() {
        return beaconId;
    }

    public List<Wifi> getWifiList() {
        return wifiList;
    }

    public void print(){
        System.out.println("beaconId: " + beaconId);
        for (Wifi wifi : wifiList)
            System.out.println("wifiName: " + wifi.getName() + ' ' + "Rssi: " + wifi.getRssi());
    }
}
