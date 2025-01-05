import java.io.*;
import java.net.*;
import java.util.Scanner;

public class UDPClient {
  public static void main(String[] args) throws IOException {
    DatagramPacket sendPacket;
    byte[] sendData;
    DatagramSocket clientSocket = new DatagramSocket();
    Scanner input = new Scanner(System.in);

    // Start a thread to receive messages from the server
    new Thread(() -> {
      try {
        byte[] receiveData = new byte[1024];
        while (true) {
          DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
          clientSocket.receive(receivePacket); // Block and wait for server messages
          String serverMessage = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
          System.out.println("\n"+ serverMessage);
        }
      } catch (IOException e) {
        System.err.println("Error receiving message: " + e.getMessage());
      }
    }).start();

    // Get the user's name and notify the server
    System.out.print("Enter your name: ");
    String name = input.nextLine();
    sendData = name.getBytes();
    sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("127.0.0.1"), 5001);
    clientSocket.send(sendPacket); // Send the name to the server
    System.out.println("You are now connected to the server as: " + name);
    System.out.println("Type your message below (type QUIT to exit):");

    while (true) {
      // Read input from the user
      String cmd = input.nextLine();

      if (cmd.equalsIgnoreCase("QUIT")) {
        // Notify the user and close the connection
        System.out.println("Exiting the chat...");
        clientSocket.close();
        System.exit(1);
      }

      // Send message to the server
      sendData = cmd.getBytes();
      sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("127.0.0.1"), 5001);
      clientSocket.send(sendPacket);
    }
  }
}
