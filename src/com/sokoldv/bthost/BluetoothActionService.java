package com.sokoldv.bthost;

import com.intel.bluetooth.BluetoothConsts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
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

/**
 * Created by UnetDeveloper on 02.11.2017.
 */

class BluetoothActionService implements DiscoveryListener {
    private final RemoteDevice remoteDevice;

    private OnDisconnectListener disconnectListener;
    private OnActionChangeListener changeListener;

    private UUID uuid = new UUID("7f07d9800c8b4a13a087223c0f5e6109", false);
    private LocalDevice localDevice;
    private DiscoveryAgent agent;
    private String serviceUrl = null;

    private Vector<RemoteDevice> devices = new Vector<>();
    private final Object lock = new Object();

    private StreamConnection connection;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private ConnectionThread connectionThread;
    private Thread checkThread;

//    private final Thread pingThread;

    interface ActionCallback {
        void onActionReply(String newAction);
    }

    BluetoothActionService(RemoteDevice remoteDevice) throws IOException {
        localDevice = LocalDevice.getLocalDevice();
        agent = localDevice.getDiscoveryAgent();
        this.remoteDevice = remoteDevice;

//        agent.startInquiry(DiscoveryAgent.GIAC, this);
//        synchronized (lock){
//            try { lock.wait(); }
//            catch (InterruptedException e) { e.printStackTrace(); }
//        }

        agent.searchServices(null, new UUID[]{uuid}, remoteDevice, this);
        synchronized (lock){
            try { lock.wait(); }
            catch (InterruptedException e) { e.printStackTrace(); }
        }

//        serviceUrl = getServiceUrl();
        if (serviceUrl == null) {
            System.out.println("No service available");
            System.exit(0);
        }
        else {
            System.out.println("Connecting to : "+serviceUrl);
        }

        connectionThread = new ConnectionThread(serviceUrl, answer -> {
            System.out.println("Disconnected");
            if (disconnectListener != null)
                disconnectListener.onDisconnect();

            connectionThread = null;
        });

        connectionThread.start();

        System.out.println("Connected");

        checkThread = new Thread(() -> {
            Object lock = new Object();
            while (connectionThread != null){
                connectionThread.addTask("GET:ACTION", (newAction) -> {
                    changeListener.onChange(newAction);
                    synchronized (lock){
                        lock.notify();
                    }
                });
                synchronized (lock){
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        checkThread.start();

//        connection = (StreamConnection) Connector.open(serviceUrl, Connector.READ_WRITE);
//        outputStream = connection.openDataOutputStream();
//        inputStream = connection.openDataInputStream();
    }

    String getServiceUrl(){
        if (serviceUrl != null)
            return serviceUrl;

        try {
            serviceUrl = agent.selectService(uuid, ServiceRecord.AUTHENTICATE_NOENCRYPT, false);
        } catch (BluetoothStateException e) {
            e.printStackTrace();
        }

        System.out.println(serviceUrl);
        return serviceUrl;
    }

    void setAction(final String action, ActionCallback callback){
        if (connectionThread != null)
            connectionThread.addTask("SET:ACTION:"+action, callback::onActionReply);
        /*
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
        */
    }

    synchronized void getAction(final ActionCallback callback){
        if (connectionThread != null)
            connectionThread.addTask("GET:ACTION", callback::onActionReply);

        /*
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
        */
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
        for (ServiceRecord record : servRecord) {
            String url = record.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, true);
            if (url.startsWith(BluetoothConsts.PROTOCOL_SCHEME_RFCOMM)){
                serviceUrl = url;
                break;
            }

            System.out.println("Not match Url: "+record.getConnectionURL(ServiceRecord.AUTHENTICATE_NOENCRYPT, true));
        }
    }

    interface OnDisconnectListener {
        void onDisconnect();
    }
    void setOnDisconnectListener(OnDisconnectListener listener){
        disconnectListener = listener;
    }

    interface OnActionChangeListener {
        void onChange(String newAction);
    }
    void setOnActionChangeListener(OnActionChangeListener listener){
        changeListener = listener;
    }

    @Override
    public void serviceSearchCompleted(int transID, int respCode) {
        System.out.println("serviceSearchCompleted");
        synchronized (lock){
            lock.notify();
        }
    }

    @Override
    public void inquiryCompleted(int discType) {
        System.out.println("inquiryCompleted");
        synchronized (lock){
            lock.notify();
        }
    }
}


class ConnectionThread extends Thread {
    public void addTask(String action, Callback callback) {
        System.out.println("Add task to queue");
        synchronized (queue){

            queue.add(new Cmd(action, callback));
        }
    }

    static class Cmd {
        private String action;
        private Callback callback;

        public Cmd(String action, Callback callback) {
            this.action = action;
            this.callback = callback;
        }
    }

    class AnswerTask implements Runnable{
        private final String answer;
        private final Callback callback;

        AnswerTask(String answer, Callback callback){
            this.answer = answer;
            this.callback = callback;
        }

        @Override
        public void run() {
            callback.onCallback(answer.split(":")[2]);
        }
    }

    interface Callback {
        void onCallback(String answer);
    }

    private final StreamConnection connection;
    private final DataOutputStream outputStream;
    private final DataInputStream inputStream;
    private final Queue<Cmd> queue = new LinkedList<>();
    private final Callback disconnectListener;

    ConnectionThread(String connectUrl, Callback disconnectListener){
        this.disconnectListener = disconnectListener;

        StreamConnection tmpConn = null;
        DataOutputStream tmpOut = null;
        DataInputStream tmpIn = null;

        try {
            tmpConn = (StreamConnection) Connector.open(connectUrl, Connector.READ_WRITE);
            tmpOut = tmpConn.openDataOutputStream();
            tmpIn = tmpConn.openDataInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        connection = tmpConn;
        outputStream = tmpOut;
        inputStream = tmpIn;
        System.out.println("Started");
    }

    @Override
    public void run() {

        System.out.println("Run");
        top:while (true){
            Cmd cmd;
            while ((cmd = queue.poll()) != null){
                System.out.println("execute cmd "+cmd.action);
                try {
                    outputStream.writeByte(1);
                    outputStream.writeInt(cmd.action.length());
                    outputStream.writeUTF(cmd.action);

                    inputStream.readByte();
                    inputStream.readInt();
                    String answer = inputStream.readUTF();

                    new Thread(new AnswerTask(answer, cmd.callback)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                    break top;
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        onDisconnect();
    }

    private void onDisconnect() {
        try {
            connection.close();
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        disconnectListener.onCallback("DISCONNECTED");
    }

    public void cancel(){
        onDisconnect();
    }
}
