import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class GWackClientGUI extends JFrame {
        private GWackClientNetworking networking;
        private DefaultListModel<String> membersListModel;
        private JList<String> membersList;
        private JTextArea messagesArea;
        private JTextField messageComposeArea;
         private JTextField nameArea;
        private JTextField hostArea;
        private JTextField portArea;
        private JButton connectButton;
        private JButton sendButton;
        private boolean isconnected = false;

        public GWackClientGUI() {
                createGUI();
                networking = new GWackClientNetworking(this);
        }

        private void createGUI() {
                setTitle("GWack -- GW Slack");
                setSize(700, 500);
                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                getContentPane().setLayout(new BorderLayout()); 

                messagesArea = new JTextArea();
                messagesArea.setEditable(false);
                JScrollPane messageDisplayPane = new JScrollPane(messagesArea);
                JPanel messageDisplayPanel = new JPanel(new BorderLayout());
                messageDisplayPanel.add(new JLabel("Messages"), BorderLayout.NORTH);
                messageDisplayPanel.add(messageDisplayPane, BorderLayout.CENTER); 

                membersListModel = new DefaultListModel<>();
                membersList =new JList<>(membersListModel);
                JScrollPane memberListScrollPane = new JScrollPane(membersList);
                JPanel memberListPanel = new JPanel(new BorderLayout());
                memberListPanel.add(new JLabel("Members Online"), BorderLayout.NORTH);
                memberListPanel.add(memberListScrollPane, BorderLayout.CENTER);

                JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                topPanel.add(new JLabel("Name")); 
                nameArea = new JTextField(10);
                topPanel.add(nameArea);
                topPanel.add(new JLabel("Host"));
                hostArea = new JTextField(10);
                topPanel.add(hostArea); 
                topPanel.add(new JLabel("Port"));
                portArea = new JTextField(5);
                topPanel.add(portArea);
                connectButton = new JButton("Connect");
                connectButton.addActionListener(new ConnectActionListener());
                topPanel.add(connectButton);

                JPanel messageInputPanel = new JPanel(new BorderLayout());
                messageComposeArea = new JTextField();
                messageComposeArea.addActionListener(new SendActionListener());
                messageInputPanel.add(new JLabel("Compose"), BorderLayout.NORTH);
                messageInputPanel.add(messageComposeArea, BorderLayout.CENTER);
                sendButton =new JButton("Send");
                sendButton.addActionListener(new SendActionListener());
                messageInputPanel.add(sendButton, BorderLayout.EAST);

                JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,memberListPanel,messageDisplayPanel);
                splitPane.setDividerLocation(150);
                add(splitPane, BorderLayout.CENTER);
                add(topPanel, BorderLayout.NORTH);
                add(messageInputPanel, BorderLayout.SOUTH);
        }

        private class ConnectActionListener implements ActionListener {
                public void actionPerformed(ActionEvent e) {
                        if(!isconnected) {
                                String host = hostArea.getText();
                                String portS = portArea.getText();
                                String username = nameArea.getText();
                                int port;
                                try{
                                        port = Integer.parseInt(portS);
                                } catch(NumberFormatException ex){
                                        JOptionPane.showMessageDialog(GWackClientGUI.this, "Invalid Port");
                                        return;
                                }
                                if (networking.connect(host,port, username)) {
                                        isconnected = true;
                                        connectButton.setText("Disconnect");
                                } else{
                                        JOptionPane.showMessageDialog(GWackClientGUI.this, "Failed to connect to the server.");
                                }
                        } else{
                                if (networking.disconnectFromServer()) {
                                        isconnected = false;
                                        connectButton.setText("Connect");
                                } else{
                                        JOptionPane.showMessageDialog(GWackClientGUI.this, "Failed to disconnect from the server.");
                                }
                        }
                }
        }  

        private class SendActionListener implements ActionListener {
                public void actionPerformed(ActionEvent e) {
                        sendMessage();
                }
        }

        private void sendMessage() {
                String message = messageComposeArea.getText().trim();
                if (!message.isEmpty()){
                        if (networking.sendMessage(message)) {
                                messageComposeArea.setText("");
                        } else {
                                JOptionPane.showMessageDialog(GWackClientGUI.this, "Sending Failed");
                        }
                }
        } 

        public void updateMembers(ArrayList<String> members) {
                membersListModel.clear();
                for (String member:members) {
                        membersListModel.addElement(member);
                }
        }

        public void newMessage(String message) {
                messagesArea.append(message + "\n");
        }
 
        public static void main(String[] args) {
                SwingUtilities.invokeLater(() -> {
                        GWackClientGUI GUI = new GWackClientGUI();
                        GUI.setVisible(true);
                });
        }
}
  