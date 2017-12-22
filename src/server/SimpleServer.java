package server;

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
    private final Map<String, String> users;
    private boolean play = true;
    private boolean auth = false;

    public SimpleServer(Socket socket, BlockingQueue<Server> serverPool, Map<String, String> users) {
        this.socket = socket;
        this.serverPool = serverPool;
        this.users = users;
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
    public boolean sendMessage(String message) {
        int retry = 0;
        while (true) {
            try {
                bWriter.write(message);
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
                play = false;
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

    private String readMessage() {
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

    private void broadCast(String message) {
        if (auth) {
            synchronized (serverPool) {
                for (Server server : serverPool) {
                    if (!server.equals(this)) {
                        server.sendMessage(userName + ": " + message);
                    }
                }
            }
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

    private void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    private String checkMessage(String message) {
        switch (message) {
            case READING_ERROR:
                exit();
                return null;
            default:
                if (message.startsWith(LOGIN_AND_HASH_CODE_PASSWORD)) {
                    String[] auths = message.split("\\u0005");
                    int index = auths.length;
                    int login = setUserName(auths[index - 2], auths[index - 1]);
                    switch (login) {
                        case 1:
                            sendMessage(REGISTER_OK + userName + "\u0005" + passwordHash);
                            auth = true;
                            return "autorized";
                        case 2:
                            sendMessage(AUTH_OK + userName + "\u0005" + passwordHash);
                            auth = true;
                            return "registered";
                        case 3:
                            sendMessage(INCORRECT_PASSWORD);
                            return "incorrect password";
                    }
                }
                return message;
        }
    }

    @Override
    public void run() {
        if (!sendMessage(LOGINANDPASSWORD_REQUEST)) {
            return;
        }
        String message;
        while (true) {
            message = readMessage();
            if ((message = checkMessage(message)) != null) {
                broadCast(message);
                System.out.println(userName + ": " + message);
            } else {
                return;
            }
        }
    }
}

//    sendMessage("Hi, " + userName);
//        sendMessage("Input name:");
//        while (true) {
//            if ((message = readMessage()) != null) {
//                if (setUserName(message)) {
//                    System.out.println(userName + " connected to server");
//                    break;
//                } else {
//                    sendMessage("This name " + message + " is busy. Choose another nickname");
//                }
//            } else {
//                break;
//            }
//        }
