package common.decoder;

public class Decoder {

    public static byte[] intAsByteArray(int integer) {
        byte[] bytes = new byte[4];
        bytes[3] = (byte) ((integer << 24) >>> 24);
        bytes[2] = (byte) ((integer << 16) >>> 24);
        bytes[1] = (byte) ((integer << 8) >>> 24);
        bytes[0] = (byte) (integer >>> 24);
        return bytes;
    }

    public static int byteArrayAsInt(byte[] bytes) {

        return ((bytes[0] & 0xFF) << 24) + ((bytes[1] & 0xFF) << 16) + ((bytes[2] & 0xFF) << 8) + (bytes[3] & 0xFF);
    }

    public static byte[] initArray(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = 0;
        }
        return bytes;
    }
}
