package client;

import common.Message;
import common.fileTransport.TransferFile;
import common.fileTransport.Transmitter;

import java.io.*;
import java.util.Scanner;

import static common.SystemMessages.*;

public class WriterClient implements Client {

    private ClientData clientData;
    private Scanner console;

    public WriterClient(ClientData clientData) {
        this.clientData = clientData;
        console = new Scanner(System.in);
    }

    private boolean send(String string) {
        int retry = 0;
        while (true) {
            try {
                byte[] temp = string.getBytes();
                clientData.getOutStream().write(temp.length);
                clientData.getOutStream().write(temp);
                clientData.getOutStream().flush();
                return true;
            } catch (IOException e) {
                retry++;
                if (retry > 3) {
                    System.out.println("Can't send message");
                    clientData.exit();
                    return false;
                } else {
                    System.out.println("Problem with send message. Retry " + retry);
                    clientData.sleep(200);
                }
            }
        }
    }

    private void normSending(String string) {
        String json;
        String[] s = string.split(" ");
        switch (s[0]) {
            case SHOW_ROOMS:
                json = clientData.getGson().toJson(new Message(SYSTEM, SHOW_ROOMS));
                send(json);
                break;
            case JOIN_ROOM:
                if (s.length == 2) {
                    json = clientData.getGson().toJson(new Message(SYSTEM, JOIN_ROOM, s[1]));
                    send(json);
                } else {
                    System.out.println("Unknown command");
                }
                break;
            case SEND_FILE:
                if (s.length == 2) {
                    File file = new File(s[1]);
                    json = clientData.getGson().toJson(new Message(SYSTEM, SEND_FILE, file.getName()));
                    send(json);
                    Transmitter transferFile = new TransferFile(clientData.getOutStream());
                    if (transferFile.transfer(file.getAbsolutePath())) {
                        System.out.println("File " + file.getName() + " transmitted successful.");
                    } else {
                        System.out.println("File transmitting error.");
                    }
                } else {
                    System.out.println("Unknown command");
                }
                break;
            case DOWNLOAD:
                if (s.length == 2) {
                    json = clientData.getGson().toJson(new Message(SYSTEM, DOWNLOAD, s[1]));
                    send(json);
                } else {
                    System.out.println("Unknown command");
                }
                break;
            case FILE_LIST:
                json = clientData.getGson().toJson(new Message(SYSTEM, FILE_LIST));
                send(json);
                break;
            case EXIT:
                json = clientData.getGson().toJson(new Message(SYSTEM, EXIT));
                send(json);
                break;
            default:
                json = clientData.getGson().toJson(new Message(USER, string));
                send(json);
                break;
        }
    }

    public void prepareString() {
        String json;
        String string = console.nextLine();
        switch (clientData.getState()) {

            case STATE_NOT_AUTHORIZED:
                String login = string.trim();
                System.out.println("Input password");
                String password = console.nextLine().trim();
                password = Integer.toString(Integer.toString(password.hashCode() + SALT.hashCode()).hashCode());
                json = clientData.getGson().toJson(new Message(SYSTEM, LOGIN_AND_HASH_CODE_PASSWORD, login, password));
                if (!send(json)) {
                    return;
                }
                clientData.setState(STATE_AUTH_REQUEST);
                break;

            case STATE_AUTH_OK:
                normSending(string);
                break;

            case STATE_REGISTER_OK:
                normSending(string);
                break;

            case STATE_SEND_LOGIN_PASSWORD:
                sendLoginPassword();
                break;
        }
    }

    @Override
    public void sendLoginPassword() {
        String json;
        json = clientData.getGson().toJson(new Message(SYSTEM, LOGIN_AND_HASH_CODE_PASSWORD, clientData.getName(), clientData.getPasswordHash()));
        if (!send(json)) {
            return;
        }
        clientData.setState(STATE_AUTH_REQUEST);
    }

    @Override
    public void run() {
        while (clientData.getPlay()) {
            prepareString();
        }
    }
}
