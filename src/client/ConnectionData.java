package client;

import java.io.*;
import java.net.Socket;

import static common.SystemMessages.*;

public class ConnectionData {

    private String address;
    private int port;
    private String name;
    private String passwordHash;
    private Client reader;
    private Client writer;
    private Socket socket;
    private BufferedReader bReader;
    private BufferedWriter bWriter;
    private volatile boolean play = true;
    private String state;

    public ConnectionData(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void connect() throws IOException {
        this.socket = new Socket(address, port);
        this.bReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        this.bWriter = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
        this.state = CONNECTED;
    }

    public void exit() {
        int retry = 0;
        while (true) {
            try {
                bReader.close();
                bWriter.close();
                socket.close();
                setPlay(false);
                synchronized (this) {
                    this.notify();
                }
                return;
            } catch (IOException e) {
                if (retry > 3) {
                    System.out.println("Can't close socket. Exit with error...");
                    return;
                } else {
                    System.out.printf("Can't close socket. Retry %d\n", retry);
                    sleep(200);
                }
            }
        }
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean getPlay() {
        return play;
    }

    public void setPlay(boolean play) {
        this.play = play;
    }

    public BufferedReader getbReader() {
        return bReader;
    }

    public BufferedWriter getbWriter() {
        return bWriter;
    }

    public void setbWriter(BufferedWriter bWriter) {
        this.bWriter = bWriter;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Client getReader() {
        return reader;
    }

    public void setReader(Client reader) {
        this.reader = reader;
    }

    public Client getWriter() {
        return writer;
    }

    public void setWriter(Client writer) {
        this.writer = writer;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
