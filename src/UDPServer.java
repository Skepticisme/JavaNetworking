import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class UDPServer {
  private static Map<String, ClientInfo> clients = new HashMap<>();

  public static void main(String[] args) {
    try (DatagramSocket serverSocket = new DatagramSocket(5001)) {
      System.out.println("Server Started. Listening for Clients on port 5001...");
      byte[] receiveData = new byte[1024];

      while (true) {
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);

        // Extract client details
        InetAddress clientAddress = receivePacket.getAddress();
        int clientPort = receivePacket.getPort();
        String clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
        String clientKey = clientAddress.toString() + ":" + clientPort;

        if (!clients.containsKey(clientKey)) {
          // Register new client
          clients.put(clientKey, new ClientInfo(clientAddress, clientPort, clientMessage, System.currentTimeMillis()));
          System.out.println("New client connected: " + clientMessage);

          // Notify all clients about the new client
          multicastMessage(serverSocket, "Server", clientMessage + " has joined the chat.", null);
        } else {
          // If the message starts with '/', it's a command
          if (clientMessage.startsWith("/")) {
            if (clientMessage.equals("/users")) {
              showUsers(serverSocket, clientAddress, clientPort);
            } else if (clientMessage.equals("/quit")) {
              handleQuit(serverSocket, clientKey);
            } else if (clientMessage.startsWith("/msg")) {
              handleSendPrivateMessage(serverSocket, clientMessage, clientKey);
            } else {
              System.out.println("Unknown command: " + clientMessage);
            }
          } else {
            // Retrieve client info and log message
            ClientInfo clientInfo = clients.get(clientKey);
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            System.out.println("[" + timestamp + "] " + clientInfo.name + ": " + clientMessage);

            // Multicast the message to other clients
            multicastMessage(serverSocket, clientInfo.name, clientMessage, clientKey);
          }
        }

        receiveData = new byte[1024];
      }
    } catch (IOException e) {
      System.err.println("Server error: " + e.getMessage());
    }
  }

  private static void multicastMessage(DatagramSocket serverSocket, String senderName, String message, String senderKey) {
    byte[] sendData;
    String formattedMessage = "[public] " + senderName + ": " + message;

    for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
      String clientKey = entry.getKey();
      ClientInfo clientInfo = entry.getValue();

      if (!clientKey.equals(senderKey)) {
        try {
          sendData = formattedMessage.getBytes();
          DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientInfo.address, clientInfo.port);
          serverSocket.send(sendPacket);
          System.out.println("Sent to " + clientInfo.name + " at " + clientInfo.address + ":" + clientInfo.port);
        } catch (IOException e) {
          System.err.println("Error sending message to " + clientInfo.name + ": " + e.getMessage());
        }
      }
    }
  }

  private static void showUsers(DatagramSocket serverSocket, InetAddress clientAddress, int clientPort) {
    // Create a StringBuilder to store the user list
    StringBuilder userList = new StringBuilder();
    userList.append("Connected Users:\n");

    for (ClientInfo client : clients.values()) {
      userList.append(client.name).append("\n");
    }

    try {
      byte[] sendData = userList.toString().getBytes();
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
      serverSocket.send(sendPacket);
      System.out.println("Sent user list to " + clientAddress + ":" + clientPort);
    } catch (IOException e) {
      System.err.println("Error sending user list: " + e.getMessage());
    }
  }

  // Helper class to store client information
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

  private static void handleQuit(DatagramSocket serverSocket, String clientKey) {
    if (clients.containsKey(clientKey)) {
      ClientInfo clientInfo = clients.remove(clientKey);
      String quitMessage = clientInfo.name + " has left the chat.";
      multicastMessage(serverSocket, "Server", quitMessage, clientKey);
      System.out.println("Client " + clientInfo.name + " has disconnected.");
    }
  }

  private static void handleSendPrivateMessage(DatagramSocket serverSocket, String clientMessage, String clientKey) {
    String[] parts = clientMessage.split(" ", 3);

    if (parts.length < 3) {
      System.out.println("Invalid private message format. Correct format: /msg [username] [message]");
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
      String formattedMessage = "[private] Private message from " + clients.get(clientKey).name + ": " + message;
      try {
        byte[] sendData = formattedMessage.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, targetClient.address, targetClient.port);
        serverSocket.send(sendPacket);
        System.out.println("Private message sent to " + targetUsername);
      } catch (IOException e) {
        System.err.println("Error sending private message to " + targetUsername + ": " + e.getMessage());
      }
    } else {
      System.out.println("User " + targetUsername + " not found.");
    }
  }
}
