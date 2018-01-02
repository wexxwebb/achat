package common.wrap;

public class Result<T> {

    private T result;
    private boolean success;
    private String message;

    public Result(T result, boolean success, String message) {
        this.result = result;
        this.success = success;
        this.message = message;
    }

    public T get() {
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
