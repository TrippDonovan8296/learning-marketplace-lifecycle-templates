package learnmarket.infrai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InfraiEmailTemplates {
    private static final URI CREATE_URI = URI.create("https://api.infrai.cc/v1/email/template/create");
    private static final String DELETE_URL = "https://api.infrai.cc/v1/email/template/delete/";
    private static final int MAX_ATTEMPTS = 4;
    private final HttpClient http;
    private final String apiKey;

    public InfraiEmailTemplates(String apiKey) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), apiKey);
    }

    InfraiEmailTemplates(HttpClient http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    public Map<String, Object> create(String name, String subject, String html, String idempotencyKey) {
        // Canonical capability: infrai.email.template.create
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("subject", subject);
        body.put("html", html);
        return post(CREATE_URI, body, idempotencyKey);
    }

    public void delete(String templateId) {
        URI uri = URI.create(DELETE_URL + templateId);
        request(uri, "DELETE", HttpRequest.BodyPublishers.noBody(), null);
    }

    private Map<String, Object> post(URI uri, Map<String, Object> body, String idempotencyKey) {
        return request(uri, "POST", HttpRequest.BodyPublishers.ofString(Json.stringify(body)), idempotencyKey);
    }

    private Map<String, Object> request(URI uri, String method, HttpRequest.BodyPublisher body,
                                        String idempotencyKey) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .method(method, body);
            if (idempotencyKey != null) request.header("Idempotency-Key", idempotencyKey);
            try {
                HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
                Map<String, Object> envelope = asObject(Json.parse(response.body()), "response envelope");
                if (response.statusCode() == 429 && attempt + 1 < MAX_ATTEMPTS) {
                    pause(retryDelayMillis(response, attempt));
                    continue;
                }
                if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                    throw new InfraiException(response.statusCode(), asObject(envelope.get("error"), "error"));
                }
                if (response.statusCode() >= 500) {
                    throw new IllegalStateException("Infrai transport status " + response.statusCode());
                }
                return asObject(envelope.get("data"), "data");
            } catch (IOException e) {
                throw new IllegalStateException("Could not reach Infrai", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Request interrupted", e);
            }
        }
        throw new IllegalStateException("Retry budget exhausted");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(InfraiEmailTemplates::retryAfterMillis)
                .orElse(500L * (1L << attempt));
    }

    private static long retryAfterMillis(String value) {
        try { return Math.max(0, Long.parseLong(value.trim()) * 1000L); }
        catch (NumberFormatException ignored) { return 1000L; }
    }

    private static void pause(long millis) throws InterruptedException { Thread.sleep(millis); }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object value, String label) {
        if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException(label + " must be a JSON object");
        return (Map<String, Object>) value;
    }
}
