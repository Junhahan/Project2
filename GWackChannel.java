import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GWackChannel {

        private ServerSocket serverSocket;
        private ConcurrentLinkedQueue<String> messageQueue;
        private ArrayList<GWackConnectedClient> clients;

        public GWackChannel(int port) throws IOException {
                serverSocket = new ServerSocket(port);
                messageQueue =new ConcurrentLinkedQueue<>();
                clients = new ArrayList<>();
        }

        public void serve() {
                try {
                while (true) {
                        Socket clientSocket = serverSocket.accept();
                        GWackConnectedClient client= new GWackConnectedClient(clientSocket);
                        addClient(client);
                        client.start();
                }
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        private synchronized void addClient(GWackConnectedClient client) {
                clients.add(client);
                sendClientList();
        }

        public synchronized void enqueueMessage(String message) {
                messageQueue.add(message);
                notifyAll();
                sendMessagesToAll();
        }
        private synchronized void sendMessagesToAll() {
                ArrayList<String> messages = dequeueAll();
                for (String message : messages) {
                        for (GWackConnectedClient client:clients) {
                                if(client.isValid()) {
                                        client.sendMessage(message);
                                }
                        }
                }
        }

        private synchronized ArrayList<String> dequeueAll() {
                ArrayList<String> messages= new ArrayList<>();
                while (!messageQueue.isEmpty()){
                        messages.add(messageQueue.poll());
                }
                return messages;
        }

        public synchronized void removeClients() {
                for (Iterator<GWackConnectedClient> iterator = clients.iterator();iterator.hasNext();) {
                        GWackConnectedClient client =iterator.next();
                        if (!client.isValid()) {
                                iterator.remove();
                        }
                }
                sendClientList();
        }
        private synchronized void sendClientList() {
                StringBuilder clientListBuilder = new StringBuilder("CLIENTLIST:");
                for (GWackConnectedClient client : clients) {
                        if (client.isValid()){
                                clientListBuilder.append(client.getClientName()).append(",");
                        }
                }
                String clientList = clientListBuilder.toString();
                for (GWackConnectedClient client : clients) {
                        if (client.isValid()) {
                                client.sendMessage(clientList);
                        }
                }
        }

        private class GWackConnectedClient extends Thread {
                private Socket socket;
                private PrintWriter out;
                private BufferedReader in;
                private String cName;
                private boolean valid;

                public GWackConnectedClient(Socket socket) {
                        this.socket = socket;
                        this.valid = true;
                        try{
                                out = new PrintWriter(socket.getOutputStream(), true);
                                in =new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                this.cName = in.readLine();
                        } catch (IOException e) {
                                valid = false;
                                e.printStackTrace();
                        }
                }

                @Override
                public void run() {
                        try{
                                String message;
                                while ((message = in.readLine()) != null) {
                                        enqueueMessage(cName+ ": " + message);
                                }
                        } catch (IOException e) {
                                valid = false;
                                e.printStackTrace();
                        }finally {
                                closeConnection();
                                removeClients();
                        }
                }

                public void sendMessage(String message) {
                        out.println(message);
                }

                public boolean isValid(){
                        return valid &&!socket.isClosed();
                }

                public String getClientName() {
                        return cName;
                }

                private void closeConnection() {
                        try{
                                socket.close();
                                out.close();
                                in.close();
                        } catch (IOException e) {
                                e.printStackTrace();
                        }finally {
                                valid = false;
                        }
                }
        }

        public static void main(String[] args) {
                try {
                        int port =8506;
                        GWackChannel server= new GWackChannel(port);
                        server.serve();
                } catch (IOException e){
                        e.printStackTrace();
                }
        }
}
