package client;

import common.Message;

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

    private boolean send(String message) {
        int retry = 0;
        while (true) {
            try {
                clientData.getbWriter().write(message);
                clientData.getbWriter().newLine();
                clientData.getbWriter().flush();
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

    public void sendString() {
        String json;
        String string = console.nextLine();
        switch (clientData.getState()) {

            case STATE_NOT_AUTHORIZED:
                String login = string;
                System.out.println("Input password");
                String password = console.nextLine();
                password = Integer.toString(Integer.toString(password.hashCode() + SALT.hashCode()).hashCode());
                json = clientData.getGson().toJson(new Message(SYSTEM, LOGIN_AND_HASH_CODE_PASSWORD, login, password));
                if (!send(json)) {
                    return;
                }
                clientData.setState(STATE_AUTH_REQUEST);
                break;

            case STATE_AUTH_OK:
                json = clientData.getGson().toJson(new Message(USER, string));
                if (!send(json)) {
                    return;
                }
                break;

            case STATE_REGISTER_OK:
                json = clientData.getGson().toJson(new Message(USER, string));
                if (!send(json)) {
                    return;
                }
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
            sendString();
        }
    }
}
