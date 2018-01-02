package common.fileTransport;

import common.decoder.Decoder;

import java.io.*;

public class TransferFile implements Transmitter {

    BufferedOutputStream output;
    String sourceFolder;

    public TransferFile(BufferedOutputStream output, String sourceFolder) {
        this.output = output;
        this.sourceFolder = sourceFolder;
    }

    public TransferFile(BufferedOutputStream output) {
        this.output = output;
        this.sourceFolder = "";
    }

    @Override
    public boolean transfer(String fileName) {
        int retry = 0;
        while (true) {
            try {
                FileInputStream fileInputStream = new FileInputStream(sourceFolder + fileName);
                int length;
                byte[] buffer = new byte[8192];
                while (true) {
                    length = fileInputStream.read(buffer);
                    if (length == -1) {
                        output.write(Decoder.intAsByteArray(-1));
                        output.flush();
                        break;
                    }
                    output.write(Decoder.intAsByteArray(length));
                    output.write(buffer, 0, length);
                    output.flush();
                }
                fileInputStream.close();
                return true;
            } catch (IOException e) {
                retry++;
                if (retry > 5) {
                    return false;
                }
            }
        }
    }
}
