package common;

import static common.SystemMessages.SEND_FILE;
import static common.SystemMessages.SYSTEM;

public class Message {

    private String type;
    private String message;
    private String option;
    private String login;
    private String passwordHash;

    public Message() {

    }

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

    public static Message getMessageForFile(String fileName, String userName) {
        Message message = new Message();
        message.type = SYSTEM;
        message.message = SEND_FILE;
        message.option = fileName;
        message.login = userName;
        return message;
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
