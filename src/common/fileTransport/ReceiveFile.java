package common.fileTransport;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ReceiveFile implements Receiver {

    private BufferedInputStream input;
    private String destination;

    public ReceiveFile(BufferedInputStream input, String destination) {
        this.input = input;
        this.destination = destination;
    }

    private boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e1) {
            return false;
        }
    }

    @Override
    public boolean receive(String fileName) {
        FileOutputStream fileOutputStream;
        int retry = 0;
        while (true) {
            try {
                int length;
                fileOutputStream = new FileOutputStream(destination  + fileName);
                while (true) {
                    length = input.read();
                    if (length == 0) break;
                    byte[] buffer = new byte[length];
                    input.read(buffer);
                    fileOutputStream.write(buffer, 0, length);
                }
                fileOutputStream.flush();
                fileOutputStream.close();
                return true;
            } catch (IOException e) {
                retry++;
                if (retry > 5) {
                    return false;
                } else {
                    if (!sleep(250)) return false;
                }
            }
        }
    }
}
