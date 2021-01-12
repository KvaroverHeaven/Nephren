/*
    This file is part of Nephren.

    DDView.java
    Copyright (C) 2020, 2021  Relius Wang

    Nephren is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Nephren is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with Nephren.  If not, see <https://www.gnu.org/licenses/>.
 */

package view;

import org.jetbrains.annotations.NotNull;
import util.HttpDownload;
import util.URIParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;

public class DDView extends JFrame implements Observer {
    private static final String[] hashAlgorStrings =
            {"MD5", "SHA-1", "SHA-256", "SHA-512"};
    private static final JLabel uriLabel = new JLabel("網址：");
    private final JTextField addTextField;
    private final JComboBox<String> hashAlgorBox;
    private final JTextField hashTextField;
    private final DownloadsTableModel tableModel;
    private final JTable table;
    private final List<JButton> buttonList;
    private final AtomicBoolean clearing;
    private String hashAlgor = "MD5";
    private String hashText = "";
    private HttpDownload selectedDownload;

    public DDView() {
        super("Nephren");
        setSize(1280, 720);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        clearing = new AtomicBoolean(false);


        var addListPanel = new JPanel();
        addListPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;

        addTextField = new JTextField(20);
        addTextField.setEditable(true);
        hashTextField = new JTextField(20);
        hashTextField.setEditable(true);
        addListPanel.add(uriLabel, c);

        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 6;
        c.fill = GridBagConstraints.BOTH;
        addListPanel.add(addTextField, c);


        hashAlgorBox = new JComboBox<>(hashAlgorStrings);
        hashAlgorBox.addActionListener(e -> hashAlgor = (String) hashAlgorBox.getSelectedItem());
        c.gridx = 0;
        c.gridy = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.NONE;
        addListPanel.add(hashAlgorBox, c);


        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 6;
        c.fill = GridBagConstraints.BOTH;
        addListPanel.add(hashTextField, c);

        var menuBar = new JMenuBar();
        var fileMenu = new JMenu("檔案");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        var addItem = new JMenuItem("加入網址", KeyEvent.VK_O);
        addItem.addActionListener(e -> {
            var result = JOptionPane.showConfirmDialog(this, addListPanel,
                    "請輸入網址和 Hash", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                hashText = hashTextField.getText();
                actionAdd(addTextField.getText(), hashAlgor, hashText);
            }
            addTextField.setText("");
            hashTextField.setText("");
        });
        fileMenu.add(addItem);

        var exitItem = new JMenuItem("結束", KeyEvent.VK_X);
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        tableModel = new DownloadsTableModel();
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (selectedDownload != null) {
                selectedDownload.deleteObserver(DDView.this);
            }
            if (!clearing.get() && table.getSelectedRow() > -1) {
                selectedDownload = tableModel.getDownload(table.getSelectedRow());
                selectedDownload.addObserver(DDView.this);
                updateButtons();
            }
        });

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        var render = new ProgressRender(0, 100);
        render.setStringPainted(true);
        table.setDefaultRenderer(JProgressBar.class, render);

        table.setRowHeight((int) render.getPreferredSize().getHeight());

        var downloadsPanel = new JPanel();
        downloadsPanel.setBorder(BorderFactory.createTitledBorder("下載列表"));
        downloadsPanel.setLayout(new BorderLayout());
        downloadsPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        var buttonsPanel = new JPanel();
        buttonList = List.of(new JButton("暫停")
                , new JButton("繼續")
                , new JButton("取消")
                , new JButton("清理"));
        buttonList.forEach(b -> {
            b.addActionListener(e -> actionDownloading(b.getText()));
            b.setEnabled(false);
            buttonsPanel.add(b);
        });

        setLayout(new BorderLayout());
        add(downloadsPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void updateButtons() {
        if (selectedDownload != null) {
            switch (selectedDownload.getStatus()) {
                case DOWNLOADING -> buttonList.forEach(b ->
                        b.setEnabled(b.getText().equals("暫停")
                                || b.getText().equals("取消")));
                case PAUSED -> buttonList.forEach(b ->
                        b.setEnabled(b.getText().equals("繼續")
                                || b.getText().equals("取消")));
                case ERROR -> buttonList.forEach(b ->
                        b.setEnabled(b.getText().equals("繼續")
                                || b.getText().equals("清理")));
                default -> buttonList.forEach(b -> b.setEnabled(b.getText().equals("清理")));
            }
        } else {
            buttonList.forEach(b -> b.setEnabled(false));
        }
    }

    private void actionAdd(@NotNull String uriString, String hashAlgor, String hash) {
        URI verifiedUri = URIParser.apply(uriString);
        if (verifiedUri != null) {
            tableModel.addDownload(new HttpDownload(verifiedUri, hashAlgor, hash));
        } else {
            JOptionPane.showMessageDialog(this,
                    "無效的下載網址", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actionDownloading(@NotNull String status) {
        switch (status) {
            case "暫停" -> {
                selectedDownload.onPause();
                updateButtons();
            }
            case "繼續" -> {
                selectedDownload.onResume();
                updateButtons();
            }
            case "取消" -> {
                selectedDownload.onCancel();
                updateButtons();
            }
            case "清理" -> {
                clearing.compareAndSet(false, true);
                tableModel.clearDownload(table.getSelectedRow());
                clearing.compareAndSet(true, false);
                selectedDownload = null;
                updateButtons();
            }
        }

    }

    @Override
    public void update(Observable o, Object arg) {
        if (selectedDownload != null && selectedDownload.equals(o)) {
            updateButtons();
        }
    }
}
