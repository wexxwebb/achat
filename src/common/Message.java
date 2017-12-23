package common;

public class Message {

    private String type;
    private String message;
    private String option;
    private String login;
    private String passwordHash;

    public Message(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public Message(String type, String message, String option) {
        this.type = type;
        this.message = message;
        this.option = option;
    }

    public Message(String type, String message, String login, String passwordHash) {
        this.type = type;
        this.message = message;
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public String getOption() {
        return option;
    }

    public String getType() {
        return type;
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

    public String getPasswordHash() {
        return passwordHash;
    }

}
