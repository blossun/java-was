package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static constants.RequestHeaderConstants.CONTENT_TYPE;

public class HttpResponse {
    private static final Logger log = LoggerFactory.getLogger(HttpResponse.class);

    private String httpVersion = "HTTP/1.1";
    private String statusCode = "400";
    private String statusMessage = "Bad Request";

    private Map<String, String> headers = new HashMap<>();

    private String body;

    public HttpResponse() {
        headers.put(CONTENT_TYPE, "text/html;charset=utf-8\r\n");
    }

    public String getHttpResponseString() {
        StringBuilder sb = new StringBuilder(String.format("%s %s %s\r\n", httpVersion, statusCode, statusMessage));
        for (String header : headers.keySet()) {
            sb.append(header).append(": ").append(headers.get(header)).append("\r\n");
        }
        sb.append("\r\n");
        sb.append(body);
        return sb.toString();
    }

    public void set200ResponseStatusLine() {
        this.statusCode = "200";
        this.statusMessage = "OK";
    }

    public void set302ResponseStatusLine() {
        this.statusCode = "302";
        this.statusMessage = "Found";
    }

    public void set404ResponseStatusLine() {
        this.statusCode = "404";
        this.statusMessage = "Not Found";
    }

    public void set500ResponseStatusLine() {
        this.statusCode = "500";
        this.statusMessage = "Internal Server Error";
    }

    public void putResponseHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

}
