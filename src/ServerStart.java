import server.Server;
import server.SimpleServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ServerStart {

    private static int port = 5555;
    private static BlockingQueue<Server> serverPool = new ArrayBlockingQueue<>(10000);
    private static ServerSocket serverSocket;
    private static Set<String> userNames = new HashSet<>();


    public static void main(String[] args) {

        Scanner console = new Scanner(System.in);
        boolean create = false;
        while (!create) {
            try {
                serverSocket = new ServerSocket(port);
                create = true;
                System.out.println("Server established on port " + port);
                System.out.println("Server expects connections...");
            } catch (IOException e) {
                System.out.println("Can't establish server");
                System.out.println(e.getMessage());
                System.out.println("Probably, port " + port + " already in use");
                System.out.println("Press Enter to try again or type new port and press enter. Type 'exit' to exit");
                while (true) {
                    String com = console.nextLine();
                    if (com.equals("exit")) {
                        return;
                    } else if (com.isEmpty()) {
                        create = false;
                        break;
                    } else {
                        try {
                            port = Integer.parseInt(com);
                            create = false;
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