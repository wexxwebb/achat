package client;

import java.io.*;
import java.util.Scanner;

import static common.SystemMessages.*;

public class WriterClient implements Client {

    private ConnectionData connectionData;
    private Scanner console;

    public WriterClient(ConnectionData connectionData) {
        this.connectionData = connectionData;
        console = new Scanner(System.in);
    }

    private boolean send(String message) {
        int retry = 0;
        while (true) {
            try {
                connectionData.getbWriter().write(message);
                connectionData.getbWriter().newLine();
                connectionData.getbWriter().flush();
                return true;
            } catch (IOException e) {
                retry++;
                if (retry > 3) {
                    System.out.println("Can't send message");
                    connectionData.exit();
                    return false;
                } else {
                    System.out.println("Problem with send message. Retry " + retry);
                    connectionData.sleep(200);
                }
            }
        }
    }

    @Override
    public void run() {
        String message;
        while (connectionData.getPlay()) {
            message = console.nextLine();
            switch (connectionData.getState()) {
                case NOT_AUTHORIZED:
                    String login = message;
                    System.out.println("Input password:");
                    String password = console.nextLine();
                    password = Integer.toString(Integer.toString(password.hashCode() + SALT.hashCode()).hashCode());
                    message = LOGIN_AND_HASH_CODE_PASSWORD + login + "\u0005" + password;
            }

            if (!send(message)) {
                return;
            }
        }
    }

}
