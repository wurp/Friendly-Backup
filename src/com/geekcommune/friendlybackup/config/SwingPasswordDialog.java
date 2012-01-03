package com.geekcommune.friendlybackup.config;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SwingPasswordDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private TextField passwordInput;

    public SwingPasswordDialog() {
        super(new JFrame(), "FriendlyBackup password request", true);
        Container parent = getParent();

        if (parent != null) {
            Dimension parentSize = parent.getSize();
            Point p = parent.getLocation();
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }
        
        JPanel messagePane = new JPanel();
        messagePane.add(new JLabel("Please enter your password"));
        getContentPane().add(messagePane);
        
        passwordInput = new TextField();
        messagePane.add(passwordInput);
        messagePane.setLayout(new GridLayout(2, 1));

        JPanel buttonPane = new JPanel();
        JButton button = new JButton("OK");
        buttonPane.add(button);
        button.addActionListener(this);
        getContentPane().add(buttonPane, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(300, 120));
        pack();
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        setVisible(false); 
        dispose(); 
    }

    public String getPassword() {
        return passwordInput.getText();
    }
}
