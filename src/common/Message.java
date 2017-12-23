package common;

public class Message {

    private String type;
    private String message;
    private String login;
    private String passwordHash;

    public Message(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public Message(String type, String message, String login) {
        this.type = type;
        this.message = message;
        this.login = login;
    }

    public Message(String type, String message, String login, String passwordHash) {
        this.type = type;
        this.message = message;
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
