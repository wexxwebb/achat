package server;

import common.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static common.SystemMessages.*;

public class SimpleServer implements Server {

    private Socket socket;
    private final BlockingQueue<Server> serverPool;
    private BufferedReader bReader;
    private BufferedWriter bWriter;
    private String userName = "Unknown";
    private String passwordHash;
    private boolean auth = false;
    private final Map<String, String> users;
    private GsonBuilder gsonBuilder;
    private Gson gson;

    public SimpleServer(Socket socket, BlockingQueue<Server> serverPool, Map<String, String> users) {
        this.socket = socket;
        this.serverPool = serverPool;
        this.users = users;
        this.gsonBuilder = new GsonBuilder();
        this.gson = gsonBuilder.create();
        int retry = 0;
        while (true) {
            try {
                bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                bWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
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

    @Override
    public boolean isAuth() {
        return auth;
    }

    @Override
    public boolean sendString(String string) {
        int retry = 0;
        while (true) {
            try {
                bWriter.write(string);
                bWriter.newLine();
                bWriter.flush();
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
        String message;
        int retry = 0;
        while (true) {
            try {
                while ((message = bReader.readLine()) != null) {
                    return message;
                }
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

    private void broadCast(Message message) {
        synchronized (serverPool) {
            message.setMessage(userName + ": " + message.getMessage());
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

    private int setUserName(String userName, String passwordHash) {
        synchronized (users) {
            String passHash = users.get(userName);
            if (passHash == null) {
                this.userName = userName;
                this.passwordHash = passwordHash;
                users.put(userName, passwordHash);
                return 1;
            } else if (passHash.equals(passwordHash)) {
                this.userName = userName;
                this.passwordHash = passwordHash;
                return 2;
            } else {
                return 3;
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
                case 1:
                    if (!sendString(gson.toJson(new Message(SYSTEM, REGISTER_ACCEPT_OK, message.getLogin(), message.getPasswordHash())))) return;
                    auth = true;
                    System.out.println(userName + " registered on server");
                    return;
                case 2:
                    if (!sendString(gson.toJson(new Message(SYSTEM, AUTH_ACCEPT_OK, message.getLogin(), message.getPasswordHash())))) return;
                    auth = true;
                    System.out.println(userName + " authenticated on server");
                    return;
                case 3:
                    if (!sendString(gson.toJson(new Message(SYSTEM, INCORRECT_PASSWORD)))) return;
                    continue;
            }
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
                Message message = gson.fromJson(string, Message.class);
                System.out.println(userName + ": " + message.getMessage());
                broadCast(message);
            }
        }
    }
}
