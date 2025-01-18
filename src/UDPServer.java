import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class UDPServer {
    private static Map<String, ClientInfo> clients = new HashMap<>();
    private static final String MULTICAST_ADDRESS = "230.0.0.1";
    private static final int MULTICAST_PORT = 5002;
    private static final String WELCOME_MESSAGE = """
        Welcome to the Chat Server!
        
        Available Commands:
        /help           - Show this help message
        /users          - Show list of connected users
        /msg [user] [message] - Send a private message to a specific user
        /quit           - Leave the chat server
        
        Regular messages will be sent to all connected users.
        Enjoy your chat!
        """;

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(5001);
             MulticastSocket multicastSocket = new MulticastSocket()) {
            
            InetAddress multicastGroup = InetAddress.getByName(MULTICAST_ADDRESS);
            multicastSocket.setTimeToLive(1); // Set TTL to 1 for local network only
            
            System.out.println("Server Started. Listening for Clients on port 5001...");
            System.out.println("Multicast group: " + MULTICAST_ADDRESS + ":" + MULTICAST_PORT);
            
            byte[] receiveData = new byte[1024];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                String clientKey = clientAddress.toString() + ":" + clientPort;

                if (!clients.containsKey(clientKey)) {
                    // Register new client
                    clients.put(clientKey, new ClientInfo(clientAddress, clientPort, clientMessage, System.currentTimeMillis()));
                    System.out.println("New client connected: " + clientMessage);

                    // Send welcome message to the new client
                    sendDirectMessage(serverSocket, WELCOME_MESSAGE, clientAddress, clientPort);

                    // Notify all clients about the new client
                    multicastMessage(multicastSocket, "Server", clientMessage + " has joined the chat.", null);
                } else {
                    if (clientMessage.startsWith("/")) {
                        handleCommand(serverSocket, multicastSocket, clientMessage, clientKey, clientAddress, clientPort);
                    } else {
                        ClientInfo clientInfo = clients.get(clientKey);
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        System.out.println("[" + timestamp + "] " + clientInfo.name + ": " + clientMessage);

                        multicastMessage(multicastSocket, clientInfo.name, clientMessage, clientKey);
                    }
                }

                receiveData = new byte[1024];
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static void handleCommand(DatagramSocket serverSocket, MulticastSocket multicastSocket, 
                                    String clientMessage, String clientKey, 
                                    InetAddress clientAddress, int clientPort) {
        if (clientMessage.equals("/help")) {
            sendDirectMessage(serverSocket, WELCOME_MESSAGE, clientAddress, clientPort);
        } else if (clientMessage.equals("/users")) {
            showUsers(serverSocket, clientAddress, clientPort);
        } else if (clientMessage.equals("/quit")) {
            handleQuit(multicastSocket, clientKey);
        } else if (clientMessage.startsWith("/msg")) {
            handleSendPrivateMessage(serverSocket, clientMessage, clientKey);
        } else {
            System.out.println("Unknown command: " + clientMessage);
            sendDirectMessage(serverSocket, "Unknown command. Type /help for available commands.", 
                            clientAddress, clientPort);
        }
    }

    private static void multicastMessage(MulticastSocket socket, String senderName, String message, String senderKey) {
        byte[] sendData;
        String formattedMessage = "[public] " + senderName + ": " + message;

        try {
            sendData = formattedMessage.getBytes();
            DatagramPacket multicastPacket = new DatagramPacket(
                sendData, 
                sendData.length, 
                InetAddress.getByName(MULTICAST_ADDRESS), 
                MULTICAST_PORT
            );
            socket.send(multicastPacket);
            System.out.println("Multicast message sent: " + formattedMessage);
        } catch (IOException e) {
            System.err.println("Error sending multicast message: " + e.getMessage());
        }
    }

    private static void sendDirectMessage(DatagramSocket socket, String message, InetAddress address, int port) {
        try {
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(sendPacket);
        } catch (IOException e) {
            System.err.println("Error sending direct message: " + e.getMessage());
        }
    }

    private static void showUsers(DatagramSocket serverSocket, InetAddress clientAddress, int clientPort) {
        StringBuilder userList = new StringBuilder("Connected Users:\n");
        for (ClientInfo client : clients.values()) {
            userList.append("- ").append(client.name).append("\n");
        }
        sendDirectMessage(serverSocket, userList.toString(), clientAddress, clientPort);
    }

    static class ClientInfo {
        InetAddress address;
        int port;
        String name;
        long connectionTime;

        ClientInfo(InetAddress address, int port, String name, long connectionTime) {
            this.address = address;
            this.port = port;
            this.name = name;
            this.connectionTime = connectionTime;
        }
    }

    private static void handleQuit(MulticastSocket multicastSocket, String clientKey) {
        if (clients.containsKey(clientKey)) {
            ClientInfo clientInfo = clients.remove(clientKey);
            String quitMessage = clientInfo.name + " has left the chat.";
            multicastMessage(multicastSocket, "Server", quitMessage, clientKey);
            System.out.println("Client " + clientInfo.name + " has disconnected.");
        }
    }

    private static void handleSendPrivateMessage(DatagramSocket serverSocket, String clientMessage, String clientKey) {
        String[] parts = clientMessage.split(" ", 3);

        if (parts.length < 3) {
            ClientInfo sender = clients.get(clientKey);
            sendDirectMessage(serverSocket, 
                            "Invalid private message format. Use: /msg [username] [message]", 
                            sender.address, sender.port);
            return;
        }

        String targetUsername = parts[1];
        String message = parts[2];

        ClientInfo targetClient = null;
        for (ClientInfo clientInfo : clients.values()) {
            if (clientInfo.name.equals(targetUsername)) {
                targetClient = clientInfo;
                break;
            }
        }

        if (targetClient != null) {
            String formattedMessage = "[private] Message from " + clients.get(clientKey).name + ": " + message;
            sendDirectMessage(serverSocket, formattedMessage, targetClient.address, targetClient.port);
            
            // Confirm to sender
            ClientInfo sender = clients.get(clientKey);
            sendDirectMessage(serverSocket, 
                            "[private] Message sent to " + targetUsername + ": " + message,
                            sender.address, sender.port);
        } else {
            ClientInfo sender = clients.get(clientKey);
            sendDirectMessage(serverSocket, 
                            "User " + targetUsername + " not found.", 
                            sender.address, sender.port);
        }
    }
}