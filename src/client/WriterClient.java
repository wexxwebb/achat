package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class WriterClient implements Client {

    private Socket socket;
    private String nickName;
    private BufferedWriter bWriter;
    private Scanner console;

    public WriterClient(Socket socket) throws IOException {
        this.socket = socket;
        bWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        console = new Scanner(System.in);
    }

    @Override
    public void run() {
        int tryCount = 0;
        while (true) {
            String message = console.nextLine();
            boolean success = false;
            while (!success) {
                try {
                    bWriter.write(message);
                    bWriter.newLine();
                    bWriter.flush();
                    success = true;
                } catch (IOException e) {
                    tryCount++;
                    if (tryCount < 6) {
                        System.out.println("Problem with send message. Retry " + tryCount);
                    } else {
                        System.out.println("Send maessage error");
                        return;
                    }
                }
            }
        }
    }
}
