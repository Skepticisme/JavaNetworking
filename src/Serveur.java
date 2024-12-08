import java.io.*;
import java.net.*;
public class Serveur {
    public static void main(String[] args) throws IOException {
        try{
            ServerSocket ss = new ServerSocket(1234);
            System.out.println("Serveur listening on port 1234");
            Socket clientSocket = ss.accept();
            System.out.println("Serveur accepted");
            System.out.println("generating Objects for input and output streams for socket");
            InputStream in = clientSocket.getInputStream();
            OutputStream os = clientSocket.getOutputStream();
            System.out.println("Waiting for 1 byte number");
            int nb = in.read();
            System.out.println("Sending Response ");
            os.write(nb*5);
            System.out.println("Closure");
            clientSocket.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
