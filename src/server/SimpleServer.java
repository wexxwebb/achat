package server;

import client.FileFounder;
import common.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import static common.SystemMessages.*;

public class SimpleServer implements Server {

    private Socket socket;
    private final BlockingQueue<Server> serverPool;
    private InputStream inStream;
    private OutputStream outStream;
    private String userName = "Unknown";
    private String passwordHash;
    private boolean auth = false;
    private final Map<String, String> users;
    private GsonBuilder gsonBuilder;
    private Gson gson;
    private String room = "all";

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
                    sleep(200);
                }
            }
        }
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getRoom() {
        return room;
    }

    @Override
    public boolean isAuth() {
        return auth;
    }

    @Override
    public boolean sendString(String string) {
        int retry = 0;
        while (true) {
            try {
                byte[] temp = string.getBytes();
                outStream.write(temp.length);
                outStream.write(string.getBytes());
                outStream.flush();
                return true;
            } catch (IOException e) {
                if (retry > 3) {
                    return false;
                } else {
                    System.out.println("Can't send message. Try " + ++retry);
                    sleep(200);
                }
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void exit() {
        int retry = 0;
        while (true) {
            try {
                synchronized (serverPool) {
                    serverPool.remove(this);
                }
                socket.close();
                System.out.printf("%s disconnected from server\n", userName);
                return;
            } catch (IOException e) {
                retry++;
                if (retry > 3) {
                    System.out.println("Can't close socket. Thread stopped with error;");
                } else {
                    System.out.printf("Can't close socket. Retry %d\n", retry);
                    sleep(200);
                }
            }
        }
    }

    private String readString() {
        int retry = 0;
        while (true) {
            try {
                int length = inStream.read();
                byte[] temp = new byte[length];
                inStream.read(temp);
                return new String(temp);
            } catch (IOException e) {
                retry++;
                if (retry > 3) {
                    return READING_ERROR;
                } else {
                    System.out.println("Error reading message. Retry " + retry);
                    sleep(200);
                }
            }
        }
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
            exit();
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
            Message message = new Message(SYSTEM, LOGINANDPASSWORD_REQUEST);
            if (!sendString(gson.toJson(message))) {
                return;
            }
            String string = readString();
            if (!checkString(string)) {
                return;
            }
            message = gson.fromJson(string, Message.class);
            switch (setUserName(message.getLogin(), message.getPasswordHash())) {
                case REGISTER:
                    if (!sendString(gson.toJson(new Message(SYSTEM, REGISTER_ACCEPT_OK, message.getLogin(), message.getPasswordHash())))) return;
                    auth = true;
                    System.out.println(userName + " registered on server");
                    broadCastAll(new Message(USER, userName + " registered on server"));
                    return;
                case AUTHENTICATED:
                    if (!sendString(gson.toJson(new Message(SYSTEM, AUTH_ACCEPT_OK, message.getLogin(), message.getPasswordHash())))) return;
                    auth = true;
                    System.out.println(userName + " authenticated on server");
                    broadCastAll(new Message(USER, userName + " authenticated on server"));
                    return;
                case DECLINE:
                    if (!sendString(gson.toJson(new Message(SYSTEM, INCORRECT_PASSWORD)))) return;
                    continue;
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

    private void receiveFile(String fileName) {
        int retry = 0;
        while (true) {
            try {
                File file = new File("ReceivedFiles/" + fileName);
                FileOutputStream fileOutStream = new FileOutputStream(file);
                int length;
                while (true) {
                    if ((length = inStream.read()) == 0) {
                        break;
                    }
                    byte[] buffer = new byte[length];
                    length = inStream.read(buffer);
                    fileOutStream.write(buffer, 0, length);
                }
                fileOutStream.flush();
                fileOutStream.close();
                System.out.println(String.format("%s transfer file %s sucessful", userName, fileName));
                broadCastAll(new Message(USER, String.format("%s shared file %s. Type '_download %s' to download this.file", userName, fileName, fileName)));
                return;
            } catch (IOException e) {
                retry++;
                if (retry > 5) {
                    System.out.println("Can't receive file. Interrupted.");
                    return;
                } else {
                    if (retry == 1) System.out.print("Receiving file...");
                    System.out.print(".");
                }
            }
        }
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

    private void transferFile(String fileName) {
        File file = new File("ReceivedFiles/" + fileName);
        int retry = 0;
        while (true) {
            try {
                FileInputStream fileInStream = new FileInputStream(file);
                byte[] buffer = new byte[127];
                int length;
                while ((length = fileInStream.read(buffer)) != -1) {
                    outStream.write(length);
                    outStream.write(buffer,0, length);
                }
                outStream.write(0);
                outStream.flush();
                fileInStream.close();
                System.out.println("File " + file.getName() + " transfered. to " + userName);
                return;
            } catch (IOException e) {
                retry++;
                if (retry > 5) {
                    System.out.printf("Can't read file %s for transfer. Interrupted\n", file.getAbsolutePath());
                } else {
                    if (retry == 1) {
                        System.out.printf("Opening file %s...\n", file.getName());
                    } else {
                        System.out.print(".");
                    }
                    sleep(500);
                }
            }
        }
    }

    private void parseMessage(String string) {
        String json;
        Message message = gson.fromJson(string, Message.class);
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

                    case SEND_FILE:
                        receiveFile(message.getOption());
                        break;

                    case DOWNLOAD:
                        if (getFileList().contains(message.getOption())) {
                            json = gson.toJson(new Message(SYSTEM, READY_TO_TRANSFER_FILE, message.getOption()));
                            sendString(json);
                            transferFile(message.getOption());
                        }
                        break;
                    case FILE_LIST:
                        json = gson.toJson(getFiles());
                        sendString(json);
                        break;

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
