package server;

public interface Server extends Runnable {

    boolean sendString(String string);
    boolean isAuth();

}
