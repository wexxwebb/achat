package client;

import common.Message;
import java.io.IOException;
import static common.SystemMessages.*;

public class ReaderClient implements Client {

    private ClientData clientData;

    public ReaderClient(ClientData clientData) {
        this.clientData = clientData;
    }

    private String readString() {
        String string;
        int retry = 0;
        while (true) {
            try {
                while ((string = clientData.getbReader().readLine()) != null) {
                    return string;
                }
            } catch (IOException e) {
                retry++;
                if (retry > 3) {
                    if (reConnect()) {
                        string = clientData.getGson().toJson(new Message(USER, "Connection restored"));
                        if (clientData.getState() == STATE_NOT_AUTHORIZED) {
                            System.out.println("Input login:");
                        }
                        return string;
                    } else {
                        return READING_ERROR;
                    }
                } else {
                    System.out.println("Message reading error. Retry " + retry);
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
                if (retry > 3) {
                    return false;
                } else {
                    System.out.printf("Connection to '%s:%d'. Retry %d\n", clientData.getAddress(), clientData.getPort(), retry);
                    clientData.sleep(1000);
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