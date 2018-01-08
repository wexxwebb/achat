package server;

import common.decoder.Decoder;
import common.fileTransport.*;
import common.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import common.sleep.Sleep;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import static common.SystemMessages.*;

public class SimpleServer implements Server {

    private Socket socket;
    private final BlockingQueue<Server> serverPool;
    private BufferedInputStream inStream;
    private BufferedOutputStream outStream;
    private String userName = "Unknown";
    private String passwordHash;
    private boolean auth = false;
    private final Map<String, String> users;
    private GsonBuilder gsonBuilder;
    private Gson gson;
    private String room = "all";
    private boolean exit = false;

    public SimpleServer(Socket socket, BlockingQueue<Server> serverPool, Map<String, String> users) {
        this.socket = socket;
        this.serverPool = serverPool;
        this.users = users;
        this.gsonBuilder = new GsonBuilder();
        this.gson = gsonBuilder.create();
        int retry = 0;
        while (true) {
            try {
                inStream = new BufferedInputStream(socket.getInputStream());
                outStream = new BufferedOutputStream(socket.getOutputStream());
                break;
            } catch (IOException e) {
                if (retry > 3) {
                    return;
                } else {
                    System.out.println("Can't establish socket. Retry " + ++retry);
                    Sleep.millis(200);
                }
            }
        }
    }

    public void setRoom(String room) {
        this.room = room;
    }

    @Override
    public String getRoom() {
        return room;
    }

    @Override
    public boolean isAuth() {
        return auth;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public boolean sendString(String string) {
        int retry = 0;
        while (!exit) {
            try {
                byte[] buffer = string.getBytes();
                outStream.write(Decoder.intAsByteArray(buffer.length));
                outStream.write(string.getBytes());
                outStream.flush();
                return true;
            } catch (IOException e) {
                retry++;
                if (retry > 5) {
                    System.out.println();
                    return false;
                } else {
                    if (retry == 1) System.out.print("Sending message.");
                    else System.out.print(".");
                    Sleep.millis(500);
                }
            }
        }
        return true;
    }

    private void exit(boolean silent) {
        int retry = 0;
        while (true) {
            try {
                synchronized (serverPool) {
                    serverPool.remove(this);
                }
                inStream.close();
                outStream.close();
                socket.close();
                if (!silent) System.out.printf("%s disconnected from server\n", userName);
                return;
            } catch (IOException e) {
                retry++;
                if (retry > 3) {
                    System.out.println("Can't close socket. Thread stopped with error;");
                } else {
                    System.out.printf("Can't close socket. Retry %d\n", retry);
                    Sleep.millis(200);
                }
            }
        }
    }

    private String readString() {
        int retry = 0;
        while (!exit) {
            try {
                byte[] intAsBytes = new byte[4];
                inStream.read(intAsBytes);
                int length = Decoder.byteArrayAsInt(intAsBytes);
                byte[] buffer = new byte[length];
                inStream.read(buffer);
                return new String(buffer);
            } catch (IOException e) {
                retry++;
                if (retry > 5) {
                    System.out.println();
                    return READING_ERROR;
                } else {
                    if (retry == 1) System.out.print("Reading message.");
                    else System.out.print(".");
                    Sleep.millis(500);
                }
            }
        }
        return gson.toJson(new Message(SYSTEM, ""));
    }

    private void broadCastRoom(Message message) {
        synchronized (serverPool) {
            message.setMessage(userName + ": " + message.getMessage());
            for (Server server : serverPool) {
                if (!server.equals(this) && server.getRoom().equals(this.getRoom())) {
                    if (server.isAuth()) server.sendString(gson.toJson(message));
                }
            }
        }
    }

    private void broadCastAll(Message message) {
        synchronized (serverPool) {
            for (Server server : serverPool) {
                if (!server.equals(this)) {
                    if (server.isAuth()) server.sendString(gson.toJson(message));
                }
            }
        }
    }

    private boolean checkString(String line) {
        if (READING_ERROR.equals(line)) {
            exit(false);
            return false;
        } else {
            return true;
        }
    }

    private static final int REGISTER = 566;
    private static final int AUTHENTICATED = 204;
    private static final int DECLINE = 433;

    private int setUserName(String userName, String passwordHash) {
        synchronized (users) {
            String passHash = users.get(userName);
            if (passHash == null) {
                this.userName = userName;
                this.passwordHash = passwordHash;
                users.put(userName, passwordHash);
                return REGISTER;
            } else if (passHash.equals(passwordHash)) {
                this.userName = userName;
                this.passwordHash = passwordHash;
                return AUTHENTICATED;
            } else {
                return DECLINE;
            }
        }
    }

    private void authenticator() {
        while (true) {
            String string = readString();
            if (!checkString(string)) {
                return;
            }
            Message message = gson.fromJson(string, Message.class);

            switch (message.getMessage()) {
                case LOGIN_AND_HASH_CODE_PASSWORD:
                    switch (setUserName(message.getLogin(), message.getPasswordHash())) {
                        case REGISTER:
                            if (!sendString(gson.toJson(new Message(SYSTEM, REGISTER_ACCEPT_OK, message.getLogin(), message.getPasswordHash()))))
                                return;
                            auth = true;
                            System.out.println(userName + " registered on server");
                            broadCastAll(new Message(USER, userName + " registered on server"));
                            return;
                        case AUTHENTICATED:
                            if (!sendString(gson.toJson(new Message(SYSTEM, AUTH_ACCEPT_OK, message.getLogin(), message.getPasswordHash()))))
                                return;
                            auth = true;
                            System.out.println(userName + " authenticated on server");
                            broadCastAll(new Message(USER, userName + " authenticated on server"));
                            return;
                        case DECLINE:
                            if (!sendString(gson.toJson(new Message(SYSTEM, INCORRECT_PASSWORD)))) return;
                            continue;
                    }
                    break;

                case SEND_FILE:
                    Receiver receiveFile = new ReceiveFile(inStream, "ReceivedFiles/");
                    if (receiveFile.receive(message.getOption())) {
                        System.out.println("File " + message.getOption() + " received successful.");
                        broadCastAll(new Message(USER, message.getLogin() + " shared file " + message.getOption()));
                        exit = true;
                        exit(true);
                    } else {
                        System.out.println("File receiving error.");
                    }
                    break;
                case DOWNLOAD:
                    String json;
                    if (getFileList().contains(message.getOption())) {
//                        json = gson.toJson(new Message(SYSTEM, READY_TO_TRANSFER_FILE, message.getOption()));
//                        sendString(json); //todo server should send approve fie existiig
                        Transmitter transferFile = new TransferFile(outStream, "ReceivedFiles/");
                        if (transferFile.transfer(message.getOption())) {
                            exit = true;
                            exit(true);
                            System.out.println("File " + message.getOption() + " transfered sucessful.");
                        } else {
                            System.out.println("File transmitting error.");
                        }
                    } else {
                        json = gson.toJson(
                                new Message(
                                        USER,
                                        String.format("File '%s' not exist.", message.getOption()))
                        );
                        sendString(json);
                        exit = exit;
                        exit(true);
                    }
                    break;
            }

        }
    }

    private Set<String> getAllRooms() {
        Set<String> rooms = new HashSet<>();
        for (Server server : serverPool) {
            rooms.add(server.getRoom());
        }
        return rooms;
    }

    private Message getRooms() {
        StringBuilder result = new StringBuilder();
        Set<String> rooms = getAllRooms();
        for (String s : rooms) {
            result.append(s);
            result.append("\n");
        }
        result.append("Type '_join <room_name> to join room'");
        return new Message(USER, result.toString());
    }

    private Message joinToRoom(String room) {
        Set<String> rooms = getAllRooms();
        setRoom(room);
        if (rooms.contains(room)) {
            return new Message(USER, String.format("%s joined to room %s", userName, room));
        } else {
            return new Message(USER, String.format("%s has created and joined to room %s", userName, room));
        }
    }

    private Message unknownCommand() {
        return new Message(USER, "unknow command");
    }

    private List<String> getFileList() {
        FileFounder fileFounder = new FileFounder();
        return fileFounder.getFileList("ReceivedFiles");
    }

    private Message getFiles() {
        StringBuilder result = new StringBuilder();
        List<String> files = getFileList();
        for (String s : files) {
            result.append(s);
            result.append("\n");
        }
        result.append("Type '_download <file_name> to download file'");
        return new Message(USER, result.toString());
    }

    private void parseMessage(String string) {
        String json;
        Message message = gson.fromJson(string, Message.class);
        //pattern command
        switch (message.getType()) {
            case SYSTEM:
                switch (message.getMessage()) {
                    case SHOW_ROOMS:
                        json = gson.toJson(getRooms());
                        sendString(json);
                        break;

                    case JOIN_ROOM:
                        Message joinToRoomMessage = joinToRoom(message.getOption());
                        json = gson.toJson(joinToRoomMessage);
                        broadCastAll(joinToRoomMessage);
                        sendString(json);
                        break;

//                    case DOWNLOAD:
//                        if (getFileList().contains(message.getOption())) {
//                            json = gson.toJson(new Message(SYSTEM, READY_TO_TRANSFER_FILE, message.getOption()));
//                            sendString(json);
//                            Transmitter transferFile = new TransferFile(outStream, "ReceivedFiles/");
//                            if (transferFile.transfer(message.getOption())) {
//                                System.out.println("File " + message.getOption() + " transfered sucessful.");
//                            } else {
//                                System.out.println("File transmitting error.");
//                            }
//                        }
//                        break;

                    case FILE_LIST:
                        json = gson.toJson(getFiles());
                        sendString(json);
                        break;
                    case EXIT:
                        broadCastAll(new Message(USER, userName + " disconnected from server"));
                        json = gson.toJson(new Message(SYSTEM, EXIT));
                        sendString(json);
                        exit = true;
                        exit(false);

                    default:
                        json = gson.toJson(unknownCommand());
                        sendString(json);
                        break;
                }
                break;
            case USER:
                System.out.println(userName + ": " + message.getMessage());
                broadCastRoom(message);
                break;
        }
    }

    @Override
    public void run() {
        authenticator();
        String string;
        while (auth) {
            string = readString();
            if (!checkString(string)) {
                return;
            } else {
                parseMessage(string);
            }
        }
    }
}
