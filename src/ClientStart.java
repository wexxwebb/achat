import client.Client;
import client.ReaderClient;
import client.WriterClient;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientStart {

    static private int port = 5555;
    static private String address = "127.0.0.1";

    public static void main(String[] args) {
        Socket socket = null;
        Scanner console = new Scanner(System.in);
        boolean success = false;
        while (!success) {
            try {
                socket = new Socket(address, port);
                success = true;
            } catch (IOException e) {
                System.out.println("Can't connect to server " + address + ":" + port);
                System.out.println("Press 'Enter' to retry");
                System.out.println("Type 'address:port' (example: 127.0.0.1:5555");
                System.out.println("Type 'exit' to return");
                String com = console.nextLine();
                if (com.equals("exit")) {
                    return;
                } else if (com.isEmpty()) {
                    success = false;
                } else {
                    String[] comAr = com.split(":");
                    address = comAr[0];
                    try {
                        port = Integer.parseInt(comAr[1]);
                    } catch (NumberFormatException e1) {
                        System.out.println("Can't read port. Type number.");
                    }
                }
            }
        }
        Client writerClient = null;
        Client readerClient = null;
        success = false;
        int tryCount = 0;
        while (!success) {
            try {
                writerClient = new WriterClient(socket);
                readerClient = new ReaderClient(socket);
                success = true;
            } catch (IOException e) {
                tryCount++;
                if (tryCount < 6) {
                    System.out.println("Can't read or write socket. Try " + tryCount);
                } else {
                    System.out.println("Socket error");
                    return;
                }
            }
        }
        Thread readerClientThread = new Thread(readerClient);
        Thread writerClientThread = new Thread(writerClient);
        readerClientThread.start();
        writerClientThread.start();
    }
}
