package fu.de190530.user.common;

public class ApiException extends RuntimeException {
    private final int http;
    private final int status;
    public ApiException(int http, int status, String message) {
        super(message);
        this.http = http;
        this.status = status;
    }
    public int getHttp() { return http; }
    public int getStatus() { return status; }
}
