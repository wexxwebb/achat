package server;

import java.io.*;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class SimpleServer implements Server {

    private Socket socket;
    private BlockingQueue<Server> serverPool;
    private BufferedReader bReader;
    private BufferedWriter bWriter;
    private String userName;
    private Set<String> userNames;

    public SimpleServer(Socket socket, BlockingQueue<Server> serverPool, Set<String> userNames) {
        this.socket = socket;
        this.serverPool = serverPool;
        boolean success = false;
        this.userNames = userNames;
        int tryCount = 0;
        while (!success) {
            try {
                bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                bWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                success = true;
            } catch (IOException e) {
                tryCount++;
                if (tryCount < 6) {
                    System.out.println("Can't create. Retry " + tryCount);
                } else {
                    return;
                }
            }
        }
    }

    @Override
    public boolean sendMessage(String message) {
        int tryCount = 0;
        boolean success = false;
        while (!success) {
            try {
                bWriter.write(message);
                bWriter.newLine();
                bWriter.flush();
                success = true;
            } catch (IOException e) {
                tryCount++;
                System.out.println("Can't send message. Try " + tryCount);
                if (tryCount > 6) {
                    return false;
                }
            }
        }
        return true;
    }

    private String readMessage() {
        int tryCount = 0;
        boolean success = false;
        String message = new String();
        while (!success) {
            try {
                while ((message = bReader.readLine()) != null) {
                    success = true;
                    break;
                }
            } catch (IOException e) {
                tryCount++;
                System.out.println("Error reading message. Try " + tryCount);
                if (tryCount < 10) {
                    return null;
                }
            }
        }
        return message;
    }

    private void broadCast(String message) {
        synchronized (serverPool) {
            for (Server server : serverPool) {
                if (!server.equals(this)) {
                    server.sendMessage(userName + ": " + message);
                }
            }
        }
    }

    public boolean setUserName(String userName) {
        if (!userNames.contains(userName)) {
            this.userName = userName;
            synchronized (userNames) {
                userNames.add(userName);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void run() {
        String message;
        sendMessage("Type your NickName:");
        while (true) {
            message = readMessage();
            if (setUserName(message)) {
                System.out.println(userName + " connected to server");
                break;
            } else {
                sendMessage("This name " + message + "is busy. Choose another nickname");
            }
        }
        sendMessage("Hi, " + userName);
        while (true) {
            message = readMessage();
            broadCast(message);
            System.out.println(userName + ": " + message);
        }
    }

}
