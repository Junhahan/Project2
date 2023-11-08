import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class GWackClientGUI extends JFrame {

        private JTextArea messageDisplayArea;
        private JTextArea messageComposeArea;
        private JButton connectButton;
        private JButton disconnectButton;
        private JButton sendButton;
        private JTextField nameField;
        private JTextField hostField;
        private JTextField portField;
        private GWackClientNetworking networking;
        private JList<String> membersList;
        private DefaultListModel<String> membersListModel;

        public GWackClientGUI() {
                createGUI();
        }

        private void createGUI() {
                setTitle("GWack -- GW Slack Simulator");
                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                setSize(700, 800);

                JPanel membersPanel = new JPanel(new BorderLayout());
                membersPanel.add(new JLabel("Members Online"), BorderLayout.NORTH);
                membersListModel = new DefaultListModel<>();
                membersList = new JList<>(membersListModel);
                JScrollPane leftPanel = new JScrollPane(membersList);
                membersPanel.add(leftPanel, BorderLayout.CENTER);
            
                messageDisplayArea = new JTextArea();
                messageDisplayArea.setEditable(false);
                JScrollPane messageScrollPane = new JScrollPane(messageDisplayArea);
                JPanel messagePanel = new JPanel(new BorderLayout());
                messagePanel.add(new JLabel("Messages"), BorderLayout.NORTH);
                messagePanel.add(messageScrollPane, BorderLayout.CENTER);
            
                messageComposeArea = new JTextArea(5, 30);
                JScrollPane composeScrollPane = new JScrollPane(messageComposeArea);
                JPanel composePanel = new JPanel(new BorderLayout());
                composePanel.add(new JLabel("Compose"), BorderLayout.NORTH);
                composePanel.add(composeScrollPane, BorderLayout.CENTER);
            
                sendButton = new JButton("Send");
                sendButton.addActionListener(e -> sendMessage());
                JPanel sendButtonPanel = new JPanel(new BorderLayout());
                sendButtonPanel.add(sendButton, BorderLayout.EAST);
                composePanel.add(sendButtonPanel, BorderLayout.EAST);
            
                connectButton = new JButton("Connect");
                disconnectButton = new JButton("Disconnect");
                disconnectButton.setEnabled(false);
                nameField = new JTextField(10);
                hostField = new JTextField(10);
                portField = new JTextField(5);
                JPanel connectionPanel = new JPanel();
                connectionPanel.add(new JLabel("Name"));
                connectionPanel.add(nameField);
                connectionPanel.add(new JLabel("IP Address"));
                connectionPanel.add(hostField);
                connectionPanel.add(new JLabel("Port"));
                connectionPanel.add(portField);
                connectionPanel.add(connectButton);
                connectionPanel.add(disconnectButton);
            
                connectButton.addActionListener(new ConnectActionListener());
                disconnectButton.addActionListener(new DisconnectActionListener());
            
                JPanel topPanel = new JPanel(new BorderLayout());
                topPanel.add(connectionPanel, BorderLayout.CENTER);
            
                add(membersPanel, BorderLayout.WEST);
                add(topPanel, BorderLayout.NORTH);
                add(messagePanel, BorderLayout.CENTER);
                add(composePanel, BorderLayout.SOUTH);
            
                pack();
                setVisible(true);
        }
            
            

        private class ConnectActionListener implements ActionListener {
                @Override
                public void actionPerformed(ActionEvent e) {
                        String name = nameField.getText();
                        String host = hostField.getText();
                        int port;
                        try {
                        port = Integer.parseInt(portField.getText());
                        networking.connect(name, host, port);
                        connectButton.setEnabled(false);
                        disconnectButton.setEnabled(true);
                        messageComposeArea.setEditable(true);
                        } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(GWackClientGUI.this, "Enter a valid port number.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                        } catch (Exception ex) {
                        JOptionPane.showMessageDialog(GWackClientGUI.this, "Could not connect to server: " + ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                        }
                }
        }

        private class DisconnectActionListener implements ActionListener {
                @Override
                public void actionPerformed(ActionEvent e) {
                        networking.disconnect();
                        connectButton.setEnabled(true);
                        disconnectButton.setEnabled(false);
                        messageComposeArea.setEditable(false);
                        messageDisplayArea.append("Disconnected from server.\n");
                }
                }

        public void newMessage(String message) {
                messageDisplayArea.append(message + "\n");
        }

        public void updateMembers(String[] clients) {
                membersListModel.clear();
                for (String client : clients) {
                        membersListModel.addElement(client);
                }
        }

        public void sendMessage() {
                String message = messageComposeArea.getText();
                if (!message.isEmpty() && networking.isConnected()) {
                        networking.writeMessage(message);
                        messageComposeArea.setText("");
                } else if (!networking.isConnected()) {
                        JOptionPane.showMessageDialog(this, "Not connected to server.", "Send Error", JOptionPane.ERROR_MESSAGE);
                }
        }

        public static void main(String[] args) {
                SwingUtilities.invokeLater(() -> new GWackClientGUI());
        }
}

class GWackClientNetworking {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String name;
        private JTextArea messageDisplayArea;

        public GWackClientNetworking(JTextArea messageDisplayArea) {
                this.messageDisplayArea = messageDisplayArea;
        }

        public void writeMessage(String message) {
                if (out != null) {
                        out.println(message);
                }
        }

        public boolean isConnected() {
                return socket != null && socket.isConnected() && !socket.isClosed();
        }
        public void connect(String name, String host, int port) throws IOException {
                if (socket != null && !socket.isClosed()) {
                disconnect();
                }
                socket = new Socket(host, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.name = name;
                writeMessage(name);
                new ReadingThread().start();
        }
        public void disconnect() {
                try {
                        if (socket != null) {
                                socket.close();
                        }
                        if (out != null) {
                                out.close();
                        }
                        if (in != null) {
                                in.close();
                        }
                } catch (IOException ex) {
                        ex.printStackTrace();
                } finally {
                        socket = null;
                        out = null;
                        in = null;
                }
        }

        private class ReadingThread extends Thread {
                public void run() {
                String message;
                        try {
                                while ((message = in.readLine()) != null) {
                                        final String finalMessage = message;
                                        SwingUtilities.invokeLater(() -> {
                                        messageDisplayArea.append(finalMessage  + "\n");
                                        });
                                }
                        } catch (IOException e) {
                                if (!socket.isClosed()) {
                                        e.printStackTrace();
                                        SwingUtilities.invokeLater(() -> {
                                        messageDisplayArea.append("Error reading from server: " + e.getMessage() + "\n");
                                        });
                                }
                        } finally {
                                try {
                                        disconnect();
                                } catch (Exception e) {
                                        SwingUtilities.invokeLater(() -> {
                                        messageDisplayArea.append("Error disconnecting: " + e.getMessage() + "\n");
                                        });
                                }
                        }
                }
        }
}
