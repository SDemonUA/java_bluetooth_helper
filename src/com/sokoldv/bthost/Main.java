package com.sokoldv.bthost;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

public class Main {
    static LocalDevice localDevice = null;
    static Scanner sc = null;
    static boolean busy = false;

    public static void main(String[] args) throws IOException {
        try {
            localDevice = LocalDevice.getLocalDevice();
        } catch (BluetoothStateException e) {
            e.printStackTrace();
            return;
        }

        localInfo();

        //connect to the server and send a line of text
        StreamConnection streamConnection=(StreamConnection)Connector.open("btspp://38D547A87026:9;authenticate=true;encrypt=true;master=false");
        //send string
        try{
            DataOutputStream outputStream = streamConnection.openDataOutputStream();
            outputStream.writeByte(3);
            outputStream.writeByte(33);
            outputStream.writeByte(2);
            outputStream.flush();


            //read response
//            InputStream inStream=streamConnection.openInputStream();
//
//            BufferedReader bReader2=new BufferedReader(new InputStreamReader(inStream));
//            String lineRead=bReader2.readLine();
//            System.out.println(lineRead);

            Scanner sc = new Scanner(System.in);
            sc.next();
            System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
        }
//        new BluetoothService(new UUID("7f07d9800c8b4a13a087223c0f5e6109", false)).start();

        System.out.print("> ");
        sc = new Scanner(System.in);

        while (true){
            String command = sc.nextLine();
            String[] cmdParts = command.trim().split(" ");

            switch (cmdParts[0]){
                case "h":
                case "help":
                    printHelp();
                    break;
                case "me":
                    localInfo();
                    break;
                case "dv":
                case "devices":
                    listDevices();
                    break;
                case "sr":
                case "services":
                    listServices();
                    break;
                case "test":
                    connectToPhone();
                    break;
                case "exit":
                    return;
            }

            while (busy){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.print("> ");
        }
    }

    private static void connectToPhone() {
        int paired = 0;
        RemoteDevice[] devices = localDevice.getDiscoveryAgent().retrieveDevices(DiscoveryAgent.PREKNOWN);
        System.out.println("Devices:");
        if (devices != null) for (int i = 0; i < devices.length; i++) {
            System.out.print(i+": ");
            System.out.println(BTDevice.describe(devices[i]));
        }

        System.out.print("Select device: \r");

        RemoteDevice device = null;
        try {
            int option = sc.nextInt();
            if (devices.length-1 >= option)
                device = devices[option];
        } catch (Exception e){
            e.printStackTrace();
        }


        if (device == null) return;
        try {
            System.out.println(String.format("%s selected", device.getFriendlyName(true)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String address = device.getBluetoothAddress();
        System.out.println(address);
        return;
    }

    private static void localInfo() {
        System.out.println(String.format("Local BT address: %s\nName: %s\nDiscoverable: %s",
                localDevice.getBluetoothAddress(),
                localDevice.getFriendlyName(),
                String.valueOf(localDevice.getDiscoverable())
        ));
    }

    private static void listServices() {
        int paired = 0;
        RemoteDevice[] devices = localDevice.getDiscoveryAgent().retrieveDevices(paired);
        System.out.println("Devices:");
        if (devices != null) for (int i = 0; i < devices.length; i++) {
            try {
                System.out.println(String.format("%d - %s (%s)", i, devices[i].getFriendlyName(false), devices[i].getBluetoothAddress()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.print("Select device: ");

        RemoteDevice device = null;
        try {
            int option = sc.nextInt();
            if (devices.length-1 >= option)
                device = devices[option];
        } catch (Exception e){
            e.printStackTrace();
        }


        if (device == null) return;
        try {
            System.out.println(String.format("%s selected", device.getFriendlyName(true)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int[] attrSet = new int[20];
        UUID[] uuids = new UUID[] {
//                new UUID("0001", true), // SDP
//                new UUID("0003", true), // RFCOMM
//                new UUID("0008", true), // OBEX
//                new UUID("000C", true), // HTTP
//                new UUID("0100", true), // L2CAP
//                new UUID("000F", true), // BNEP
//                new UUID("0111", true),  // Serial Port
//                new UUID("1108", true), // headset a2dp
//                new UUID("1112", true), // headset hsp
                new UUID("7f07d9800c8b4a13a087223c0f5e6110", false)
        };
        try {
            localDevice.getDiscoveryAgent().searchServices(null, uuids, device, new MyDiscoveryListener());
            busy = true;
        } catch (BluetoothStateException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void listDevices() {
        System.out.println("Start device inquiry");
        try {
            boolean started = localDevice.getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, new MyDiscoveryListener());
            if (!started) System.out.println("Inquiry was not started");
            else busy = true;
        } catch (BluetoothStateException e) {
            e.printStackTrace();
        }
    }

    private static void printHelp() {
        System.out.println("\n======================================");
        System.out.println("help, h\tthis help\n");
        System.out.println("me\tlocal device info\n");
        System.out.println("devices, dv\tlist discoverable devices\n");
        System.out.println("services, sr\tlist discoverable services\n");
        System.out.println("exit");
        System.out.println("======================================\n");
    }

    private static void test (){
        String address = "38:D5:47:A8:70:26";

        try {
            Connector.open("btspp://38D547A87026;2");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    static DiscoveryListener discoveryListener = new MyDiscoveryListener;

    static class MyDiscoveryListener implements DiscoveryListener {
        @Override
        public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass deviceClass) {
            try {
                System.out.println(String.format("Remote device found\n===============\nAddress: %s\nName: %s\n",
                        remoteDevice.getBluetoothAddress(), remoteDevice.getFriendlyName(false)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void servicesDiscovered(int i, ServiceRecord[] serviceRecords) {
            System.out.println("Service records discovered:");
            for (ServiceRecord serviceRecord : serviceRecords) {
                if (serviceRecord == null) break;

                System.out.println(serviceRecord.getConnectionURL(ServiceRecord.AUTHENTICATE_ENCRYPT, true));
                for (int j : serviceRecord.getAttributeIDs()) {
                    DataElement value = serviceRecord.getAttributeValue(j);
                    System.out.println(String.format(" %d : %s", j, value.toString()));
                }
            }
        }

        @Override
        public void serviceSearchCompleted(int i, int i1) {
            System.out.println("============END============");
            busy = false;
        }

        @Override
        public void inquiryCompleted(int i) {
            System.out.println("============END============");
            busy = false;
        }
    };
}


class BTDevice{
    static String describe(RemoteDevice device){
        String descr = null;
        try {
            descr = String.format("%s (%s)", device.getFriendlyName(false), device.getBluetoothAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return descr;
    }
}
