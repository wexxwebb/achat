package client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.Socket;

import static common.SystemMessages.*;

public class ClientData {

    private String address;
    private int port;
    private String name;
    private String passwordHash;
    private Socket socket;
    private BufferedInputStream inStream;
    private BufferedOutputStream outStream;
    private volatile boolean play = true;
    private volatile int state;
    private GsonBuilder gsonBuilder = new GsonBuilder();
    private Gson gson = gsonBuilder.create();


    public ClientData(String address, int port) {
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
        this.inStream = new BufferedInputStream(socket.getInputStream());
        this.outStream = new BufferedOutputStream(socket.getOutputStream());
        if (state == STATE_AUTH_OK || state == STATE_REGISTER_OK) {
            this.state = STATE_CONNECTED_AGAIN;
        } else {
            this.state = STATE_CONNECTED;
        }
    }

    public void exit() {
        int retry = 0;
        while (true) {
            try {
                inStream.close();
                outStream.close();
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

    public Gson getGson() {
        return gson;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public boolean getPlay() {
        return play;
    }

    public void setPlay(boolean play) {
        this.play = play;
    }

    public BufferedInputStream getInStream() {
        return inStream;
    }

    public BufferedOutputStream getOutStream() {
        return outStream;
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

}
