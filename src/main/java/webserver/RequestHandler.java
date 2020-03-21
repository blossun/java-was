package webserver;

import db.DataBase;
import model.HttpRequest;
import model.HttpResponse;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;

import static constants.CommonConstants.*;
import static constants.ContentTypeConstants.TEXT_CSS;
import static constants.ErrorConstants.METHOD_NOT_ALLOWED;
import static constants.RequestHeaderConstants.CONTENT_TYPE;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            HttpRequest httpRequest = new HttpRequest(br);
            HttpResponse httpResponse = new HttpResponse();
            if (httpRequest.isUrlNull()) {
                log.error("400 Bad Request");
                return;
            }
            switch (httpRequest.getHttpMethod()) {
                case GET:
                    httpGetRequestHandler(out, httpRequest, httpResponse);
                    break;
                case POST:
                    httpPostRequestHandler(out, httpRequest, httpResponse);
                    break;
                case PUT:
                    break;
                case DELETE:
                    break;
                default:
                    log.error(METHOD_NOT_ALLOWED);
            }
            DataOutputStream dos = new DataOutputStream(out);
            dos.writeBytes(httpResponse.getHttpResponseString());
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void httpPostRequestHandler(OutputStream out, HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        String url = httpRequest.getUrl();
        if (url.contains("create")) {
            userCreateRequestHandler(out, httpRequest, httpResponse);
        } else if (url.contains("login")) {
            loginRequestHandler(out, httpRequest, httpResponse);
        }
    }

    private void loginRequestHandler(OutputStream out, HttpRequest httpRequest, HttpResponse httpResponse) {
        User loginUser = DataBase.findUserById(httpRequest.getParams().get(USER_ID)).orElse(null);
        String inputPassword = httpRequest.getParams().get(PASSWORD);
        boolean setCookie = false;
        if (loginUser != null && loginUser.getPassword().equals(inputPassword)) {
            setCookie = true;
        }
        String redirectUrl = "/index.html";
        httpResponse.set302ResponseStatusLine();
        httpResponse.putResponseHeader("Location", redirectUrl);
        httpResponse.putResponseHeader("Set-Cookie", LOGIN_COOKIE_ID + "=" + setCookie + "; Path=/\r\n");
    }

    private void userCreateRequestHandler(OutputStream out, HttpRequest httpRequest, HttpResponse httpResponse) throws UnsupportedEncodingException {
        User newUser = new User(httpRequest.getParams());
        DataBase.addUser(newUser);
        log.debug("user : {}", newUser);
        String redirectUrl = "/index.html";
        httpResponse.set302ResponseStatusLine();
        httpResponse.putResponseHeader("Location", redirectUrl);
    }

    private void httpGetRequestHandler(OutputStream out, HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        String url = httpRequest.getUrl();
        if (url.contains("/user/list")) {
            showUserList(out, httpRequest, httpResponse);
            return;
        }
        byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
        log.trace("body : {}", new String(body, UTF_8));
        DataOutputStream dos = new DataOutputStream(out);
        httpResponse.set200ResponseStatusLine();
        if (url.endsWith(".css")) {
            httpResponse.putResponseHeader(CONTENT_TYPE, TEXT_CSS);
        }
    }

    private void showUserList(OutputStream out, HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        boolean isLogined;
        String url = httpRequest.getUrl();
        Map<String, String> cookies = httpRequest.getCookies();
        isLogined = Boolean.parseBoolean(cookies.get(LOGIN_COOKIE_ID));
        DataOutputStream dos = new DataOutputStream(out);
        if (!isLogined) {
            httpResponse.set302ResponseStatusLine();
            httpResponse.putResponseHeader("Location", "/index.html");
            httpResponse.putResponseHeader("Set-Cookie", LOGIN_COOKIE_ID + "=" + isLogined + "; Path=/\r\n");
            return;
        }
        byte[] body = hardBar(url);
        httpResponse.set200ResponseStatusLine();
    }

    private byte[] hardBar(String url) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<tbody>");
        Collection<User> users = DataBase.findAll();
        int i = 1;
        for (User user : users) {
            sb.append("<tr>").append("<th scope=\"row\">")
                    .append(i).append("</th> <td>").append(user.getUserId())
                    .append("</td> <td>").append(user.getName())
                    .append("</td> <td>").append(user.getEmail())
                    .append("</td><td><a href=\"#\" class=\"btn btn-success\" role=\"button\">수정</a></td>").append("</tr>");
            i++;
        }
        sb.append("</tbody>");
        byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
        body = new String(body, StandardCharsets.UTF_8).replaceAll("\\{\\{users}}", sb.toString()).getBytes();
        return body;
    }
}
