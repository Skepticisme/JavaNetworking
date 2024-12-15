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
          clients.put(clientKey, new ClientInfo(clientAddress, clientPort, clientMessage));
          System.out.println("New client connected: " + clientMessage);

          // Notify all clients about the new client
          multicastMessage(serverSocket, "Server", clientMessage + " has joined the chat.", null);
        } else {
          // Retrieve client info and log message
          ClientInfo clientInfo = clients.get(clientKey);
          Timestamp timestamp = new Timestamp(System.currentTimeMillis());
          System.out.println("[" + timestamp + "] " + clientInfo.name + ": " + clientMessage);

          // Multicast the message
          multicastMessage(serverSocket, clientInfo.name, clientMessage, clientKey);
        }

        // Clear the receive buffer for the next message
        receiveData = new byte[1024];
      }
    } catch (IOException e) {
      System.err.println("Server error: " + e.getMessage());
    }
  }

  private static void multicastMessage(DatagramSocket serverSocket, String senderName, String message, String senderKey) {
    byte[] sendData;
    String formattedMessage = senderName + ": " + message;

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

  // Helper class to store client information
  static class ClientInfo {
    InetAddress address;
    int port;
    String name;

    ClientInfo(InetAddress address, int port, String name) {
      this.address = address;
      this.port = port;
      this.name = name;
    }
  }
}
