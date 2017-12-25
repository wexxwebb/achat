package server;

public interface Server extends Runnable {

    boolean sendString(String string);
    public boolean isAuth();
    public String getRoom();
    public void print(String string);
    public void setLoggedServer(Server loggedServer);

}
