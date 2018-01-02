import client.Client;
import client.ClientData;
import client.ReaderClient;
import client.WriterClient;

import java.io.IOException;
import java.util.Scanner;

public class ClientStart {

    static private int port;
    static private String address;

    public static void main(String[] args) {
        address = args[0];
        port = Integer.parseInt(args[1]);
        Scanner scanner = new Scanner(System.in);
        ClientData clientData = new ClientData(address, port);
        int retry = 0;
        connection:
        while (true) {
            try {
                if (retry == 0) {
                    System.out.printf("Connection to '%s:%d'...", clientData.getAddress(), clientData.getPort());
                }
                clientData.connect();
                System.out.println("Success!");
                Client reader = new ReaderClient(clientData);
                Client writer = new WriterClient(clientData);
                Thread readerThread = new Thread(reader);
                Thread writerThread = new Thread(writer);
                readerThread.start();
                writerThread.start();
                break;
            } catch (IOException e) {
                retry++;
                if (retry < 5) {
                    System.out.print(".");
                    continue;
                } else {
                    System.out.printf("\nCan't create socket '%s:%d'. Sysytem message: '%s'\n",
                            clientData.getAddress(), clientData.getPort(), e.getMessage());
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
                                clientData.setAddress(s[0]);
                                clientData.setPort(Integer.parseInt(s[1]));
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
                    synchronized (clientData) {
                        try {
                            clientData.wait();
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
