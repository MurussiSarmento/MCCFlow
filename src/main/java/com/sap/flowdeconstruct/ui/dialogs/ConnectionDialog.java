package com.sap.flowdeconstruct.ui.dialogs;

import com.sap.flowdeconstruct.model.FlowConnection;
import com.sap.flowdeconstruct.i18n.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ConnectionDialog extends JDialog {
    private final JComboBox<FlowConnection.DirectionStyle> directionCombo;
    private final JTextField protocolField;
    private boolean confirmed = false;

    public ConnectionDialog(Frame owner, FlowConnection connection) {
        super(owner, I18n.t("connection.dialog.title"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        directionCombo = new JComboBox<>(FlowConnection.DirectionStyle.values());
        directionCombo.setSelectedItem(connection.getDirectionStyle());

        protocolField = new JTextField(connection.getProtocol() != null ? connection.getProtocol() : "", 20);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel(I18n.t("connection.dialog.direction")), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(directionCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(new JLabel(I18n.t("connection.dialog.protocol")), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(protocolField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton(I18n.t("dialog.ok"));
        JButton cancel = new JButton(I18n.t("dialog.cancel"));
        buttons.add(cancel);
        buttons.add(ok);

        ok.addActionListener((ActionEvent e) -> {
            confirmed = true;
            setVisible(false);
        });
        cancel.addActionListener((ActionEvent e) -> {
            confirmed = false;
            setVisible(false);
        });

        // ESC to cancel
        getRootPane().registerKeyboardAction(e -> {
            confirmed = false; setVisible(false);
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Enter to OK
        getRootPane().setDefaultButton(ok);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() { return confirmed; }
    public FlowConnection.DirectionStyle getSelectedDirectionStyle() {
        return (FlowConnection.DirectionStyle) directionCombo.getSelectedItem();
    }
    public String getProtocol() { return protocolField.getText().trim(); }
}