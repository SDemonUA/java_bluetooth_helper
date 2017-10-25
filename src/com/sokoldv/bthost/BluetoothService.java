package com.sokoldv.bthost;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class BluetoothService extends Thread {
    private final UUID uuid;
    private ServiceRecord serviceRecord;

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

        try {
            lc.setDiscoverable(DiscoveryAgent.GIAC);
        } catch (BluetoothStateException e) {
            e.printStackTrace();
        }

        StreamConnectionNotifier connection;
        try {
            connection = (StreamConnectionNotifier) Connector.open(String.format("btspp://localhost:%s;name=Test", uuid.toString()));
            serviceRecord = lc.getRecord(connection);
            setAttributes(serviceRecord);
            lc.updateRecord(serviceRecord);
            System.out.println(
                    String.format("Connection created : %s", uuid.toString())
            );

        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return;
        }

        StreamConnection sc;
        try {
            sc = connection.acceptAndOpen();
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
        DataElement name = new DataElement(DataElement.STRING, "Test Server");

        sr.setAttributeValue(0x0003, new DataElement(DataElement.UUID, uuid));
    }
}
