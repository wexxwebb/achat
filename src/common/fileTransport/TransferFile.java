package common.fileTransport;

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
                byte[] buffer = new byte[127];
                while (true) {
                   length = fileInputStream.read(buffer);
                   if (length == -1) {
                       output.write(0);
                       break;
                   }
                   output.write(length);
                   output.write(buffer, 0, length);
                }
                fileInputStream.close();
                output.flush();
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
