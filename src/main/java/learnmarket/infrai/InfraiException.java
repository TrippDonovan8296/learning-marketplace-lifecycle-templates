package learnmarket.infrai;

import java.util.Map;

public final class InfraiException extends RuntimeException {
    private final int status;
    private final Map<String, Object> error;

    public InfraiException(int status, Map<String, Object> error) {
        super(String.valueOf(error.getOrDefault("code", "request rejected")) + ": "
                + error.getOrDefault("message", error.getOrDefault("hint", "request rejected")));
        this.status = status;
        this.error = Map.copyOf(error);
    }

    public int status() { return status; }
    public Map<String, Object> error() { return error; }
}
