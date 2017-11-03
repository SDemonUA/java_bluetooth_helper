package com.sokoldv.bthost;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import javax.bluetooth.*;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class ActionManager {
    private static BluetoothActionService bluetoothActionService;

    static JButton[] buttons = new JButton[3];
    static String[] icons = new String[]{ "flash", "vibrate", "photo" };
    static String[] actions = new String[]{ "flash", "vibrate", "photo" };
    static String[] titles = new String[]{ "Ready", "Steady", "Go" };
    static int[] borderColors = new int[]{ 0xc9def3, 0xc9ead1, 0xfbd2d0 };

    public static void main(String[] args) throws IOException {
        Window window = new Window();
        window.setButtonClickListener((event, button, id) -> bluetoothActionService.setAction(id, window::focusButton));

        for (int i = 0; i < actions.length; i++) {
            window.addButton(actions[i], icons[i], titles[i], borderColors[i]);
        }

        window.showWindow();

        RemoteDevice device = selectDeviceToConnectTo(window);



//        bluetoothActionService = new BluetoothActionService();
//
//
//        new Thread(() -> {
//            final Object lock = new Object();
//
//            while (true){
//                bluetoothActionService.getAction(newAction -> {
//                    window.focusButton(newAction);
//
//                    synchronized (lock){
//                        lock.notify();
//                    }
//                });
//
//                synchronized (lock){
//                    try { lock.wait(); }
//                    catch (InterruptedException e) { e.printStackTrace(); }
//                }
//
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
    }

    private static RemoteDevice selectDeviceToConnectTo(Window window) {
        JDialog dialog = new JDialog(window, "Select Device", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(200, 25));
        progressBar.setString("Scanning...");
        progressBar.setStringPainted(true);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.add(progressBar);

        JScrollPane body = new JScrollPane(content);

        dialog.setLayout(new BorderLayout());
        dialog.add(body, BorderLayout.CENTER);
        dialog.setSize(350, 300);
        dialog.setResizable(false);

        final RemoteDevice[] selectedDevice = {null};
        new Thread(() -> {
            BTUtils.getDevices(devices -> {
                if (devices == null) {
                    System.out.println("No devices found!");
                    System.exit(0);
                }

                JButton[] buttons = new JButton[devices.length];
                ActionListener actionListener = e -> {
                    JButton btn = (JButton) e.getSource();
                    for (int i = 0; i < buttons.length; i++) {
                        if (buttons[i] == btn){
                            selectedDevice[0] = devices[i];
                            dialog.setVisible(false);
                        }
                    }
                };

                for (int i = 0; i < devices.length; i++) {
                    RemoteDevice device = devices[i];
                    JButton option = new JButton();
                    String name = "????";
                    try {
                        name = device.getFriendlyName(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    option.setText(String.format("%s (%s)", name, device.getBluetoothAddress()));
                    option.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    option.setSize(300, 30);

                    option.setBorderPainted(false);
                    option.setContentAreaFilled(false);
                    option.setMargin(new Insets(5, 5, 5, 5));
                    buttons[i] = option;
                    option.addActionListener(actionListener);
                }

                content.remove(progressBar);
                for (JButton button : buttons) {
                    content.add(button);
                }

                dialog.setVisible(true);
            });
        }).start();

        dialog.setVisible(true);

        return selectedDevice[0];
    }
}

class Window extends JFrame {

    private JPanel body;
    private int padding = 40;
    private Color btnActiveColor = new Color(0xfafafa);
    private Color btnInactiveColor = Color.WHITE;

    private HashMap<String, JButton> buttonsMap = new HashMap<>();

    private OnButtonClickListener btnClickListener;

    Window(){
        super();

        setTitle("Unet Control Panel");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        initBody();
    }

    private void initBody() {
        body = new JPanel(new GridLayout(1, 3, padding, padding));
        body.setBorder(new EmptyBorder(padding, padding, padding, padding));
        body.setBackground(Color.WHITE);

        add(body, BorderLayout.CENTER);
    }

    public void addButton(String id, String iconFile, String title, int bgColor){

        ImageIcon icon = new ImageIcon(
                Toolkit.getDefaultToolkit().getImage(
                            this.getClass().getResource(String.format("/%s.png", iconFile)
                        )
                )
        );
        JButton btn = new JButton(title, icon);
        buttonsMap.put(id, btn);

        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);

        btn.setFont(new Font("sans-serif", Font.PLAIN, 24));
        btn.setIconTextGap(padding);
        btn.setDefaultCapable(true);

        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);

        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        CompoundBorder border = BorderFactory.createCompoundBorder(
                new LineBorder(new Color(bgColor), 2, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20));

        btn.setBorder(border);
        btn.setBackground(btnInactiveColor);

        btn.addActionListener((e -> {
            System.out.println("On click: "+id);
            onBtnClick(e, btn, id);
        }));

        btn.addFocusListener(btnFocusListener);

        body.add(btn);
    }

    void focusButton(String id){
        if (buttonsMap.get(id) != null)
            buttonsMap.get(id).grabFocus();
    }

    FocusAdapter btnFocusListener = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            JButton btn = (JButton) e.getComponent();
            btn.setBackground(btnActiveColor);
            btn.setBorderPainted(true);
        }

        @Override
        public void focusLost(FocusEvent e) {
            JButton btn = (JButton) e.getComponent();
            btn.setBackground(btnInactiveColor);
            btn.setBorderPainted(false);
        }
    };

    interface OnButtonClickListener{
        void onClick(ActionEvent event, JButton button, String id);
    }
    public void setButtonClickListener(OnButtonClickListener listener){
        btnClickListener = listener;
    }
    public void removeButtonClickListener(){
        btnClickListener = null;
    }
    private void onBtnClick(ActionEvent event, JButton button, String id) {
        if (btnClickListener != null)
            btnClickListener.onClick(event, button, id);
    }

    public void showWindow() {
        setVisible(true);
        setSize(getPreferredSize());
    }

}

class BTUtils {
    static private final Object lock = new Object();
    static private Vector<RemoteDevice> devices;

    static private DiscoveryListener discoveryListener = new DiscoveryListener() {
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
            System.out.println("Inquiry ended");
            synchronized (lock){
                lock.notify();
            }
        }
    };

    interface OnGetDevicesCallback{
        void onGetDevices(RemoteDevice[] devices);
    }
    static public void getDevices(OnGetDevicesCallback callback) {
        LocalDevice localDevice = null;
        try {
            localDevice = LocalDevice.getLocalDevice();
        } catch (BluetoothStateException e) {
            e.printStackTrace();
            callback.onGetDevices(new RemoteDevice[0]);
            return;
        }

        DiscoveryAgent agent = localDevice.getDiscoveryAgent();

        try {
            devices = new Vector<>();
            System.out.println("Inquiry started");
            agent.startInquiry(DiscoveryAgent.GIAC, discoveryListener);
        } catch (BluetoothStateException e) {
            e.printStackTrace();
            callback.onGetDevices(new RemoteDevice[0]);
            return;
        }
        synchronized (lock){
            try{ lock.wait(); }
            catch (InterruptedException e){ e.printStackTrace(); }
        }

        System.out.println("Devices gotten");

        callback.onGetDevices(devices.toArray(new RemoteDevice[0]));
    }
}
