import server.Server;
import server.SimpleServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ServerStart {

    private static int port = 5555;
    private static BlockingQueue<Server> serverPool = new ArrayBlockingQueue<>(10000);
    private static ServerSocket serverSocket;
    private static Map<String, String> userNames = new HashMap<>();


    public static void main(String[] args) {

        Scanner console = new Scanner(System.in);
        create: while (true) {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Server established on port " + port);
                System.out.println("Server expects connections...");
                break create;
            } catch (IOException e) {
                System.out.println("Can't establish server");
                System.out.println(e.getMessage());
                System.out.println("Probably, port " + port + " already in use");
                System.out.println("Press Enter to retry, type new port and press enter, type 'exit' to exit");
                while (true) {
                    String com = console.nextLine();
                    if (com.equals("exit")) {
                        return;
                    } else if (com.isEmpty()) {
                        continue create;
                    } else {
                        try {
                            port = Integer.parseInt(com);
                            break;
                        } catch (NumberFormatException e1) {
                            System.out.println("Type number only to change port or 'exit' to exit");
                        }
                    }
                }
            }
        }
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                Server newServer = new SimpleServer(socket, serverPool, userNames);
                Thread thread = new Thread(newServer);
                thread.start();
                synchronized (serverPool) {
                    serverPool.add(newServer);
                }
            } catch (IOException e) {
                System.out.println("Connection not established");
            }
        }
    }
}