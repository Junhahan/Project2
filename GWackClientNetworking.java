import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class GWackClientNetworking {
        private final GWackClientGUI gui;
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String host;
        private int port;
        private String name;

        public GWackClientNetworking(GWackClientGUI gui) {
                this.gui = gui;
        }

        public boolean connect(String host, int port, String username) {
                this.host = host;
                this.port = port;
                this.name = username;

                try{
                        socket = new Socket(host,port);
                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        writer = new PrintWriter(socket.getOutputStream(), true);

                        writer.println("SECRET");
                        writer.println("3c3c4ac618656ae32b7f3431e75f7b26b1a14a87");
                        writer.println("NAME");
                        writer.println(username);

                        ReadingThread readingThread = new ReadingThread();
                        readingThread.start();
                        updateMemberList();
                        return true;
                } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                }
        }

        public boolean disconnectFromServer() {
                try{
                        if (socket !=null) {
                                socket.close();
                                reader.close();
                                writer.close();
                                updateMemberList();
                                return true;
                        }
                }catch (IOException e){
                        e.printStackTrace();
                }
                return false;
        }
        private void Message(String message) {
                gui.newMessage(message);
        }
        public boolean sendMessage(String message) {
                try{
                        if (socket != null) {
                                writer.println(message);
                                Message("[" + name + "] "+message);
                                return true;
                        }
                } catch (Exception e){
                        e.printStackTrace();
                }
                return false;
        }

        private void updateMemberList() {
                ArrayList<String> members = new ArrayList<>();
                gui.updateMembers(members);
        }

        private void updateMemberList(ArrayList<String> members) {
                gui.updateMembers(members);
        }

        private class ReadingThread extends Thread {
                @Override
                public void run() {
                        try{
                                String line;
                                ArrayList<String> members = new ArrayList<>();
                                boolean isUpdatingMembers = false;
                
                                while ((line = reader.readLine()) != null) {
                                        if (line.equals("START_CLIENT_LIST")){
                                                isUpdatingMembers = true;
                                                members.clear();
                                                continue;
                                        }
                                        if (line.equals("END_CLIENT_LIST")) {
                                                isUpdatingMembers = false;
                                                updateMemberList(members);
                                                continue;
                                        }
                                        if (isUpdatingMembers) {
                                                members.add(line);
                                        } else{
                                                Message(line);
                                        }
                                }
                        }catch (IOException e) {
                                e.printStackTrace();
                        }
                }
        }
    
}