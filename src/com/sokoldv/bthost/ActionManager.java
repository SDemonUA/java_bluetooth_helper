package com.sokoldv.bthost;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
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
        bluetoothActionService = new BluetoothActionService();
        JFrame window = initWindow();


        new Thread(() -> {
            final Object lock = new Object();

            while (true){
                bluetoothActionService.getAction(newAction -> {
                    onActionChange(newAction);

                    synchronized (lock){
                        lock.notify();
                    }
                });

                synchronized (lock){
                    try { lock.wait(); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    static void onActionChange(String action){
        for (int i = 0; i < actions.length; i++) {
            if (actions[i].equalsIgnoreCase(action)){
                buttons[i].grabFocus();
            }
        }
    }

    static JFrame initWindow(){
        JFrame window = new JFrame("Unet Control Panel");

        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setLayout(new BorderLayout());

//        window.setUndecorated(true);

        window.setResizable(false);
        // Now we can count window size with its system frame and our bottomBar
//        Dimension dim = new Dimension(332*3+10*4, 600);// map.getPreferredSize();
//        Dimension winPref = window.getSize();
//
//        int width = (int) (dim.getWidth() + winPref.getWidth());//-bottomBar.getWidth());
//        int height = (int) (dim.getHeight() + window.getPreferredSize().getHeight());
//        window.setSize(width, height);

        int padding = 40;
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, padding, padding));
        buttonPanel.setBorder(new EmptyBorder(padding, padding, padding, padding));
        buttonPanel.setBackground(Color.WHITE);

        for (int i = 0; i < 3; i++) {
            ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(ActionManager.class.getResource(String.format("/%s.png", icons[i]))));
            JButton btn = new JButton(titles[i], icon);
            buttons[i] = btn;

            btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            btn.setHorizontalTextPosition(SwingConstants.CENTER);
            btn.setFont(new Font("sans-serif", Font.PLAIN, 24));
            btn.setIconTextGap(40);
            btn.setDefaultCapable(true);

            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(true);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            CompoundBorder border = BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(borderColors[i]), 2, true),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20));

            btn.setBorder(border);
            btn.setBackground(Color.WHITE);

            btn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JButton source = (JButton) e.getSource();
                    System.out.println(source.getText());
                    String item = source.getText();
                    for (int i1 = 0; i1 < titles.length; i1++) {
                        if (titles[i1].equals(item)){
                            bluetoothActionService.setAction(actions[i1], ActionManager::onActionChange);
                            break;
                        }
                    }
                }
            });
//            btn.addMouseListener(new MouseAdapter() {
//                @Override
//                public void mousePressed(MouseEvent e) {
//                    super.mousePressed(e);
//                    System.out.println("pressed");
//                }
//
//                @Override
//                public void mouseReleased(MouseEvent e) {
//                    super.mouseReleased(e);
//                    System.out.println("released");
//                }
//
//                @Override
//                public void mouseEntered(MouseEvent e) {
//                    super.mouseEntered(e);
//                    System.out.println("entered");
//                    ((JButton) e.getComponent()).setBorderPainted(true);
//                }
//
//                @Override
//                public void mouseExited(MouseEvent e) {
//                    super.mouseExited(e);
//                    System.out.println("exited");
//                    ((JButton) e.getComponent()).setBorderPainted(false);
//                }
//            });

            btn.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    super.focusGained(e);
                    JButton button = (JButton) e.getComponent();
                    button.setBackground(new Color(0xfafafa));
                    button.setBorderPainted(true);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    super.focusLost(e);
                    JButton button = (JButton) e.getComponent();
                    button.setBackground(Color.WHITE);
                    button.setBorderPainted(false);
                }
            });

            buttonPanel.add(btn);
        }

        window.add(buttonPanel, BorderLayout.CENTER);
        window.setVisible(true);
        window.setSize(window.getPreferredSize());

        return window;
    }
}

