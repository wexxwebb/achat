package client;

import common.Message;
import common.decoder.Decoder;
import common.fileTransport.ReceiveFile;
import common.fileTransport.Receiver;
import common.sleep.Sleep;

import java.io.*;
import java.net.Socket;

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
                byte[] intAsBytes = new byte[4];
                clientData.getInStream().read(intAsBytes);
                int length = Decoder.byteArrayAsInt(intAsBytes);
                byte[] buffer = new byte[length];
                clientData.getInStream().read(buffer);
                return new String(buffer);
            } catch (IOException e) {
                retry++;
                if (retry > 5) {
                    if (reConnect()) {
                        string = clientData.getGson().toJson(new Message(USER, "\nConnection restored. " + showForUser()));
                        clientData.setState(STATE_SEND_LOGIN_PASSWORD);
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
                    Sleep.millis(1000);
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
                        System.out.printf("\nConnection to '%s:%d'...", clientData.getAddress(), clientData.getPort());
                    } else {
                        System.out.print(".");
                    }
                    Sleep.millis(1000);
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

//            case READY_TO_TRANSFER_FILE:
//                try {
//                    Socket socket = new Socket(clientData.getAddress(), clientData.getPort());
//                    BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
//                    Receiver receiver = new ReceiveFile(input, "Downloads/");
//                    if (receiver.receive(message.getOption())) {
//                        input.close();
//                        socket.close();
//                        System.out.printf("File '%s' received successful", message.getOption());
//                    } else {
//                        System.out.println("File receiving error.");
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                break;

            case EXIT:
                clientData.exit();
                break;
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