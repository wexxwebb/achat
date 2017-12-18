package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ReaderClient implements Client {

    private Socket socket;
    private BufferedReader bReader;

    public ReaderClient(Socket socket) throws IOException {
        this.socket = socket;
        bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
        String message;
        int tryCount = 0;
        while (true) {
            try {
                while ((message = bReader.readLine()) != null) {
                    System.out.println(message);
                    break;
                }
            } catch (IOException e) {
                tryCount++;
                System.out.println("Message reading error. Retry " + tryCount);
                if (tryCount < 6) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    return;
                }
            }
        }
    }
}
