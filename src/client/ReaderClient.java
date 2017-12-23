package client;

import common.Message;

import java.io.*;

import static common.SystemMessages.*;

public class ReaderClient implements Client {

    private ClientData clientData;

    public ReaderClient(ClientData clientData) {
        this.clientData = clientData;
    }

    private String showForUser() {
        switch (clientData.getState()) {
            case STATE_NOT_AUTHORIZED:
                return "Input login:";
            case STATE_CONNECTED_AGAIN:
                return "Press 'Enter' for authentication";
            default:
                return "";
        }
    }

    private String readString() {
        String string;
        int retry = 0;
        while (true) {
            try {
                int lenght = clientData.getInStream().read();
                byte[] result = new byte[lenght];
                clientData.getInStream().read(result);
                return new String(result);
            } catch (IOException e) {
                retry++;
                if (retry > 5) {
                    if (reConnect()) {
                        string = clientData.getGson().toJson(new Message(USER, "\nConnection restored. " + showForUser()));
                        return string;
                    } else {
                        return READING_ERROR;
                    }
                } else {
                    if (retry == 1) {
                        System.out.print("Reading message...");
                    } else {
                        System.out.print(".");
                    }
                    clientData.sleep(200);
                }
            }
        }
    }

    private boolean reConnect() {
        int retry = 0;
        while (true) {
            try {
                clientData.connect();
                return true;
            } catch (IOException e) {
                retry++;
                if (retry > 5) {
                    System.out.printf(" Can't connection to '%s:%d'. Exit!\n", clientData.getAddress(), clientData.getPort());
                    return false;
                } else {
                    if (retry == 1) {
                        System.out.printf("\nConnection to '%s:%d'...", clientData.getAddress(), clientData.getPort(), retry);
                    } else {
                        System.out.print(".");
                    }
                    clientData.sleep(500);
                }
            }
        }
    }

    private void auth_ok(Message message, String showInConsole) {
        clientData.setPlay(true);
        clientData.setState(STATE_AUTH_OK);
        clientData.setName(message.getLogin());
        clientData.setPasswordHash(message.getPasswordHash());
        System.out.println(showInConsole);
        System.out.println("Hi, " + clientData.getName());
    }

    private void handleSysMessages(Message message) {
        switch (message.getMessage()) {
            case LOGINANDPASSWORD_REQUEST:
                switch (clientData.getState()) {

                    case STATE_CONNECTED:
                        System.out.println("Input login:");
                        clientData.setState(STATE_NOT_AUTHORIZED);
                        break;

                    case STATE_CONNECTED_AGAIN:
                        clientData.setState(STATE_SEND_LOGIN_PASSWORD);
                        break;
                }
                break;

            case AUTH_ACCEPT_OK:
                auth_ok(message, "Authentication... OK.");
                break;

            case REGISTER_ACCEPT_OK:
                auth_ok(message, "Registration... OK.");
                break;

            case INCORRECT_PASSWORD:
                System.out.println("Authentication... FAIL! Incorrect password");
                clientData.setState(STATE_NOT_AUTHORIZED);
                break;

            case READY_TO_TRANSFER_FILE:
                receiveFile(message.getOption());
                break;
        }
    }

    private void receiveFile(String fileName) {
        int retry = 0;
        while (true) {
            try {
                File file = new File("Downloads/" + fileName);
                FileOutputStream fileOutStream = new FileOutputStream(file);
                int length;
                while (true) {
                    if ((length = clientData.getInStream().read()) == 0) {
                        break;
                    }
                    byte[] buffer = new byte[length];
                    length = clientData.getInStream().read(buffer);
                    fileOutStream.write(buffer, 0, length);
                }
                fileOutStream.flush();
                fileOutStream.close();
                System.out.println(String.format("File %s received sucessful.", fileName));
                return;
            } catch (IOException e) {
                retry++;
                if (retry > 5) {
                    System.out.println("Can't receive file. Interrupted.");
                    return;
                } else {
                    if (retry == 1) System.out.print("Receiving file ...");
                    System.out.print(".");
                }
            }
        }
    }

    private void parseString() {
        String string = readString();
        if (READING_ERROR.equals(string)) {
            clientData.exit();
            return;
        }
        Message message = clientData.getGson().fromJson(string, Message.class);
        switch (message.getType()) {
            case SYSTEM:
                handleSysMessages(message);
                break;

            case USER:
                System.out.println(message.getMessage());
                break;
        }
    }

    @Override
    public void sendLoginPassword() {

    }

    @Override
    public void run() {
        while (clientData.getPlay()) {
            parseString();
        }
    }
}