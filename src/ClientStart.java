import client.Client;
import client.ConnectionData;
import client.ReaderClient;
import client.WriterClient;

import java.io.IOException;
import java.util.Scanner;

public class ClientStart {

    static private int port = 5555;
    static private String address = "127.0.0.1";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConnectionData connectionData = new ConnectionData(address, port);
        int retry = 0;
        connection:
        while (true) {
            try {
                System.out.printf("Connection to '%s:%d...", connectionData.getAddress(), connectionData.getPort());
                connectionData.connect();
                System.out.println("Success!");
                Client reader = new ReaderClient(connectionData);
                Client writer = new WriterClient(connectionData);
                Thread readerThread = new Thread(reader);
                Thread writerThread = new Thread(writer);
                readerThread.start();
                writerThread.start();
                break;
            } catch (IOException e) {
                retry++;
                if (retry < 3) {
                    System.out.printf("Can't create socket '%s:%d'. Retry %d\n",
                            connectionData.getAddress(), connectionData.getPort(), retry);
                    continue;
                } else {
                    System.out.printf("Can't create socket '%s:%d'. Sysytem message: '%s'\n",
                            connectionData.getAddress(), connectionData.getPort(), e.getMessage());
                    System.out.println("Press enter to retry, type 'address:port' to change, type 'exit' to exit");
                    parseInt:
                    while (true) {
                        String com = scanner.nextLine();
                        if (com.isEmpty()) {
                            retry = 0;
                            continue connection;
                        } else if ("exit".equals(com)) {
                            return;
                        } else {
                            String[] s = com.split(":");
                            if (s[1].matches("[0-9]+")) {
                                connectionData.setAddress(s[0]);
                                connectionData.setPort(Integer.parseInt(s[1]));
                                retry = 0;
                                break parseInt;
                            } else {
                                System.out.println("Port should be numeric!");
                            }
                        }
                    }
                }
            }
        }
        Thread exit = new Thread(
                () -> {
                    synchronized (connectionData) {
                        try {
                            connectionData.wait();
                            System.exit(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                });
        exit.start();
    }
}
