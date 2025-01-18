import java.io.*;
import java.net.*;
import java.util.Scanner;

public class UDPClient {
    private static final String MULTICAST_ADDRESS = "230.0.0.1";
    private static final int MULTICAST_PORT = 5002;

    public static void main(String[] args) throws IOException {
        DatagramSocket clientSocket = new DatagramSocket();
        MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT);
        Scanner input = new Scanner(System.in);

        // Join multicast group
        InetAddress multicastGroup = InetAddress.getByName(MULTICAST_ADDRESS);
        multicastSocket.joinGroup(multicastGroup);

        // Start a thread to receive direct messages from the server
        new Thread(() -> {
            try {
                byte[] receiveData = new byte[1024];
                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    clientSocket.receive(receivePacket);
                    String serverMessage = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                    System.out.println("\n" + serverMessage);
                }
            } catch (IOException e) {
                System.err.println("Error receiving direct message: " + e.getMessage());
            }
        }).start();

        // Start a thread to receive multicast messages
        new Thread(() -> {
            try {
                byte[] receiveData = new byte[1024];
                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    multicastSocket.receive(receivePacket);
                    String multicastMessage = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                    System.out.println("\n" + multicastMessage);
                }
            } catch (IOException e) {
                System.err.println("Error receiving multicast message: " + e.getMessage());
            }
        }).start();

        // Get the user's name and notify the server
        System.out.print("Enter your name: ");
        String name = input.nextLine();
        
        // Send name to server
        byte[] sendData = name.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(
            sendData, 
            sendData.length, 
            InetAddress.getByName("127.0.0.1"), 
            5001
        );
        clientSocket.send(sendPacket);

        System.out.println("Connecting to server as: " + name);
        System.out.println("Waiting for server welcome message...");

        while (true) {
            String message = input.nextLine();
            
            if (message.equalsIgnoreCase("/quit")) {
                // Send quit message to server
                sendData = "/quit".getBytes();
                sendPacket = new DatagramPacket(
                    sendData, 
                    sendData.length, 
                    InetAddress.getByName("127.0.0.1"), 
                    5001
                );
                clientSocket.send(sendPacket);
                
                // Clean up and exit
                multicastSocket.leaveGroup(multicastGroup);
                multicastSocket.close();
                clientSocket.close();
                System.out.println("Disconnected from server. Goodbye!");
                System.exit(0);
            }

            // Send message to server
            sendData = message.getBytes();
            sendPacket = new DatagramPacket(
                sendData, 
                sendData.length, 
                InetAddress.getByName("127.0.0.1"), 
                5001
            );
            clientSocket.send(sendPacket);
        }
    }
}