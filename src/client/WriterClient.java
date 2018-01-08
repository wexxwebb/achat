package client;

import common.Message;
import common.decoder.Decoder;
import common.fileTransport.ReceiveFile;
import common.fileTransport.Receiver;
import common.fileTransport.TransferFile;
import common.fileTransport.Transmitter;
import common.sleep.Sleep;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import static common.SystemMessages.*;

public class WriterClient implements Client {

    private ClientData clientData;
    private Scanner console;

    public WriterClient(ClientData clientData) {
        this.clientData = clientData;
        console = new Scanner(System.in);
    }

    @SuppressWarnings("Duplicates")
    private boolean send(String string, BufferedOutputStream output) {
        int retry = 0;
        while (true) {
            try {
                byte[] buffer = string.getBytes();
                output.write(Decoder.intAsByteArray(buffer.length));
                output.write(string.getBytes());
                output.flush();
                return true;
            } catch (IOException e) {
                retry++;
                if (retry > 3) {
                    System.out.println("Can't send message");
                    clientData.exit(); //todo need fix not right when file transmitting
                    return false;
                } else {
                    System.out.println("Problem with send message. Retry " + retry);
                    Sleep.millis(200);
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
                send(json, clientData.getOutStream());
                break;
            case JOIN_ROOM:
                if (s.length == 2) {
                    json = clientData.getGson().toJson(new Message(SYSTEM, JOIN_ROOM, s[1]));
                    send(json, clientData.getOutStream());
                } else {
                    System.out.println("Unknown command");
                }
                break;
            case SEND_FILE:
                if (s.length == 2) {
                    File file = new File(s[1]);
                    if (file.exists()) {
                        try {
                            Socket socket = new Socket(clientData.getAddress(), clientData.getPort());
                            BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
                            json = clientData.getGson().toJson(Message.getMessageForFile(file.getName(), clientData.getName()));
                            send(json, output);
                            Transmitter transferFile = new TransferFile(output);
                            if (transferFile.transfer(file.getAbsolutePath())) {
                                output.close();
                                socket.close();
                                System.out.println("File " + file.getName() + " transmitted successful.");
                            } else {
                                System.out.println("File transmitting error.");
                            }
                        } catch (IOException e) {
                            e.printStackTrace(); //todo handle exception need
                        }

                    } else {
                        System.out.printf("File '%s' not exist.\n", file.getName());
                    }
                } else {
                    System.out.println("Unknown command");
                }
                break;

            case DOWNLOAD:
                if (s.length == 2) {
                    try {
                        Socket socket = new Socket(clientData.getAddress(), clientData.getPort());
                        BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
                        BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
                        json = clientData.getGson().toJson(
                                new Message(SYSTEM, DOWNLOAD, s[1])
                        );
                        send(json, output);
                        //todo client shold accept aproove file existing from server
                        Receiver receiver = new ReceiveFile(input, "Downloads/");
                        if (receiver.receive(s[1])) {
                            input.close();
                            output.close();
                            socket.close();
                            System.out.printf("File '%s' received successful.\n", s[1]);
                        } else {
                            System.out.println("File receiving error.");
                        }
                    } catch (IOException e) {
                        e.printStackTrace(); //todo need handle exception
                    }
                    break;
                } else {
                    System.out.println("Unknown command");
                }
                break;

            case FILE_LIST:
                json = clientData.getGson().toJson(new Message(SYSTEM, FILE_LIST));
                send(json, clientData.getOutStream());
                break;
            case EXIT:
                json = clientData.getGson().toJson(new Message(SYSTEM, EXIT));
                send(json, clientData.getOutStream());
                break;
            default:
                json = clientData.getGson().toJson(new Message(USER, string));
                send(json, clientData.getOutStream());
                break;
        }
    }

    private void prepareString() {
        String json;
        String string = console.nextLine();
        switch (clientData.getState()) {
            case STATE_NOT_AUTHORIZED:
                String login = string.trim();
                System.out.println("Input password");
                String password = console.nextLine().trim();
                password = Integer.toString(Integer.toString(password.hashCode() + SALT.hashCode()).hashCode());
                json = clientData.getGson().toJson(
                        new Message(
                                SYSTEM,
                                LOGIN_AND_HASH_CODE_PASSWORD,
                                login,
                                password)
                );
                if (!send(json, clientData.getOutStream())) {
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
        String json = clientData.getGson().toJson(
                new Message(
                        SYSTEM,
                        LOGIN_AND_HASH_CODE_PASSWORD,
                        clientData.getName(),
                        clientData.getPasswordHash()
                )
        );
        if (!send(json, clientData.getOutStream())) {
            return;
        }
        clientData.setState(STATE_AUTH_REQUEST);
    }

    @Override
    public void run() {
        System.out.println("Input login:");
        while (clientData.getPlay()) {
            prepareString();
        }
    }
}
