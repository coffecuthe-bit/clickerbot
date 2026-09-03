package t.bot.worker.service.clicker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope("prototype")
public class HttpRequestBuilder {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    public final List<String> fullSetCookieHeaders = new CopyOnWriteArrayList<>();

    public HttpRequestBuilder() {
        this.httpClient = HttpClient.newBuilder()
                .executor(Executors.newCachedThreadPool())
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private String buildCookieHeader() {
        if (fullSetCookieHeaders.isEmpty()) {
            return null;
        }

        return fullSetCookieHeaders.stream()
                .map(cookie -> cookie.split(";")[0])
                .collect(Collectors.joining("; "));
    }

    public HttpRequest buildPostRequest(String url, Object body, boolean needJSESSIONID) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            if (needJSESSIONID) {
                String cookieHeader = buildCookieHeader();
                if (cookieHeader != null) {
                    request.header("Cookie", cookieHeader);
                }
            }

            return request.build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpRequest buildGetRequest(String url, boolean needJSESSIONID) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();

        if (needJSESSIONID) {
            String cookieHeader = buildCookieHeader();
            if (cookieHeader != null) {
                builder.header("Cookie", cookieHeader);
            }
        }
        return builder.build();
    }

    public <T> T sendRequest(HttpRequest request, Class<T> clazz, boolean needSearchJSESSIONID) {
        try {
            log.debug("Sending {} request to: {}", request.method(), request.uri());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 200) {
                log.info("Response status: {} code {}", response.statusCode(), request.uri());
            }
            if (needSearchJSESSIONID) {
                List<String> setCookieHeaders = response.headers().allValues("Set-Cookie");
                if (!setCookieHeaders.isEmpty()) {
                    fullSetCookieHeaders.clear();
                    fullSetCookieHeaders.addAll(setCookieHeaders);
                    log.info("Saved {} cookies", fullSetCookieHeaders.size());
                }
            }

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                if (response.body() == null || response.body().isEmpty()) {
                    return null;
                }
                if (clazz == String.class) {
                    return (T) response.body();
                }
                return objectMapper.readValue(response.body(), clazz);
            }

            log.error("Error response code: {} body: {}", response.statusCode(), response.body());
            throw new RuntimeException("Error response code: " + response.statusCode());

        } catch (IOException | InterruptedException e) {
            log.error("Request failed", e);
            throw new RuntimeException(e);
        }
    }

    public void clearCookies() {
        fullSetCookieHeaders.clear();
        log.info("All cookies cleared");
    }
}