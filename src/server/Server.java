package server;

public interface Server extends Runnable {

    boolean sendMessage(String message);

}
