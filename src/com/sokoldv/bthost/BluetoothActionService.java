package com.sokoldv.bthost;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.swing.*;

/**
 * Created by UnetDeveloper on 02.11.2017.
 */

class BluetoothActionService implements DiscoveryListener {
    private UUID uuid = new UUID("7f07d9800c8b4a13a087223c0f5e6109", false);
    private LocalDevice localDevice;
    private DiscoveryAgent agent;
    private String serviceUrl = null;

    private Vector<RemoteDevice> devices = new Vector<>();
    private final Object lock = new Object();

    private final StreamConnection connection;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;

//    private final Thread pingThread;

    interface ActionCallback {
        void onActionReply(String newAction);
    }

    BluetoothActionService() throws IOException {
        localDevice = LocalDevice.getLocalDevice();
        agent = localDevice.getDiscoveryAgent();

        agent.startInquiry(DiscoveryAgent.GIAC, this);
        synchronized (lock){
            try { lock.wait(); }
            catch (InterruptedException e) { e.printStackTrace(); }
        }

        serviceUrl = getServiceUrl();
        if (serviceUrl == null) {
            System.out.println("No service available");
            System.exit(0);
        }

        connection = (StreamConnection) Connector.open(serviceUrl, Connector.READ_WRITE);
        outputStream = connection.openDataOutputStream();
        inputStream = connection.openDataInputStream();

//        pingThread = new Thread(() -> {
//            while (true){
//                synchronized (BluetoothActionService.this){
//                    try {
//                        String message = "PING";
//                        outputStream.writeByte(1);
//                        outputStream.writeInt(message.length());
//                        outputStream.writeUTF(message);
//
//                        inputStream.readByte();
//                        inputStream.readInt();
//
//                        String utf = inputStream.readUTF();
//                        if (!utf.equalsIgnoreCase("PING")){
//
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//
//                }
//
//                try {
//                    wait(2000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        pingThread.start();
    }

    synchronized String getServiceUrl(){
        if (serviceUrl != null)
            return serviceUrl;

        try {
            serviceUrl = agent.selectService(uuid, ServiceRecord.AUTHENTICATE_ENCRYPT, true);
        } catch (BluetoothStateException e) {
            e.printStackTrace();
        }

        System.out.println(serviceUrl);
        return serviceUrl;
    }

    synchronized void setAction(final String action, ActionCallback callback){

        new Thread(() -> {
//            String url = getServiceUrl();
//            StreamConnection connection = null;
            try {
//                connection = (StreamConnection) Connector.open(serviceUrl, Connector.WRITE);
//                DataOutputStream outputStream = connection.openDataOutputStream();

                String message = "SET:ACTION:"+action;
                outputStream.writeByte(1); // PACKET_STRING
                outputStream.writeInt(message.length());
                outputStream.writeUTF(message);

                inputStream.readByte();
                inputStream.readInt();
                String newAction = inputStream.readUTF();
                callback.onActionReply(newAction);

//                outputStream.close();
//                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            finally {
//                if (connection != null){
//                    try {
//                        connection.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
        }).start();
    }

    synchronized void getAction(final ActionCallback callback){
        new Thread(() -> {
//            String url = getServiceUrl();
//            StreamConnection connection = null;
            try {
//                connection = (StreamConnection) Connector.open(serviceUrl, Connector.READ_WRITE);
//                DataOutputStream outputStream = connection.openDataOutputStream();
//                DataInputStream inputStream = connection.openDataInputStream();

                String message = "GET:ACTION";
                outputStream.writeByte(1); // PACKET_STRING
                outputStream.writeInt(message.length());
                outputStream.writeUTF(message);

                byte packet_type = inputStream.readByte();
                if (packet_type != 1){
                    System.out.println("Server should reply with PACKET_STRING instead of "+packet_type);
                    return;
                }

                int len = inputStream.readInt();
                String msg = inputStream.readUTF();

                callback.onActionReply(msg.split(":")[2]);
                System.out.println("Server said "+msg);
//                outputStream.close();
//                inputStream.close();
//                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            finally {
//                if (connection != null){
//                    try {
//                        connection.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
        }).start();
    }

    private void printDevices() {
        int i=1;
        for (RemoteDevice device : devices) {
            System.out.print(String.format("#%d\t%s ", i++, device.getBluetoothAddress()));
            try {
                System.out.println(String.format("(%s)", device.getFriendlyName(false)));
            } catch (IOException e) {
                System.out.println();
                e.printStackTrace();
            }
        }
        System.out.println("end");
    }

    @Override
    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        devices.add(btDevice);
    }

    @Override
    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {

    }

    @Override
    public void serviceSearchCompleted(int transID, int respCode) {

    }

    @Override
    public void inquiryCompleted(int discType) {
        System.out.println("inquiryCompleted");
        synchronized (lock){
            lock.notify();
        }
    }
}
