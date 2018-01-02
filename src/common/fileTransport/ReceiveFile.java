package common.fileTransport;

import common.decoder.Decoder;
import common.sleep.Sleep;

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

    @Override
    public boolean receive(String fileName) {
        FileOutputStream fileOutputStream;
        int retry = 0;
        while (true) {
            try {
                fileOutputStream = new FileOutputStream(destination  + fileName);
                byte[] buffer = new byte[8192];
                int length;
                while (true) {
                    length = input.read(buffer);
                    if (length == -1) break;
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
                    Sleep.millis(250);
                }
            }
        }
    }
}
