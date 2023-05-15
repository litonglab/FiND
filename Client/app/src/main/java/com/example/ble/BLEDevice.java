package com.example.ble;

public class BLEDevice {

    public String bleAddress;

    @Override
    public boolean equals(Object obj){
        if (obj instanceof BLEDevice){
            return bleAddress.equals(((BLEDevice) obj).bleAddress);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode(){
        return bleAddress.hashCode();
    }
}

