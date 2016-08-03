package com.geekcommune.friendlybackup.config;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

public class SwingPassphraseDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private JPasswordField passphraseInput;

    public SwingPassphraseDialog() {
        super(new JFrame(), "FriendlyBackup passphrase request", true);
        Container parent = getParent();

        if (parent != null) {
            Dimension parentSize = parent.getSize();
            Point p = parent.getLocation();
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }
        
        JPanel messagePane = new JPanel();
        messagePane.add(new JLabel("Please enter your passphrase"));
        getContentPane().add(messagePane);
        
        passphraseInput = new JPasswordField(30);
        passphraseInput.addActionListener(this);
        messagePane.add(passphraseInput);
        messagePane.setLayout(new GridLayout(2, 1));

        JPanel buttonPane = new JPanel();
        JButton button = new JButton("OK");
        buttonPane.add(button);
        button.addActionListener(this);
        getContentPane().add(buttonPane, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        //setPreferredSize(new Dimension(300, 120));
        pack();
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        setVisible(false); 
        dispose(); 
    }

    public char[] getPassphrase() {
        char[] retval = passphraseInput.getPassword();
        if( retval != null && retval.length == 0 ) {
            retval = null;
        }
        
        return retval;
    }
}
