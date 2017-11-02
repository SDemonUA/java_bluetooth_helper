package com.sokoldv.bthost;

import sun.awt.image.ToolkitImage;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.tools.Tool;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Paths;

public class ActionManager {
    public static void main(String[] args) {


        JFrame window = initWindow();

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

        // Buttons
        String[] icons = new String[]{ "flash", "vibrate", "photo" };
        String[] titles = new String[]{ "Ready", "Steady", "Go" };
        int[] borderColors = new int[]{ 0xc9def3, 0xc9ead1, 0xfbd2d0 };

        for (int i = 0; i < 3; i++) {
            JButton btn = new JButton(titles[i], new ImageIcon(String.format("./assets/%s.png", icons[i])));
            btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            btn.setHorizontalTextPosition(SwingConstants.CENTER);
            btn.setFont(new Font("sans-serif", Font.PLAIN, 24));
            btn.setIconTextGap(40);
            btn.setDefaultCapable(false);

            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            CompoundBorder border = BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(borderColors[i]), 2, true),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20));

            btn.setBorder(border);
            btn.setBackground(new Color(0xFAFAFA));

            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    super.mousePressed(e);
                    System.out.println("pressed");
                    ((JButton) e.getComponent()).setContentAreaFilled(true);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    super.mouseReleased(e);
                    System.out.println("released");
                    ((JButton) e.getComponent()).setContentAreaFilled(false);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    super.mouseEntered(e);
                    System.out.println("entered");
                    ((JButton) e.getComponent()).setBorderPainted(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    super.mouseExited(e);
                    System.out.println("exited");
                    ((JButton) e.getComponent()).setBorderPainted(false);
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

class MyButton extends JButton{
    public MyButton() {
        super();
    }

    public MyButton(Icon icon) {
        super(icon);
    }

    public MyButton(String text) {
        super(text);
    }

    public MyButton(Action a) {
        super(a);
    }

    public MyButton(String text, Icon icon) {
        super(text, icon);
    }

    @Override
    protected void paintBorder(Graphics g) {
        super.paintBorder(g);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Dimension originalSize = super.getPreferredSize();
        int gap = (int) (originalSize.height * 0.6);
        int x = originalSize.width + gap;
        int y = gap;
        int diameter = originalSize.height - (gap * 2);

        g.setColor(Color.cyan);
        g.fillOval(x, y, diameter, diameter);

    }
}
