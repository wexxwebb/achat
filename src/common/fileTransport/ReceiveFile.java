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
                int length;
                fileOutputStream = new FileOutputStream(destination  + fileName);
                byte[] lengthAsByteArray = new byte[4];
                while (true) {
                    lengthAsByteArray = Decoder.initArray(lengthAsByteArray);
                    input.read(lengthAsByteArray);
                    length = Decoder.byteArrayAsInt(lengthAsByteArray);
                    if (length == -1) break;
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
                    Sleep.millis(250);
                }
            }
        }
    }
}
