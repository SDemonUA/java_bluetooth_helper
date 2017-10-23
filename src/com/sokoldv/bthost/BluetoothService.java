package com.sokoldv.bthost;

import javax.bluetooth.*;
import javax.microedition.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class BluetoothService extends Thread {
    private final UUID uuid;

    BluetoothService(UUID uuid){
        this.uuid = uuid;
    }

    @Override
    public void run() {
        LocalDevice lc;
        try {
            lc = LocalDevice.getLocalDevice();
        } catch (BluetoothStateException e) {
            e.printStackTrace();
            return;
        }

        String connection_url = String.format("btspp://localhost:3;",
                lc.getBluetoothAddress());

        try {
            lc.setDiscoverable(DiscoveryAgent.GIAC);
        } catch (BluetoothStateException e) {
            e.printStackTrace();
        }

        ServiceRecord serviceRecord;
        Connection connection;
        try {
            connection = Connector.open(connection_url);
            serviceRecord = lc.getRecord(connection);
            setAttributes(serviceRecord);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        StreamConnection sc;
        try {
            sc = ((StreamConnectionNotifier) connection).acceptAndOpen();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        InputStream in;
        try {
            in = sc.openInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        byte[] buffer = new byte[1024];
        short read = 0;
        while (true){
            try {
                read = (short) in.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            System.out.print(String.format("Received:%s\n", new String(Arrays.copyOf(buffer, read))));
        }
    }

    private void setAttributes(ServiceRecord sr) {
//        DataElement name = new DataElement(DataElement.STRING, "Test Server");
//        sr.setAttributeValue(0b0000, name);
    }
}
