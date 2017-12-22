package client;

import java.io.IOException;

import static common.SystemMessages.*;

public class ReaderClient implements Client {

    private ConnectionData connectionData;

    public ReaderClient(ConnectionData connectionData) {
        this.connectionData = connectionData;
    }

    private String readMessage() {
        String message;
        int retry = 0;
        while (true) {
            try {
                while (connectionData.getPlay() && (message = connectionData.getbReader().readLine()) != null) {
                    return message;
                }
            } catch (IOException e) {
                retry++;
                if (retry > 3) {
                    return READING_ERROR;
                } else {
                    System.out.println("Message reading error. Retry " + retry);
                    connectionData.sleep(200);
                }
            }
        }
    }

    private boolean checkPrint(String message) {
        switch (message) {
            case READING_ERROR:
                int retry = 0;
                while (true) {
                    try {
                        System.out.printf("Connection to '%s:%d'...",
                                connectionData.getAddress(), connectionData.getPort());
                        connectionData.connect();
                        System.out.println("Success!");
                        return true;
                    } catch (IOException e) {
                        retry++;
                        if (retry > 3) {
                            System.out.println("Exit with connection error");
                            connectionData.exit();
                            return false;
                        } else {
                            System.out.println("Can't create socket. Retry " + retry);
                        }
                    }
                }
            case LOGINANDPASSWORD_REQUEST:
                System.out.println("Input login:");
                connectionData.setState(NOT_AUTHORIZED);
                return true;
            case INCORRECT_PASSWORD:
                System.out.println("Authentication... FAIL! Incorrect password");
                System.out.println("Input login:");
                connectionData.setState(NOT_AUTHORIZED);
                return true;
            default:
                if (message.startsWith(AUTH_OK)) {
                    connectionData.setState(AUTH_OK);
                    String[] auths = message.split("\\u0005");
                    connectionData.setName(auths[auths.length - 2]);
                    connectionData.setPasswordHash(auths[auths.length - 1]);
                    System.out.println("Authetication... OK.");
                    System.out.println("Hi, " + connectionData.getName());
                    return true;
                }
                if (message.startsWith(REGISTER_OK)) {
                    connectionData.setState(AUTH_OK);
                    String[] auths = message.split("\\u0005");
                    connectionData.setName(auths[auths.length - 2]);
                    connectionData.setPasswordHash(auths[auths.length - 1]);
                    System.out.println("Register... OK.");
                    System.out.println("Hi, " + connectionData.getName());
                    return true;
                }
                System.out.println(message);
                return true;
        }
    }

    @Override
    public void run() {
        String message;
        while (connectionData.getPlay()) {
            message = readMessage();
            if (!checkPrint(message)) {
                return;
            }
        }
    }
}
