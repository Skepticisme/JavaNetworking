import java.net.*;
import java.io.*;
import java.util.Scanner;
public class Client {
    public static void main(String[] args) throws IOException {
        try {
            Socket s = new Socket("localhost", 1234);
            System.out.println("Client connected to localhost:1234");
            InputStream in = s.getInputStream();
            OutputStream os = s.getOutputStream();
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter a number");
            int nb = sc.nextInt();
            os.write(nb);
            System.out.println("Waiting for response");
            int res = in.read();
            System.out.println("Response: " + res);
            s.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

