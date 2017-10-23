package com.sokoldv.bthost;

import com.intel.bluetooth.btspp.Connection;

import javax.bluetooth.*;
import java.io.IOException;
import java.util.Scanner;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;

public class Main {
    static LocalDevice localDevice = null;
    static Scanner sc = null;
    static boolean busy = false;

    public static void main(String[] args) {
        try {
            localDevice = LocalDevice.getLocalDevice();
        } catch (BluetoothStateException e) {
            e.printStackTrace();
            return;
        }

        localInfo();

        new BluetoothService(new UUID("7f07d9800c8b4a13a087223c0f5e6109", false)).start();

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
        for (int i = 0; i < devices.length; i++) {
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
                new UUID("0003", true), // RFCOMM
//                new UUID("0008", true), // OBEX
//                new UUID("000C", true), // HTTP
//                new UUID("0100", true), // L2CAP
//                new UUID("000F", true), // BNEP
//                new UUID("0111", true),  // Serial Port
//                new UUID("1108", true), // headset a2dp
//                new UUID("1112", true), // headset hsp
//                new UUID("7f07d980-0c8b-4a13-a087-223c0f5e6109", false)
        };
        try {
            localDevice.getDiscoveryAgent().searchServices(null, uuids, device, new MyDiscoveryListener());
            busy = true;
        } catch (BluetoothStateException e) {
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
