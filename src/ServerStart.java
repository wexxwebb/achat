import common.sleep.Sleep;
import common.wrap.Result;
import server.Server;
import server.SimpleServer;
import server.logging.Logger;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerStart {

    private static BlockingQueue<Server> serverPool = new ArrayBlockingQueue<>(10000);
    private static Map<String, String> userNames = new HashMap<>();
    private static Scanner console = new Scanner(System.in);

    private static Result<Integer> setPort(String strPort, int retryLimit) {
        if ("exit".equals(strPort)) {
            return new Result<>(null, false, "Exit");
        } else {
            if (strPort.matches("\\b[0-9]+\\b")) {
                int port = Integer.parseInt(strPort);
                return new Result<>(port, true, String.format("Port set to '%d' successful", port));
            } else {
                retryLimit--;
                if (retryLimit < 0) return new Result<>(0, false, "Exit but retry limit is over");
                else {
                    System.out.printf("Can't parse argument: '%s' (may be number only). Input new port or 'exit' to exit\n", strPort);
                    return setPort(console.nextLine(), retryLimit);
                }
            }
        }
    }

    private static Result<ServerSocket> setServer(int port, int retryLimit, long latency) {
        int retry = 0;
        while (true) {
            try {
                return new Result<>(new ServerSocket(port), true, String.format("Server established on port '%s' successful", port));
            } catch (IOException e) {
                retry++;
                if (retry > retryLimit) {
                    System.out.println(e.getMessage());
                    System.out.println("Press 'Enter' to retry, input new port to change, input 'exit' to exit");
                    String string = console.nextLine();
                    if (string.isEmpty()) return setServer(port, retryLimit, 500);
                    else {
                        Result<Integer> newPort = setPort(string, 999);
                        if (newPort.get() == null) return new Result<>(null, false, newPort.getMessage());
                        return setServer(newPort.get(), 5, 500);
                    }
                } else if (retry == 1) System.out.printf("Establishing server on port '%d'.", port);
                else System.out.print(".");
                Sleep.millis(latency);
            }
        }
    }

    public static void main(String[] args) {

        Result<Integer> port = setPort(args[0], 999);
        if (!port.isSuccess()) return;

        AtomicInteger messagesCount = new AtomicInteger(0);

        Result<ServerSocket> serverSocket = setServer(port.get(), 5, 500);
        if (!serverSocket.isSuccess()) {
            return;
        } else {
            System.out.println("Server established on port " + port.get());
            System.out.println("Server expects connections...");
        }

        while (true) {
            try {
                Socket socket = serverSocket.get().accept();
                Server newServer = new SimpleServer(socket, serverPool, userNames);
                Logger logger = new Logger(newServer, messagesCount);
                logger.setLogging(true);
                Server loggedServer = (Server) Proxy.newProxyInstance(
                        Logger.class.getClassLoader(),
                        new Class[]{Server.class},
                        logger
                );
                newServer.setLoggedServer(loggedServer);
                Thread thread = new Thread(loggedServer);
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