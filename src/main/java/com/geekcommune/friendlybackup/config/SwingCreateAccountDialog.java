package com.geekcommune.friendlybackup.config;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class SwingCreateAccountDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private JPasswordField passphraseInput = new JPasswordField(30);
    private JPasswordField repeatPassphraseInput = new JPasswordField(30);
    private JTextField usernameInput = new JTextField(30);
    private JTextField emailInput = new JTextField(30);;
    

    public SwingCreateAccountDialog() {
        super(new JFrame(), "Create Friendly Backup identity", true);
        Container parent = getParent();

        if (parent != null) {
            Dimension parentSize = parent.getSize();
            Point p = parent.getLocation();
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }
        
        JPanel messagePane = new JPanel();
        addLabelledField(messagePane, "Name: ", usernameInput);
        addLabelledField(messagePane, "Email: ", emailInput);
        addLabelledField(messagePane, "Passphrase: ", passphraseInput);
        addLabelledField(messagePane, "Repeat passphrase: ", repeatPassphraseInput);

        getContentPane().add(messagePane);
        
        messagePane.setLayout(new GridLayout(4, 2));

        JPanel buttonPane = new JPanel();
        JButton button = new JButton("OK");
        buttonPane.add(button);
        button.addActionListener(this);
        getContentPane().add(buttonPane, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        pack();
        setVisible(true);
    }

    private void addLabelledField(
            JPanel messagePane,
            String label,
            JTextField field) {
        messagePane.add(new JLabel(label));
        messagePane.add(field);
    }

    public void actionPerformed(ActionEvent e) {
        if( isInputValid() ) {
            setVisible(false); 
            dispose(); 
        }
    }

    public char[] getPassphrase() {
        char[] retval = passphraseInput.getPassword();
        if( retval != null && retval.length == 0 ) {
            retval = null;
        }
        
        return retval;
    }
    
    public String getName() {
        return usernameInput.getText();
    }
    
    public String getEmail() {
        return emailInput.getText();
    }

    public boolean isInputValid() {
        if( !Arrays.equals(
                passphraseInput.getPassword(),
                repeatPassphraseInput.getPassword()) ) {
            passphraseInput.setText("");
            repeatPassphraseInput.setText("");
            JOptionPane.showMessageDialog(this, "Please re-enter your passphrase & repeat passphrase.  They did not match.");
            return false;
        }
        
        //TODO validate username, email, password length
        
        return true;
    }
}
